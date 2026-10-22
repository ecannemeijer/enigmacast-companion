package com.enigmacast.util

/**
 * User-visible text built outside the UI (view models, sessions, domain helpers).
 *
 * JVM-versie van `com.enigmacast.util.UiText` uit EnigmaCast. Het enige verschil is dat
 * [resolve] hier niet bestaat: een JVM-module heeft geen `Context`. De Android-app levert
 * een `fun UiText.resolve(context: Context): String`-extensie en een tabel die
 * [CoreStrings]-ids op `R.string`-ids afbeeldt.
 */
sealed interface UiText {
    /** Text that is not translated: receiver data, names, raw system error messages. */
    data class Raw(val value: String) : UiText

    /** A message id from [CoreStrings]; the app maps it to its own string resource. */
    data class Res(val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Plural(val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    /** Several texts shown together, e.g. a message followed by a list of tuner users. */
    data class Joined(val parts: List<UiText>, val separator: String = "\n") : UiText

    companion object {
        val Empty: UiText = Raw("")

        fun res(id: Int, vararg args: Any): UiText = Res(id, args.toList())

        fun plural(id: Int, count: Int, vararg args: Any): UiText = Plural(id, count, args.toList())
    }
}

fun UiText.isEmpty(): Boolean = when (this) {
    is UiText.Raw -> value.isEmpty()
    is UiText.Joined -> parts.all { it.isEmpty() }
    else -> false
}

fun UiText.isNotEmpty(): Boolean = !isEmpty()

/** An error that carries its own translated message (see [uiMessage]). */
interface HasUiText {
    val uiText: UiText
}

/** An I/O failure the app raises itself, with a translated message for the screen. */
class UiTextException(override val uiText: UiText, message: String) : java.io.IOException(message), HasUiText

/**
 * The message to show for a failure: the app's own translated text when it has one,
 * otherwise the system's message as-is (not translated), otherwise [fallback].
 */
fun Throwable.uiMessage(fallback: UiText): UiText {
    (this as? HasUiText)?.let { return it.uiText }
    val text = message?.takeIf { it.isNotBlank() } ?: return fallback
    return UiText.Raw(text)
}
