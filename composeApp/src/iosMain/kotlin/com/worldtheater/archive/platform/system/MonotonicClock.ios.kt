package com.worldtheater.archive.platform.system

import kotlinx.cinterop.alloc
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec

@OptIn(ExperimentalForeignApi::class)
actual fun elapsedRealtimeMillis(): Long = memScoped {
    val ts = alloc<timespec>()
    if (clock_gettime(CLOCK_MONOTONIC.toUInt(), ts.ptr) == 0) {
        ts.tv_sec * 1_000L + ts.tv_nsec / 1_000_000L
    } else {
        currentTimeMillis()
    }
}
