package com.worldtheater.archive.ui.widget

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.platform.system.AppBackHandler
import com.worldtheater.archive.ui.theme.EmphasizedAccelerateEasing
import com.worldtheater.archive.ui.theme.EmphasizedDecelerateEasing

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    enableSlideAnimation: Boolean = true,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable (triggerClose: (() -> Unit) -> Unit) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val onDismissState = rememberUpdatedState(onDismissRequest)

    val transitionState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    var pendingClose by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestClose(closeAction: () -> Unit) {
        if (!transitionState.currentState || !transitionState.targetState) return
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        pendingClose = closeAction
        transitionState.targetState = false
    }

    LaunchedEffect(transitionState.isIdle, transitionState.currentState) {
        if (transitionState.isIdle && !transitionState.currentState) {
            pendingClose?.invoke()
            pendingClose = null
        }
    }

    AppBackHandler(enabled = transitionState.currentState && dismissOnBackPress) {
        requestClose { onDismissState.value.invoke() }
    }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = fadeOut(animationSpec = tween(durationMillis = 150))
    ) {
        val density = LocalDensity.current
        val imeBottomDp = with(density) {
            WindowInsets.ime.getBottom(this).toDp()
        }

        val animatedImeBottom by animateDpAsState(
            targetValue = imeBottomDp,
            animationSpec = spring(
                dampingRatio = 1.0f,
                stiffness = 700f,
                visibilityThreshold = Dp.VisibilityThreshold
            ),
            label = "ImeBottomAnim"
        )

        val safeAnimatedImeBottom = animatedImeBottom.coerceAtLeast(0.dp)

        var cardBoundsInParent by remember { mutableStateOf<Rect?>(null) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(dialogDimColor()),
            contentAlignment = Alignment.Center
        ) {
            val horizontalMargin = 24.dp
            val maxDialogWidth = 420.dp
            val maxByScreen = (maxWidth - horizontalMargin * 2).coerceAtLeast(0.dp)
            val targetWidth = if (maxByScreen < maxDialogWidth) maxByScreen else maxDialogWidth

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dismissOnClickOutside) {
                        if (dismissOnClickOutside) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val bounds = cardBoundsInParent
                                    if (bounds != null && !bounds.contains(tapOffset)) {
                                        requestClose { onDismissState.value.invoke() }
                                    }
                                }
                            )
                        }
                    }
            )
            Surface(
                shadowElevation = 0.dp,
                shape = Color.Transparent.let { RectangleShape },
                color = Color.Transparent,
                modifier = Modifier
                    .width(targetWidth)
                    .padding(bottom = safeAnimatedImeBottom)
                    .onGloballyPositioned { coordinates ->
                        cardBoundsInParent = coordinates.boundsInParent()
                    }
                    .let {
                        if (enableSlideAnimation) {
                            it.animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { with(density) { 40.dp.roundToPx() } },
                                    animationSpec = tween(
                                        durationMillis = 220,
                                        easing = EmphasizedDecelerateEasing
                                    )
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { with(density) { 40.dp.roundToPx() } },
                                    animationSpec = tween(
                                        durationMillis = 180,
                                        easing = EmphasizedAccelerateEasing
                                    )
                                )
                            )
                        } else {
                            it
                        }
                    }
            ) {
                // Pass the requestClose function to the content so it can trigger the exit animation
                content { action -> requestClose(action) }
            }
        }
    }
}
