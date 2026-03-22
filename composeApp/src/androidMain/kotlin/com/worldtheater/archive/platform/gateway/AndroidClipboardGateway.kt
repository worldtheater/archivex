package com.worldtheater.archive.platform.gateway

import com.worldtheater.archive.AppContextHolder
import com.worldtheater.archive.util.DeviceUtils

class AndroidClipboardGateway : com.worldtheater.archive.platform.gateway.ClipboardGateway {
    override fun clear() {
        DeviceUtils.clearClipboard(AppContextHolder.appContext)
    }

    override fun copy(label: String, text: String) {
        DeviceUtils.copyToClipboard(
            context = AppContextHolder.appContext,
            label = label,
            text = text
        )
    }
}
