package com.enigmacast.data.openwebif

import com.enigmacast.util.HasUiText
import com.enigmacast.util.UiText
import java.io.IOException

/** [uiText] is what the screen shows; app-made failures are translated, others pass the raw message. */
class OpenWebifException(
    message: String,
    val statusCode: Int? = null,
    override val uiText: UiText = UiText.Raw(message),
) : IOException(message), HasUiText
