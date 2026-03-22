package com.worldtheater.archive.platform.gateway

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

class AndroidTransferFileGateway(
    private val app: Application
) : com.worldtheater.archive.platform.gateway.TransferFileGateway {
    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        app.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: throw IOException("Failed to open output stream")
    }

    override fun writeBytes(uriString: String, bytes: ByteArray) {
        writeBytes(Uri.parse(uriString), bytes)
    }

    private fun readBytes(uri: Uri): ByteArray {
        return app.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IOException("Failed to open input stream")
    }

    override fun readBytes(uriString: String): ByteArray {
        return readBytes(Uri.parse(uriString))
    }

    private fun createFile(treeUri: Uri, mimeType: String, displayName: String): Uri {
        val docFile = DocumentFile.fromTreeUri(app, treeUri)
            ?: throw IOException("Failed to open selected directory")
        return docFile.createFile(mimeType, displayName)?.uri
            ?: throw IOException("Failed to create file in selected directory")
    }

    override fun createFile(treeUriString: String, mimeType: String, displayName: String): String {
        return createFile(Uri.parse(treeUriString), mimeType, displayName).toString()
    }

    private fun deleteFile(uri: Uri): Boolean {
        return DocumentFile.fromSingleUri(app, uri)?.delete() == true
    }

    override fun deleteFile(uriString: String): Boolean {
        return deleteFile(Uri.parse(uriString))
    }
}
