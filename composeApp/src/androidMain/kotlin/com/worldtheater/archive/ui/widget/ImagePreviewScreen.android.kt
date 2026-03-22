package com.worldtheater.archive.ui.widget

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import archivex.composeapp.generated.resources.Res
import archivex.composeapp.generated.resources.action_cancel
import com.worldtheater.archive.platform.system.AppBackHandler
import com.worldtheater.archive.ui.theme.DefaultAppBarButtonSize
import com.worldtheater.archive.ui.theme.isAppInDarkTheme
import com.worldtheater.archive.ui.theme.rememberTopBarPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

private const val IMAGE_PREVIEW_MIN_SCALE = 1f
private const val IMAGE_PREVIEW_MAX_SCALE = 5f
private const val IMAGE_PREVIEW_DOUBLE_TAP_SCALE = 2.5f
private const val IMAGE_PREVIEW_DOUBLE_TAP_ANIMATION_MS = 180
private const val IMAGE_PREVIEW_OVERLAY_ANIMATION_MS = 160
private const val IMAGE_PREVIEW_DRAG_DEADBAND_DP = 8f
private const val IMAGE_PREVIEW_EDGE_GAP_DP = 32f
private const val IMAGE_PREVIEW_FLING_LOOKBACK_MILLIS = 120L

private data class ImagePreviewPanSample(
    val timeMillis: Long,
    val offset: Offset
)

