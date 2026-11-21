package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.LockService
import com.example.MainActivity
import com.example.R
import com.example.data.FocusScheduleRepository
import com.example.data.FocusSettingsRepository
import com.example.data.FocusStatsRepository
import com.example.data.FocusXpRepository
import com.example.data.NothingWidgetKind
import com.example.data.SingleWidgetConfig
import com.example.data.WidgetStudioRepository
import com.example.data.WidgetSurfaceStyle
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class WidgetVisualPalette(
    val cardBg: Color,
    val chipBg: Color,
    val accent: Color,
    val accentSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val inactiveDot: Color,
    val activeDot: Color,
    val success: Color
)

internal fun resolveWidgetPalette(
    config: SingleWidgetConfig,
    appTheme: AppVisualTheme
): WidgetVisualPalette {
    if (config.useNothingMono) {
        val bg = when (config.surfaceStyle) {
            WidgetSurfaceStyle.SOLID -> Color(0xFF101013)
            WidgetSurfaceStyle.ELEVATED -> Color(0xFF1B1B20)
            WidgetSurfaceStyle.GLASS -> Color(0xDD14161D)
        }
        val chip = when (config.surfaceStyle) {
            WidgetSurfaceStyle.SOLID -> Color(0xFF1E1E24)
            WidgetSurfaceStyle.ELEVATED -> Color(0xFF282830)
            WidgetSurfaceStyle.GLASS -> Color(0xFF232733)
        }
        return WidgetVisualPalette(
            cardBg = bg,
            chipBg = chip,
            accent = Color(0xFFD71921),
            accentSoft = Color(0xFF3A1317),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF9C9CA6),
            inactiveDot = Color(0xFF303038),
            activeDot = Color(0xFFFFFFFF),
            success = Color(0xFF10B981)
        )
    }

    val theme = FocusThemePalette.from(appTheme)
    val bg = when (config.surfaceStyle) {
        WidgetSurfaceStyle.SOLID -> theme.surface
        WidgetSurfaceStyle.ELEVATED -> theme.surfaceElevated
        WidgetSurfaceStyle.GLASS -> theme.surface.copy(alpha = 0.88f)
    }
    return WidgetVisualPalette(
        cardBg = bg,
        chipBg = theme.surfaceElevated,
        accent = theme.primary,
        accentSoft = theme.primaryContainer,
        textPrimary = theme.textPrimary,
        textSecondary = theme.textSecondary,
        inactiveDot = theme.borderHighlight.copy(alpha = 0.72f),
        activeDot = theme.textPrimary,
        success = theme.success
    )
}

internal fun loadWidgetDotTypeface(context: Context): Typeface {
    return runCatching {
        ResourcesCompat.getFont(context, R.font.dotgothic16) ?: Typeface.MONOSPACE
    }.getOrDefault(Typeface.MONOSPACE)
}

internal fun renderDotWordBitmap(
    context: Context,
    text: String,
    colorArgb: Int,
    textSizeSp: Float,
    widthDp: Float,
    heightDp: Float,
    centered: Boolean = false,
    leadingDotColorArgb: Int? = null,
    density: Float = 3f
): Bitmap {
    val scale = density.coerceAtLeast(3f)
    val w = (widthDp * scale).roundToInt().coerceAtLeast(120)
    val h = (heightDp * scale).roundToInt().coerceAtLeast(48)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = colorArgb
        textSize = textSizeSp * scale
        typeface = loadWidgetDotTypeface(context)
        isFakeBoldText = true
        letterSpacing = 0.06f
        textAlign = if (centered) Paint.Align.CENTER else Paint.Align.LEFT
    }

    var startX = if (centered) w / 2f else 0f
    if (leadingDotColorArgb != null && !centered) {
        val dotRadius = 3.2f * scale
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = leadingDotColorArgb
            style = Paint.Style.FILL
        }
        canvas.drawCircle(dotRadius + 1f * scale, h / 2f, dotRadius, dotPaint)
        startX = dotRadius * 2f + 7f * scale
    }

    val availWidth = (w - if (centered) 8f * scale else startX + 4f * scale).coerceAtLeast(32f)
    val measured = paint.measureText(text)
    if (measured > availWidth && measured > 0f) {
        paint.textSize = paint.textSize * (availWidth / measured)
    }
    val textY = h / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(text, startX, textY, paint)
    return bmp
}

