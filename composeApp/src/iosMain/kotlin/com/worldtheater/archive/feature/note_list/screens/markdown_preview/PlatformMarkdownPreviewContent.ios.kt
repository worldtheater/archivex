@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.io.encoding.ExperimentalEncodingApi::class
)

package com.worldtheater.archive.feature.note_list.screens.markdown_preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeImageBitmap
import archivex.composeapp.generated.resources.Res
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.stat

private const val IOS_MERMAID_RESULT_HANDLER_NAME = "mermaidResult"
private const val IOS_MERMAID_ERROR_HANDLER_NAME = "mermaidError"
private const val IOS_MERMAID_HEIGHT_HANDLER_NAME = "mermaidHeight"

private object IosMermaidScriptRepository {
    private val mutex = Mutex()
    private var cachedScript: String? = null

    suspend fun load(): String = mutex.withLock {
        cachedScript ?: withContext(Dispatchers.Default) {
            loadTextFromUri(Res.getUri("files/mermaid/mermaid.min.js"))
        }.also { cachedScript = it }
    }
}

@Composable
internal actual fun rememberPlatformMarkdownPreviewRuntime(): PlatformMarkdownPreviewRuntime {
    val renderer = remember { IosSharedMermaidRenderer() }
    return remember(renderer) { IosMarkdownPreviewRuntime(renderer) }
}

private class IosMarkdownPreviewRuntime(
    private val renderer: IosSharedMermaidRenderer,
) : PlatformMarkdownPreviewRuntime {
    override suspend fun loadMermaidScript(): String = IosMermaidScriptRepository.load()

    override suspend fun decodePreviewDiagram(
        payload: MermaidSnapshotCachedDiagramPayload,
        maxDisplayWidthPx: Int,
        maxDisplayHeightPx: Int
    ): MermaidSnapshotDiagram? = withContext(Dispatchers.Default) {
        decodeIosPreviewDiagram(payload)
    }

    override suspend fun renderDiagram(
        mermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int,
        maxDisplayHeightPx: Int,
        script: String
    ): MermaidSnapshotDiagram {
        val payload = renderer.render(
            mermaidScript = script,
            mermaidCode = mermaidCode,
            themeConfig = themeConfig,
            renderWidthPx = renderWidthPx
        )
        return decodeIosPreviewDiagram(payload) ?: error("Failed to decode Mermaid preview PNG")
    }

    override fun dispose() {
        renderer.dispose()
    }
}

@Composable
internal actual fun rememberPlatformMermaidSnapshotCacheIo(): MermaidSnapshotCacheIo {
    return remember { IosMermaidSnapshotCacheIo }
}

private object IosMermaidSnapshotCacheIo : MermaidSnapshotCacheIo {
    override suspend fun ensureCacheDirectory(): String = withContext(Dispatchers.Default) {
        val path = "${NSHomeDirectory()}/Library/Caches/mermaid-preview"
        ensureDirectoryPath(path)
        path
    }

    override suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.Default) {
        runCatching { readBytesFromPath(path) }.getOrNull()
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray) {
        withContext(Dispatchers.Default) {
            runCatching { writeBytesToPath(path, bytes) }
        }
    }

    override suspend fun readText(path: String): String? = withContext(Dispatchers.Default) {
        runCatching { readBytesFromPath(path).decodeToString() }.getOrNull()
    }

    override suspend fun writeText(path: String, text: String) {
        withContext(Dispatchers.Default) {
            runCatching { writeBytesToPath(path, text.encodeToByteArray()) }
        }
    }

    override suspend fun listFiles(directoryPath: String): List<MermaidSnapshotCacheFileEntry> = withContext(Dispatchers.Default) {
        val names = NSFileManager.defaultManager.contentsOfDirectoryAtPath(directoryPath, error = null)
            ?.mapNotNull { it as? String }
            .orEmpty()
        names.map { name ->
            MermaidSnapshotCacheFileEntry(
                name = name,
                lastModifiedEpochMillis = fileLastModifiedEpochMillis("$directoryPath/$name")
            )
        }
    }

    override suspend fun deleteFile(path: String) {
        withContext(Dispatchers.Default) {
            runCatching {
                remove(path)
            }
        }
    }
}

