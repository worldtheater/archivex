package com.worldtheater.archive.platform.gateway

import com.worldtheater.archive.AppContextHolder
import com.worldtheater.archive.util.DeviceUtils

class AndroidHapticFeedbackGateway : com.worldtheater.archive.platform.gateway.HapticFeedbackGateway {
    override fun vibrate(milliseconds: Long, effect: Int?) {
        if (effect == null) {
            DeviceUtils.vibrate(context = AppContextHolder.appContext, milliseconds = milliseconds)
        } else {
            DeviceUtils.vibrate(
                context = AppContextHolder.appContext,
                milliseconds = milliseconds,
                effect = effect
            )
        }
    }
}
