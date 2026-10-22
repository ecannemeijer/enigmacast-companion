apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

dependencies {
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
