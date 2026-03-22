package com.worldtheater.archive.platform.system

import platform.Foundation.NSUUID

actual fun generateUuidString(): String = NSUUID().UUIDString()