private class IosSharedMermaidRenderer :
    MermaidSnapshotManagedWebRenderer<MermaidSnapshotCachedDiagramPayload>(
        timeoutMs = 12_000L,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    ) {
    private val messageHandler = IosMermaidScriptMessageHandler(hostCallbacks)
    private val iosWebHostAdapter = IosMermaidWebHostAdapter(messageHandler)

    override val webHostAdapter: MermaidSnapshotWebHostAdapter
        get() = iosWebHostAdapter

    suspend fun render(
        mermaidScript: String,
        mermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int
    ): MermaidSnapshotCachedDiagramPayload {
        return renderSnapshot(
            mermaidCode = mermaidCode,
            themeConfig = themeConfig,
            renderWidthPx = renderWidthPx,
            beforePrepare = { ensureLoaded(mermaidScript) }
        )
    }

    fun dispose() {
        disposeRenderer()
    }

    private suspend fun ensureLoaded(mermaidScript: String) {
        iosWebHostAdapter.ensureLoaded(mermaidScript)
    }

    override suspend fun decodeRasterizedPayload(
        payload: MermaidSnapshotRasterizedPayload
    ): MermaidSnapshotCachedDiagramPayload {
        return decodeIosRasterizedPayload(payload.dataUrl, payload.width, payload.height)
    }

}

private class IosMermaidWebHostAdapter(
    messageHandler: IosMermaidScriptMessageHandler
) : MermaidSnapshotWebHostAdapter {
    private var pageReady = CompletableDeferred<Unit>()
    private var loadedScriptFingerprint: Int? = null
    private val navigationDelegate = IosMermaidNavigationDelegate(
        onPageLoaded = {
            if (!pageReady.isCompleted) {
                pageReady.complete(Unit)
            }
        }
    )
    private val webView: WKWebView = WKWebView(
        frame = CGRectMake(0.0, 0.0, 1.0, 1.0),
        configuration = WKWebViewConfiguration().apply {
            userContentController = WKUserContentController().apply {
                addScriptMessageHandler(messageHandler, IOS_MERMAID_RESULT_HANDLER_NAME)
                addScriptMessageHandler(messageHandler, IOS_MERMAID_ERROR_HANDLER_NAME)
                addScriptMessageHandler(messageHandler, IOS_MERMAID_HEIGHT_HANDLER_NAME)
            }
        }
    ).apply {
        navigationDelegate = this@IosMermaidWebHostAdapter.navigationDelegate
        scrollView.scrollEnabled = false
    }

    suspend fun ensureLoaded(mermaidScript: String) {
        val fingerprint = mermaidScript.hashCode()
        if (loadedScriptFingerprint == fingerprint && pageReady.isCompleted) return
        pageReady = CompletableDeferred()
        loadedScriptFingerprint = fingerprint
        webView.loadHTMLString(buildIosMermaidHostHtml(mermaidScript), baseURL = null)
        pageReady.await()
    }

    override suspend fun prepare(renderWidthPx: Int) {
        webView.setFrame(CGRectMake(0.0, 0.0, renderWidthPx.toDouble(), 4096.0))
    }

    override suspend fun evaluateRender(script: String) {
        webView.evaluateJavaScript(script, completionHandler = null)
    }

    override fun dispose() {
        pageReady.cancel()
        webView.configuration.userContentController.removeScriptMessageHandlerForName(IOS_MERMAID_RESULT_HANDLER_NAME)
        webView.configuration.userContentController.removeScriptMessageHandlerForName(IOS_MERMAID_ERROR_HANDLER_NAME)
        webView.configuration.userContentController.removeScriptMessageHandlerForName(IOS_MERMAID_HEIGHT_HANDLER_NAME)
        webView.stopLoading()
        webView.loadHTMLString("", baseURL = null)
    }
}

private class IosMermaidScriptMessageHandler(
    private val callbacks: MermaidSnapshotHostCallbacks
) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        when (didReceiveScriptMessage.name) {
            IOS_MERMAID_RESULT_HANDLER_NAME -> {
                val body = (didReceiveScriptMessage.body as? NSString)?.toString()
                    ?: didReceiveScriptMessage.body.toString()
                val payload = parseMermaidRasterizedPayload(body)
                if (payload != null) {
                    callbacks.onRasterized(payload)
                } else {
                    callbacks.onError("Invalid Mermaid result payload")
                }
            }

            IOS_MERMAID_ERROR_HANDLER_NAME -> {
                val message = (didReceiveScriptMessage.body as? NSString)?.toString()
                    ?: didReceiveScriptMessage.body.toString()
                callbacks.onError(message)
            }

            IOS_MERMAID_HEIGHT_HANDLER_NAME -> {
                val height = when (val body = didReceiveScriptMessage.body) {
                    is NSNumber -> body.intValue
                    is NSString -> body.toString().toIntOrNull()
                    else -> body.toString().toIntOrNull()
                }
                if (height != null) {
                    callbacks.onHeightChanged(height)
                }
            }
        }
    }
}

