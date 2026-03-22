package com.worldtheater.archive.feature.note_list.screens.markdown_preview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal data class MermaidSnapshotRasterizedPayload(
    val dataUrl: String,
    val width: Int,
    val height: Int,
)

internal interface MermaidSnapshotHostCallbacks {
    fun onHeightChanged(height: Int)
    fun onError(message: String)
    fun onRasterized(payload: MermaidSnapshotRasterizedPayload)
}

internal interface MermaidSnapshotWebHostAdapter {
    suspend fun prepare(renderWidthPx: Int)
    suspend fun evaluateRender(script: String)
    fun dispose()
}

internal fun parseMermaidRasterizedPayload(body: String): MermaidSnapshotRasterizedPayload? {
    val parts = body.split('\u0000')
    if (parts.size != 3) return null
    val width = parts[1].toIntOrNull() ?: return null
    val height = parts[2].toIntOrNull() ?: return null
    return MermaidSnapshotRasterizedPayload(
        dataUrl = parts[0],
        width = width,
        height = height
    )
}

internal abstract class MermaidSnapshotManagedWebRenderer<Result>(
    timeoutMs: Long,
    protected val scope: CoroutineScope,
) {
    protected abstract val webHostAdapter: MermaidSnapshotWebHostAdapter

    protected val hostCallbacks: MermaidSnapshotHostCallbacks = object : MermaidSnapshotHostCallbacks {
        override fun onHeightChanged(height: Int) = onHostHeightChanged(height)

        override fun onError(message: String) {
            failActive(IllegalStateException(message))
        }

        override fun onRasterized(payload: MermaidSnapshotRasterizedPayload) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.Default) {
                        decodeRasterizedPayload(payload)
                    }
                }.onSuccess(::completeActive)
                    .onFailure(::failActive)
            }
        }
    }

    private val renderMutex = Mutex()
    private var renderGeneration = 0L
    private var activeRequest: ActiveRenderRequest<Result>? = null
    private val timeoutMs = timeoutMs

    protected open fun onHostHeightChanged(height: Int) = Unit

    protected abstract suspend fun decodeRasterizedPayload(
        payload: MermaidSnapshotRasterizedPayload
    ): Result

    protected suspend fun renderSnapshot(
        mermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int,
        beforePrepare: suspend () -> Unit = {}
    ): Result {
        return renderMutex.withLock {
            withTimeout(timeoutMs) {
                beforePrepare()
                webHostAdapter.prepare(renderWidthPx)
                renderGeneration += 1
                val request = ActiveRenderRequest<Result>(
                    generation = renderGeneration,
                    result = CompletableDeferred()
                )
                activeRequest = request
                webHostAdapter.evaluateRender(
                    buildRenderJavascript(
                        mermaidCode = mermaidCode,
                        themeConfig = themeConfig,
                        renderWidthPx = renderWidthPx
                    )
                )
                request.result.await()
            }
        }
    }

    protected fun disposeRenderer() {
        scope.cancel()
        cancelActive()
        webHostAdapter.dispose()
    }

    private fun buildRenderJavascript(
        mermaidCode: String,
        themeConfig: String,
        renderWidthPx: Int
    ): String {
        val escapedCode = quoteJavascriptString(mermaidCode)
        val escapedThemeConfig = quoteJavascriptString(themeConfig)
        return "window.renderMermaid($escapedCode, $escapedThemeConfig, $renderWidthPx);"
    }

    private fun completeActive(result: Result) {
        val request = activeRequest ?: return
        if (request.generation == renderGeneration && !request.result.isCompleted) {
            request.result.complete(result)
        }
        if (activeRequest === request) {
            activeRequest = null
        }
    }

    private fun failActive(error: Throwable) {
        val request = activeRequest ?: return
        if (request.generation == renderGeneration && !request.result.isCompleted) {
            request.result.completeExceptionally(error)
        }
        if (activeRequest === request) {
            activeRequest = null
        }
    }

    private fun cancelActive() {
        activeRequest?.result?.cancel()
        activeRequest = null
        renderGeneration += 1
    }

    private data class ActiveRenderRequest<Result>(
        val generation: Long,
        val result: CompletableDeferred<Result>
    )
}
