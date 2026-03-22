package com.worldtheater.archive.platform.auth

class DefaultSensitiveAuthSessionStore : SensitiveAuthSessionStore {
    override var sensitiveNoteAuthPassedInSession: Boolean
        get() = passedInSession
        set(value) {
            passedInSession = value
        }

    companion object {
        private var passedInSession: Boolean = false

        fun reset() {
            passedInSession = false
        }
    }
}
