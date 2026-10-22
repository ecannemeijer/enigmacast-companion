apply(plugin = "com.android.application")
// AGP 9 heeft ingebouwde Kotlin-ondersteuning; de losse kotlin-android-plugin is niet meer nodig.
apply(plugin = "org.jetbrains.kotlin.plugin.compose")
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.enigmacast.companion"
    compileSdk = 37
    // De offline SDK heeft build-tools 36.1.0; expliciet zetten voorkomt een download.
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.enigmacast.companion"
        minSdk = 26
        targetSdk = 37
        versionCode = 113
        versionName = "1.0.0-beta.14"
        buildConfigField("String", "TMDB_API_KEY", "\"${localProperty("TMDB_API_KEY")}\"")
    }

    buildTypes {
        release {
            // Sideload: dezelfde debug-key als de TV-release, zodat updates elkaar kunnen vervangen.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    lint {
        // De offline cache heeft de lint-tool niet; release bouwen zonder lintVital.
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*",
        )
    }
}

configurations.configureEach {
    resolutionStrategy {
        // Offline-cache: alleen deze versies zijn lokaal beschikbaar.
        force("androidx.profileinstaller:profileinstaller:1.4.1")
        force("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
        // Media3 vraagt exifinterface 1.3.6; de offline cache heeft 1.4.2.
        force("androidx.exifinterface:exifinterface:1.4.2")
    }
    // Profileinstaller schrijft alleen baseline-profielen en trekt androidx.concurrent +
    // ListenableFuture mee; de offline cache heeft daar geen volledige set van.
    exclude(group = "androidx.profileinstaller", module = "profileinstaller")
    exclude(group = "androidx.concurrent", module = "concurrent-futures")
}

dependencies {
    // String-invoke omdat de plugins via apply() zijn toegepast; de Kotlin-DSL-accessors
    // worden alleen voor de plugins { }-DSL gegenereerd.
    "implementation"(project(":shared:core-model"))
    "implementation"(project(":shared:core-protocol"))
    "implementation"(project(":shared:core-openwebif"))


    "implementation"(libs.androidx.core.ktx)
    "implementation"(libs.androidx.activity.compose)
    "implementation"(libs.androidx.lifecycle.runtime.compose)
    "implementation"(libs.androidx.lifecycle.viewmodel.compose)
    "implementation"(libs.kotlinx.coroutines.android)
    "implementation"(libs.okhttp)
    "implementation"(libs.kotlinx.serialization.json)
    "implementation"(libs.media3.exoplayer)
    "implementation"(libs.media3.exoplayer.hls)
    "implementation"(libs.media3.ui.compose)
    "implementation"("io.coil-kt.coil3:coil-compose:3.6.0")
    "implementation"("io.coil-kt.coil3:coil-network-okhttp:3.6.0")

    "implementation"(platform(libs.androidx.compose.bom))
    "implementation"(libs.androidx.compose.ui)
    "implementation"(libs.androidx.compose.foundation)
    "implementation"(libs.androidx.compose.material3)
    "implementation"(libs.androidx.compose.material.icons)
    "implementation"(libs.androidx.compose.ui.tooling.preview)
    "debugImplementation"(libs.androidx.compose.ui.tooling)

    "testImplementation"(libs.junit)
    "testImplementation"(libs.kotlinx.coroutines.test)
}

/** Waarde uit de git-ignored local.properties (NAME=value), of "" als hij ontbreekt. */
fun localProperty(name: String): String {
    val file = rootProject.file("local.properties")
    if (!file.isFile) return ""
    return file.readLines().map { it.trim() }.firstOrNull { it.startsWith("$name=") }
        ?.substringAfter('=')?.trim().orEmpty()
}
