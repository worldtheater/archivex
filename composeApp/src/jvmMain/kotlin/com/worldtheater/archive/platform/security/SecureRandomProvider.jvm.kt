package com.worldtheater.archive.platform.security

import java.security.SecureRandom

private val secureRandom by lazy { SecureRandom() }

internal actual fun secureRandomIntPlatform(bound: Int): Int {
    require(bound > 0) { "bound must be > 0" }
    return secureRandom.nextInt(bound)
}
