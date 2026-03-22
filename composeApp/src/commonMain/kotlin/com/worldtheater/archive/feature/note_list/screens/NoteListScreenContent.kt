package com.worldtheater.archive.feature.note_list.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.worldtheater.archive.ui.theme.appOnSurfaceA45
import com.worldtheater.archive.ui.theme.appSurfaceA45
import com.worldtheater.archive.ui.widget.capsuleBgColor
import com.worldtheater.archive.ui.widget.capsuleContentColor
import com.worldtheater.archive.ui.widget.offsetShadow

@Composable
fun NoteListScreenContent(
    modifier: Modifier = Modifier,
    isSearchActive: Boolean,
    isMoveMode: Boolean,
    showBackToTopFab: Boolean,
    isMoveFabEnabled: Boolean,
    fabIcon: ImageVector,
    fabDescription: String,
    backToTopDescription: String,
    onFabClick: () -> Unit,
    onBackToTopClick: () -> Unit,
    moveFabContainerColor: Color,
    moveFabContentColor: Color,
    moveModeTitle: String,
    moveModeHint: String,
    mainContent: @Composable BoxScope.() -> Unit,
    topBarContent: @Composable BoxScope.() -> Unit,
    dialogContent: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = showBackToTopFab,
                        enter = fadeIn(animationSpec = tween(160)),
                        exit = fadeOut(animationSpec = tween(120))
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            FloatingActionButton(
                                onClick = onBackToTopClick,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.offsetShadow(),
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                                containerColor = capsuleBgColor(),
                                contentColor = capsuleContentColor()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowUpward,
                                    contentDescription = backToTopDescription
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (!isSearchActive || isMoveMode) {
                        if (!isMoveFabEnabled && isMoveMode) {
                            Surface(
                                modifier = Modifier
                                    .size(56.dp)
                                    .offsetShadow(),
                                shape = RoundedCornerShape(50),
                                color = moveFabContainerColor.takeIf { it != Color.Unspecified } ?: appSurfaceA45(),
                                contentColor = moveFabContentColor.takeIf { it != Color.Unspecified }
                                    ?: appOnSurfaceA45()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = fabIcon,
                                        contentDescription = fabDescription
                                    )
                                }
                            }
                        } else {
                            FloatingActionButton(
                                onClick = onFabClick,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.offsetShadow(),
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                                containerColor = moveFabContainerColor,
                                contentColor = moveFabContentColor
                            ) {
                                Icon(
                                    imageVector = fabIcon,
                                    contentDescription = fabDescription
                                )
                            }
                        }
                    }
                }
            }
        ) { _ ->
            Box(
                modifier = modifier
                    .fillMaxSize()
            ) {
                mainContent()
                topBarContent()
            }
        }

        AnimatedVisibility(
            visible = isMoveMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 102.dp),
            enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(220)
            ),
            exit = fadeOut(animationSpec = tween(140)) + slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(180)
            )
        ) {
            MoveModeFloatingHint(
                title = moveModeTitle,
                hint = moveModeHint
            )
        }

        dialogContent()
    }
}
