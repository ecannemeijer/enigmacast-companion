package com.enigmacast.domain.tuner

import com.enigmacast.util.CoreKey
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnTunerKeysTest {
    @Test
    fun channelKeysZapTheLocalStream() {
        assertEquals(OwnKeyAction.NEXT, OwnTunerKeys.action(CoreKey.CHANNEL_UP))
        assertEquals(OwnKeyAction.NEXT, OwnTunerKeys.action(CoreKey.DPAD_UP))
        assertEquals(OwnKeyAction.PREVIOUS, OwnTunerKeys.action(CoreKey.CHANNEL_DOWN))
        assertEquals(OwnKeyAction.PREVIOUS, OwnTunerKeys.action(CoreKey.DPAD_DOWN))
    }

    @Test
    fun overlayKeys() {
        assertEquals(OwnKeyAction.OK, OwnTunerKeys.action(CoreKey.DPAD_CENTER))
        assertEquals(OwnKeyAction.BACK, OwnTunerKeys.action(CoreKey.BACK))
        assertEquals(OwnKeyAction.DETAILS, OwnTunerKeys.action(CoreKey.INFO))
        assertEquals(OwnKeyAction.DETAILS, OwnTunerKeys.action(CoreKey.GUIDE))
        assertEquals(OwnKeyAction.LIST, OwnTunerKeys.action(CoreKey.MENU))
        assertEquals(OwnKeyAction.LIST, OwnTunerKeys.action(CoreKey.DPAD_LEFT))
        assertEquals(OwnKeyAction.BANNER, OwnTunerKeys.action(CoreKey.DPAD_RIGHT))
        assertEquals(OwnKeyAction.CONSUME, OwnTunerKeys.action(CoreKey.PROG_RED))
        assertEquals(OwnKeyAction.PASS, OwnTunerKeys.action(CoreKey.VOLUME_UP))
    }

    @Test
    fun digitsFromBothRows() {
        assertEquals(OwnKeyAction.DIGIT, OwnTunerKeys.action(CoreKey.DIGIT_7))
        assertEquals(7, OwnTunerKeys.digit(CoreKey.DIGIT_7))
        assertEquals(OwnKeyAction.DIGIT, OwnTunerKeys.action(CoreKey.NUMPAD_3))
        assertEquals(3, OwnTunerKeys.digit(CoreKey.NUMPAD_3))
        assertEquals(-1, OwnTunerKeys.digit(CoreKey.DPAD_UP))
    }
}
