apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    // Alleen de entiteiten (geen DAO's): de telefoon bouwt zijn eigen Room-database.
    api(libs.room.common)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
