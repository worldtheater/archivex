package com.worldtheater.archive.platform.gateway

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

object DesktopTransferFileGateway : TransferFileGateway {
    override fun writeBytes(uriString: String, bytes: ByteArray) {
        File(uriString).writeBytes(bytes)
    }

    override fun readBytes(uriString: String): ByteArray {
        return File(uriString).readBytes()
    }

    override fun createFile(treeUriString: String, mimeType: String, displayName: String): String {
        val dir = File(treeUriString)
        require(dir.exists() || dir.mkdirs()) { "Unable to create directory: $treeUriString" }
        return File(dir, displayName).absolutePath
    }

    override fun deleteFile(uriString: String): Boolean = File(uriString).delete()
}

object DesktopClipboardGateway : ClipboardGateway {
    override fun clear() {
        copy("", "")
    }

    override fun copy(label: String, text: String) {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        }
    }
}
