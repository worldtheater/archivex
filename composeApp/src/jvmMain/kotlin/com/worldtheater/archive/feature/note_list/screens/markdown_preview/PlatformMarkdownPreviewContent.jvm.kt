package com.worldtheater.archive.feature.note_list.screens.markdown_preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color as FxColor
import javafx.scene.web.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import netscape.javascript.JSObject
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.resume

private const val JVM_MERMAID_DEFAULT_WEBVIEW_HEIGHT_DP = MermaidPreviewDefaults.DEFAULT_HEIGHT_DP
private const val JVM_MERMAID_SNAPSHOT_MAX_HEIGHT_PX = 4096

private object JvmMermaidScriptRepository {
    @Volatile
    private var cachedScript: String? = null

    fun load(): String {
        cachedScript?.let { return it }
        return synchronized(this) {
            cachedScript ?: checkNotNull(JvmMermaidSvgRenderer::class.java.getResourceAsStream("/mermaid/mermaid.min.js")) {
                "Missing Mermaid resource: /mermaid/mermaid.min.js"
            }.bufferedReader().use { it.readText() }.also { cachedScript = it }
        }
    }
}

private object JvmMermaidRendererHolder {
    @Volatile
    private var renderer: JvmMermaidSvgRenderer? = null

    fun get(): JvmMermaidSvgRenderer {
        renderer?.let { return it }
        return synchronized(this) {
            renderer ?: JvmMermaidSvgRenderer.create().also { renderer = it }
        }
    }
}

@Composable
internal actual fun rememberPlatformMarkdownPreviewRuntime(): PlatformMarkdownPreviewRuntime {
    return remember { JvmMarkdownPreviewRuntime() }
}

@Composable
internal actual fun rememberPlatformMermaidSnapshotCacheIo(): MermaidSnapshotCacheIo {
    return remember { JvmMermaidSnapshotCacheIo }
}

private object JvmMermaidSnapshotCacheIo : MermaidSnapshotCacheIo {
    private val cacheDir: Path by lazy {
        val home = System.getProperty("user.home").orEmpty().ifBlank { "." }
        Paths.get(home, ".archivex", "cache", "mermaid")
    }

    override suspend fun ensureCacheDirectory(): String = withContext(Dispatchers.IO) {
        Files.createDirectories(cacheDir)
        cacheDir.toString()
    }

    override suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val filePath = Paths.get(path)
        if (!Files.exists(filePath)) return@withContext null
        runCatching { Files.readAllBytes(filePath) }.getOrNull()
    }

    override suspend fun writeBytes(path: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            runCatching {
                Files.write(
                    Paths.get(path),
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                )
            }
        }
    }

    override suspend fun readText(path: String): String? = withContext(Dispatchers.IO) {
        val filePath = Paths.get(path)
        if (!Files.exists(filePath)) return@withContext null
        runCatching { Files.readString(filePath) }.getOrNull()
    }

    override suspend fun writeText(path: String, text: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                Files.writeString(
                    Paths.get(path),
                    text,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                )
            }
        }
    }

    override suspend fun listFiles(directoryPath: String): List<MermaidSnapshotCacheFileEntry> = withContext(Dispatchers.IO) {
        val directory = Paths.get(directoryPath)
        if (!Files.exists(directory)) return@withContext emptyList()
        Files.list(directory).use { stream ->
            stream.map { path ->
                MermaidSnapshotCacheFileEntry(
                    name = path.fileName.toString(),
                    lastModifiedEpochMillis = Files.getLastModifiedTime(path).toMillis()
                )
            }.toList()
        }
    }

    override suspend fun deleteFile(path: String) {
        withContext(Dispatchers.IO) {
            runCatching { Files.deleteIfExists(Paths.get(path)) }
        }
    }
}

private class JvmMarkdownPreviewRuntime : PlatformMarkdownPreviewRuntime {
    private val renderMutex = Mutex()

    override suspend fun loadMermaidScript(): String = withContext(Dispatchers.IO) {
        JvmMermaidScriptRepository.load()
    }

    override suspend fun decodePreviewDiagram(
        payload: MermaidSnapshotCachedDiagramPayload,
        maxDisplayWidthPx: Int,
        maxDisplayHeightPx: Int
    ): MermaidSnapshotDiagram? {
        return decodeJvmPreviewDiagram(payload, maxDisplayWidthPx, maxDisplayHeightPx)?.toCommonDiagram()
    }

