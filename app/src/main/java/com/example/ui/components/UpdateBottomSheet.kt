package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GitHubReleaseInfo
import com.example.data.GitHubUpdater
import com.example.data.UpdateUiState
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    installedVersion: String,
    updateState: UpdateUiState,
    release: GitHubReleaseInfo,
    onUpdateNow: () -> Unit,
    onOpenInstallPermission: () -> Unit,
    onInstallDownloaded: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDownloading = updateState is UpdateUiState.Downloading

    ModalBottomSheet(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        sheetState = sheetState,
        containerColor = if (palette.isLiquidGlass) Color(0xFA090B10) else palette.surface,
        contentColor = palette.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.76f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(palette.borderHighlight)
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with themed Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(palette.primary.copy(alpha = 0.24f), palette.primaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NewReleases,
                        contentDescription = "Update Available",
                        tint = palette.primary,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Update Available",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "v$installedVersion",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primaryContainer
                        ) {
                            Text(
                                text = release.tagName,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                        if (release.apkSizeBytes > 0L) {
                            Text(
                                text = "· ${GitHubUpdater.formatBytesReadable(release.apkSizeBytes)}",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.5.sp,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }

            // State-specific sections: Permission Warning / Download Progress / Error
            when (updateState) {
                is UpdateUiState.NeedsInstallPermission -> {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.primaryContainer.copy(alpha = 0.78f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Allow Focus Lock to install app updates in Android Settings to continue.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                is UpdateUiState.Downloading -> {
                    val animatedProgress by animateFloatAsState(
                        targetValue = updateState.progress.coerceIn(0f, 1f),
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "apk_download_progress"
                    )
                    val pct = (animatedProgress * 100).toInt()
                    val barBrush = remember(palette) {
                        Brush.horizontalGradient(
                            colors = listOf(palette.primaryDeep, palette.primary)
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading ${release.apkFileName}...",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (updateState.totalBytes > 0L) {
                                    "$pct% · ${GitHubUpdater.formatBytesReadable(updateState.downloadedBytes)}"
                                } else {
                                    GitHubUpdater.formatBytesReadable(updateState.downloadedBytes)
                                },
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(palette.surfaceElevated)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        scaleX = animatedProgress.coerceAtLeast(0.03f)
                                    }
                                    .clip(RoundedCornerShape(50))
                                    .background(barBrush)
                            )
                        }
                    }
                }

                is UpdateUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = updateState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.primary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                else -> Unit
            }

            // Primary & Secondary Action Buttons
            val ctaInteraction = remember { MutableInteractionSource() }
            val ctaScale = rememberSpringPressScale(ctaInteraction)

            when (updateState) {
                is UpdateUiState.NeedsInstallPermission -> {
                    Button(
                        onClick = {
                            haptics.tap()
                            onOpenInstallPermission()
                        },
                        interactionSource = ctaInteraction,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = ctaScale
                                scaleY = ctaScale
                            }
                            .testTag("grant_install_permission_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Allow & Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is UpdateUiState.ReadyToInstall -> {
                    Button(
                        onClick = {
                            haptics.confirm()
                            onInstallDownloaded()
                        },
                        interactionSource = ctaInteraction,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = ctaScale
                                scaleY = ctaScale
                            }
                            .testTag("install_apk_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Install Update",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is UpdateUiState.Downloading -> {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Downloading Release...",
                                color = palette.textSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            haptics.confirm()
                            onUpdateNow()
                        },
                        interactionSource = ctaInteraction,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = ctaScale
                                scaleY = ctaScale
                            }
                            .testTag("update_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update Now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!isDownloading) {
                TextButton(
                    onClick = {
                        haptics.tick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("update_later_button")
                ) {
                    Text(
                        text = "Later",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 14.sp,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}
