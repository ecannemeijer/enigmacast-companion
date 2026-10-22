package com.enigmacast.domain.validation

import com.enigmacast.domain.model.ReceiverConfig
import com.enigmacast.domain.network.WireGuardText
import com.enigmacast.util.CoreStrings
import com.enigmacast.util.UiText

data class ValidationResult(
    val errors: List<UiText>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

object ReceiverConfigValidator {
    private val hostPattern = Regex("^[A-Za-z0-9._\\-]+$")

    fun validate(config: ReceiverConfig): ValidationResult {
        val errors = mutableListOf<UiText>()
        if (config.name.isBlank()) errors += UiText.res(CoreStrings.VALID_NAME_REQUIRED)
        if (config.host.isBlank() || !hostPattern.matches(config.host)) {
            errors += UiText.res(CoreStrings.VALID_HOST)
        }
        if (config.httpPort !in 1..65535) errors += UiText.res(CoreStrings.VALID_HTTP_PORT)
        if (config.streamingPort !in 1..65535) errors += UiText.res(CoreStrings.VALID_STREAM_PORT)
        if (config.timeoutSeconds !in 1..60) errors += UiText.res(CoreStrings.VALID_TIMEOUT)
        if (config.remoteHost.isNotBlank() && !hostPattern.matches(config.remoteHost.trim())) {
            errors += UiText.res(CoreStrings.VALID_REMOTE_HOST)
        }
        val remotePorts = listOf(config.remoteHttpPort, config.remoteStreamingPort, config.remoteTranscodingPort)
        if (remotePorts.any { it !in 0..65535 }) errors += UiText.res(CoreStrings.VALID_REMOTE_PORT)
        if (config.transcodingPort !in 1..65535) errors += UiText.res(CoreStrings.VALID_TRANSCODING_PORT)
        if (config.wireGuard && !WireGuardText.summarize(config.wireGuardConfig).valid) {
            errors += UiText.res(CoreStrings.VALID_WIREGUARD)
        }
        return ValidationResult(errors)
    }
}
