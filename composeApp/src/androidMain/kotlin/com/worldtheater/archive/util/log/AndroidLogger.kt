package com.worldtheater.archive.util.log

import android.util.Log

class AndroidLogger(private val baseTag: String) : L.Logger {

    override fun writeLog(logLevel: Int, tag: String?, msg: String, throwable: Throwable?) {
        val content = if (tag.isNullOrEmpty()) msg else "$tag-->$msg"
        when (logLevel) {
            L.LEVEL_VERBOSE -> Log.v(baseTag, content)
            L.LEVEL_DEBUG -> Log.d(baseTag, content)
            L.LEVEL_INFO -> Log.i(baseTag, content)
            L.LEVEL_WARN -> Log.w(baseTag, content)
            L.LEVEL_ERROR -> if (throwable != null) {
                Log.e(baseTag, content, throwable)
            } else {
                Log.e(baseTag, content)
            }

            else -> Log.e(baseTag, "invalid log level: $logLevel, $content")
        }
    }

}
