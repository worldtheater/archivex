package com.worldtheater.archive.platform.gateway

interface BackupDirectoryGateway {
    fun persistBackupDirectoryPermission(uriString: String)
    fun readableBackupDirectoryPath(uriString: String): String
}

interface TransferFileGateway {
    fun writeBytes(uriString: String, bytes: ByteArray)
    fun readBytes(uriString: String): ByteArray
    fun createFile(treeUriString: String, mimeType: String, displayName: String): String
    fun deleteFile(uriString: String): Boolean
}

interface ClipboardGateway {
    fun clear()
    fun copy(label: String, text: String)
}

interface HapticFeedbackGateway {
    fun vibrate(milliseconds: Long, effect: Int? = null)
}

interface ExternalLinkOpener {
    fun open(url: String): Boolean
}

class DefaultExternalLinkOpener : ExternalLinkOpener {
    override fun open(url: String): Boolean = openExternalUrlByPlatform(url)
}

expect fun openExternalUrlByPlatform(url: String): Boolean