@Composable
actual fun ImagePreviewScreen(
    image: ImagePreviewData,
    onDismiss: () -> Unit
) {
    val visibleState = remember(image) {
        MutableTransitionState(false).apply { targetState = true }
    }
    val isDarkTheme = isAppInDarkTheme()
    val backgroundColor = if (isDarkTheme) {
        Color.Black.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.background
    }
    val contentColor = if (isDarkTheme) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val topBarPadding = rememberTopBarPadding()
    val requestDismiss = {
        visibleState.targetState = false
    }

    LaunchedEffect(visibleState.currentState, visibleState.targetState) {
        if (!visibleState.currentState && !visibleState.targetState) {
            onDismiss()
        }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(IMAGE_PREVIEW_OVERLAY_ANIMATION_MS)),
        exit = fadeOut(animationSpec = tween(IMAGE_PREVIEW_OVERLAY_ANIMATION_MS))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            AppBackHandler(onBack = requestDismiss)

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val viewConfiguration = remember(context) { ViewConfiguration.get(context) }
                val density = LocalDensity.current
                val viewportSizePx = with(density) {
                    Size(maxWidth.toPx(), maxHeight.toPx())
                }
                val touchSlop = remember(viewConfiguration) {
                    viewConfiguration.scaledTouchSlop.toFloat()
                }
                val minimumFlingVelocity = remember(viewConfiguration) {
                    viewConfiguration.scaledMinimumFlingVelocity.toFloat()
                }
                val dragReleaseThresholdPx = remember(density, touchSlop) {
                    maxOf(touchSlop, with(density) { IMAGE_PREVIEW_DRAG_DEADBAND_DP.dp.toPx() })
                }
                val edgeGapPx = remember(density) {
                    with(density) { IMAGE_PREVIEW_EDGE_GAP_DP.dp.toPx() }
                }
                val imageAspectRatio = remember(image.widthDp, image.heightDp) {
                    if (image.widthDp <= 0 || image.heightDp <= 0) {
                        1f
                    } else {
                        image.widthDp.toFloat() / image.heightDp.toFloat()
                    }
                }
                val fittedImageSizePx = remember(viewportSizePx, imageAspectRatio) {
                    fitImageIntoViewport(
                        viewportSize = viewportSizePx,
                        imageAspectRatio = imageAspectRatio
                    )
                }
                val imageWidthDp = with(density) { fittedImageSizePx.width.toDp() }
                val imageHeightDp = with(density) { fittedImageSizePx.height.toDp() }

                var scale by remember(image) { mutableFloatStateOf(IMAGE_PREVIEW_MIN_SCALE) }
                var offset by remember(image) { mutableStateOf(Offset.Zero) }
                var flingToken by remember(image) { mutableIntStateOf(0) }
                val scroller = remember(context, image) { OverScroller(context) }
                var activePointerId by remember(image) {
                    mutableIntStateOf(MotionEvent.INVALID_POINTER_ID)
                }
                var dragStartTouch by remember(image) { mutableStateOf<Offset?>(null) }
                var dragAnchorTouch by remember(image) { mutableStateOf<Offset?>(null) }
                var isDragging by remember(image) { mutableStateOf(false) }
                var dragFilter by remember(image) { mutableStateOf<OneEuroOffsetFilter?>(null) }
                var panSamples by remember(image) {
                    mutableStateOf<ArrayDeque<ImagePreviewPanSample>>(ArrayDeque())
                }
                var transitionJob by remember(image) { mutableStateOf<Job?>(null) }

                fun boundedOffset(rawOffset: Offset, scaleValue: Float): Offset {
                    val maxOffset = calculateMaxOffset(
                        viewportSize = viewportSizePx,
                        contentSize = fittedImageSizePx,
                        scale = scaleValue,
                        edgeGapPx = edgeGapPx
                    )
                    return rawOffset.clamp(maxOffset)
                }

                fun stopFling() {
                    if (!scroller.isFinished) {
                        scroller.forceFinished(true)
                    }
                }

                fun stopTransition() {
                    transitionJob?.cancel()
                    transitionJob = null
                }

                fun applyScaleAndPan(
                    previousScale: Float,
                    nextScale: Float,
                    panDelta: Offset,
                    focus: Offset
                ) {
                    val scaleRatio = nextScale / previousScale
                    val focusFromCenter = focus - viewportSizePx.center()
                    val rawOffset = Offset(
                        x = offset.x * scaleRatio + panDelta.x + focusFromCenter.x * (1f - scaleRatio),
                        y = offset.y * scaleRatio + panDelta.y + focusFromCenter.y * (1f - scaleRatio)
                    )

                    scale = nextScale
                    offset = boundedOffset(rawOffset, nextScale)
                }

                fun applyTransform(
                    zoomFactor: Float,
                    panDelta: Offset,
                    focus: Offset
                ) {
                    val previousScale = scale
                    val nextScale = (previousScale * zoomFactor)
                        .coerceIn(IMAGE_PREVIEW_MIN_SCALE, IMAGE_PREVIEW_MAX_SCALE)
                    applyScaleAndPan(
                        previousScale = previousScale,
                        nextScale = nextScale,
                        panDelta = panDelta,
                        focus = focus
                    )
                }

                fun recordPanSample(timeMillis: Long) {
                    val samples = ArrayDeque(panSamples)
                    samples.addLast(ImagePreviewPanSample(timeMillis, offset))
                    while (samples.size > 2 &&
                        timeMillis - samples.first().timeMillis > IMAGE_PREVIEW_FLING_LOOKBACK_MILLIS
                    ) {
                        samples.removeFirst()
                    }
                    panSamples = samples
                }

                fun startFling(velocityX: Float, velocityY: Float) {
                    val maxOffset = calculateMaxOffset(
                        viewportSize = viewportSizePx,
                        contentSize = fittedImageSizePx,
                        scale = scale,
                        edgeGapPx = edgeGapPx
                    )
                    if (maxOffset == Offset.Zero) return
                    if (Offset(velocityX, velocityY).magnitude() < minimumFlingVelocity) return

                    scroller.fling(
                        offset.x.toInt(),
                        offset.y.toInt(),
                        velocityX.toInt(),
                        velocityY.toInt(),
                        -maxOffset.x.toInt(),
                        maxOffset.x.toInt(),
                        -maxOffset.y.toInt(),
                        maxOffset.y.toInt()
                    )
                    flingToken += 1
                }

                fun startFlingFromPanHistory() {
                    val samples = panSamples
                    if (samples.size < 2) return
                    val first = samples.first()
                    val last = samples.last()
                    val deltaTimeMillis = (last.timeMillis - first.timeMillis).coerceAtLeast(1L)
                    val deltaSeconds = deltaTimeMillis / 1000f
                    val velocity = Offset(
                        x = (last.offset.x - first.offset.x) / deltaSeconds,
                        y = (last.offset.y - first.offset.y) / deltaSeconds
                    )
                    startFling(velocity.x, velocity.y)
                }

                LaunchedEffect(viewportSizePx, fittedImageSizePx, scale) {
                    offset = boundedOffset(offset, scale)
                }

                LaunchedEffect(flingToken) {
                    if (flingToken == 0) return@LaunchedEffect

                    while (scroller.computeScrollOffset()) {
                        offset = boundedOffset(
                            rawOffset = Offset(
                                x = scroller.currX.toFloat(),
                                y = scroller.currY.toFloat()
                            ),
                            scaleValue = scale
                        )
                        withFrameNanos { }
                    }
                }

                var lastScaleFocus by remember { mutableStateOf<Offset?>(null) }
                fun resetDragTracking() {
                    isDragging = false
                    dragStartTouch = null
                    dragAnchorTouch = null
                    dragFilter = null
                    panSamples = ArrayDeque()
                }

                fun toggleDoubleTapZoom(focus: Offset) {
                    stopFling()
                    stopTransition()
                    resetDragTracking()
                    val targetScale = if (scale > IMAGE_PREVIEW_MIN_SCALE + 0.01f) {
                        IMAGE_PREVIEW_MIN_SCALE
                    } else {
                        IMAGE_PREVIEW_DOUBLE_TAP_SCALE.coerceAtMost(IMAGE_PREVIEW_MAX_SCALE)
                    }

                    if (targetScale == IMAGE_PREVIEW_MIN_SCALE) {
                        val startScale = scale
                        val startOffset = offset
                        transitionJob = scope.launch {
                            val progress = Animatable(0f)
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = IMAGE_PREVIEW_DOUBLE_TAP_ANIMATION_MS)
                            ) {
                                val t = value
                                scale = lerp(startScale, IMAGE_PREVIEW_MIN_SCALE, t)
                                offset = boundedOffset(lerp(startOffset, Offset.Zero, t), scale)
                            }
                            scale = IMAGE_PREVIEW_MIN_SCALE
                            offset = Offset.Zero
                            transitionJob = null
                        }
                    } else {
                        val startScale = scale
                        val startOffset = offset
                        val targetOffset = calculateTargetOffsetForScale(
                            currentOffset = offset,
                            previousScale = scale,
                            nextScale = targetScale,
                            focus = focus,
                            viewportSize = viewportSizePx
                        ).let { boundedOffset(it, targetScale) }
                        transitionJob = scope.launch {
                            val progress = Animatable(0f)
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = IMAGE_PREVIEW_DOUBLE_TAP_ANIMATION_MS)
                            ) {
                                val t = value
                                scale = lerp(startScale, targetScale, t)
                                offset = boundedOffset(lerp(startOffset, targetOffset, t), scale)
                            }
                            scale = targetScale
                            offset = targetOffset
                            transitionJob = null
                        }
                    }
                }

                val scaleGestureDetector = ScaleGestureDetector(
                    context,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                            stopFling()
                            stopTransition()
                            resetDragTracking()
                            lastScaleFocus = Offset(detector.focusX, detector.focusY)
                            return true
                        }

                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            val currentFocus = Offset(detector.focusX, detector.focusY)
                            val panDelta = lastScaleFocus?.let { currentFocus - it } ?: Offset.Zero
                            lastScaleFocus = currentFocus
                            applyTransform(
                                zoomFactor = detector.scaleFactor,
                                panDelta = panDelta,
                                focus = currentFocus
                            )
                            return true
                        }

                        override fun onScaleEnd(detector: ScaleGestureDetector) {
                            lastScaleFocus = null
                        }
                    }
                )
                val gestureDetector = remember(context) {
                    GestureDetector(
                        context,
                        object : GestureDetector.SimpleOnGestureListener() {
                            override fun onDown(e: MotionEvent): Boolean = true

                            override fun onDoubleTap(e: MotionEvent): Boolean {
                                toggleDoubleTapZoom(Offset(e.x, e.y))
                                return true
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInteropFilter { motionEvent ->
                            val gestureHandled = gestureDetector.onTouchEvent(motionEvent)
                            scaleGestureDetector.onTouchEvent(motionEvent)

                            when (motionEvent.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    stopFling()
                                    if (!gestureHandled) {
                                        stopTransition()
                                    }
                                    activePointerId = motionEvent.getPointerId(0)
                                    val touch = Offset(motionEvent.x, motionEvent.y)
                                    dragStartTouch = touch
                                    dragAnchorTouch = touch
                                    isDragging = false
                                    dragFilter = OneEuroOffsetFilter()
                                    panSamples = ArrayDeque()
                                }

                                MotionEvent.ACTION_POINTER_DOWN -> {
                                    resetDragTracking()
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    if (scaleGestureDetector.isInProgress) {
                                        resetDragTracking()
                                    } else {
                                        val pointerIndex = motionEvent.findPointerIndex(activePointerId)
                                        if (pointerIndex >= 0) {
                                            val rawTouch = Offset(
                                                motionEvent.getX(pointerIndex),
                                                motionEvent.getY(pointerIndex)
                                            )
                                            val filteredTouch = (dragFilter ?: OneEuroOffsetFilter().also {
                                                dragFilter = it
                                            }).filter(rawTouch, motionEvent.eventTime)
                                            val startTouch = dragStartTouch ?: filteredTouch
                                            val anchorTouch = dragAnchorTouch ?: filteredTouch
                                            val totalDelta = filteredTouch - startTouch

                                            if (!isDragging) {
                                                if (totalDelta.magnitude() > touchSlop) {
                                                    isDragging = true
                                                    dragAnchorTouch = filteredTouch
                                                    panSamples = ArrayDeque<ImagePreviewPanSample>().apply {
                                                        addLast(ImagePreviewPanSample(motionEvent.eventTime, offset))
                                                    }
                                                }
                                            } else {
                                                val dragFromAnchor = filteredTouch - anchorTouch
                                                val releasedDelta = dragFromAnchor.releaseDeadband(dragReleaseThresholdPx)
                                                if (releasedDelta != Offset.Zero) {
                                                    applyTransform(
                                                        zoomFactor = 1f,
                                                        panDelta = releasedDelta,
                                                        focus = filteredTouch
                                                    )
                                                    dragAnchorTouch = anchorTouch + releasedDelta
                                                    recordPanSample(motionEvent.eventTime)
                                                }
                                            }
                                        }
                                    }
                                }

                                MotionEvent.ACTION_POINTER_UP -> {
                                    val liftedPointerId = motionEvent.getPointerId(motionEvent.actionIndex)
                                    if (liftedPointerId == activePointerId) {
                                        val replacementIndex = if (motionEvent.actionIndex == 0) 1 else 0
                                        if (replacementIndex < motionEvent.pointerCount) {
                                            activePointerId = motionEvent.getPointerId(replacementIndex)
                                            val replacementTouch = Offset(
                                                motionEvent.getX(replacementIndex),
                                                motionEvent.getY(replacementIndex)
                                            )
                                            dragStartTouch = replacementTouch
                                            dragAnchorTouch = replacementTouch
                                            isDragging = false
                                            dragFilter = OneEuroOffsetFilter()
                                            panSamples = ArrayDeque()
                                        } else {
                                            activePointerId = MotionEvent.INVALID_POINTER_ID
                                            resetDragTracking()
                                        }
                                    }
                                }

                                MotionEvent.ACTION_UP -> {
                                    if (!scaleGestureDetector.isInProgress && isDragging) {
                                        recordPanSample(motionEvent.eventTime)
                                        startFlingFromPanHistory()
                                    }
                                    lastScaleFocus = null
                                    activePointerId = MotionEvent.INVALID_POINTER_ID
                                    resetDragTracking()
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    lastScaleFocus = null
                                    activePointerId = MotionEvent.INVALID_POINTER_ID
                                    resetDragTracking()
                                }
                            }

                            true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = image.bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(imageWidthDp)
                            .height(imageHeightDp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }
            }

            IconButton(
                onClick = requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = topBarPadding, end = 12.dp)
                    .size(DefaultAppBarButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.action_cancel),
                    tint = contentColor
                )
            }
        }
    }
}

