package com.jioremote.app.adb

import com.jioremote.app.data.requireChannelNumber

/** Fixed commands only: never interpolate arbitrary user-supplied shell input. */
object RemoteCommands {
    const val packageName = "com.jio.media.jiotvplus"
    const val launchApp = "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
    const val probe = "echo jioremote-ready"

    private const val digitPauseSeconds = "0.150"

    fun channel(number: String): String {
        requireChannelNumber(number)
        return number.map { "input keyevent KEYCODE_$it" }
            .joinToString(" && sleep $digitPauseSeconds && ")
    }

    fun key(code: Int): String {
        require(code in setOf(164, 25, 24))
        return "input keyevent $code"
    }
}
