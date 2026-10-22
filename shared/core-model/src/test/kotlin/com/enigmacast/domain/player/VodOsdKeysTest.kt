package com.enigmacast.domain.player

import com.enigmacast.util.CoreKey
import com.enigmacast.domain.player.VodOsdKeys.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class VodOsdKeysTest {
    private fun hidden(code: Int, repeat: Int = 0) = VodOsdKeys.action(code, repeat, osdShown = false)
    private fun shown(code: Int, repeat: Int = 0) = VodOsdKeys.action(code, repeat, osdShown = true)

    @Test fun okPausesAndShowsWhenHidden() {
        assertEquals(Action.TogglePause(show = true), hidden(CoreKey.DPAD_CENTER))
        assertEquals(Action.TogglePause(show = true), hidden(CoreKey.ENTER))
        // Holding OK does not toggle again and again.
        assertEquals(Action.Show, hidden(CoreKey.DPAD_CENTER, repeat = 3))
    }

    @Test fun leftRightSeekTenSecondsAtOnceWhenHidden() {
        assertEquals(Action.Scrub(-10_000L), hidden(CoreKey.DPAD_LEFT))
        assertEquals(Action.Scrub(10_000L), hidden(CoreKey.DPAD_RIGHT))
    }

    @Test fun upDownOnlyShow() {
        assertEquals(Action.Show, hidden(CoreKey.DPAD_UP))
        assertEquals(Action.Show, hidden(CoreKey.DPAD_DOWN))
    }

    @Test fun shownTheFocusedControlGetsTheDpad() {
        listOf(CoreKey.DPAD_CENTER, CoreKey.DPAD_LEFT, CoreKey.DPAD_RIGHT, CoreKey.DPAD_UP, CoreKey.DPAD_DOWN)
            .forEach { assertEquals(Action.Pass, shown(it)) }
    }

    @Test fun mediaKeysAlwaysWork() {
        assertEquals(Action.TogglePause(show = true), shown(CoreKey.MEDIA_PLAY_PAUSE))
        assertEquals(Action.Scrub(30_000L), shown(CoreKey.MEDIA_FAST_FORWARD))
        assertEquals(Action.Scrub(-30_000L), hidden(CoreKey.MEDIA_REWIND))
    }

    @Test fun otherKeysAreNotTheOsds() {
        assertEquals(Action.Pass, hidden(CoreKey.BACK))
        assertEquals(Action.Pass, hidden(CoreKey.INFO))
    }

    @Test fun holdingSeeksFaster() {
        assertEquals(10_000L, VodOsdKeys.scrubStep(0))
        assertEquals(30_000L, VodOsdKeys.scrubStep(8))
        assertEquals(60_000L, VodOsdKeys.scrubStep(20))
    }

    @Test fun timesAsInStremio() {
        assertEquals("0:04 / 1:45:45", VodOsdKeys.times(4_000, 6_345_000))
        assertEquals("12:03 / 42:00", VodOsdKeys.times(723_000, 2_520_000))
        assertEquals("0:00", VodOsdKeys.times(0, 0))
    }

    @Test fun titles() {
        assertEquals("Häxan (1922)", VodOsdKeys.title("Häxan", 1922))
        assertEquals("Häxan (1922)", VodOsdKeys.title("Häxan (1922)", 1922))
        assertEquals("Dark · S1E2 · Lies", VodOsdKeys.title("Dark", 2017, 1, 2, "Lies"))
        assertEquals("Dark · S1E2", VodOsdKeys.title("Dark", null, 1, 2, null))
    }

    @Test fun speedLabels() {
        assertEquals("1×", VodOsdKeys.speedLabel(1f))
        assertEquals("1.25×", VodOsdKeys.speedLabel(1.25f))
        assertEquals("0.5×", VodOsdKeys.speedLabel(0.5f))
    }

    @Test fun bufferedBarIsAheadOfThePlayedPartOnly() {
        org.junit.Assert.assertEquals(0.5f, VodOsdKeys.bufferedFraction(10_000, 50_000, 100_000), 0.0001f)
        org.junit.Assert.assertEquals(0f, VodOsdKeys.bufferedFraction(60_000, 50_000, 100_000), 0.0001f)
        org.junit.Assert.assertEquals(0f, VodOsdKeys.bufferedFraction(0, 50_000, 0), 0.0001f)
        org.junit.Assert.assertEquals(1f, VodOsdKeys.bufferedFraction(0, 150_000, 100_000), 0.0001f)
    }
}
