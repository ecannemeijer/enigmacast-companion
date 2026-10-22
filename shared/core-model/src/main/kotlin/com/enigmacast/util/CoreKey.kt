package com.enigmacast.util

/**
 * Android `KeyEvent`-codes, hier gedupliceerd zodat de gedeelde JVM-module geen
 * `android.view.KeyEvent` nodig heeft. De waarden zijn identiek aan
 * `android.view.KeyEvent.KEYCODE_*`; de app vertaalt ze één-op-één.
 */
object CoreKey {
    const val BACK = 4
    const val DIGIT_0 = 7
    const val DIGIT_1 = 8
    const val DIGIT_2 = 9
    const val DIGIT_3 = 10
    const val DIGIT_4 = 11
    const val DIGIT_5 = 12
    const val DIGIT_6 = 13
    const val DIGIT_7 = 14
    const val DIGIT_8 = 15
    const val DIGIT_9 = 16
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22
    const val DPAD_CENTER = 23
    const val VOLUME_UP = 24
    const val VOLUME_DOWN = 25
    const val ENTER = 66
    const val MENU = 82
    const val MEDIA_PLAY_PAUSE = 85
    const val MEDIA_STOP = 86
    const val MEDIA_NEXT = 87
    const val MEDIA_PREVIOUS = 88
    const val MEDIA_REWIND = 89
    const val MEDIA_FAST_FORWARD = 90
    const val MEDIA_PLAY = 126
    const val MEDIA_PAUSE = 127
    const val MEDIA_RECORD = 130
    const val NUMPAD_0 = 144
    const val NUMPAD_1 = 145
    const val NUMPAD_2 = 146
    const val NUMPAD_3 = 147
    const val NUMPAD_4 = 148
    const val NUMPAD_5 = 149
    const val NUMPAD_6 = 150
    const val NUMPAD_7 = 151
    const val NUMPAD_8 = 152
    const val NUMPAD_9 = 153
    const val NUMPAD_ENTER = 160
    const val INFO = 165
    const val CHANNEL_UP = 166
    const val CHANNEL_DOWN = 167
    const val TV = 170
    const val GUIDE = 172
    const val PROG_RED = 183
    const val PROG_GREEN = 184
    const val PROG_YELLOW = 185
    const val PROG_BLUE = 186
    const val MEDIA_AUDIO_TRACK = 222
    const val TV_RADIO = 232
    const val TV_TELETEXT = 233
    const val TV_CONTENTS_MENU = 256
    const val TV_MEDIA_CONTEXT_MENU = 257
    const val HELP = 259
}
