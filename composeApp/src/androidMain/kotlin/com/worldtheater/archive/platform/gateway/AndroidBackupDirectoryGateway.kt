package com.worldtheater.archive.platform.gateway

import android.content.Intent
import android.net.Uri
import com.worldtheater.archive.AppContextHolder
import com.worldtheater.archive.util.FileUtils

class AndroidBackupDirectoryGateway : BackupDirectoryGateway {
    override fun persistBackupDirectoryPermission(uriString: String) {
        val uri = Uri.parse(uriString)
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        AppContextHolder.appContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    override fun readableBackupDirectoryPath(uriString: String): String {
        return FileUtils.getReadablePathFromUri(AppContextHolder.appContext, Uri.parse(uriString))
    }
}