    override suspend fun renderDiagram(
        mermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int,
        maxDisplayHeightPx: Int,
        script: String
    ): MermaidSnapshotDiagram {
        return renderMutex.withLock {
            val renderer = withContext(Dispatchers.Default) {
                JvmMermaidRendererHolder.get()
            }
            val rendered = suspendCancellableCoroutine<RenderedMermaidDiagramPayload> { continuation ->
                renderer.render(
                    mermaidScript = script,
                    mermaidCode = mermaidCode,
                    themeConfig = themeConfig,
                    renderWidthPx = renderWidthPx
                ) { diagram ->
                    if (continuation.isActive) {
                        continuation.resume(diagram)
                    }
                }
                continuation.invokeOnCancellation {
                    renderer.cancel()
                }
            }
            decodeJvmPreviewDiagram(rendered.toCachedPayload(), renderWidthPx, maxDisplayHeightPx)?.toCommonDiagram()
                ?: error("Failed to decode Mermaid preview PNG")
        }
    }
}

private class JvmMermaidSvgRenderer(
    private val webView: WebView,
    private val rootPane: StackPane,
    private val scene: Scene,
) {
    private data class PendingRender(
        val mermaidScript: String,
        val mermaidCode: String,
        val themeConfig: String,
        val renderWidthPx: Int,
        val onRendered: (RenderedMermaidDiagramPayload) -> Unit
    )

    private var loadedScriptFingerprint: Int? = null
    private var latestCode: String = ""
    private var latestThemeConfig: String = ""
    private var latestMeasuredHeightPx: Int = JVM_MERMAID_DEFAULT_WEBVIEW_HEIGHT_DP
    private var onRendered: (RenderedMermaidDiagramPayload) -> Unit = {}
    private var bridge: MermaidBridge? = null
    private var pendingRender: PendingRender? = null
    private var renderGeneration: Long = 0L

    init {
        webView.isContextMenuEnabled = false
        webView.style = "-fx-background-color: transparent;"
        webView.engine.isJavaScriptEnabled = true
        webView.engine.loadWorker.stateProperty().addListener { _, _, newState ->
            when (newState) {
                Worker.State.SUCCEEDED -> {
                    val newBridge = MermaidBridge(
                        heightChangedCallback = { height ->
                            latestMeasuredHeightPx = height.coerceAtLeast(1)
                        },
                        errorCallback = {},
                        pngRenderedCallback = { pngDataUrl, width, height ->
                            renderPng(pngDataUrl, width, height)
                        }
                    )
                    bridge = newBridge
                    val window = webView.engine.executeScript("window") as JSObject
                    window.setMember("CodexBridge", newBridge)
                    pendingRender?.let {
                        pendingRender = null
                        render(it.mermaidScript, it.mermaidCode, it.themeConfig, it.renderWidthPx, it.onRendered)
                    }
                }

                Worker.State.FAILED -> {
                    webView.engine.loadWorker.exception?.printStackTrace()
                }

                else -> Unit
            }
        }

        rootPane.style = "-fx-background-color: transparent;"
        scene.fill = FxColor.TRANSPARENT
        rootPane.children.setAll(webView)
    }

    fun render(
        mermaidScript: String,
        mermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int,
        onRendered: (RenderedMermaidDiagramPayload) -> Unit
    ) {
        val scriptFingerprint = mermaidScript.hashCode()
        latestCode = mermaidCode
        latestThemeConfig = themeConfig
        latestMeasuredHeightPx = JVM_MERMAID_DEFAULT_WEBVIEW_HEIGHT_DP
        this.onRendered = onRendered
        renderGeneration += 1
        val generation = renderGeneration
        Platform.runLater {
            if (bridge == null || loadedScriptFingerprint != scriptFingerprint) {
                loadedScriptFingerprint = scriptFingerprint
                pendingRender = PendingRender(mermaidScript, mermaidCode, themeConfig, renderWidthPx, onRendered)
                webView.engine.loadContent(
                    buildStandardMermaidSnapshotHtmlDocument(
                        bridge = MermaidBridgeSpec(
                            mode = MermaidBridgeMode.JavascriptObject,
                            targetName = "CodexBridge"
                        ),
                        mermaidScript = mermaidScript
                    )
                )
                return@runLater
            }
            val width = renderWidthPx
            val height = JVM_MERMAID_SNAPSHOT_MAX_HEIGHT_PX
            webView.prefWidth = width.toDouble()
            webView.minWidth = width.toDouble()
            webView.maxWidth = width.toDouble()
            webView.prefHeight = height.toDouble()
            rootPane.prefWidth = width.toDouble()
            rootPane.prefHeight = height.toDouble()
            rootPane.resize(width.toDouble(), height.toDouble())
            webView.resize(width.toDouble(), height.toDouble())
            rootPane.applyCss()
            rootPane.layout()
            val escapedCode = quoteJavascriptString(mermaidCode)
            val escapedThemeConfig = quoteJavascriptString(themeConfig)
            webView.properties["renderGeneration"] = generation
            webView.engine.executeScript("window.renderMermaid($escapedCode, $escapedThemeConfig, $width);")
        }
    }

    fun cancel() {
        renderGeneration += 1
        pendingRender = null
    }

    private fun renderPng(pngDataUrl: String, width: Int, height: Int) {
        val generation = (webView.properties["renderGeneration"] as? Long) ?: return
        val base64Payload = pngDataUrl.substringAfter("base64,", missingDelimiterValue = "")
        if (base64Payload.isEmpty()) {
            return
        }
        val pngBytes = Base64.getDecoder().decode(base64Payload)
        val targetHeight = maxOf(height, latestMeasuredHeightPx).coerceAtLeast(1)
        if (generation == renderGeneration) {
            onRendered(
                RenderedMermaidDiagramPayload(
                    widthDp = width.coerceAtLeast(1),
                    heightDp = targetHeight,
                    pngBytes = pngBytes
                )
            )
        }
    }

    companion object {
        fun create(): JvmMermaidSvgRenderer {
            ensureFxStarted()
            val rendererHolder = arrayOfNulls<JvmMermaidSvgRenderer>(1)
            val latch = CountDownLatch(1)
            Platform.runLater {
                val webView = WebView()
                val rootPane = StackPane()
                val scene = Scene(rootPane)
                rendererHolder[0] = JvmMermaidSvgRenderer(
                    webView = webView,
                    rootPane = rootPane,
                    scene = scene,
                )
                latch.countDown()
            }
            latch.await()
            return checkNotNull(rendererHolder[0])
        }
    }
}

