package com.enigmacast.companion.data.net

import kotlin.random.Random

/** Exponentiële backoff met jitter, zodat twee telefoons niet synchroon blijven hangen. */
object Backoff {
    fun delayMs(attempt: Int, random: Random = Random.Default): Long {
        val base = (250L shl attempt.coerceIn(0, 5)).coerceAtMost(8_000L)
        val jitter = 0.8 + random.nextDouble() * 0.4
        return (base * jitter).toLong().coerceAtLeast(100L)
    }
}
