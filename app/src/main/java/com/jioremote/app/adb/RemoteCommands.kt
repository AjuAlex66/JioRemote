package com.jioremote.app.adb

/** Fixed commands only: never interpolate arbitrary user-supplied shell input. */
object RemoteCommands {
    const val packageName = "com.jio.media.jiotvplus"
    const val launchApp = "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
    const val probe = "echo jioremote-ready"

    fun channel(id: String): String {
        require(id.matches(Regex("[0-9]{1,12}"))) { "Invalid channel ID" }
        return "am start -W -a android.intent.action.VIEW -d " +
            "'jiotvplus://$id?resume=true&source=watchnext' -p $packageName"
    }

    fun key(code: Int): String {
        require(code in setOf(3, 4, 19, 20, 21, 22, 23, 24, 25, 66, 67))
        return "input keyevent $code"
    }
}
