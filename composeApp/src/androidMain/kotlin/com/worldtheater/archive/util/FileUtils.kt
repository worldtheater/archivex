package com.worldtheater.archive.util

import android.content.Context
import android.net.Uri
import java.net.URLDecoder

object FileUtils {

    private const val TAG = "FileUtils"

    fun getReadablePathFromUri(context: Context, uri: Uri): String {
        try {
            val path = uri.path ?: return uri.toString()
            // Standard DocumentProvider pattern
            // content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FBackup
            // path: /tree/primary:Documents/Backup
            if (path.contains(":")) {
                val parts = path.split(":")
                if (parts.size > 1) {
                    val rootType = parts[0].substringAfterLast("/") // "primary"
                    val relativePath = parts[1] // "Documents/Backup"

                    val rootName = if (rootType.equals("primary", ignoreCase = true)) {
                        "Internal Storage"
                    } else {
                        rootType.replaceFirstChar { it.uppercase() }
                    }
                    return "$rootName > $relativePath"
                }
            }
            // Fallback: try decoding
            return URLDecoder.decode(uri.toString(), "UTF-8")
        } catch (e: Exception) {
            return uri.toString()
        }
    }
}
