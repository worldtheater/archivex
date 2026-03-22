@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.worldtheater.archive.platform.gateway

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.UIKit.UIPasteboard
import platform.UIKit.UISelectionFeedbackGenerator
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.fwrite
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.stat

object IosTransferFileGateway : TransferFileGateway {
    override fun writeBytes(uriString: String, bytes: ByteArray) {
        val file = fopen(uriString, "wb") ?: return
        try {
            if (bytes.isNotEmpty()) {
                bytes.usePinned { pinned ->
                    fwrite(
                        pinned.addressOf(0),
                        1.convert(),
                        bytes.size.convert(),
                        file
                    )
                }
            }
        } finally {
            fclose(file)
        }
    }

    override fun readBytes(uriString: String): ByteArray {
        val file = fopen(uriString, "rb") ?: return ByteArray(0)
        return try {
            val size = memScoped {
                val st = alloc<stat>()
                if (platform.posix.stat(uriString, st.ptr) != 0) return@memScoped 0
                st.st_size.toInt().coerceAtLeast(0)
            }
            if (size <= 0) return ByteArray(0)
            val result = ByteArray(size)
            result.usePinned { pinned ->
                fseek(file, 0, SEEK_SET)
                val read = fread(
                    pinned.addressOf(0),
                    1.convert(),
                    size.convert(),
                    file
                ).toInt()
                when {
                    read == size -> result
                    read > 0 -> result.copyOf(read)
                    else -> ByteArray(0)
                }
            }
        } finally {
            fclose(file)
        }
    }

    override fun createFile(treeUriString: String, mimeType: String, displayName: String): String {
        ensureDirectory(treeUriString)
        val path = "$treeUriString/$displayName"
        val file = fopen(path, "ab")
        if (file != null) {
            fclose(file)
        }
        return path
    }

    override fun deleteFile(uriString: String): Boolean {
        return remove(uriString) == 0
    }
}

object IosClipboardGateway : ClipboardGateway {
    override fun clear() {
        UIPasteboard.generalPasteboard.string = ""
    }

    override fun copy(label: String, text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}

object IosHapticFeedbackGateway : HapticFeedbackGateway {
    override fun vibrate(milliseconds: Long, effect: Int?) {
        val pulseCount = when {
            effect != null && effect >= 200 -> 3
            effect != null && effect >= 120 -> 2
            milliseconds >= 24L -> 2
            else -> 1
        }
        repeat(pulseCount) {
            val generator = UISelectionFeedbackGenerator()
            generator.prepare()
            generator.selectionChanged()
        }
    }
}

object IosBackupDirectoryGateway : BackupDirectoryGateway {
    override fun persistBackupDirectoryPermission(uriString: String) = Unit

    override fun readableBackupDirectoryPath(uriString: String): String = uriString
}

class IosBackupDocumentPickerGateway : BackupDocumentPickerGateway {
    @Composable
    override fun rememberActions(
        onBackupDirPicked: (String?) -> Unit
    ): BackupDocumentPickerActions {
        val launchBackupDirPicker = remember { { onBackupDirPicked(null) } }
        val requestRestoreDocument = remember { suspend { null } }
        return BackupDocumentPickerActions(
            launchBackupDirPicker = launchBackupDirPicker,
            requestRestoreDocument = requestRestoreDocument
        )
    }
}

class IosImportExportDocumentPickerGateway : ImportExportDocumentPickerGateway {
    @Composable
    override fun rememberActions(
        onImportDocumentPicked: (String?) -> Unit,
        onExportDocumentPicked: (String?) -> Unit
    ): ImportExportDocumentPickerActions {
        val launchImportPicker = remember { { onImportDocumentPicked(null) } }
        val launchExportPicker = remember { { _: String -> onExportDocumentPicked(null) } }
        return ImportExportDocumentPickerActions(
            launchImportPicker = launchImportPicker,
            launchExportPicker = launchExportPicker
        )
    }
}

private fun ensureDirectory(path: String) {
    if (NSFileManager.defaultManager.fileExistsAtPath(path)) return
    mkdir(path, 0x1EDu) // 0755
}
