package com.worldtheater.archive.feature.note_list.screens.markdown_preview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File

private const val SNAPSHOT_MERMAID_LAYOUT_HEIGHT_PX = 4096
private const val SNAPSHOT_MERMAID_RENDER_TIMEOUT_MS = 12000L
private object AndroidMermaidScriptRepository {
    @Volatile
    private var cachedScript: String? = null

    suspend fun load(appContext: Context): String {
        cachedScript?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedScript ?: appContext.assets.open("mermaid/mermaid.min.js").bufferedReader().use { it.readText() }
                .also { cachedScript = it }
        }
    }
}

@Composable
internal actual fun rememberPlatformMarkdownPreviewRuntime(): PlatformMarkdownPreviewRuntime {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { AndroidMarkdownPreviewRuntime(appContext) }
}

@Composable
internal actual fun rememberPlatformMermaidSnapshotCacheIo(): MermaidSnapshotCacheIo {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { AndroidMermaidSnapshotCacheIo(appContext) }
}

private class AndroidMarkdownPreviewRuntime(
    appContext: Context
) : PlatformMarkdownPreviewRuntime {
    private val applicationContext = appContext.applicationContext
    private val renderer = AndroidMermaidSnapshotRenderer(appContext)

    override suspend fun loadMermaidScript(): String = AndroidMermaidScriptRepository.load(applicationContext)

    override suspend fun decodePreviewDiagram(
        payload: MermaidSnapshotCachedDiagramPayload,
        maxDisplayWidthPx: Int,
        maxDisplayHeightPx: Int
    ): MermaidSnapshotDiagram? = decodeAndroidPreviewDiagram(payload, maxDisplayWidthPx, maxDisplayHeightPx)

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
        return decodeAndroidPreviewDiagram(payload, renderWidthPx, maxDisplayHeightPx)
            ?: error("Failed to decode Mermaid preview PNG")
    }

    override fun dispose() {
        renderer.dispose()
    }
}

private class AndroidMermaidSnapshotCacheIo(
    private val appContext: Context
) : MermaidSnapshotCacheIo {
    override suspend fun ensureCacheDirectory(): String = withContext(Dispatchers.IO) {
        File(appContext.cacheDir, "mermaid-preview").also { it.mkdirs() }.absolutePath
    }

    override suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        runCatching { file.readBytes() }.getOrNull()
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            runCatching { File(path).writeBytes(bytes) }
        }
    }

    override suspend fun readText(path: String): String? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        runCatching { file.readText() }.getOrNull()
    }

    override suspend fun writeText(path: String, text: String) {
        withContext(Dispatchers.IO) {
            runCatching { File(path).writeText(text) }
        }
    }

    override suspend fun listFiles(directoryPath: String): List<MermaidSnapshotCacheFileEntry> = withContext(Dispatchers.IO) {
        File(directoryPath).listFiles()?.map { file ->
            MermaidSnapshotCacheFileEntry(
                name = file.name,
                lastModifiedEpochMillis = file.lastModified()
            )
        }.orEmpty()
    }

    override suspend fun deleteFile(path: String) {
        withContext(Dispatchers.IO) {
            runCatching { File(path).delete() }
        }
    }
}

private fun decodeAndroidPreviewDiagram(
    payload: MermaidSnapshotCachedDiagramPayload,
    maxDisplayWidthPx: Int,
    maxDisplayHeightPx: Int
): MermaidSnapshotDiagram? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(payload.pngBytes, 0, payload.pngBytes.size, bounds)
    val sampleSize = computeAndroidInSampleSize(
        sourceWidth = bounds.outWidth,
        sourceHeight = bounds.outHeight,
        targetWidth = maxDisplayWidthPx,
        targetHeight = maxDisplayHeightPx
    )
    val bitmap = BitmapFactory.decodeByteArray(
        payload.pngBytes,
        0,
        payload.pngBytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )?.asImageBitmap()
        ?: return null
    return MermaidSnapshotDiagram(
        bitmap = bitmap,
        widthDp = payload.widthDp,
        heightDp = payload.heightDp,
        pngBytes = payload.pngBytes
    )
}

