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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
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
        runCatching {
            NothingFocusWidget().updateAll(context.applicationContext)
        }
    }
}

class NothingFocusWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = FocusSettingsRepository.getCurrent(context)
        val xpState = FocusXpRepository.getCurrent(context)
        val isLocked = LockService.currentSessionStartTimeMs > 0L

        val goalMs = settings.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
        val progressFraction = (xpState.todayFocusMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
        val progressPercent = (progressFraction * 100f).toInt().coerceIn(0, 999)

        val ringBitmap = if (settings.widgetShowDottedRing) {
            renderDottedRingBitmap(
                context = context,
                progressFraction = if (isLocked) 1f else progressFraction,
                studyHoursText = formatWidgetHours(xpState.todayFocusMs, isLocked),
                percentText = if (settings.widgetShowGoalPercent) "${progressPercent}%" else null,
                showStudyHours = settings.widgetShowStudyHours,
                isLocked = isLocked
            )
        } else {
            null
        }

        val headerBitmap = renderDotMatrixHeaderBitmap(
            context = context,
            label = "FOCUS LOCK",
            isLocked = isLocked
        )

        provideContent {
            NothingWidgetContent(
                settings = settings,
                xpState = xpState,
                isLocked = isLocked,
                progressPercent = progressPercent,
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
        isLocked: Boolean,
        progressPercent: Int,
        headerBitmap: Bitmap,
        ringBitmap: Bitmap?
    ) {
        val cardBg = Color(0xFF1B1B1D)
        val surfaceChip = Color(0xFF26262A)
        val nothingRed = Color(0xFFD71921)
        val white = Color(0xFFFFFFFF)
        val muted = Color(0xFF9A9AA3)

        val rootAction = if (isLocked) {
            actionStartActivity<MainActivity>()
        } else {
            actionStartActivity<QuickLockActivity>()
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(26.dp)
                .background(ColorProvider(cardBg))
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Row: Dot-matrix FOCUS LOCK header + status dot
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(headerBitmap),
                        contentDescription = "Focus Lock",
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(16.dp)
                    )
                    if (settings.widgetShowStreakBadge && xpState.currentStreak > 0) {
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(10.dp)
                                .background(ColorProvider(surfaceChip))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${xpState.currentStreak}D",
                                style = TextStyle(
                                    color = ColorProvider(white),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Center: Dotted circular ring OR compact study readout
                if (ringBitmap != null) {
                    Image(
                        provider = ImageProvider(ringBitmap),
                        contentDescription = "Focus progress",
                        modifier = GlanceModifier.size(84.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.padding(vertical = 8.dp)
                    ) {
                        if (settings.widgetShowStudyHours) {
                            Text(
                                text = formatWidgetHours(xpState.todayFocusMs, isLocked),
                                style = TextStyle(
                                    color = ColorProvider(white),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                        if (settings.widgetShowGoalPercent) {
                            Text(
                                text = "${progressPercent}% GOAL",
                                style = TextStyle(
                                    color = ColorProvider(nothingRed),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }

                if (settings.widgetShowRankXpTag) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "${xpState.rank.title.uppercase(Locale.US)} · ${xpState.totalXp} XP",
                        style = TextStyle(
                            color = ColorProvider(muted),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1
                    )
                }

                if (settings.widgetShowQuickLockButton) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(14.dp)
                            .background(ColorProvider(if (isLocked) surfaceChip else nothingRed))
                            .clickable(rootAction)
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLocked) "LOCKED" else "TAP TO LOCK",
                            style = TextStyle(
                                color = ColorProvider(white),
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

    companion object {
        private val RESPONSIVE_SIZES = setOf(
            DpSize(120.dp, 120.dp),
            DpSize(180.dp, 180.dp),
            DpSize(240.dp, 240.dp)
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

        private fun renderDotMatrixHeaderBitmap(
            context: Context,
            label: String,
            isLocked: Boolean
        ): Bitmap {
            val widthPx = 220
            val heightPx = 32
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(if (isLocked) "#10B981" else "#D71921")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(10f, heightPx / 2f, 5.5f, dotPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 20f
                typeface = loadDotTypeface(context)
                letterSpacing = 0.08f
            }
            val textY = heightPx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, 24f, textY, textPaint)

            return bitmap
        }

        private fun renderDottedRingBitmap(
            context: Context,
            progressFraction: Float,
            studyHoursText: String,
            percentText: String?,
            showStudyHours: Boolean,
            isLocked: Boolean
        ): Bitmap {
            val sizePx = 196
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val cx = sizePx / 2f
            val cy = sizePx / 2f
            val radius = sizePx * 0.42f
            val totalDots = 32
            val activeDots = (totalDots * progressFraction.coerceIn(0f, 1f)).toInt()
                .let { count -> if (progressFraction > 0f && count == 0) 1 else count }

            val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#D71921")
                style = Paint.Style.FILL
            }
            val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#38383E")
                style = Paint.Style.FILL
            }

            for (i in 0 until totalDots) {
                val angleRad = Math.toRadians((-90.0 + (360.0 / totalDots) * i))
                val dx = cx + (radius * cos(angleRad)).toFloat()
                val dy = cy + (radius * sin(angleRad)).toFloat()
                val isActive = i < activeDots
                canvas.drawCircle(
                    dx,
                    dy,
                    if (isActive) 4.3f else 3.4f,
                    if (isActive) activePaint else inactivePaint
                )
            }

            val dotFace = loadDotTypeface(context)

            if (showStudyHours) {
                val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = 23f
                    typeface = dotFace
                    textAlign = Paint.Align.CENTER
                }
                val mainY = if (percentText != null) cy + 2f else cy + 8f
                canvas.drawText(studyHoursText, cx, mainY, mainPaint)
            }

            if (percentText != null) {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor(if (isLocked) "#10B981" else "#D71921")
                    textSize = 15f
                    typeface = dotFace
                    textAlign = Paint.Align.CENTER
                }
                val subY = if (showStudyHours) cy + 24f else cy + 6f
                canvas.drawText(percentText, cx, subY, subPaint)
            }

            return bitmap
        }
    }
}
