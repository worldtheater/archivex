package com.worldtheater.archive

import android.content.Context

object AppContextHolder {

    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

}