private fun computeAndroidInSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) return 1
    var sampleSize = 1
    var currentWidth = sourceWidth
    var currentHeight = sourceHeight
    while (currentWidth / 2 >= targetWidth && currentHeight / 2 >= targetHeight) {
        sampleSize *= 2
        currentWidth /= 2
        currentHeight /= 2
    }
    return sampleSize.coerceAtLeast(1)
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
private class AndroidMermaidSnapshotRenderer(
    private val appContext: Context
) : MermaidSnapshotManagedWebRenderer<MermaidSnapshotCachedDiagramPayload>(
    timeoutMs = SNAPSHOT_MERMAID_RENDER_TIMEOUT_MS,
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val androidWebHostAdapter = AndroidMermaidWebHostAdapter(
        appContext = appContext,
        callbacks = hostCallbacks
    )

    override val webHostAdapter: MermaidSnapshotWebHostAdapter
        get() = androidWebHostAdapter

    suspend fun render(
        mermaidScript: String,
        mermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int
    ): MermaidSnapshotCachedDiagramPayload = withContext(Dispatchers.Main.immediate) {
        renderSnapshot(
            mermaidCode = mermaidCode,
            themeConfig = themeConfig,
            renderWidthPx = renderWidthPx,
            beforePrepare = { androidWebHostAdapter.ensureLoaded(mermaidScript) }
        )
    }

    fun dispose() {
        disposeRenderer()
    }

    override suspend fun decodeRasterizedPayload(
        payload: MermaidSnapshotRasterizedPayload
    ): MermaidSnapshotCachedDiagramPayload {
        val base64Payload = payload.dataUrl.substringAfter("base64,", missingDelimiterValue = "")
        require(base64Payload.isNotEmpty()) { "Rasterized Mermaid PNG payload is empty" }
        val pngBytes = Base64.decode(base64Payload, Base64.DEFAULT)
        return MermaidSnapshotCachedDiagramPayload(
            widthDp = payload.width.coerceAtLeast(1),
            heightDp = payload.height.coerceAtLeast(1),
            pngBytes = pngBytes
        )
    }

}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
private class AndroidMermaidWebHostAdapter(
    appContext: Context,
    callbacks: MermaidSnapshotHostCallbacks
) : MermaidSnapshotWebHostAdapter {
    private var pageReady = CompletableDeferred<Unit>()
    private var loadedScriptFingerprint: Int? = null
    private val webView: WebView = WebView(appContext).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = false
        settings.blockNetworkLoads = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        addJavascriptInterface(AndroidMermaidSnapshotBridge(callbacks), "AndroidBridge")
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!pageReady.isCompleted) {
                    pageReady.complete(Unit)
                }
            }
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            SNAPSHOT_MERMAID_LAYOUT_HEIGHT_PX
        )
    }

    suspend fun ensureLoaded(mermaidScript: String) {
        val fingerprint = mermaidScript.hashCode()
        if (loadedScriptFingerprint == fingerprint && pageReady.isCompleted) return
        pageReady = CompletableDeferred()
        loadedScriptFingerprint = fingerprint
        webView.loadDataWithBaseURL(
            "https://localhost/",
            buildStandardMermaidSnapshotHtmlDocument(
                bridge = MermaidBridgeSpec(
                    mode = MermaidBridgeMode.AndroidInterface,
                    targetName = "AndroidBridge"
                ),
                mermaidScript = mermaidScript
            ),
            "text/html",
            "utf-8",
            null
        )
        pageReady.await()
    }

    override fun dispose() {
        pageReady.cancel()
        webView.removeJavascriptInterface("AndroidBridge")
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.destroy()
    }

    private fun prepareWebViewLayout(renderWidthPx: Int) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            renderWidthPx,
            View.MeasureSpec.EXACTLY
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(
            SNAPSHOT_MERMAID_LAYOUT_HEIGHT_PX,
            View.MeasureSpec.EXACTLY
        )
        webView.measure(widthSpec, heightSpec)
        webView.layout(0, 0, renderWidthPx, SNAPSHOT_MERMAID_LAYOUT_HEIGHT_PX)
    }

    override suspend fun prepare(renderWidthPx: Int) {
        pageReady.await()
        prepareWebViewLayout(renderWidthPx)
    }

    override suspend fun evaluateRender(script: String) {
        webView.evaluateJavascript(script, null)
    }
}

private class AndroidMermaidSnapshotBridge(
    private val callbacks: MermaidSnapshotHostCallbacks
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onHeightChanged(height: Int) {
        mainHandler.post {
            callbacks.onHeightChanged(height)
        }
    }

    @JavascriptInterface
    fun onError(message: String?) {
        mainHandler.post {
            callbacks.onError(message ?: "Mermaid render failed")
        }
    }

    @JavascriptInterface
    fun onRasterizedPng(dataUrl: String?, width: Int, height: Int) {
        val safeDataUrl = dataUrl ?: return
        mainHandler.post {
            callbacks.onRasterized(
                MermaidSnapshotRasterizedPayload(
                    dataUrl = safeDataUrl,
                    width = width,
                    height = height
                )
            )
        }
    }
}
