package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AllowedApp
import com.example.data.FocusSettingsRepository
import com.example.data.LockAppLayout
import com.example.data.NotificationBlockerRepository
import com.example.data.ScheduledLockLayoutConfig
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Redesigned Scheduled Focus Lock Screen.
 *
 * Built around a unified "Sanctuary Monolith" architectural layout:
 * - Borderless editorial ambient header (live status + clock/date + schedule title)
 * - Single cohesive Monolith card housing the theme-adaptive hero ring, integrated
 *   session window progress rail, and concentric 3-column bento telemetry strip
 * - Optional matching Essential Apps companion tray and quiet auto-unlock footer
 *
 * Every container shares concentric corner geometry, top-lit hairline rim lighting,
 * and strictly inherits the active [AppVisualTheme] typography, surfaces, and ring style.
 */
@Composable
fun ScheduledLockScreen(
    scheduleName: String,
    @Suppress("UNUSED_PARAMETER") timeRangeLabel: String,
    startTimeMs: Long,
    endTimeMs: Long,
    @Suppress("UNUSED_PARAMETER") layoutConfig: ScheduledLockLayoutConfig,
    dailyGoalPercent: Int,
    currentStreak: Int,
    allowedApps: List<AllowedApp>,
    appLayout: LockAppLayout,
    pillButtonSizeDp: Int = FocusSettingsRepository.DEFAULT_PILL_BUTTON_SIZE_DP,
    iconCache: Map<String, ImageBitmap> = emptyMap(),
    isPreview: Boolean = false,
    onLaunchApp: ((AllowedApp) -> Unit)? = null,
    onTimerFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val lockPalette = remember(palette) { LockPalette.fromFocusTheme(palette) }
    val blockerState by NotificationBlockerRepository.observe(context).collectAsState()

    var nowMs by remember(startTimeMs, endTimeMs, isPreview) {
        mutableLongStateOf(
            if (isPreview) startTimeMs + ((endTimeMs - startTimeMs) * 0.34f).toLong()
            else System.currentTimeMillis()
        )
    }

    LaunchedEffect(endTimeMs, isPreview) {
        if (!isPreview) {
            while (true) {
                val current = System.currentTimeMillis()
                nowMs = current
                if (endTimeMs > 0L && current >= endTimeMs) {
                    onTimerFinished?.invoke()
                    break
                }
                delay(1000L)
            }
        }
    }

    val totalDurationMs = (endTimeMs - startTimeMs).coerceAtLeast(60_000L)
    val elapsedMs = (nowMs - startTimeMs).coerceIn(0L, totalDurationMs)
    val remainingMs = (endTimeMs - nowMs).coerceAtLeast(0L)
    val progressFraction = (1f - (remainingMs.toFloat() / totalDurationMs.toFloat())).coerceIn(0f, 1f)
    val elapsedMins = (elapsedMs / 60_000L).coerceAtLeast(0L)

    val startClockText = remember(startTimeMs) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(startTimeMs))
    }
    val endClockText = remember(endTimeMs) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(endTimeMs))
    }
    val liveClockText = remember(nowMs / 30_000L) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(nowMs))
    }
    val liveDateText = remember(nowMs / 3_600_000L) {
        SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(nowMs)).uppercase(Locale.US)
    }
    val silencedCount = if (isPreview) 3 else blockerState.activeSessionBlocked.size
    val scale = if (isPreview) 0.68f else 1f

    val totalSec = remainingMs / 1000L
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    val formattedRemaining = if (hours > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
    val percentDone = (progressFraction * 100f).roundToInt().coerceIn(0, 100)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag(if (isPreview) "scheduled_lock_preview_canvas" else "scheduled_lock_screen_root")
    ) {
        ThemeBackdropLayer(palette = palette)

        // Soft centered focal aura behind the monolith to anchor depth
        SanctuaryAmbientHalo(
            accent = palette.primary,
            alpha = if (palette.isLiquidGlass) 0.085f else 0.11f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isPreview) {
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    } else {
                        Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 20.dp)
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((16f * scale).dp)
            ) {
                // 1. Borderless Editorial Ambient Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((6f * scale).dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .testTag("scheduled_lock_header")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((7f * scale).dp)
                    ) {
                        StatusPulseDot(
                            color = palette.primary,
                            scale = scale
                        )
                        Text(
                            text = "FOCUS LOCKED",
                            fontFamily = JetBrainsMono,
                            fontSize = (10.5f * scale).coerceAtLeast(7.5f).sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.8.sp,
                            color = palette.primary
                        )
                        Box(
                            modifier = Modifier
                                .size((3f * scale).coerceAtLeast(2f).dp)
                                .clip(CircleShape)
                                .background(palette.textSecondary.copy(alpha = 0.45f))
                        )
                        Text(
                            text = "$liveClockText · $liveDateText",
                            fontFamily = JetBrainsMono,
                            fontSize = (10.5f * scale).coerceAtLeast(7.5f).sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = scheduleName.ifBlank { "Scheduled Focus" },
                        fontFamily = palette.displayFont,
                        fontSize = (26f * scale).coerceAtLeast(16f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 2. Unified Sanctuary Monolith Card (Timer Ring + Window Rail + Integrated Bento Metrics)
                SanctuaryMonolithCard(
                    palette = palette,
                    cornerRadius = (32f * scale).coerceAtLeast(20f).dp,
                    contentPaddingHorizontal = (20f * scale).coerceAtLeast(12f).dp,
                    contentPaddingVertical = (22f * scale).coerceAtLeast(14f).dp,
                    verticalSpacing = (18f * scale).coerceAtLeast(11f).dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scheduled_lock_monolith_card")
                ) {
                    // Hero Theme-Adaptive Timer Ring
                    val ringSize = (208f * scale).coerceIn(116f, 220f).dp
                    ThemedTimerRing(
                        progress = progressFraction.coerceAtLeast(0.02f),
                        size = ringSize,
                        palette = palette
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((4f * scale).coerceAtLeast(2f).dp)
                        ) {
                            Text(
                                text = "REMAINING",
                                fontFamily = JetBrainsMono,
                                fontSize = (9.5f * scale).coerceAtLeast(6.5f).sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.9.sp,
                                color = palette.textSecondary.copy(alpha = 0.78f)
                            )
                            Text(
                                text = formattedRemaining,
                                fontFamily = palette.numeralFont,
                                fontSize = ((if (hours > 0L) 30f else 38f) * scale).coerceAtLeast(18f).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                color = palette.textPrimary,
                                maxLines = 1,
                                modifier = Modifier.testTag("scheduled_lock_countdown_text")
                            )
                            Text(
                                text = "$percentDone%",
                                fontFamily = JetBrainsMono,
                                fontSize = (11f * scale).coerceAtLeast(7.5f).sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = palette.primary
                            )
                        }
                    }

                    // Minimal inline session window strip (circular ring is the sole progress bar)
                    SessionWindowStrip(
                        startClockText = startClockText,
                        endClockText = endClockText,
                        elapsedMins = elapsedMins,
                        scale = scale,
                        palette = palette,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ThemedDivider(palette = palette)

                    // Concentric 3-Tile Bento Telemetry Row (Goal · Streak · Silenced)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scheduled_lock_telemetry_row"),
                        horizontalArrangement = Arrangement.spacedBy((10f * scale).coerceAtLeast(6f).dp)
                    ) {
                        val goalAchieved = dailyGoalPercent >= 100
                        SanctuaryBentoTile(
                            icon = if (goalAchieved) Icons.Outlined.CheckCircle else Icons.Outlined.Flag,
                            iconTint = if (goalAchieved) palette.success else palette.primary,
                            value = "$dailyGoalPercent%",
                            valueColor = if (goalAchieved) palette.success else palette.textPrimary,
                            label = "GOAL",
                            scale = scale,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scheduled_tile_goal")
                        )
                        SanctuaryBentoTile(
                            icon = Icons.Outlined.LocalFireDepartment,
                            iconTint = palette.warning,
                            value = "${currentStreak}d",
                            valueColor = palette.textPrimary,
                            label = "STREAK",
                            scale = scale,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scheduled_tile_streak")
                        )
                        SanctuaryBentoTile(
                            icon = Icons.Outlined.NotificationsOff,
                            iconTint = if (silencedCount > 0) palette.primary else palette.textSecondary,
                            value = "$silencedCount",
                            valueColor = palette.textPrimary,
                            label = "SILENCED",
                            scale = scale,
                            palette = palette,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scheduled_tile_silenced")
                        )
                    }
                }

                // 3. Matching Companion Essential Apps Tray (Only shown when allowed apps exist)
                if (allowedApps.isNotEmpty()) {
                    SanctuaryMonolithCard(
                        palette = palette,
                        cornerRadius = (26f * scale).coerceAtLeast(18f).dp,
                        contentPaddingHorizontal = (16f * scale).coerceAtLeast(10f).dp,
                        contentPaddingVertical = (14f * scale).coerceAtLeast(9f).dp,
                        verticalSpacing = (10f * scale).coerceAtLeast(6f).dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scheduled_lock_apps_tray")
                    ) {
                        Text(
                            text = "ESSENTIAL ACCESS",
                            fontFamily = JetBrainsMono,
                            fontSize = (9.5f * scale).coerceAtLeast(6.8f).sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.8.sp,
                            color = palette.textSecondary.copy(alpha = 0.72f)
                        )
                        LockAllowedAppsSection(
                            apps = if (isPreview) allowedApps.take(3) else allowedApps,
                            layout = appLayout,
                            palette = lockPalette,
                            onAppClick = { app -> onLaunchApp?.invoke(app) },
                            isPreview = isPreview,
                            pillButtonSizeDp = pillButtonSizeDp.coerceIn(
                                FocusSettingsRepository.MIN_PILL_BUTTON_SIZE_DP,
                                FocusSettingsRepository.MAX_PILL_BUTTON_SIZE_DP
                            ),
                            iconCache = iconCache,
                            readFromCacheOnly = isPreview
                        )
                    }
                }

                // 4. Quiet Borderless Auto-Unlock Caption
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6f * scale).coerceAtLeast(4f).dp),
                    modifier = Modifier
                        .padding(top = (2f * scale).dp)
                        .testTag("scheduled_lock_unlock_footer")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockClock,
                        contentDescription = null,
                        tint = palette.textSecondary.copy(alpha = 0.65f),
                        modifier = Modifier.size((13f * scale).coerceAtLeast(9f).dp)
                    )
                    Text(
                        text = "Unlocks automatically at $endClockText",
                        fontFamily = JetBrainsMono,
                        fontSize = (11f * scale).coerceAtLeast(7.8f).sp,
                        color = palette.textSecondary.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Unified Sanctuary Completion Screen shown when a scheduled focus timer finishes.
 * Stays visible until the user explicitly taps the Done button to dismiss it.
 */
@Composable
fun ScheduledSuccessScreen(
    scheduleName: String,
    durationMs: Long,
    encouragementMessage: String = "Well done — you stayed locked in and finished your focus block.",
    dailyGoalPercent: Int? = null,
    currentStreak: Int? = null,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()

    val durationShort = remember(durationMs) {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
        val totalMinutes = totalSeconds / 60L
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        when {
            hours > 0L && mins > 0L -> "${hours}h ${mins}m"
            hours > 0L -> "${hours}h"
            totalMinutes > 0L -> "${totalMinutes}m"
            else -> "${totalSeconds}s"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("scheduled_success_screen")
    ) {
        ThemeBackdropLayer(palette = palette)

        SanctuaryAmbientHalo(
            accent = palette.success,
            alpha = if (palette.isLiquidGlass) 0.09f else 0.12f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Borderless Ambient Status Eyebrow matching ScheduledLockScreen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPulseDot(
                        color = palette.success,
                        scale = 1f
                    )
                    Text(
                        text = "SESSION COMPLETE",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = palette.success
                    )
                }

                // Unified Completion Monolith Card
                SanctuaryMonolithCard(
                    palette = palette,
                    cornerRadius = 32.dp,
                    contentPaddingHorizontal = 22.dp,
                    contentPaddingVertical = 28.dp,
                    verticalSpacing = 20.dp,
                    accentRim = palette.success,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Theme-Adaptive Completion Halo Emblem
                    CompletionHaloEmblem(palette = palette)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scheduleName.ifBlank { "Scheduled Focus" },
                            fontFamily = palette.displayFont,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            color = palette.textPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = encouragementMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = palette.bodyFont,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("scheduled_success_message")
                        )
                    }

                    ThemedDivider(palette = palette)

                    // Integrated Concentric Bento Recap Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SanctuaryBentoTile(
                            icon = Icons.Outlined.Schedule,
                            iconTint = palette.primary,
                            value = durationShort,
                            valueColor = palette.textPrimary,
                            label = "FOCUSED",
                            scale = 1f,
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                        if (dailyGoalPercent != null) {
                            SanctuaryBentoTile(
                                icon = Icons.Outlined.CheckCircle,
                                iconTint = palette.success,
                                value = "$dailyGoalPercent%",
                                valueColor = if (dailyGoalPercent >= 100) palette.success else palette.textPrimary,
                                label = "GOAL",
                                scale = 1f,
                                palette = palette,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            SanctuaryBentoTile(
                                icon = Icons.Outlined.CheckCircle,
                                iconTint = palette.success,
                                value = "Done",
                                valueColor = palette.success,
                                label = "STATUS",
                                scale = 1f,
                                palette = palette,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (currentStreak != null) {
                            SanctuaryBentoTile(
                                icon = Icons.Outlined.LocalFireDepartment,
                                iconTint = palette.warning,
                                value = "${currentStreak}d",
                                valueColor = palette.textPrimary,
                                label = "STREAK",
                                scale = 1f,
                                palette = palette,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Manual Dismiss Action Button (replaces 3-second auto-removal)
                    val dismissInteraction = remember { MutableInteractionSource() }
                    val dismissScale = rememberSpringPressScale(dismissInteraction)
                    Button(
                        onClick = {
                            haptics.confirm()
                            onDismiss()
                        },
                        interactionSource = dismissInteraction,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .graphicsLayer {
                                scaleX = dismissScale
                                scaleY = dismissScale
                            }
                            .testTag("scheduled_success_dismiss_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Done",
                            fontFamily = palette.bodyFont,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SanctuaryAmbientHalo(
    accent: Color,
    alpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val center = Offset(size.width / 2f, size.height * 0.42f)
                val radius = (minOf(size.width, size.height) * 0.62f).coerceAtLeast(1f)
                val brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = alpha),
                        accent.copy(alpha = alpha * 0.28f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                )
                onDrawBehind {
                    drawCircle(brush = brush, radius = radius, center = center)
                }
            }
    )
}

@Composable
internal fun StatusPulseDot(
    color: Color,
    scale: Float = 1f
) {
    Box(
        modifier = Modifier
            .size((12f * scale).coerceAtLeast(8f).dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((5.5f * scale).coerceAtLeast(3.8f).dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
internal fun SanctuaryMonolithCard(
    palette: FocusThemePalette,
    cornerRadius: Dp,
    contentPaddingHorizontal: Dp,
    contentPaddingVertical: Dp,
    verticalSpacing: Dp,
    accentRim: Color? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val rimTopColor = accentRim?.copy(alpha = 0.34f) ?: palette.borderHighlight.copy(
        alpha = if (palette.isLiquidGlass) 0.34f else 0.48f
    )
    val rimMidColor = palette.border.copy(alpha = if (palette.isLiquidGlass) 0.16f else 0.34f)
    val rimBrush = remember(rimTopColor, rimMidColor) {
        Brush.verticalGradient(
            0.0f to rimTopColor,
            0.45f to rimMidColor,
            1.0f to Color.Transparent
        )
    }

    Column(
        modifier = modifier
            .focusGlassPanel(
                shape = shape,
                palette = palette,
                elevated = false,
                highlightAccent = accentRim?.copy(alpha = 0.55f)
            )
            .border(width = 1.dp, brush = rimBrush, shape = shape)
            .padding(horizontal = contentPaddingHorizontal, vertical = contentPaddingVertical),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content
    )
}

@Composable
internal fun SanctuaryBentoTile(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    valueColor: Color,
    label: String,
    scale: Float,
    palette: FocusThemePalette,
    modifier: Modifier = Modifier
) {
    val tileShape = remember(scale) {
        RoundedCornerShape((20f * scale).coerceAtLeast(13f).dp)
    }
    val fill = if (palette.isLiquidGlass) {
        Color(0x6606080E)
    } else {
        palette.background.copy(alpha = 0.44f)
    }
    val strokeColor = palette.border.copy(alpha = if (palette.isLiquidGlass) 0.24f else 0.62f)

    Column(
        modifier = modifier
            .clip(tileShape)
            .background(fill, tileShape)
            .border(width = 0.75.dp, color = strokeColor, shape = tileShape)
            .padding(
                horizontal = (8f * scale).coerceAtLeast(5f).dp,
                vertical = (12f * scale).coerceAtLeast(7f).dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((4f * scale).coerceAtLeast(2f).dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size((15f * scale).coerceAtLeast(10f).dp)
        )
        Text(
            text = value,
            fontFamily = palette.numeralFont,
            fontSize = (16.5f * scale).coerceAtLeast(10.5f).sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontFamily = JetBrainsMono,
            fontSize = (9f * scale).coerceAtLeast(6.5f).sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
            color = palette.textSecondary.copy(alpha = 0.82f),
            maxLines = 1
        )
    }
}

@Composable
private fun SessionWindowStrip(
    startClockText: String,
    endClockText: String,
    elapsedMins: Long,
    scale: Float,
    palette: FocusThemePalette,
    modifier: Modifier = Modifier
) {
    val pillShape = remember(scale) { RoundedCornerShape(50) }
    val fill = if (palette.isLiquidGlass) {
        Color(0x5206080E)
    } else {
        palette.background.copy(alpha = 0.42f)
    }
    val borderColor = palette.border.copy(alpha = if (palette.isLiquidGlass) 0.24f else 0.56f)

    Row(
        modifier = modifier
            .clip(pillShape)
            .background(fill, pillShape)
            .border(width = 0.75.dp, color = borderColor, shape = pillShape)
            .padding(
                horizontal = (14f * scale).coerceAtLeast(9f).dp,
                vertical = (8f * scale).coerceAtLeast(5f).dp
            )
            .testTag("scheduled_lock_window_rail"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$startClockText → $endClockText",
            fontFamily = JetBrainsMono,
            fontSize = (10.5f * scale).coerceAtLeast(7.5f).sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${elapsedMins}m in",
            fontFamily = JetBrainsMono,
            fontSize = (10f * scale).coerceAtLeast(7f).sp,
            fontWeight = FontWeight.Medium,
            color = palette.primary,
            maxLines = 1
        )
    }
}

@Composable
private fun CompletionHaloEmblem(palette: FocusThemePalette) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension * 0.46f

            when (palette.theme) {
                AppVisualTheme.NOTHING_CRIMSON -> {
                    val dots = 24
                    for (i in 0 until dots) {
                        val rad = (-90.0 + (360.0 / dots) * i) * PI / 180.0
                        val x = cx + (outerR * cos(rad)).toFloat()
                        val y = cy + (outerR * sin(rad)).toFloat()
                        drawCircle(
                            color = palette.success.copy(alpha = if (i % 2 == 0) 0.72f else 0.30f),
                            radius = if (i % 2 == 0) 2.2.dp.toPx() else 1.4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                AppVisualTheme.SOLAR_AMBER -> {
                    val ticks = 24
                    for (i in 0 until ticks) {
                        val rad = (-90.0 + (360.0 / ticks) * i) * PI / 180.0
                        val isMajor = i % 3 == 0
                        val inner = outerR - if (isMajor) 4.dp.toPx() else 2.dp.toPx()
                        val outer = outerR + if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                        drawLine(
                            color = palette.success.copy(alpha = if (isMajor) 0.75f else 0.32f),
                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                            end = Offset(cx + (outer * cos(rad)).toFloat(), cy + (outer * sin(rad)).toFloat()),
                            strokeWidth = if (isMajor) 1.6.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                AppVisualTheme.LIQUID_GLASS, AppVisualTheme.NORDIC_SAGE -> {
                    drawCircle(
                        color = palette.success.copy(alpha = 0.14f),
                        radius = outerR,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                palette.success.copy(alpha = 0.25f),
                                palette.success.copy(alpha = 0.75f),
                                palette.success.copy(alpha = 0.25f)
                            )
                        ),
                        radius = outerR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.75.dp.toPx())
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(palette.success),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Session complete",
                tint = palette.background,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