private fun fitImageIntoViewport(
    viewportSize: Size,
    imageAspectRatio: Float
): Size {
    if (viewportSize.width <= 0f || viewportSize.height <= 0f || imageAspectRatio <= 0f) {
        return Size.Zero
    }

    val viewportAspectRatio = viewportSize.width / viewportSize.height
    return if (viewportAspectRatio > imageAspectRatio) {
        Size(
            width = viewportSize.height * imageAspectRatio,
            height = viewportSize.height
        )
    } else {
        Size(
            width = viewportSize.width,
            height = viewportSize.width / imageAspectRatio
        )
    }
}

private fun calculateMaxOffset(
    viewportSize: Size,
    contentSize: Size,
    scale: Float,
    edgeGapPx: Float
): Offset {
    val scaledWidth = contentSize.width * scale
    val scaledHeight = contentSize.height * scale
    val maxX = if (scaledWidth > viewportSize.width) {
        ((scaledWidth - viewportSize.width) / 2f) + edgeGapPx
    } else {
        0f
    }
    val maxY = if (scaledHeight > viewportSize.height) {
        ((scaledHeight - viewportSize.height) / 2f) + edgeGapPx
    } else {
        0f
    }
    return Offset(maxX, maxY)
}

private fun Offset.clamp(maxOffset: Offset): Offset = Offset(
    x = x.coerceIn(-maxOffset.x, maxOffset.x),
    y = y.coerceIn(-maxOffset.y, maxOffset.y)
)

