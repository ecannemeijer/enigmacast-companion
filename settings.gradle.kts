pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "enigmacast-companion"

// Fase 0: alleen de gedeelde JVM-modules. De Android-app (`mobile:app`) en de
// TV-brug (`companion-bridge`, in de EnigmaCast-repo) volgen in fase 2.
include(":shared:core-model")
include(":shared:core-openwebif")
include(":shared:core-protocol")

// Fase 1: bewijst de TLS-route (certificaat, PKCS#12, SPKI-pinning) op de JVM.
include(":tools:tls-spike")

// Fase 2: de mobiele app, met een mock-brug die het echte protocol spreekt.
include(":mobile:app")