private data class RenderedMermaidDiagramPayload(
    val widthDp: Int,
    val heightDp: Int,
    val pngBytes: ByteArray
)

private fun RenderedMermaidDiagramPayload.toCachedPayload(): MermaidSnapshotCachedDiagramPayload {
    return MermaidSnapshotCachedDiagramPayload(
        pngBytes = pngBytes,
        widthDp = widthDp,
        heightDp = heightDp
    )
}

private fun decodeJvmPreviewDiagram(
    payload: MermaidSnapshotCachedDiagramPayload,
    maxDisplayWidthPx: Int,
    maxDisplayHeightPx: Int
): RenderedMermaidDiagram? {
    val bufferedImage = ImageIO.read(ByteArrayInputStream(payload.pngBytes)) ?: return null
    val scaledImage = scaleJvmPreviewImage(bufferedImage, maxDisplayWidthPx, maxDisplayHeightPx)
    return RenderedMermaidDiagram(
        bitmap = scaledImage.toComposeImageBitmap(),
        widthDp = payload.widthDp,
        heightDp = payload.heightDp,
        pngBytes = payload.pngBytes
    )
}

private fun scaleJvmPreviewImage(
    image: BufferedImage,
    maxDisplayWidthPx: Int,
    maxDisplayHeightPx: Int
): BufferedImage {
    if (image.width <= 0 || image.height <= 0) return image
    val scale = minOf(
        1.0,
        maxDisplayWidthPx.toDouble() / image.width.toDouble(),
        maxDisplayHeightPx.toDouble() / image.height.toDouble()
    )
    if (scale >= 0.999) return image
    val targetWidth = maxOf(1, (image.width * scale).toInt())
    val targetHeight = maxOf(1, (image.height * scale).toInt())
    val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
    val graphics = scaled.createGraphics()
    try {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null)
    } finally {
        graphics.dispose()
    }
    return scaled
}

private data class RenderedMermaidDiagram(
    val bitmap: ImageBitmap,
    val widthDp: Int,
    val heightDp: Int,
    val pngBytes: ByteArray
)

private fun RenderedMermaidDiagram.toCommonDiagram(): MermaidSnapshotDiagram {
    return MermaidSnapshotDiagram(
        bitmap = bitmap,
        widthDp = widthDp,
        heightDp = heightDp,
        pngBytes = pngBytes
    )
}

class MermaidBridge(
    private val heightChangedCallback: (Int) -> Unit,
    private val errorCallback: (String) -> Unit,
    private val pngRenderedCallback: (String, Int, Int) -> Unit
) {
    fun onHeightChanged(height: Int) {
        heightChangedCallback(height)
    }

    fun onError(message: String?) {
        errorCallback(message ?: "Mermaid render failed")
    }

    fun onRasterizedPng(dataUrl: String?, width: Int, height: Int) {
        val safeDataUrl = dataUrl ?: return
        pngRenderedCallback(safeDataUrl, width, height)
    }
}

private val JVM_FX_INITIALIZED = AtomicBoolean(false)
private const val JVM_MERMAID_PANEL_CONTROLLER_KEY = "JvmMermaidPanelController"

private fun ensureFxStarted() {
    if (JVM_FX_INITIALIZED.compareAndSet(false, true)) {
        val latch = CountDownLatch(1)
        try {
            Platform.startup {
                latch.countDown()
            }
        } catch (_: IllegalStateException) {
            latch.countDown()
        }
        latch.await()
        Platform.setImplicitExit(false)
    }
}