private class IosMermaidNavigationDelegate(
    private val onPageLoaded: () -> Unit
) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        didFinishNavigation: WKNavigation?
    ) {
        onPageLoaded()
    }
}

private fun decodeIosRasterizedPayload(
    pngDataUrl: String,
    width: Int,
    height: Int
): MermaidSnapshotCachedDiagramPayload {
    val payload = pngDataUrl.substringAfter("base64,", missingDelimiterValue = "")
    require(payload.isNotEmpty()) { "Rasterized Mermaid PNG payload is empty" }
    val pngBytes = kotlin.io.encoding.Base64.decode(payload)
    return MermaidSnapshotCachedDiagramPayload(
        widthDp = width.coerceAtLeast(1),
        heightDp = height.coerceAtLeast(1),
        pngBytes = pngBytes
    )
}

private fun decodeIosPreviewDiagram(payload: MermaidSnapshotCachedDiagramPayload): MermaidSnapshotDiagram? {
    val bitmap = runCatching { SkiaImage.makeFromEncoded(payload.pngBytes).toComposeImageBitmap() }.getOrNull()
        ?: return null
    return MermaidSnapshotDiagram(
        bitmap = bitmap,
        widthDp = payload.widthDp,
        heightDp = payload.heightDp,
        pngBytes = payload.pngBytes
    )
}

private fun buildIosMermaidHostHtml(
    mermaidScript: String
): String {
    return buildStandardMermaidSnapshotHtmlDocument(
        bridge = MermaidBridgeSpec(
            mode = MermaidBridgeMode.IosWebkitHandlers,
            targetName = "window.webkit.messageHandlers",
            resultName = IOS_MERMAID_RESULT_HANDLER_NAME,
            errorName = IOS_MERMAID_ERROR_HANDLER_NAME,
            heightName = IOS_MERMAID_HEIGHT_HANDLER_NAME
        ),
        mermaidScript = mermaidScript
    )
}

private fun loadTextFromUri(uriString: String): String {
    val url = NSURL.URLWithString(uriString) ?: error("Invalid resource URI: $uriString")
    val path = url.path ?: error("Missing resource path: $uriString")
    val file = fopen(path, "rb") ?: error("Unable to open resource: $uriString")
    try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        check(size >= 0) { "Unable to determine resource size: $uriString" }
        fseek(file, 0, SEEK_SET)
        val bytes = ByteArray(size.toInt())
        val read = bytes.usePinned { pinned ->
            fread(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
        }
        check(read.toInt() == bytes.size) { "Unable to fully read resource: $uriString" }
        return bytes.decodeToString()
    } finally {
        fclose(file)
    }
}

private fun readBytesFromPath(path: String): ByteArray {
    val file = fopen(path, "rb") ?: error("Unable to open file: $path")
    try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        check(size >= 0) { "Unable to determine file size: $path" }
        fseek(file, 0, SEEK_SET)
        if (size == 0L) return ByteArray(0)
        val bytes = ByteArray(size.toInt())
        val read = bytes.usePinned { pinned ->
            fread(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
        }
        check(read.toInt() == bytes.size) { "Unable to fully read file: $path" }
        return bytes
    } finally {
        fclose(file)
    }
}

private fun writeBytesToPath(path: String, bytes: ByteArray) {
    val file = fopen(path, "wb") ?: error("Unable to open file for writing: $path")
    try {
        if (bytes.isNotEmpty()) {
            val written = bytes.usePinned { pinned ->
                platform.posix.fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
            }
            check(written.toInt() == bytes.size) { "Unable to fully write file: $path" }
        }
    } finally {
        fclose(file)
    }
}

private fun fileLastModifiedEpochMillis(path: String): Long {
    return memScoped {
        val fileStat = alloc<stat>()
        if (platform.posix.stat(path, fileStat.ptr) != 0) return@memScoped 0L
        fileStat.st_mtimespec.tv_sec * 1000L +
            (fileStat.st_mtimespec.tv_nsec / 1_000_000L)
    }
}

private fun ensureDirectoryPath(path: String) {
    if (NSFileManager.defaultManager.fileExistsAtPath(path)) return
    mkdir(path, 0x1EDu)
}
