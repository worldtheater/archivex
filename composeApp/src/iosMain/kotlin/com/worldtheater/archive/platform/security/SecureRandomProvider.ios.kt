package com.worldtheater.archive.platform.security

import platform.posix.arc4random_uniform

internal actual fun secureRandomIntPlatform(bound: Int): Int {
    require(bound > 0) { "bound must be > 0" }
    return arc4random_uniform(bound.toUInt()).toInt()
}
