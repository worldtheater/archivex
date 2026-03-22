package com.worldtheater.archive.domain

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

enum class BiometricReAuthInterval(val millis: Long) {
    THIRTY_SECONDS(30_000L),
    ONE_MINUTE(60_000L),
    FIVE_MINUTES(5 * 60_000L),
    FIFTEEN_MINUTES(15 * 60_000L)
}

enum class SensitiveNoteAuthFrequency {
    EVERY_TIME,
    ONCE_PER_APP_START,
    NEVER
}

enum class BackupSecurityMode {
    CROSS_DEVICE,
    DEVICE_BOUND
}
