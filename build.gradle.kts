import org.gradle.api.tasks.compile.JavaCompile

// AGP en de Kotlin-plugins via de buildscript-classpath: de plugin-markers staan niet in de
// offline cache van deze machine (zie docs/adr/0003-offline-build.md).
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.4.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.4.20")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.20")
    }
    configurations.classpath {
        resolutionStrategy {
            // De offline cache bevat deze versies wel; KGP vraagt om een oudere.
            force("com.google.errorprone:error_prone_annotations:2.36.0")
        }
    }
}

// Alleen de JVM-modules krijgen de Kotlin/JVM-plugin; de Android-app zet zijn eigen plugins op.
subprojects {
    if (path.startsWith(":shared") || path.startsWith(":tools")) {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
            // De Android-app compileert op JVM 17; de gedeelde klassen moeten daarmee
            // overweg kunnen (bytecode 17, gebouwd met JDK 21).
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(17)
        }
        // De offline cache bevat geen kotlin-scripting-compiler-embeddable; die is alleen nodig
        // voor Kotlin-scripts, niet voor deze modules.
        configurations.configureEach {
            if (name.startsWith("kotlinCompilerPluginClasspath")) {
                exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-embeddable")
            }
        }
    }
}