private fun calculateTargetOffsetForScale(
    currentOffset: Offset,
    previousScale: Float,
    nextScale: Float,
    focus: Offset,
    viewportSize: Size
): Offset {
    val scaleRatio = nextScale / previousScale
    val focusFromCenter = focus - viewportSize.center()
    return Offset(
        x = currentOffset.x * scaleRatio + focusFromCenter.x * (1f - scaleRatio),
        y = currentOffset.y * scaleRatio + focusFromCenter.y * (1f - scaleRatio)
    )
}

private fun Offset.magnitude(): Float = hypot(x, y)

private fun Size.center(): Offset = Offset(x = width / 2f, y = height / 2f)

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun lerp(start: Offset, stop: Offset, fraction: Float): Offset = Offset(
    x = lerp(start.x, stop.x, fraction),
    y = lerp(start.y, stop.y, fraction)
)

private fun Offset.releaseDeadband(deadband: Float): Offset {
    val distance = magnitude()
    if (distance <= deadband || distance == 0f) return Offset.Zero
    val scale = (distance - deadband) / distance
    return Offset(x * scale, y * scale)
}

private class OneEuroOffsetFilter(
    minCutoff: Float = 1.15f,
    beta: Float = 0.03f,
    derivativeCutoff: Float = 1f
) {
    private val xFilter = OneEuroFilter1D(minCutoff, beta, derivativeCutoff)
    private val yFilter = OneEuroFilter1D(minCutoff, beta, derivativeCutoff)

    fun filter(point: Offset, timeMillis: Long): Offset = Offset(
        x = xFilter.filter(point.x, timeMillis),
        y = yFilter.filter(point.y, timeMillis)
    )
}

