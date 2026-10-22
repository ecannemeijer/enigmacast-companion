package com.enigmacast.companion.data.net

import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class BackoffTest {

    @Test
    fun growsWithAttemptsAndCaps() {
        val first = Backoff.delayMs(0, Random(1))
        val fourth = Backoff.delayMs(3, Random(1))
        val far = Backoff.delayMs(20, Random(1))
        assertTrue("eerste poging moet kort zijn: $first", first in 100..400)
        assertTrue("vierde poging moet langer zijn: $fourth", fourth in 1_400..2_600)
        assertTrue("verre poging moet aftoppen: $far", far in 5_000..10_000)
    }

    @Test
    fun jitterStaysInBand() {
        repeat(200) {
            val delay = Backoff.delayMs(2)
            assertTrue("jitter buiten band: $delay", delay in 700..1_300)
        }
    }
}
