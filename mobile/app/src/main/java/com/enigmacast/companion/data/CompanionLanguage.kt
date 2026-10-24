package com.enigmacast.companion.data

/** The app languages offered in More › Display. The empty tag follows the device language. */
enum class CompanionLanguage(val tag: String, val label: String) {
    SYSTEM("", "System"),
    DUTCH("nl", "Nederlands"),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français"),
    ITALIAN("it", "Italiano"),
}
