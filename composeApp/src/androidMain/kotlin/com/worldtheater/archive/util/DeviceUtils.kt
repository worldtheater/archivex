package com.worldtheater.archive.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.worldtheater.archive.AppContextHolder
import com.worldtheater.archive.util.KtExt.safeCastTo

object DeviceUtils {

    fun vibrate(
        context: Context = AppContextHolder.appContext,
        milliseconds: Long,
        effect: Int = VibrationEffect.DEFAULT_AMPLITUDE
    ) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                .safeCastTo<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(VIBRATOR_SERVICE).safeCastTo<Vibrator>()
        }
        vibrator?.vibrate(
            VibrationEffect.createOneShot(milliseconds, effect)
        )
    }

    fun clearClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard?.clearPrimaryClip()
        } else {
            val clip = ClipData.newPlainText("", "")
            clipboard?.setPrimaryClip(clip)
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }
}
