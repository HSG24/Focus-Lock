package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AllowedApp
import com.example.data.FocusSettingsRepository
import com.example.data.LockAppLayout
import com.example.data.LockModuleAlignment
import com.example.data.NotificationBlockerRepository
import com.example.data.ScheduledBottomDockMode
import com.example.data.ScheduledLockLayoutConfig
import com.example.data.ScheduledLockModuleType
import com.example.data.ScheduledTimerStyle
import com.example.ui.theme.DotGothic16
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
 * Timer-only Scheduled Focus Lock Screen (no password/PIN unlock).
 * Shared identically between the live fullscreen overlay and the interactive Lock Studio preview
 * so every customization updates immediately with 1:1 visual fidelity.
 */
@Composable
fun ScheduledLockScreen(
    scheduleName: String,
    timeRangeLabel: String,
    startTimeMs: Long,
    endTimeMs: Long,
    layoutConfig: ScheduledLockLayoutConfig,
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
    val haptics = rememberFocusHaptics()
    val lockPalette = remember(palette) { LockPalette.fromFocusTheme(palette) }
    val blockerState by NotificationBlockerRepository.observe(context).collectAsState()

    var nowMs by remember(startTimeMs, endTimeMs, isPreview) {
        mutableLongStateOf(
            if (isPreview) startTimeMs + ((endTimeMs - startTimeMs) * 0.34f).toLong()
            else System.currentTimeMillis()
        )
    }
    var resistedCheckIns by remember(startTimeMs, isPreview) {
        mutableIntStateOf(if (isPreview) 2 else 0)
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

    val previewScale = if (isPreview) 0.62f else 1f
    val verticalGap = (layoutConfig.spacingDensity.gapDp * previewScale).dp
    val enabledModules = remember(layoutConfig.modules) {
        layoutConfig.modules.filter { it.enabled }
    }
    val startClockText = remember(startTimeMs) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(startTimeMs))
    }
    val endClockText = remember(endTimeMs) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(endTimeMs))
    }
    val silencedCount = if (isPreview) 3 else blockerState.activeSessionBlocked.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag(if (isPreview) "scheduled_lock_preview_canvas" else "scheduled_lock_screen_root")
    ) {
        ThemeBackdropLayer(palette = palette)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isPreview) Modifier.padding(horizontal = 13.dp, vertical = 12.dp)
                    else Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 22.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(verticalGap)
            ) {
                enabledModules.forEach { mod ->
                    val horizontalAlignment = when (mod.alignment) {
                        LockModuleAlignment.START -> Alignment.Start
                        LockModuleAlignment.CENTER -> Alignment.CenterHorizontally
                        LockModuleAlignment.END -> Alignment.End
                    }
                    val textAlign = when (mod.alignment) {
                        LockModuleAlignment.START -> TextAlign.Start
                        LockModuleAlignment.CENTER -> TextAlign.Center
                        LockModuleAlignment.END -> TextAlign.End
                    }
                    val scale = mod.size.scaleFactor * previewScale

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = horizontalAlignment
                    ) {
                        when (mod.type) {
                            ScheduledLockModuleType.CLOCK_DATE -> {
                                ScheduledClockDateModule(
                                    nowMs = nowMs,
                                    scale = scale,
                                    textAlign = textAlign,
                                    palette = palette
                                )
                            }

                            ScheduledLockModuleType.SCHEDULE_BADGE -> {
                                ScheduledBadgeModule(
                                    scheduleName = scheduleName,
                                    timeRangeLabel = timeRangeLabel,
                                    scale = scale,
                                    palette = palette
                                )
                            }

                            ScheduledLockModuleType.COUNTDOWN_TIMER -> {
                                ScheduledCountdownModule(
                                    remainingMs = remainingMs,
                                    progressFraction = progressFraction,
                                    timerStyle = layoutConfig.timerStyle,
                                    scale = scale,
                                    palette = palette
                                )
                            }

                            ScheduledLockModuleType.SESSION_TIMELINE -> {
                                ScheduledSessionTimelineModule(
                                    startClockText = startClockText,
                                    endClockText = endClockText,
                                    elapsedMs = elapsedMs,
                                    remainingMs = remainingMs,
                                    progressFraction = progressFraction,
                                    scale = scale,
                                    palette = palette
                                )
                            }

                            ScheduledLockModuleType.FOCUS_MANTRA -> {
                                val mantra = layoutConfig.customMantra.ifBlank {
                                    "Stay present. Protect your focus."
                                }
                                ScheduledMantraModule(
                                    mantra = mantra,
                                    scale = scale,
                                    textAlign = textAlign,
                                    palette = palette
                                )
                            }

                            ScheduledLockModuleType.DAILY_PROGRESS -> {
                                ScheduledProgressPillModule(
                                    dailyGoalPercent = dailyGoalPercent,
                                    currentStreak = currentStreak,
                                    scale = scale,
                                    palette = palette
                                )
                            }

                            ScheduledLockModuleType.ALLOWED_APPS -> {
                                if (allowedApps.isNotEmpty()) {
                                    LockAllowedAppsSection(
                                        apps = if (isPreview) allowedApps.take(3) else allowedApps,
                                        layout = appLayout,
                                        palette = lockPalette,
                                        onAppClick = { app -> onLaunchApp?.invoke(app) },
                                        isPreview = isPreview,
                                        pillButtonSizeDp = (pillButtonSizeDp * mod.size.scaleFactor)
                                            .toInt()
                                            .coerceIn(
                                                FocusSettingsRepository.MIN_PILL_BUTTON_SIZE_DP,
                                                FocusSettingsRepository.MAX_PILL_BUTTON_SIZE_DP
                                            ),
                                        iconCache = iconCache,
                                        readFromCacheOnly = isPreview
                                    )
                                }
                            }
                        }
                    }
                }

                // Compact bottom dock sits directly below the Goal & Streak pill
                ScheduledLockBottomDock(
                    mode = layoutConfig.bottomDockMode,
                    endClockText = endClockText,
                    progressFraction = progressFraction,
                    silencedCount = silencedCount,
                    resistedCheckIns = resistedCheckIns,
                    onLogResisted = {
                        if (!isPreview) {
                            haptics.tick()
                            resistedCheckIns += 1
                        }
                    },
                    scale = previewScale,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ScheduledLockBottomDock(
    mode: ScheduledBottomDockMode,
    endClockText: String,
    progressFraction: Float,
    silencedCount: Int,
    resistedCheckIns: Int,
    onLogResisted: () -> Unit,
    scale: Float,
    palette: FocusThemePalette,
    modifier: Modifier = Modifier
) {
    val pillShape = remember { RoundedCornerShape(50) }
    val consoleShape = remember { RoundedCornerShape((28f * scale).coerceAtLeast(18f).dp) }
    val pct = (progressFraction * 100f).roundToInt().coerceIn(0, 100)

    when (mode) {
        ScheduledBottomDockMode.MINIMAL -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6f * scale).coerceAtLeast(4f).dp),
                    modifier = Modifier
                        .clip(pillShape)
                        .background(palette.surfaceElevated.copy(alpha = 0.78f), pillShape)
                        .padding(
                            horizontal = (14f * scale).coerceAtLeast(9f).dp,
                            vertical = (7f * scale).coerceAtLeast(4f).dp
                        )
                        .testTag("scheduled_bottom_dock_minimal")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockClock,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size((13f * scale).coerceAtLeast(9f).dp)
                    )
                    Text(
                        text = "Unlocks automatically at $endClockText",
                        fontFamily = JetBrainsMono,
                        fontSize = (10.5f * scale).coerceAtLeast(7.5f).sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        ScheduledBottomDockMode.COMPACT -> {
            Row(
                modifier = modifier.testTag("scheduled_bottom_dock_compact"),
                horizontalArrangement = Arrangement.spacedBy((8f * scale).coerceAtLeast(5f).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6f * scale).coerceAtLeast(4f).dp),
                    modifier = Modifier
                        .weight(1f)
                        .focusGlassPanel(shape = pillShape, palette = palette, elevated = true)
                        .padding(
                            horizontal = (12f * scale).coerceAtLeast(8f).dp,
                            vertical = (9f * scale).coerceAtLeast(5f).dp
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size((14f * scale).coerceAtLeast(9f).dp)
                    )
                    Text(
                        text = if (silencedCount == 0) "Quiet" else "$silencedCount silenced",
                        fontFamily = JetBrainsMono,
                        fontSize = (10.5f * scale).coerceAtLeast(7.2f).sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1.25f)
                        .focusGlassPanel(shape = pillShape, palette = palette, elevated = true)
                        .padding(
                            horizontal = (12f * scale).coerceAtLeast(8f).dp,
                            vertical = (9f * scale).coerceAtLeast(5f).dp
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockClock,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size((13f * scale).coerceAtLeast(8.5f).dp)
                    )
                    Spacer(modifier = Modifier.size((5f * scale).coerceAtLeast(3f).dp))
                    Text(
                        text = "Unlocks $endClockText",
                        fontFamily = JetBrainsMono,
                        fontSize = (10.5f * scale).coerceAtLeast(7.2f).sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        ScheduledBottomDockMode.CONSOLE -> {
            Column(
                modifier = modifier
                    .focusGlassPanel(shape = consoleShape, palette = palette)
                    .padding(
                        horizontal = (14f * scale).coerceAtLeast(9f).dp,
                        vertical = (12f * scale).coerceAtLeast(7f).dp
                    )
                    .testTag("scheduled_bottom_dock_console"),
                verticalArrangement = Arrangement.spacedBy((9f * scale).coerceAtLeast(5f).dp)
            ) {
                // Top row: Auto-unlock ETA + Live completion badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((6f * scale).coerceAtLeast(4f).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LockClock,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size((14f * scale).coerceAtLeast(9f).dp)
                        )
                        Text(
                            text = "Unlocks at $endClockText",
                            fontFamily = JetBrainsMono,
                            fontSize = (11f * scale).coerceAtLeast(7.5f).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(palette.primaryContainer, pillShape)
                            .padding(
                                horizontal = (8f * scale).coerceAtLeast(5f).dp,
                                vertical = (3f * scale).coerceAtLeast(2f).dp
                            )
                    ) {
                        Text(
                            text = "$pct%",
                            fontFamily = DotGothic16,
                            fontSize = (10.5f * scale).coerceAtLeast(7f).sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    }
                }

                // Dual utility pills: Silenced Alerts + Tactile Stay-Locked Check-In
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((8f * scale).coerceAtLeast(5f).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((6f * scale).coerceAtLeast(4f).dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(palette.surfaceElevated.copy(alpha = 0.85f), pillShape)
                            .padding(
                                horizontal = (11f * scale).coerceAtLeast(7f).dp,
                                vertical = (8f * scale).coerceAtLeast(4.5f).dp
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size((13f * scale).coerceAtLeast(8.5f).dp)
                        )
                        Text(
                            text = if (silencedCount == 0) "0 silenced" else "$silencedCount silenced",
                            fontFamily = JetBrainsMono,
                            fontSize = (10f * scale).coerceAtLeast(7f).sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(palette.primary.copy(alpha = 0.14f), pillShape)
                            .clickable(onClick = onLogResisted)
                            .padding(
                                horizontal = (11f * scale).coerceAtLeast(7f).dp,
                                vertical = (8f * scale).coerceAtLeast(4.5f).dp
                            )
                            .testTag("scheduled_lock_resisted_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Log resisted urge",
                            tint = palette.primary,
                            modifier = Modifier.size((13f * scale).coerceAtLeast(8.5f).dp)
                        )
                        Spacer(modifier = Modifier.size((5f * scale).coerceAtLeast(3f).dp))
                        Text(
                            text = if (resistedCheckIns == 0) "Stay locked" else "Resisted · $resistedCheckIns",
                            fontFamily = JetBrainsMono,
                            fontSize = (10f * scale).coerceAtLeast(7f).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledClockDateModule(
    nowMs: Long,
    scale: Float,
    textAlign: TextAlign,
    palette: FocusThemePalette
) {
    val timeText = remember(nowMs / 30_000L) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(nowMs))
    }
    val dateText = remember(nowMs / 3_600_000L) {
        SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(nowMs)).uppercase(Locale.US)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8f * scale).coerceAtLeast(4f).dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.surfaceElevated.copy(alpha = 0.65f))
            .padding(
                horizontal = (13f * scale).coerceAtLeast(8f).dp,
                vertical = (6f * scale).coerceAtLeast(3f).dp
            )
    ) {
        Text(
            text = timeText,
            fontFamily = JetBrainsMono,
            fontSize = (12f * scale).coerceAtLeast(8f).sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            textAlign = textAlign
        )
        Box(
            modifier = Modifier
                .size((3.5f * scale).coerceAtLeast(2.5f).dp)
                .clip(CircleShape)
                .background(palette.primary)
        )
        Text(
            text = dateText,
            fontFamily = JetBrainsMono,
            fontSize = (11f * scale).coerceAtLeast(7.5f).sp,
            color = palette.textSecondary,
            textAlign = textAlign
        )
    }
}

@Composable
private fun ScheduledBadgeModule(
    scheduleName: String,
    timeRangeLabel: String,
    scale: Float,
    palette: FocusThemePalette
) {
    val shape = remember { RoundedCornerShape(50) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8f * scale).coerceAtLeast(4f).dp),
        modifier = Modifier
            .focusGlassPanel(shape = shape, palette = palette, elevated = true)
            .padding(
                horizontal = (14f * scale).coerceAtLeast(8f).dp,
                vertical = (8f * scale).coerceAtLeast(4f).dp
            )
    ) {
        Box(
            modifier = Modifier
                .size((7f * scale).coerceAtLeast(4.5f).dp)
                .clip(CircleShape)
                .background(palette.primary)
        )
        Text(
            text = scheduleName,
            style = MaterialTheme.typography.titleMedium,
            fontSize = (13.5f * scale).coerceAtLeast(8.5f).sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size((12f * scale).coerceAtLeast(8f).dp)
        )
        Text(
            text = timeRangeLabel,
            fontFamily = JetBrainsMono,
            fontSize = (11f * scale).coerceAtLeast(7.5f).sp,
            color = palette.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun ScheduledCountdownModule(
    remainingMs: Long,
    progressFraction: Float,
    timerStyle: ScheduledTimerStyle,
    scale: Float,
    palette: FocusThemePalette
) {
    val totalSec = remainingMs / 1000L
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    val formatted = if (hours > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    when (timerStyle) {
        ScheduledTimerStyle.DIGITS_ONLY -> {
            val pillShape = remember { RoundedCornerShape(32.dp) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((4f * scale).coerceAtLeast(2f).dp),
                modifier = Modifier
                    .focusGlassPanel(shape = pillShape, palette = palette)
                    .padding(
                        horizontal = (24f * scale).coerceAtLeast(12f).dp,
                        vertical = (16f * scale).coerceAtLeast(8f).dp
                    )
            ) {
                Text(
                    text = formatted,
                    fontFamily = palette.numeralFont,
                    fontSize = (42f * scale).coerceAtLeast(22f).sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = "REMAINING",
                    fontFamily = JetBrainsMono,
                    fontSize = (10f * scale).coerceAtLeast(7f).sp,
                    letterSpacing = 1.6.sp,
                    color = palette.textSecondary
                )
            }
        }

        ScheduledTimerStyle.DOT_RING, ScheduledTimerStyle.MINIMAL_ARC -> {
            val ringSize = (174f * scale).coerceIn(88f, 220f).dp
            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension * 0.44f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    if (timerStyle == ScheduledTimerStyle.DOT_RING) {
                        val dotCount = 36
                        val activeDots = (progressFraction * dotCount).toInt().coerceIn(1, dotCount)
                        val dotR = (size.minDimension * 0.019f).coerceAtLeast(2f)
                        for (i in 0 until dotCount) {
                            val angle = (-90.0 + (i * 360.0 / dotCount)) * PI / 180.0
                            val dx = center.x + (radius * cos(angle)).toFloat()
                            val dy = center.y + (radius * sin(angle)).toFloat()
                            val isActive = i < activeDots
                            drawCircle(
                                color = if (isActive) palette.primary else palette.surfaceElevated.copy(alpha = 0.85f),
                                radius = if (isActive) dotR * 1.15f else dotR * 0.85f,
                                center = Offset(dx, dy)
                            )
                        }
                    } else {
                        val strokeW = (size.minDimension * 0.045f).coerceAtLeast(4f)
                        val arcTopLeft = Offset(center.x - radius, center.y - radius)
                        val arcSize = Size(radius * 2f, radius * 2f)
                        drawArc(
                            color = palette.surfaceElevated.copy(alpha = 0.78f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(palette.primarySecondary, palette.primary, palette.primarySecondary)
                            ),
                            startAngle = -90f,
                            sweepAngle = (progressFraction * 360f).coerceAtLeast(8f),
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((2f * scale).coerceAtLeast(1f).dp)
                ) {
                    Text(
                        text = formatted,
                        fontFamily = palette.numeralFont,
                        fontSize = (28f * scale).coerceAtLeast(15f).sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}% DONE",
                        fontFamily = JetBrainsMono,
                        fontSize = (9.5f * scale).coerceAtLeast(6.5f).sp,
                        letterSpacing = 1.1.sp,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Minimal Nothing dot-matrix session window timeline showing start time,
 * current dot position inside the schedule block, elapsed vs remaining minutes, and unlock time.
 */
@Composable
private fun ScheduledSessionTimelineModule(
    startClockText: String,
    endClockText: String,
    elapsedMs: Long,
    remainingMs: Long,
    progressFraction: Float,
    scale: Float,
    palette: FocusThemePalette
) {
    val shape = remember { RoundedCornerShape(26.dp) }
    val elapsedMins = (elapsedMs / 60_000L).coerceAtLeast(0L)
    val leftMins = ((remainingMs + 59_999L) / 60_000L).coerceAtLeast(0L)

    Column(
        modifier = Modifier
            .widthIn(max = (320f * scale).coerceIn(176f, 380f).dp)
            .fillMaxWidth()
            .focusGlassPanel(shape = shape, palette = palette)
            .padding(
                horizontal = (16f * scale).coerceAtLeast(10f).dp,
                vertical = (11f * scale).coerceAtLeast(6.5f).dp
            )
            .testTag("scheduled_session_timeline_module"),
        verticalArrangement = Arrangement.spacedBy((7f * scale).coerceAtLeast(4f).dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = startClockText,
                fontFamily = JetBrainsMono,
                fontSize = (10f * scale).coerceAtLeast(6.8f).sp,
                color = palette.textSecondary
            )
            Text(
                text = "${elapsedMins}m IN · ${leftMins}m LEFT",
                fontFamily = DotGothic16,
                fontSize = (10f * scale).coerceAtLeast(6.8f).sp,
                color = palette.primary
            )
            Text(
                text = endClockText,
                fontFamily = JetBrainsMono,
                fontSize = (10f * scale).coerceAtLeast(6.8f).sp,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height((10f * scale).coerceAtLeast(6f).dp)
        ) {
            val dotCount = 18
            val activeCount = (progressFraction * dotCount).roundToInt().coerceIn(1, dotCount)
            val gap = size.width / dotCount.toFloat()
            val baseR = (size.height * 0.28f).coerceAtLeast(1.8f)
            for (i in 0 until dotCount) {
                val cx = i * gap + gap / 2f
                val isHead = i == activeCount - 1
                val isPassed = i < activeCount
                val color = when {
                    isHead -> palette.primary
                    isPassed -> palette.textPrimary.copy(alpha = 0.88f)
                    else -> palette.surfaceElevated.copy(alpha = 0.9f)
                }
                drawCircle(
                    color = color,
                    radius = if (isHead) baseR * 1.45f else baseR,
                    center = Offset(cx, size.height / 2f)
                )
            }
        }
    }
}

@Composable
private fun ScheduledMantraModule(
    mantra: String,
    scale: Float,
    textAlign: TextAlign,
    palette: FocusThemePalette
) {
    val shape = remember { RoundedCornerShape(24.dp) }
    Box(
        modifier = Modifier
            .focusGlassPanel(shape = shape, palette = palette)
            .padding(
                horizontal = (16f * scale).coerceAtLeast(9f).dp,
                vertical = (10f * scale).coerceAtLeast(5f).dp
            )
    ) {
        Text(
            text = "\"$mantra\"",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = (13f * scale).coerceAtLeast(8.5f).sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary.copy(alpha = 0.92f),
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ScheduledProgressPillModule(
    dailyGoalPercent: Int,
    currentStreak: Int,
    scale: Float,
    palette: FocusThemePalette
) {
    val pillShape = remember { RoundedCornerShape(50) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((12f * scale).coerceAtLeast(6f).dp),
        modifier = Modifier
            .focusGlassPanel(shape = pillShape, palette = palette, elevated = true)
            .padding(
                horizontal = (18f * scale).coerceAtLeast(10f).dp,
                vertical = (10f * scale).coerceAtLeast(5.5f).dp
            )
            .testTag("scheduled_goal_streak_pill")
    ) {
        Text(
            text = "GOAL $dailyGoalPercent%",
            fontFamily = DotGothic16,
            fontSize = (13f * scale).coerceAtLeast(8.5f).sp,
            fontWeight = FontWeight.Bold,
            color = palette.primary
        )
        Box(
            modifier = Modifier
                .size((4f * scale).coerceAtLeast(2.5f).dp)
                .clip(CircleShape)
                .background(palette.textSecondary.copy(alpha = 0.45f))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((4f * scale).coerceAtLeast(2.5f).dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = palette.warning,
                modifier = Modifier.size((15.5f * scale).coerceAtLeast(9.5f).dp)
            )
            Text(
                text = "${currentStreak}d streak",
                fontFamily = JetBrainsMono,
                fontSize = (12.5f * scale).coerceAtLeast(8f).sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
        }
    }
}

/**
 * Celebratory 3-second completion screen shown when a scheduled focus timer finishes.
 * If the phone was locked when the timer finished, LockService defers displaying this
 * overlay until immediately after the user unlocks their device.
 */
@Composable
fun ScheduledSuccessScreen(
    scheduleName: String,
    durationMs: Long,
    encouragementMessage: String = "Well done — you stayed locked in and finished your focus block.",
    displayDurationMs: Long = 3_000L,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val pillShape = remember { RoundedCornerShape(50) }
    val cardShape = remember { RoundedCornerShape(32.dp) }
    val totalSteps = (displayDurationMs / 1_000L).toInt().coerceAtLeast(1)
    var remainingSeconds by remember(displayDurationMs) { mutableIntStateOf(totalSteps) }

    LaunchedEffect(displayDurationMs) {
        var secs = totalSteps
        while (secs > 1) {
            delay(1_000L)
            secs -= 1
            remainingSeconds = secs
        }
    }

    val durationLabel = remember(durationMs) {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
        val totalMinutes = totalSeconds / 60L
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        when {
            hours > 0L && mins > 0L -> "${hours}h ${mins}m focused"
            hours > 0L -> "${hours}h focused"
            totalMinutes > 0L -> "${totalMinutes}m focused"
            else -> "${totalSeconds}s focused"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("scheduled_success_screen")
    ) {
        ThemeBackdropLayer(palette = palette)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .focusGlassPanel(shape = cardShape, palette = palette, elevated = true)
                    .padding(horizontal = 26.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Celebratory circular badge
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(palette.success.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(palette.success),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Session complete",
                            tint = palette.background,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SESSION COMPLETE",
                        fontFamily = DotGothic16,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = encouragementMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("scheduled_success_message")
                    )
                }

                // Schedule name + focused duration pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(pillShape)
                        .background(palette.surfaceElevated.copy(alpha = 0.9f), pillShape)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(palette.success)
                    )
                    Text(
                        text = scheduleName.ifBlank { "Scheduled Focus" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(palette.textSecondary.copy(alpha = 0.45f))
                    )
                    Text(
                        text = durationLabel,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primary,
                        maxLines = 1
                    )
                }

                // 3-second dot-matrix unlock countdown pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(pillShape)
                        .background(palette.primaryContainer.copy(alpha = 0.7f), pillShape)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("scheduled_success_countdown")
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (dot in 1..totalSteps) {
                            val active = dot <= remainingSeconds
                            Box(
                                modifier = Modifier
                                    .size(if (active) 7.dp else 5.5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) palette.primary
                                        else palette.textSecondary.copy(alpha = 0.28f)
                                    )
                            )
                        }
                    }
                    Text(
                        text = "Unlocking in ${remainingSeconds}s",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primary
                    )
                }
            }
        }
    }
}

