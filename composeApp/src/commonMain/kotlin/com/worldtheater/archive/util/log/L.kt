package com.worldtheater.archive.util.log

object L {

    const val LEVEL_VERBOSE = 0b1 shl 0
    const val LEVEL_DEBUG = 0b1 shl 1
    const val LEVEL_INFO = 0b1 shl 2
    const val LEVEL_WARN = 0b1 shl 3
    const val LEVEL_ERROR = 0b1 shl 4

    const val LEVEL_MASK_NONE = 0
    const val LEVEL_MASK_ALL = 0b11111

    private var logger: Logger? = null
    private var logLevelMask: Int = LEVEL_MASK_ALL

    fun init(levelMask: Int, impl: Logger) {
        logLevelMask = levelMask
        logger = impl
    }

    fun v(tag: String, msg: String) = log(LEVEL_VERBOSE, tag, msg)
    fun d(tag: String, msg: String) = log(LEVEL_DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(LEVEL_INFO, tag, msg)
    fun w(tag: String, msg: String) = log(LEVEL_WARN, tag, msg)
    fun e(tag: String, msg: String) = log(LEVEL_ERROR, tag, msg)
    fun e(tag: String, msg: String, throwable: Throwable?) = log(LEVEL_ERROR, tag, msg, throwable)

    fun v(msg: String) = log(LEVEL_VERBOSE, null, msg)
    fun d(msg: String) = log(LEVEL_DEBUG, null, msg)
    fun i(msg: String) = log(LEVEL_INFO, null, msg)
    fun w(msg: String) = log(LEVEL_WARN, null, msg)
    fun e(msg: String) = log(LEVEL_ERROR, null, msg)
    fun e(msg: String, throwable: Throwable?) = log(LEVEL_ERROR, null, msg, throwable)

    private fun log(logLevel: Int, tag: String?, msg: String, throwable: Throwable? = null) {
        if (logLevelMask and logLevel != 0) {
            logger?.writeLog(logLevel, tag, msg, throwable)
        }
    }

    interface Logger {

        fun writeLog(logLevel: Int, tag: String?, msg: String, throwable: Throwable? = null)
    }

}