private class OneEuroFilter1D(
    private val minCutoff: Float,
    private val beta: Float,
    private val derivativeCutoff: Float
) {
    private var lastTimeMillis: Long? = null
    private val valueFilter = LowPassFilter()
    private val derivativeFilter = LowPassFilter()
    private var lastRawValue = 0f

    fun filter(value: Float, timeMillis: Long): Float {
        val previousTimeMillis = lastTimeMillis
        if (previousTimeMillis == null) {
            lastTimeMillis = timeMillis
            lastRawValue = value
            return valueFilter.filter(value, 1f)
        }

        val deltaTimeSeconds = ((timeMillis - previousTimeMillis).coerceAtLeast(1L)) / 1000f
        lastTimeMillis = timeMillis

        val rawDerivative = (value - lastRawValue) / deltaTimeSeconds
        lastRawValue = value

        val filteredDerivative = derivativeFilter.filter(
            value = rawDerivative,
            alpha = smoothingFactor(deltaTimeSeconds, derivativeCutoff)
        )
        val cutoff = minCutoff + beta * abs(filteredDerivative)
        return valueFilter.filter(
            value = value,
            alpha = smoothingFactor(deltaTimeSeconds, cutoff)
        )
    }
}

private class LowPassFilter {
    private var hasValue = false
    private var previous = 0f

    fun filter(value: Float, alpha: Float): Float {
        val clampedAlpha = alpha.coerceIn(0f, 1f)
        if (!hasValue) {
            hasValue = true
            previous = value
            return value
        }
        previous = clampedAlpha * value + (1f - clampedAlpha) * previous
        return previous
    }
}

private fun smoothingFactor(deltaTimeSeconds: Float, cutoff: Float): Float {
    val effectiveCutoff = cutoff.coerceAtLeast(0.0001f)
    val rate = 2f * PI.toFloat() * effectiveCutoff * deltaTimeSeconds
    return rate / (rate + 1f)
}
