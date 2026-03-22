package com.worldtheater.archive.platform.gateway

object DesktopHapticFeedbackGateway : HapticFeedbackGateway {
    override fun vibrate(milliseconds: Long, effect: Int?) = Unit
}
