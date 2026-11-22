package com.jioremote.app.data

data class Channel(val id: String, val name: String) {
    init {
        require(id.matches(Regex("[0-9]{1,12}"))) { "Channel ID must contain only digits" }
        require(name.isNotBlank() && name.length <= 48) { "Channel name must be 1–48 characters" }
    }
}
