package com.worldtheater.archive.util

object KtExt {

    inline fun <reified T> Any?.safeCastTo(): T? {
        if (this == null || this !is T) {
            return null
        }
        return this
    }
}