private fun fallbackWidgetViews(context: Context, appWidgetId: Int, label: String) {
    val fallback = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
        setTextViewText(android.R.id.text1, label)
    }
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
}

// ============================================================================
// 1. DOT-MATRIX FOCUS CLOCK & QUICK LOCK (2x2)
// ============================================================================

class NothingClockWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(130.dp, 130.dp), DpSize(180.dp, 180.dp), DpSize(240.dp, 240.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FocusSettingsRepository.getCurrent(context)
        val config = WidgetStudioRepository.getConfig(context, NothingWidgetKind.DOT_CLOCK)
        val palette = resolveWidgetPalette(config, settings.appTheme)
        val isLocked = LockService.currentSessionStartTimeMs > 0L

        val now = Date()
        val timeText = SimpleDateFormat(if (config.use24HourClock) "HH:mm" else "hh:mm", Locale.US).format(now)
        val amPm = SimpleDateFormat("a", Locale.US).format(now).uppercase(Locale.US)
        val dateText = SimpleDateFormat("EEE · MMM d", Locale.US).format(now).uppercase(Locale.US)

        val density = context.resources.displayMetrics.density.coerceAtLeast(2.5f)
        val headerBmp = renderDotWordBitmap(
            context = context,
            text = if (isLocked) "LOCKED" else "FOCUS TIME",
            colorArgb = palette.textPrimary.toArgb(),
            textSizeSp = 10.5f,
            widthDp = 128f,
            heightDp = 20f,
            leadingDotColorArgb = if (config.showHeaderDots) {
                (if (isLocked) palette.success else palette.accent).toArgb()
            } else null,
            density = density
        )
        val clockBmp = renderDotWordBitmap(
            context = context,
            text = timeText,
            colorArgb = palette.textPrimary.toArgb(),
            textSizeSp = 38f,
            widthDp = 160f,
            heightDp = 56f,
            centered = true,
            density = density
        )

        provideContent {
            val tapAction = if (config.enableQuickLockTap) {
                actionStartActivity<QuickLockActivity>()
            } else {
                actionStartActivity<MainActivity>()
            }
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(config.cornerStyle.radiusDp.dp)
                    .background(ColorProvider(palette.cardBg))
                    .clickable(tapAction)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(headerBmp),
                            contentDescription = "Focus Clock",
                            modifier = GlanceModifier.defaultWeight().height(16.dp)
                        )
                        if (!config.use24HourClock) {
                            Text(
                                text = amPm,
                                style = TextStyle(
                                    color = ColorProvider(palette.accent),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = ImageProvider(clockBmp),
                        contentDescription = timeText,
                        modifier = GlanceModifier.fillMaxWidth().height(54.dp)
                    )

                    if (config.showSecondaryMetric) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = dateText,
                            style = TextStyle(
                                color = ColorProvider(palette.textSecondary),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(50.dp)
                            .background(ColorProvider(if (isLocked) palette.success else palette.chipBg))
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isLocked -> "SESSION ACTIVE"
                                config.enableQuickLockTap -> "● TAP TO LOCK"
                                else -> "OPEN FOCUS"
                            },
                            style = TextStyle(
                                color = ColorProvider(if (isLocked) Color.Black else palette.textPrimary),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        fallbackWidgetViews(context, appWidgetId, "Focus Clock")
    }
}

class NothingClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NothingClockWidget()
}

// ============================================================================
// 2. CONSISTENCY MATRIX HEATMAP WIDGET (4x2)
// ============================================================================

class NothingHeatmapWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(250.dp, 120.dp), DpSize(320.dp, 150.dp), DpSize(360.dp, 180.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FocusSettingsRepository.getCurrent(context)
        val xpState = FocusXpRepository.getCurrent(context)
        val config = WidgetStudioRepository.getConfig(context, NothingWidgetKind.HEATMAP_MATRIX)
        val palette = resolveWidgetPalette(config, settings.appTheme)
        val sessions = FocusStatsRepository.getCachedSessions(context)

        val density = context.resources.displayMetrics.density.coerceAtLeast(2.5f)
        val headerBmp = renderDotWordBitmap(
            context = context,
            text = "CONSISTENCY MATRIX",
            colorArgb = palette.textPrimary.toArgb(),
            textSizeSp = 10.5f,
            widthDp = 180f,
            heightDp = 20f,
            leadingDotColorArgb = if (config.showHeaderDots) palette.accent.toArgb() else null,
            density = density
        )
        val matrixBmp = renderHeatmapDotsBitmap(
            sessions = sessions,
            dailyGoalMinutes = settings.dailyGoalMinutes,
            palette = palette,
            density = density
        )

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(config.cornerStyle.radiusDp.dp)
                    .background(ColorProvider(palette.cardBg))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(14.dp)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(headerBmp),
                            contentDescription = "Consistency Matrix",
                            modifier = GlanceModifier.defaultWeight().height(16.dp)
                        )
                        if (config.showSecondaryMetric) {
                            Box(
                                modifier = GlanceModifier
                                    .cornerRadius(50.dp)
                                    .background(ColorProvider(palette.chipBg))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${xpState.currentStreak}D STREAK",
                                    style = TextStyle(
                                        color = ColorProvider(palette.accent),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Image(
                        provider = ImageProvider(matrixBmp),
                        contentDescription = "Focus Consistency Grid",
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    )
                }
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        fallbackWidgetViews(context, appWidgetId, "Consistency Matrix")
    }

    companion object {
        internal fun renderHeatmapDotsBitmap(
            sessions: List<com.example.Session>,
            dailyGoalMinutes: Long,
            palette: WidgetVisualPalette,
            density: Float = 3f
        ): Bitmap {
            val cols = 12
            val rows = 7
            val scale = density.coerceAtLeast(3f)
            val widthPx = (280f * scale).roundToInt().coerceAtLeast(780)
            val heightPx = (96f * scale).roundToInt().coerceAtLeast(260)
            val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayTotals = mutableMapOf<Long, Long>()
            for (s in sessions) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = s.startTime
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dayTotals[c.timeInMillis] = (dayTotals[c.timeInMillis] ?: 0L) + s.duration
            }

            val goalMs = dailyGoalMinutes.coerceAtLeast(15L) * 60_000L
            val totalCells = cols * rows
            val cellW = widthPx.toFloat() / cols
            val cellH = heightPx.toFloat() / rows
            val dotRadius = minOf(cellW, cellH) * 0.34f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

            for (col in 0 until cols) {
                for (row in 0 until rows) {
                    val cellIdx = col * rows + row
                    val daysAgo = (totalCells - 1) - cellIdx
                    val cal = (todayCal.clone() as Calendar).apply { add(Calendar.DATE, -daysAgo) }
                    val ms = dayTotals[cal.timeInMillis] ?: 0L
                    val ratio = (ms.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)

                    paint.color = when {
                        daysAgo == 0 && ms > 0L -> palette.accent.toArgb()
                        ratio >= 0.85f -> palette.accent.toArgb()
                        ratio >= 0.45f -> palette.activeDot.copy(alpha = 0.85f).toArgb()
                        ratio > 0f -> palette.activeDot.copy(alpha = 0.42f).toArgb()
                        else -> palette.inactiveDot.toArgb()
                    }
                    val cx = col * cellW + cellW / 2f
                    val cy = row * cellH + cellH / 2f
                    canvas.drawCircle(cx, cy, if (daysAgo == 0) dotRadius * 1.15f else dotRadius, paint)
                }
            }
            return bmp
        }
    }
}

class NothingHeatmapWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NothingHeatmapWidget()
}

// ============================================================================
// 3. NEXT SCHEDULE COUNTDOWN WIDGET (4x2)
// ============================================================================

class NothingScheduleWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(250.dp, 120.dp), DpSize(320.dp, 150.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FocusSettingsRepository.getCurrent(context)
        val schedState = FocusScheduleRepository.getCurrent(context)
        val config = WidgetStudioRepository.getConfig(context, NothingWidgetKind.NEXT_SCHEDULE)
        val palette = resolveWidgetPalette(config, settings.appTheme)

        val now = System.currentTimeMillis()
        val next = FocusScheduleRepository.findNextUpcomingWindow(schedState.schedules, now)
        val density = context.resources.displayMetrics.density.coerceAtLeast(2.5f)

        val headerBmp = renderDotWordBitmap(
            context = context,
            text = if (next?.isCurrentlyActive == true) "LOCK ACTIVE" else "NEXT SCHEDULE",
            colorArgb = palette.textPrimary.toArgb(),
            textSizeSp = 10.5f,
            widthDp = 160f,
            heightDp = 20f,
            leadingDotColorArgb = if (config.showHeaderDots) {
                (if (next?.isCurrentlyActive == true) palette.success else palette.accent).toArgb()
            } else null,
            density = density
        )

        val countdownLabel = when {
            next == null -> "NO SCHEDULE"
            next.isCurrentlyActive -> "ENDS ${FocusScheduleRepository.formatRelativeCountdown(next.endTimeMs, now)}"
            else -> "IN ${FocusScheduleRepository.formatRelativeCountdown(next.startTimeMs, now).uppercase(Locale.US)}"
        }

        val countdownBmp = renderDotWordBitmap(
            context = context,
            text = countdownLabel,
            colorArgb = if (next != null) palette.accent.toArgb() else palette.textSecondary.toArgb(),
            textSizeSp = 16f,
            widthDp = 130f,
            heightDp = 28f,
            density = density
        )

        val railBmp = renderDottedCapsuleBarBitmap(
            progress = when {
                next == null -> 0f
                next.isCurrentlyActive -> {
                    val total = (next.endTimeMs - next.startTimeMs).coerceAtLeast(1L)
                    ((now - next.startTimeMs).toFloat() / total.toFloat()).coerceIn(0f, 1f)
                }
                else -> 0.18f
            },
            palette = palette,
            density = density
        )

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(config.cornerStyle.radiusDp.dp)
                    .background(ColorProvider(palette.cardBg))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(14.dp)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(headerBmp),
                            contentDescription = "Next Schedule",
                            modifier = GlanceModifier.defaultWeight().height(16.dp)
                        )
                        if (next != null && config.showSecondaryMetric) {
                            Box(
                                modifier = GlanceModifier
                                    .cornerRadius(50.dp)
                                    .background(ColorProvider(palette.chipBg))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = next.schedule.repeatSummary().uppercase(Locale.US),
                                    style = TextStyle(
                                        color = ColorProvider(palette.textSecondary),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = next?.schedule?.name ?: "Create a Focus Schedule",
                                style = TextStyle(
                                    color = ColorProvider(palette.textPrimary),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = next?.schedule?.formattedTimeRange(config.use24HourClock)
                                    ?: "Tap to open Schedule tab",
                                style = TextStyle(
                                    color = ColorProvider(palette.textSecondary),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Image(
                            provider = ImageProvider(countdownBmp),
                            contentDescription = countdownLabel,
                            modifier = GlanceModifier.width(98.dp).height(24.dp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    Image(
                        provider = ImageProvider(railBmp),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxWidth().height(12.dp)
                    )
                }
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        fallbackWidgetViews(context, appWidgetId, "Next Schedule")
    }
}

class NothingScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NothingScheduleWidget()
}

// ============================================================================
// 4. DAILY GOAL CAPSULE BAR WIDGET (4x1)
// ============================================================================

class NothingGoalBarWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(250.dp, 64.dp), DpSize(320.dp, 76.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FocusSettingsRepository.getCurrent(context)
        val xpState = FocusXpRepository.getCurrent(context)
        val config = WidgetStudioRepository.getConfig(context, NothingWidgetKind.GOAL_BAR)
        val palette = resolveWidgetPalette(config, settings.appTheme)
        val isLocked = LockService.currentSessionStartTimeMs > 0L

        val goalMs = settings.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
        val progress = (xpState.todayFocusMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
        val pct = (progress * 100f).toInt().coerceIn(0, 999)
        val hoursText = NothingFocusWidget.formatWidgetHours(xpState.todayFocusMs, isLocked)

        val density = context.resources.displayMetrics.density.coerceAtLeast(2.5f)
        val barBmp = renderDottedCapsuleBarBitmap(
            progress = progress,
            palette = palette,
            density = density
        )

        provideContent {
            val action = if (config.enableQuickLockTap) {
                actionStartActivity<QuickLockActivity>()
            } else {
                actionStartActivity<MainActivity>()
            }
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(config.cornerStyle.radiusDp.dp)
                    .background(ColorProvider(palette.cardBg))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (config.showHeaderDots) "● $hoursText" else hoursText,
                            style = TextStyle(
                                color = ColorProvider(palette.textPrimary),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        if (config.showSecondaryMetric) {
                            Text(
                                text = "$pct%",
                                style = TextStyle(
                                    color = ColorProvider(palette.accent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(7.dp))
                    Image(
                        provider = ImageProvider(barBmp),
                        contentDescription = "Goal Progress $pct%",
                        modifier = GlanceModifier.fillMaxWidth().height(12.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                Box(
                    modifier = GlanceModifier
                        .size(40.dp)
                        .cornerRadius(50.dp)
                        .background(ColorProvider(if (isLocked) palette.success else palette.accent))
                        .clickable(action),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLocked) "ON" else "GO",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        fallbackWidgetViews(context, appWidgetId, "Daily Goal Bar")
    }
}

class NothingGoalBarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NothingGoalBarWidget()
}

// ============================================================================
// 5. STREAK & RANK BADGE WIDGET (2x2)
// ============================================================================

class NothingStreakRankWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(130.dp, 130.dp), DpSize(180.dp, 180.dp), DpSize(240.dp, 240.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FocusSettingsRepository.getCurrent(context)
        val xpState = FocusXpRepository.getCurrent(context)
        val config = WidgetStudioRepository.getConfig(context, NothingWidgetKind.STREAK_RANK)
        val palette = resolveWidgetPalette(config, settings.appTheme)

        val density = context.resources.displayMetrics.density.coerceAtLeast(2.5f)
        val headerBmp = renderDotWordBitmap(
            context = context,
            text = "STREAK",
            colorArgb = palette.textPrimary.toArgb(),
            textSizeSp = 10.5f,
            widthDp = 110f,
            heightDp = 20f,
            leadingDotColorArgb = if (config.showHeaderDots) palette.accent.toArgb() else null,
            density = density
        )
        val streakBadgeBmp = renderStreakDotHaloBitmap(
            context = context,
            streakDays = xpState.currentStreak,
            palette = palette,
            sizePx = (140f * density).roundToInt().coerceAtLeast(420)
        )

        val sessions = FocusStatsRepository.getCachedSessions(context)
        val bestStreak = maxOf(com.example.calculateMaxStreak(sessions), xpState.currentStreak)

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(config.cornerStyle.radiusDp.dp)
                    .background(ColorProvider(palette.cardBg))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(14.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(headerBmp),
                            contentDescription = "Streak",
                            modifier = GlanceModifier.defaultWeight().height(16.dp)
                        )
                        if (config.showSecondaryMetric) {
                            Text(
                                text = "BEST $bestStreak",
                                style = TextStyle(
                                    color = ColorProvider(palette.textSecondary),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = ImageProvider(streakBadgeBmp),
                        contentDescription = "${xpState.currentStreak} day streak",
                        modifier = GlanceModifier.size(96.dp)
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(50.dp)
                            .background(ColorProvider(palette.chipBg))
                            .padding(vertical = 5.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${xpState.rank.title.uppercase(Locale.US)} · ${xpState.totalXp} XP",
                            style = TextStyle(
                                color = ColorProvider(palette.textPrimary),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        fallbackWidgetViews(context, appWidgetId, "Streak & Rank")
    }
}

class NothingStreakRankWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NothingStreakRankWidget()
}

// ============================================================================
// SHARED HIGH-DPI CANVAS BITMAP RENDERERS
// ============================================================================

internal fun renderDottedCapsuleBarBitmap(
    progress: Float,
    palette: WidgetVisualPalette,
    density: Float = 3f
): Bitmap {
    val scale = density.coerceAtLeast(3f)
    val widthPx = (240f * scale).roundToInt().coerceAtLeast(640)
    val heightPx = (14f * scale).roundToInt().coerceAtLeast(42)
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val count = 24
    val clamped = progress.coerceIn(0f, 1f)
    val filled = if (clamped > 0f) (count * clamped).roundToInt().coerceIn(1, count) else 0
    val gap = 4f * scale
    val segW = (widthPx - gap * (count - 1)) / count.toFloat()
    val cy = heightPx / 2f
    val r = minOf(segW * 0.48f, heightPx * 0.38f)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    for (i in 0 until count) {
        val cx = i * (segW + gap) + segW / 2f
        paint.color = when {
            filled > 0 && i == filled - 1 -> palette.accent.toArgb()
            i < filled -> palette.activeDot.toArgb()
            else -> palette.inactiveDot.toArgb()
        }
        val radius = if (filled > 0 && i == filled - 1) r * 1.18f else r
        canvas.drawRoundRect(
            RectF(cx - radius, cy - radius, cx + radius, cy + radius),
            radius,
            radius,
            paint
        )
    }
    return bmp
}

internal fun renderStreakDotHaloBitmap(
    context: Context,
    streakDays: Int,
    palette: WidgetVisualPalette,
    sizePx: Int = 420
): Bitmap {
    val safeSize = sizePx.coerceAtLeast(360)
    val scale = safeSize / 420f
    val bmp = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cx = safeSize / 2f
    val cy = safeSize / 2f
    val radius = safeSize * 0.42f
    val dots = 28
    val litCount = (streakDays.coerceAtLeast(0).coerceAtMost(dots))

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    for (i in 0 until dots) {
        val angle = Math.toRadians(-90.0 + (360.0 / dots) * i)
        val dx = cx + (radius * cos(angle)).toFloat()
        val dy = cy + (radius * sin(angle)).toFloat()
        val isHead = litCount > 0 && i == litCount - 1
        val isLit = i < litCount
        paint.color = when {
            isHead -> palette.accent.toArgb()
            isLit -> palette.activeDot.toArgb()
            else -> palette.inactiveDot.toArgb()
        }
        val dotR = when {
            isHead -> 10.5f * scale
            isLit -> 7.5f * scale
            else -> 6.5f * scale
        }
        canvas.drawCircle(dx, dy, dotR, paint)
    }

    val dotFace = loadWidgetDotTypeface(context)
    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = palette.textPrimary.toArgb()
        textSize = 128f * scale
        typeface = dotFace
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    val numY = cy - 8f * scale - (numPaint.descent() + numPaint.ascent()) / 2f
    canvas.drawText(streakDays.toString(), cx, numY, numPaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = palette.accent.toArgb()
        textSize = 44f * scale
        typeface = dotFace
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    val subY = cy + 68f * scale - (subPaint.descent() + subPaint.ascent()) / 2f
    canvas.drawText(if (streakDays == 1) "DAY" else "DAYS", cx, subY, subPaint)

    return bmp
}
