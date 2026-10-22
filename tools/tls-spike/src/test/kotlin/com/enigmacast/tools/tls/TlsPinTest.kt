package com.enigmacast.tools.tls

import java.io.File
import java.math.BigInteger
import java.net.InetAddress
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * JVM-bewijs voor de gekozen TLS-route (docs/tls-spike.md, optie B):
 *
 * 1. een EC P-256-sleutel en een zelfondertekend certificaat genereren;
 * 2. dat als PKCS#12 opslaan en opnieuw laden;
 * 3. een TLS-server starten met die sleutel;
 * 4. een client laten verbinden die **alleen de SPKI-pin** vertrouwt;
 * 5. bewijzen dat een verkeerde pin de handshake laat falen.
 *
 * Wat dit niet dekt (en op een echt toestel moet): Android Keystore, `NanoHTTPD.makeSecure`,
 * TLS op oude TV-SoC's en het gedrag bij app-herstart.
 */
class TlsPinTest {

    private val password: CharArray = ByteArray(24).also { SecureRandom().nextBytes(it) }
        .joinToString("") { "%02x".format(it) }
        .toCharArray()

    @Test
    fun selfSignedCertificateCanBePinnedAndServedOverTls() {
        val keyPair = generateKeyPair()
        val certificate = selfSignedCertificate(keyPair)
        val pin = pinOf(certificate)

        val p12 = writePkcs12(keyPair, certificate)
        val serverContext = serverContext(p12, password)
        val server = serverContext.serverSocketFactory.createServerSocket(0) as SSLServerSocket
        server.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
        val port = server.localPort

        val serverError = arrayOfNulls<Throwable>(1)
        val worker = thread(isDaemon = true, name = "tls-spike-server") {
            try {
                (server.accept() as SSLSocket).use { socket ->
                    socket.soTimeout = 5_000
                    socket.startHandshake()
                    socket.outputStream.write(PAYLOAD)
                    socket.outputStream.flush()
                }
            } catch (error: Throwable) {
                serverError[0] = error
            }
        }

        try {
            val received = connect(port, pin)
            assertEquals(String(PAYLOAD), String(received))
            worker.join(5_000)
            assertEquals("serverfout: ${serverError[0]}", null, serverError[0])
        } finally {
            runCatching { server.close() }
        }
    }

    @Test
    fun aWrongPinIsRejectedBeforeAnyPayloadIsRead() {
        val keyPair = generateKeyPair()
        val certificate = selfSignedCertificate(keyPair)
        val pin = pinOf(certificate)
        val otherPin = pinOf(selfSignedCertificate(generateKeyPair()))
        assertNotEquals(pin, otherPin)

        val server = serverContext(writePkcs12(keyPair, certificate), password)
            .serverSocketFactory.createServerSocket(0) as SSLServerSocket
        val worker = thread(isDaemon = true) {
            runCatching { (server.accept() as SSLSocket).use { it.startHandshake() } }
        }
        try {
            try {
                connect(server.localPort, otherPin)
                fail("een verkeerde pin mag niet worden geaccepteerd")
            } catch (expected: SSLHandshakeException) {
                assertTrue(expected.message.orEmpty().isNotEmpty())
            } catch (expected: CertificateException) {
                assertTrue(expected.message.orEmpty().isNotEmpty())
            }
        } finally {
            runCatching { server.close() }
            worker.join(5_000)
        }
    }

    @Test
    fun thePinIsStableAcrossReloads() {
        val keyPair = generateKeyPair()
        val certificate = selfSignedCertificate(keyPair)
        val p12 = writePkcs12(keyPair, certificate)
        val reloaded = loadPkcs12(p12, password)
        val reloadedCertificate = reloaded.getCertificate(ALIAS) as X509Certificate
        assertEquals(pinOf(certificate), pinOf(reloadedCertificate))
        assertTrue(pinOf(certificate).isNotBlank())
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }

    private fun selfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val name = X500Name("CN=enigmacast-companion")
        val builder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.valueOf(now),
            Date(now - 60_000),
            Date(now + TEN_YEARS_MS),
            name,
            keyPair.public,
        )
        builder.addExtension(
            Extension.subjectAlternativeName,
            false,
            GeneralNames(GeneralName(GeneralName.iPAddress, "127.0.0.1")),
        )
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }

    private fun writePkcs12(keyPair: KeyPair, certificate: X509Certificate): File {
        val store = KeyStore.getInstance("PKCS12")
        store.load(null, null)
        store.setKeyEntry(ALIAS, keyPair.private, password, arrayOf(certificate))
        val file = File.createTempFile("enigmacast-tls-", ".p12").apply { deleteOnExit() }
        file.outputStream().use { store.store(it, password) }
        return file
    }

    private fun loadPkcs12(file: File, password: CharArray): KeyStore =
        KeyStore.getInstance("PKCS12").apply { file.inputStream().use { load(it, password) } }

    private fun serverContext(file: File, password: CharArray): SSLContext {
        val store = loadPkcs12(file, password)
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagers.init(store, password)
        return SSLContext.getInstance("TLS").apply { init(keyManagers.keyManagers, null, SecureRandom()) }
    }

    private fun clientContext(expectedPin: String): SSLContext {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val certificate = chain?.firstOrNull()
                    ?: throw CertificateException("geen servercertificaat")
                if (pinOf(certificate) != expectedPin) {
                    throw CertificateException("certificaatpin komt niet overeen")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        return SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
    }

    private fun connect(port: Int, pin: String): ByteArray {
        val socket = clientContext(pin).socketFactory.createSocket(
            InetAddress.getByName("127.0.0.1"),
            port,
        ) as SSLSocket
        socket.use {
            it.soTimeout = 5_000
            it.startHandshake()
            return it.inputStream.readBytes()
        }
    }

    private fun pinOf(certificate: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private companion object {
        const val ALIAS = "enigmacast-tls"
        const val TEN_YEARS_MS = 10L * 365 * 24 * 60 * 60 * 1000
        val PAYLOAD = "enigmacast-tls-ok".toByteArray()

        init {
            if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
        }
    }
}
