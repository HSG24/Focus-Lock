package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import com.example.data.FocusSettingsRepository
import com.example.data.FocusSettingsState
import com.example.data.FocusXpRepository
import com.example.data.FocusXpState
import com.example.data.NothingWidgetKind
import com.example.data.SingleWidgetConfig
import com.example.data.WidgetStudioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object NothingFocusWidgetUpdater {
    private val updateScope = CoroutineScope(Dispatchers.IO)

    fun scheduleUpdateAll(context: Context) {
        val appCtx = context.applicationContext
        updateScope.launch {
            updateAllWidgets(appCtx)
        }
    }

    suspend fun updateAllWidgets(context: Context) {
        val appCtx = context.applicationContext
        runCatching { NothingFocusWidget().updateAll(appCtx) }
        runCatching { NothingClockWidget().updateAll(appCtx) }
        runCatching { NothingHeatmapWidget().updateAll(appCtx) }
        runCatching { NothingScheduleWidget().updateAll(appCtx) }
        runCatching { NothingGoalBarWidget().updateAll(appCtx) }
        runCatching { NothingStreakRankWidget().updateAll(appCtx) }
    }
}

class NothingFocusWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FocusSettingsRepository.getCurrent(context)
        val xpState = FocusXpRepository.getCurrent(context)
        val widgetConfig = WidgetStudioRepository.getConfig(context, NothingWidgetKind.GOAL_RING)
        val widgetPalette = resolveWidgetPalette(widgetConfig, settings.appTheme)
        val isLocked = LockService.currentSessionStartTimeMs > 0L

        val goalMs = settings.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
        val progressFraction = (xpState.todayFocusMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
        val progressPercent = (progressFraction * 100f).toInt().coerceIn(0, 999)

        val density = context.resources.displayMetrics.density.coerceAtLeast(2f)
        val canvasSizePx = (180f * density).roundToInt().coerceAtLeast(720)

        val ringBitmap = if (settings.widgetShowDottedRing) {
            renderDottedRingBitmap(
                context = context,
                progressFraction = if (isLocked) 1f else progressFraction,
                studyHoursText = formatWidgetHours(xpState.todayFocusMs, isLocked),
                percentText = if (settings.widgetShowGoalPercent) "${progressPercent}%" else null,
                showStudyHours = settings.widgetShowStudyHours,
                canvasSizePx = canvasSizePx,
                accentColorArgb = widgetPalette.accent.toArgb(),
                completedColorArgb = widgetPalette.activeDot.toArgb(),
                inactiveColorArgb = widgetPalette.inactiveDot.toArgb(),
                textColorArgb = widgetPalette.textPrimary.toArgb()
            )
        } else {
            null
        }

        val headerBitmap = renderDotMatrixHeaderBitmap(
            context = context,
            label = "FOCUS LOCK",
            isLocked = isLocked,
            density = density,
            dotColorArgb = if (widgetConfig.showHeaderDots) {
                (if (isLocked) widgetPalette.success else widgetPalette.accent).toArgb()
            } else null,
            textColorArgb = widgetPalette.textPrimary.toArgb()
        )

        provideContent {
            NothingWidgetContent(
                settings = settings,
                xpState = xpState,
                widgetConfig = widgetConfig,
                widgetPalette = widgetPalette,
                progressPercent = progressPercent,
                isLocked = isLocked,
                headerBitmap = headerBitmap,
                ringBitmap = ringBitmap
            )
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable
    ) {
        val fallback = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Focus Lock")
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }

    @Composable
    private fun NothingWidgetContent(
        settings: FocusSettingsState,
        xpState: FocusXpState,
        widgetConfig: SingleWidgetConfig,
        widgetPalette: WidgetVisualPalette,
        progressPercent: Int,
        isLocked: Boolean,
        headerBitmap: Bitmap,
        ringBitmap: Bitmap?
    ) {
        val widgetSize = LocalSize.current
        val ringDisplaySize = minOf(widgetSize.height * 0.64f, widgetSize.width * 0.72f)
            .coerceIn(76.dp, 176.dp)

        val cardBg = widgetPalette.cardBg
        val chipBg = widgetPalette.chipBg
        val accent = widgetPalette.accent
        val white = widgetPalette.textPrimary
        val footerMuted = widgetPalette.textSecondary
        val tapAction = if (widgetConfig.enableQuickLockTap) {
            actionStartActivity<QuickLockActivity>()
        } else {
            actionStartActivity<MainActivity>()
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(widgetConfig.cornerStyle.radiusDp.dp)
                .background(ColorProvider(cardBg))
                .clickable(tapAction),
            contentAlignment = Alignment.Center
        ) {
            // Center: Enlarged Dotted Circular Progress Gauge (62%-66% height) or Compact Readout
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (ringBitmap != null) {
                    Image(
                        provider = ImageProvider(ringBitmap),
                        contentDescription = "Focus progress",
                        modifier = GlanceModifier.size(ringDisplaySize)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (settings.widgetShowStudyHours) {
                            Text(
                                text = formatWidgetHours(xpState.todayFocusMs, isLocked),
                                style = TextStyle(
                                    color = ColorProvider(white),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                        if (settings.widgetShowGoalPercent) {
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = "${progressPercent}%",
                                style = TextStyle(
                                    color = ColorProvider(accent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }

            // Top Row anchored 14dp from top edge
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, start = 14.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(headerBitmap),
                        contentDescription = "Focus Lock",
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(18.dp)
                    )
                    if (settings.widgetShowStreakBadge) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(50.dp)
                                .background(ColorProvider(chipBg))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${xpState.currentStreak}D",
                                style = TextStyle(
                                    color = ColorProvider(white),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Footer anchored 14dp from bottom edge
            if (settings.widgetShowRankXpTag) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = GlanceModifier
                            .padding(bottom = 14.dp, start = 12.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = xpState.rank.title.uppercase(Locale.US),
                            style = TextStyle(
                                color = ColorProvider(white),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = " • ${xpState.totalXp} XP",
                            style = TextStyle(
                                color = ColorProvider(footerMuted),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val RESPONSIVE_SIZES = setOf(
            DpSize(120.dp, 120.dp),
            DpSize(160.dp, 160.dp),
            DpSize(180.dp, 180.dp),
            DpSize(220.dp, 220.dp),
            DpSize(260.dp, 260.dp)
        )

        fun formatWidgetHours(todayFocusMs: Long, isLocked: Boolean): String {
            if (isLocked && LockService.currentSessionStartTimeMs > 0L) {
                val elapsedMs = (System.currentTimeMillis() - LockService.currentSessionStartTimeMs).coerceAtLeast(0L)
                val totalMins = elapsedMs / 60_000L
                val secs = (elapsedMs / 1000L) % 60L
                return String.format(Locale.US, "%02d:%02d", totalMins, secs)
            }
            val totalMins = (todayFocusMs / 60_000L).coerceAtLeast(0L)
            val hrs = totalMins / 60L
            val remMins = totalMins % 60L
            return String.format(Locale.US, "%02dH %02dM", hrs, remMins)
        }

        private fun loadDotTypeface(context: Context): Typeface {
            return runCatching {
                ResourcesCompat.getFont(context, R.font.dotgothic16) ?: Typeface.MONOSPACE
            }.getOrDefault(Typeface.MONOSPACE)
        }

        internal fun renderDotMatrixHeaderBitmap(
            context: Context,
            label: String,
            isLocked: Boolean,
            density: Float = 3f,
            dotColorArgb: Int? = android.graphics.Color.parseColor(if (isLocked) "#10B981" else "#D71921"),
            textColorArgb: Int = android.graphics.Color.WHITE
        ): Bitmap {
            val scale = density.coerceAtLeast(3f)
            val widthPx = (140f * scale).roundToInt().coerceAtLeast(420)
            val heightPx = (22f * scale).roundToInt().coerceAtLeast(66)
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            var textStartX = 1f * scale
            if (dotColorArgb != null) {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    color = dotColorArgb
                    style = Paint.Style.FILL
                }
                val dotRadius = 3.2f * scale
                canvas.drawCircle(dotRadius + 1f * scale, heightPx / 2f, dotRadius, dotPaint)
                textStartX = dotRadius * 2f + 7f * scale
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = textColorArgb
                textSize = 11.5f * scale
                typeface = loadDotTypeface(context)
                isFakeBoldText = true
                letterSpacing = 0.08f
            }
            val textY = heightPx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, textStartX, textY, textPaint)

            return bitmap
        }

        internal fun renderDottedRingBitmap(
            context: Context,
            progressFraction: Float,
            studyHoursText: String,
            percentText: String?,
            showStudyHours: Boolean,
            canvasSizePx: Int = 720,
            accentColorArgb: Int = android.graphics.Color.parseColor("#D71921"),
            completedColorArgb: Int = android.graphics.Color.WHITE,
            inactiveColorArgb: Int = android.graphics.Color.parseColor("#3A3A40"),
            textColorArgb: Int = android.graphics.Color.WHITE
        ): Bitmap {
            val sizePx = canvasSizePx.coerceAtLeast(640)
            val scale = sizePx / 720f
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val cx = sizePx / 2f
            val cy = sizePx / 2f
            val radius = sizePx * 0.44f
            val totalDots = 40
            val clamped = progressFraction.coerceIn(0f, 1f)
            val activeDots = if (clamped > 0f) {
                (totalDots * clamped).roundToInt().coerceIn(1, totalDots)
            } else {
                0
            }
            val headIndex = (activeDots - 1).coerceAtLeast(0)

            val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = inactiveColorArgb
                style = Paint.Style.FILL
            }
            val completedPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = completedColorArgb
                style = Paint.Style.FILL
            }
            val headPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = accentColorArgb
                style = Paint.Style.FILL
            }

            val inactiveDotRadius = 10.8f * scale
            val completedDotRadius = 11.4f * scale
            val headDotRadius = 15.2f * scale

            for (i in 0 until totalDots) {
                val angleRad = Math.toRadians(-90.0 + (360.0 / totalDots) * i)
                val dx = cx + (radius * cos(angleRad)).toFloat()
                val dy = cy + (radius * sin(angleRad)).toFloat()
                when {
                    i == headIndex && activeDots > 0 -> canvas.drawCircle(dx, dy, headDotRadius, headPaint)
                    i < activeDots -> canvas.drawCircle(dx, dy, completedDotRadius, completedPaint)
                    else -> canvas.drawCircle(dx, dy, inactiveDotRadius, inactivePaint)
                }
            }

            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = inactiveColorArgb
                style = Paint.Style.STROKE
                strokeWidth = 5.5f * scale
                strokeCap = Paint.Cap.ROUND
            }
            val tickOuter = radius - 26f * scale
            val tickInner = radius - 48f * scale
            for (deg in listOf(-90.0, 0.0, 90.0, 180.0)) {
                val rad = Math.toRadians(deg)
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                canvas.drawLine(
                    cx + tickInner * cosA,
                    cy + tickInner * sinA,
                    cx + tickOuter * cosA,
                    cy + tickOuter * sinA,
                    tickPaint
                )
            }

            val dotFace = loadDotTypeface(context)

            if (showStudyHours) {
                val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    color = textColorArgb
                    textSize = 122f * scale
                    typeface = dotFace
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val maxInnerWidth = sizePx * 0.64f
                val measured = mainPaint.measureText(studyHoursText)
                if (measured > maxInnerWidth && measured > 0f) {
                    mainPaint.textSize = mainPaint.textSize * (maxInnerWidth / measured)
                }
                val mainCenterOffset = -(mainPaint.descent() + mainPaint.ascent()) / 2f
                val mainY = if (percentText != null) {
                    cy - 22f * scale + mainCenterOffset
                } else {
                    cy + mainCenterOffset
                }
                canvas.drawText(studyHoursText, cx, mainY, mainPaint)
            }

            if (percentText != null) {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    color = accentColorArgb
                    textSize = 76f * scale
                    typeface = dotFace
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val subCenterOffset = -(subPaint.descent() + subPaint.ascent()) / 2f
                val subY = if (showStudyHours) {
                    cy + 78f * scale + subCenterOffset
                } else {
                    cy + subCenterOffset
                }
                canvas.drawText(percentText, cx, subY, subPaint)
            }

            return bitmap
        }
    }
}
