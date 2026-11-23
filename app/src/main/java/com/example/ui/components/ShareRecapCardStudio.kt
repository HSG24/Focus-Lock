package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.Session
import com.example.data.FocusXpState
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.DotGothic16
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

enum class RecapCardTheme(val label: String) {
    NOTHING_MONO("Nothing"),
    LIQUID_GLASS("Glass"),
    CRIMSON("Crimson"),
    AMBER("Amber"),
    SAGE("Sage")
}

enum class RecapTimeframe(val label: String, val days: Int) {
    TODAY("Today", 1),
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    CUSTOM("Custom", 1)
}

@Immutable
data class RecapCardData(
    val timeframeLabel: String,
    val dateRangeLabel: String,
    val totalFocusMs: Long,
    val sessionCount: Int,
    val dailyGoalMinutes: Long,
    val goalPercent: Int,
    val currentStreak: Int,
    val longestSessionMs: Long,
    /** Last 35 days normalized intensity 0..3 ending on the anchor date */
    val miniHeatmapLevels: List<Int>
)

@Immutable
data class RecapCardOptions(
    val theme: RecapCardTheme = RecapCardTheme.NOTHING_MONO,
    val showStreakAndBest: Boolean = true,
    val showGoalProgress: Boolean = true,
    val showMiniHeatmap: Boolean = true
)

object ShareRecapCardRenderer {

    private data class CardColors(
        val bgTop: Int,
        val bgBottom: Int,
        val panel: Int,
        val accent: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val dotInactive: Int,
        val dotMid: Int
    )

    private fun resolveColors(theme: RecapCardTheme): CardColors = when (theme) {
        RecapCardTheme.NOTHING_MONO -> CardColors(
            bgTop = android.graphics.Color.parseColor("#0C0C0F"),
            bgBottom = android.graphics.Color.parseColor("#16161B"),
            panel = android.graphics.Color.parseColor("#1E1E24"),
            accent = android.graphics.Color.parseColor("#D71921"),
            textPrimary = android.graphics.Color.WHITE,
            textSecondary = android.graphics.Color.parseColor("#9B9BA6"),
            dotInactive = android.graphics.Color.parseColor("#2B2B33"),
            dotMid = android.graphics.Color.parseColor("#B0B0BC")
        )
        RecapCardTheme.LIQUID_GLASS -> {
            val p = FocusThemePalette.from(AppVisualTheme.LIQUID_GLASS)
            CardColors(
                bgTop = android.graphics.Color.parseColor("#0A0D14"),
                bgBottom = android.graphics.Color.parseColor("#161D2B"),
                panel = android.graphics.Color.parseColor("#202838"),
                accent = p.primary.toArgb(),
                textPrimary = p.textPrimary.toArgb(),
                textSecondary = p.textSecondary.toArgb(),
                dotInactive = android.graphics.Color.parseColor("#2B3447"),
                dotMid = android.graphics.Color.parseColor("#9DB2CE")
            )
        }
        RecapCardTheme.CRIMSON -> {
            val p = FocusThemePalette.from(AppVisualTheme.NOTHING_DOT)
            CardColors(
                bgTop = p.background.toArgb(),
                bgBottom = android.graphics.Color.parseColor("#1B0B0E"),
                panel = p.surfaceElevated.toArgb(),
                accent = p.primary.toArgb(),
                textPrimary = p.textPrimary.toArgb(),
                textSecondary = p.textSecondary.toArgb(),
                dotInactive = android.graphics.Color.parseColor("#2C2225"),
                dotMid = android.graphics.Color.parseColor("#C98A90")
            )
        }
        RecapCardTheme.AMBER -> {
            val p = FocusThemePalette.from(AppVisualTheme.SOLAR_AMBER)
            CardColors(
                bgTop = p.background.toArgb(),
                bgBottom = android.graphics.Color.parseColor("#1D160B"),
                panel = p.surfaceElevated.toArgb(),
                accent = p.primary.toArgb(),
                textPrimary = p.textPrimary.toArgb(),
                textSecondary = p.textSecondary.toArgb(),
                dotInactive = android.graphics.Color.parseColor("#2F281C"),
                dotMid = android.graphics.Color.parseColor("#D5B278")
            )
        }
        RecapCardTheme.SAGE -> {
            CardColors(
                bgTop = android.graphics.Color.parseColor("#070908"),
                bgBottom = android.graphics.Color.parseColor("#0E1A16"),
                panel = android.graphics.Color.parseColor("#1C2420"),
                accent = android.graphics.Color.parseColor("#8EC2A1"),
                textPrimary = android.graphics.Color.parseColor("#F4F7F4"),
                textSecondary = android.graphics.Color.parseColor("#9EADA4"),
                dotInactive = android.graphics.Color.parseColor("#22302B"),
                dotMid = android.graphics.Color.parseColor("#8AB8A6")
            )
        }
    }

    fun buildRecapData(
        sessions: List<Session>,
        xpState: FocusXpState,
        dailyGoalMinutes: Long,
        timeframe: RecapTimeframe,
        customDateMs: Long? = null
    ): RecapCardData {
        val anchorCal = Calendar.getInstance().apply {
            if (timeframe == RecapTimeframe.CUSTOM && customDateMs != null) {
                timeInMillis = customDateMs
            }
        }
        val endDayCal = (anchorCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val spanDays = if (timeframe == RecapTimeframe.CUSTOM) 1 else timeframe.days
        val startDayCal = (anchorCal.clone() as Calendar).apply {
            add(Calendar.DATE, -(spanDays - 1))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val filtered = sessions.filter { it.startTime in startDayCal.timeInMillis..endDayCal.timeInMillis }
        val totalMs = filtered.sumOf { it.duration }
        val longestSessionMs = filtered.maxOfOrNull { it.duration } ?: 0L
        val targetMs = dailyGoalMinutes.coerceAtLeast(1L) * 60_000L * spanDays
        val goalPct = ((totalMs.toFloat() / targetMs.toFloat()) * 100f).roundToInt().coerceIn(0, 999)

        val fmt = SimpleDateFormat("MMM d", Locale.US)
        val fullDayFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
        val dateRangeLabel = if (spanDays == 1) {
            fullDayFmt.format(anchorCal.time)
        } else {
            "${fmt.format(startDayCal.time)} – ${fmt.format(anchorCal.time)}"
        }
        val timeframeChip = if (timeframe == RecapTimeframe.CUSTOM) {
            fmt.format(anchorCal.time).uppercase(Locale.US)
        } else {
            timeframe.label.uppercase(Locale.US)
        }

        val anchorMidnight = (anchorCal.clone() as Calendar).apply {
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
        val singleDayGoal = dailyGoalMinutes.coerceAtLeast(15L) * 60_000L
        val miniHeatmap = (34 downTo 0).map { daysAgo ->
            val cal = (anchorMidnight.clone() as Calendar).apply { add(Calendar.DATE, -daysAgo) }
            val ms = dayTotals[cal.timeInMillis] ?: 0L
            val ratio = (ms.toFloat() / singleDayGoal.toFloat()).coerceIn(0f, 1f)
            when {
                ratio >= 0.85f -> 3
                ratio >= 0.45f -> 2
                ratio > 0f -> 1
                else -> 0
            }
        }

        return RecapCardData(
            timeframeLabel = timeframeChip,
            dateRangeLabel = dateRangeLabel,
            totalFocusMs = totalMs,
            sessionCount = filtered.size,
            dailyGoalMinutes = dailyGoalMinutes,
            goalPercent = goalPct,
            currentStreak = xpState.currentStreak,
            longestSessionMs = longestSessionMs,
            miniHeatmapLevels = miniHeatmap
        )
    }

    fun formatDurationHeadline(ms: Long): String {
        val totalMins = (ms / 60_000L).coerceAtLeast(0L)
        val hrs = totalMins / 60L
        val mins = totalMins % 60L
        return if (hrs > 0L) "${hrs}h ${mins}m" else "${mins}m"
    }

    fun renderCardBitmap(
        context: Context,
        data: RecapCardData,
        options: RecapCardOptions,
        widthPx: Int = 1080,
        heightPx: Int = 1350
    ): Bitmap {
        val colors = resolveColors(options.theme)
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = widthPx / 1080f

        val dotFace = runCatching {
            ResourcesCompat.getFont(context, R.font.dotgothic16) ?: Typeface.MONOSPACE
        }.getOrDefault(Typeface.MONOSPACE)
        val monoFace = runCatching {
            ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
        }.getOrDefault(Typeface.MONOSPACE)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, widthPx.toFloat(), heightPx.toFloat(),
                colors.bgTop, colors.bgBottom, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()),
            72f * s, 72f * s, bgPaint
        )

        // Subtle ambient glow
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                widthPx * 0.8f, heightPx * 0.16f, widthPx * 0.55f,
                intArrayOf((colors.accent and 0x00FFFFFF) or 0x26000000, android.graphics.Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), glowPaint)

        val pad = 84f * s

        // Top header: Accent dot + FOCUS LOCK + timeframe pill
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent }
        canvas.drawCircle(pad + 14f * s, pad + 26f * s, 12f * s, dotPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textPrimary
            textSize = 36f * s
            typeface = dotFace
            isFakeBoldText = true
            letterSpacing = 0.12f
        }
        canvas.drawText("FOCUS LOCK", pad + 44f * s, pad + 38f * s, brandPaint)

        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.panel }
        val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.accent
            textSize = 28f * s
            typeface = monoFace
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val chipW = 216f * s
        val chipH = 62f * s
        val chipRect = RectF(widthPx - pad - chipW, pad - 4f * s, widthPx - pad, pad - 4f * s + chipH)
        canvas.drawRoundRect(chipRect, chipH / 2f, chipH / 2f, chipPaint)
        canvas.drawText(
            data.timeframeLabel,
            chipRect.centerX(),
            chipRect.centerY() - (chipTextPaint.descent() + chipTextPaint.ascent()) / 2f,
            chipTextPaint
        )

        // Date subtitle
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondary
            textSize = 32f * s
            typeface = monoFace
        }
        canvas.drawText(data.dateRangeLabel, pad, pad + 116f * s, datePaint)

        // Hero Focus Duration
        val heroY = 395f * s
        val heroLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondary
            textSize = 28f * s
            typeface = monoFace
            letterSpacing = 0.14f
        }
        canvas.drawText("DEEP FOCUS TIME", pad, heroY - 115f * s, heroLabelPaint)

        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textPrimary
            textSize = 152f * s
            typeface = dotFace
            isFakeBoldText = true
        }
        val headline = formatDurationHeadline(data.totalFocusMs)
        canvas.drawText(headline, pad, heroY + 25f * s, heroPaint)

        val sessionsText = "${data.sessionCount} ${if (data.sessionCount == 1) "session" else "sessions"} completed"
        canvas.drawText(sessionsText, pad, heroY + 96f * s, datePaint)

        var cursorY = heroY + 175f * s

        // Goal progress capsule bar
        if (options.showGoalProgress) {
            val panelRect = RectF(pad, cursorY, widthPx - pad, cursorY + 170f * s)
            canvas.drawRoundRect(panelRect, 48f * s, 48f * s, chipPaint)

            val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textSecondary
                textSize = 26f * s
                typeface = monoFace
            }
            val pctP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.accent
                textSize = 34f * s
                typeface = dotFace
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("GOAL PROGRESS", pad + 42f * s, cursorY + 62f * s, labelP)
            canvas.drawText("${data.goalPercent}%", widthPx - pad - 42f * s, cursorY + 64f * s, pctP)

            // 24 dotted capsules
            val barLeft = pad + 42f * s
            val barRight = widthPx - pad - 42f * s
            val barY = cursorY + 116f * s
            val count = 24
            val clamped = (data.goalPercent / 100f).coerceIn(0f, 1f)
            val filled = if (clamped > 0f) (count * clamped).roundToInt().coerceIn(1, count) else 0
            val gap = 12f * s
            val dotD = ((barRight - barLeft) - gap * (count - 1)) / count.toFloat()
            val pDot = Paint(Paint.ANTI_ALIAS_FLAG)
            for (i in 0 until count) {
                val cx = barLeft + i * (dotD + gap) + dotD / 2f
                pDot.color = when {
                    filled > 0 && i == filled - 1 -> colors.accent
                    i < filled -> colors.textPrimary
                    else -> colors.dotInactive
                }
                canvas.drawCircle(cx, barY, if (i == filled - 1) dotD * 0.56f else dotD * 0.46f, pDot)
            }
            cursorY += 210f * s
        }

        // Streak & Best Session dual curved pills
        if (options.showStreakAndBest) {
            val halfGap = 24f * s
            val cardW = (widthPx - pad * 2f - halfGap) / 2f
            val cardH = 178f * s

            val leftRect = RectF(pad, cursorY, pad + cardW, cursorY + cardH)
            val rightRect = RectF(pad + cardW + halfGap, cursorY, widthPx - pad, cursorY + cardH)
            canvas.drawRoundRect(leftRect, 48f * s, 48f * s, chipPaint)
            canvas.drawRoundRect(rightRect, 48f * s, 48f * s, chipPaint)

            val kPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textSecondary
                textSize = 24f * s
                typeface = monoFace
            }
            val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textPrimary
                textSize = 54f * s
                typeface = dotFace
                isFakeBoldText = true
            }
            val streakLabel = "${data.currentStreak} ${if (data.currentStreak == 1) "DAY" else "DAYS"}"
            canvas.drawText("ACTIVE STREAK", leftRect.left + 40f * s, leftRect.top + 58f * s, kPaint)
            canvas.drawText(streakLabel, leftRect.left + 40f * s, leftRect.top + 132f * s, vPaint)

            val bestLabel = if (data.longestSessionMs > 0L) {
                formatDurationHeadline(data.longestSessionMs).uppercase(Locale.US)
            } else {
                "—"
            }
            canvas.drawText("BEST SESSION", rightRect.left + 40f * s, rightRect.top + 58f * s, kPaint)
            canvas.drawText(bestLabel, rightRect.left + 40f * s, rightRect.top + 132f * s, vPaint)

            cursorY += cardH + 40f * s
        }

        // Mini 5-week dot heatmap
        if (options.showMiniHeatmap) {
            val hmRect = RectF(pad, cursorY, widthPx - pad, cursorY + 230f * s)
            canvas.drawRoundRect(hmRect, 48f * s, 48f * s, chipPaint)

            val kPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textSecondary
                textSize = 24f * s
                typeface = monoFace
            }
            canvas.drawText("35-DAY CONSISTENCY", hmRect.left + 42f * s, hmRect.top + 54f * s, kPaint)

            val cols = 7
            val rows = 5
            val gridLeft = hmRect.left + 48f * s
            val gridRight = hmRect.right - 48f * s
            val gridTop = hmRect.top + 82f * s
            val gridBottom = hmRect.bottom - 30f * s
            val cellW = (gridRight - gridLeft) / cols.toFloat()
            val cellH = (gridBottom - gridTop) / rows.toFloat()
            val r = minOf(cellW, cellH) * 0.31f
            val dotP = Paint(Paint.ANTI_ALIAS_FLAG)
            for (i in data.miniHeatmapLevels.indices) {
                val col = i % cols
                val row = i / cols
                val lvl = data.miniHeatmapLevels[i]
                dotP.color = when (lvl) {
                    3 -> colors.accent
                    2 -> colors.textPrimary
                    1 -> colors.dotMid
                    else -> colors.dotInactive
                }
                canvas.drawCircle(
                    gridLeft + col * cellW + cellW / 2f,
                    gridTop + row * cellH + cellH / 2f,
                    r,
                    dotP
                )
            }
        }

        // Minimal footer watermark
        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondary
            textSize = 26f * s
            typeface = monoFace
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FOCUS LOCK · DEEP FOCUS RECAP", widthPx / 2f, heightPx - 58f * s, footPaint)

        return bmp
    }

    fun shareRecapBitmap(context: Context, bitmap: Bitmap): Result<Unit> = runCatching {
        val file = File(context.cacheDir, "focus_lock_recap_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My Focus Lock recap")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Focus Recap Card"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareRecapCardStudio(
    allSessions: List<Session>,
    xpState: FocusXpState,
    dailyGoalMinutes: Long
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val scope = rememberCoroutineScope()

    var timeframe by remember { mutableStateOf(RecapTimeframe.TODAY) }
    var customDateMs by remember { mutableLongStateOf(startOfLocalDay(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }

    var options by remember {
        mutableStateOf(
            RecapCardOptions(
                theme = when (palette.theme) {
                    AppVisualTheme.LIQUID_GLASS -> RecapCardTheme.LIQUID_GLASS
                    AppVisualTheme.NOTHING_DOT -> RecapCardTheme.NOTHING_MONO
                    AppVisualTheme.SOLAR_AMBER -> RecapCardTheme.AMBER
                }
            )
        )
    }
    var isExporting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val todayStartMs = remember { startOfLocalDay(System.currentTimeMillis()) }
    val canStepForward = customDateMs < todayStartMs

    val recapData = remember(allSessions, xpState, dailyGoalMinutes, timeframe, customDateMs) {
        ShareRecapCardRenderer.buildRecapData(
            sessions = allSessions,
            xpState = xpState,
            dailyGoalMinutes = dailyGoalMinutes,
            timeframe = timeframe,
            customDateMs = if (timeframe == RecapTimeframe.CUSTOM) customDateMs else null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Timeframe selector (Today · 7 Days · 30 Days · Custom)
        ElasticMorphingSegmentedPill(
            segments = remember {
                RecapTimeframe.entries.map {
                    ElasticPillSegment(it.label, "recap_timeframe_${it.name.lowercase()}")
                }
            },
            selectedIndex = RecapTimeframe.entries.indexOf(timeframe).coerceAtLeast(0),
            onSelectIndex = { idx ->
                haptics.segment()
                timeframe = RecapTimeframe.entries.getOrNull(idx) ?: RecapTimeframe.TODAY
            },
            height = 42.dp,
            fontSize = 12.sp
        )

        // Minimal curved Custom Date picker bar (visible when Custom is selected)
        AnimatedVisibility(visible = timeframe == RecapTimeframe.CUSTOM) {
            val formattedCustomDate = remember(customDateMs) {
                SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(Date(customDateMs))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGlassPanel(shape = RoundedCornerShape(50), palette = palette)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("recap_custom_date_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceElevated)
                        .clickable {
                            haptics.tick()
                            customDateMs = shiftLocalDays(customDateMs, -1)
                        }
                        .testTag("recap_custom_date_prev"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Previous Day",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            haptics.tap()
                            showDatePicker = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("recap_custom_date_picker_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Pick Date",
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedCustomDate,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceElevated.copy(alpha = if (canStepForward) 1f else 0.4f))
                        .clickable(enabled = canStepForward) {
                            haptics.tick()
                            val next = shiftLocalDays(customDateMs, +1)
                            customDateMs = next.coerceAtMost(todayStartMs)
                        }
                        .testTag("recap_custom_date_next"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Next Day",
                        tint = if (canStepForward) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Live Recap Card Preview
        RecapCardLivePreview(
            data = recapData,
            options = options
        )

        // Card Theme & Modules pills
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusGlassPanel(shape = RoundedCornerShape(28.dp), palette = palette)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CARD THEME",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = palette.textSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecapCardTheme.entries.forEach { cardTheme ->
                    val selected = options.theme == cardTheme
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) palette.primary else palette.surfaceElevated)
                            .clickable {
                                haptics.tick()
                                options = options.copy(theme = cardTheme)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("recap_theme_${cardTheme.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cardTheme.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) palette.onPrimary else palette.textSecondary
                        )
                    }
                }
            }

            ThemedDivider(palette = palette)

            ThemedSettingsSwitchRow(
                title = "Goal Progress Capsule",
                subtitle = "Dotted goal completion bar & percentage",
                checked = options.showGoalProgress,
                onCheckedChange = { options = options.copy(showGoalProgress = it) },
                testTag = "recap_toggle_goal"
            )
            ThemedSettingsSwitchRow(
                title = "Streak & Best Session",
                subtitle = "Active focus streak and longest single block",
                checked = options.showStreakAndBest,
                onCheckedChange = { options = options.copy(showStreakAndBest = it) },
                testTag = "recap_toggle_streak"
            )
            ThemedSettingsSwitchRow(
                title = "35-Day Consistency Grid",
                subtitle = "Minimal Nothing dot-matrix heatmap",
                checked = options.showMiniHeatmap,
                onCheckedChange = { options = options.copy(showMiniHeatmap = it) },
                testTag = "recap_toggle_heatmap"
            )
        }

        if (statusMessage != null) {
            Text(
                text = statusMessage!!,
                fontSize = 12.sp,
                color = palette.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Button(
            onClick = {
                haptics.tap()
                isExporting = true
                statusMessage = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        val bmp = ShareRecapCardRenderer.renderCardBitmap(context, recapData, options)
                        ShareRecapCardRenderer.shareRecapBitmap(context, bmp)
                    }
                    isExporting = false
                    if (result.isSuccess) {
                        haptics.confirm()
                    } else {
                        haptics.reject()
                        statusMessage = "Could not export recap image."
                    }
                }
            },
            enabled = !isExporting,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primary,
                contentColor = palette.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("share_recap_image_button")
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = palette.onPrimary
                )
            } else {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Image Card", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDatePicker) {
        val initialUtcMillis = remember(customDateMs) { localDayToUtcMidnight(customDateMs) }
        val todayUtcMillis = remember(todayStartMs) { localDayToUtcMidnight(todayStartMs) }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayUtcMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utcMs ->
                            customDateMs = utcMidnightToLocalDay(utcMs).coerceAtMost(todayStartMs)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = palette.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun startOfLocalDay(ms: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun shiftLocalDays(ms: Long, deltaDays: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = ms
        add(Calendar.DATE, deltaDays)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun localDayToUtcMidnight(localMs: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMs }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcMidnightToLocalDay(utcMs: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

@Composable
private fun RecapCardLivePreview(
    data: RecapCardData,
    options: RecapCardOptions
) {
    val bgBrush = remember(options.theme) {
        when (options.theme) {
            RecapCardTheme.NOTHING_MONO -> Brush.linearGradient(listOf(Color(0xFF0C0C0F), Color(0xFF16161B)))
            RecapCardTheme.LIQUID_GLASS -> Brush.linearGradient(listOf(Color(0xFF0A0D14), Color(0xFF161D2B)))
            RecapCardTheme.CRIMSON -> Brush.linearGradient(listOf(Color(0xFF0D0B0C), Color(0xFF1B0B0E)))
            RecapCardTheme.AMBER -> Brush.linearGradient(listOf(Color(0xFF0D0C0A), Color(0xFF1D160B)))
            RecapCardTheme.SAGE -> Brush.linearGradient(listOf(Color(0xFF0B0E0D), Color(0xFF0E1A16)))
        }
    }
    val panelColor = remember(options.theme) {
        when (options.theme) {
            RecapCardTheme.NOTHING_MONO -> Color(0xFF1E1E24)
            RecapCardTheme.LIQUID_GLASS -> Color(0xFF202838)
            RecapCardTheme.CRIMSON -> Color(0xFF231618)
            RecapCardTheme.AMBER -> Color(0xFF241E14)
            RecapCardTheme.SAGE -> Color(0xFF16221E)
        }
    }
    val accent = remember(options.theme) {
        when (options.theme) {
            RecapCardTheme.NOTHING_MONO -> Color(0xFFD71921)
            RecapCardTheme.LIQUID_GLASS -> FocusThemePalette.from(AppVisualTheme.LIQUID_GLASS).primary
            RecapCardTheme.CRIMSON -> FocusThemePalette.from(AppVisualTheme.NOTHING_DOT).primary
            RecapCardTheme.AMBER -> FocusThemePalette.from(AppVisualTheme.SOLAR_AMBER).primary
            RecapCardTheme.SAGE -> Color(0xFF8EC2A1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(32.dp))
            .background(bgBrush)
            .padding(22.dp)
            .testTag("recap_card_preview")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
                    Text(
                        text = "FOCUS LOCK",
                        fontFamily = DotGothic16,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(panelColor)
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = data.timeframeLabel,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }

            // Hero stat
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = data.dateRangeLabel,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = Color(0xFF9B9BA6)
                )
                Text(
                    text = ShareRecapCardRenderer.formatDurationHeadline(data.totalFocusMs),
                    fontFamily = DotGothic16,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${data.sessionCount} ${if (data.sessionCount == 1) "session" else "sessions"} completed",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = Color(0xFF9B9BA6)
                )
            }

            // Optional panels
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (options.showGoalProgress) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(panelColor)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GOAL PROGRESS",
                                fontFamily = JetBrainsMono,
                                fontSize = 9.5.sp,
                                color = Color(0xFF9B9BA6)
                            )
                            Text(
                                text = "${data.goalPercent}%",
                                fontFamily = DotGothic16,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            val count = 24
                            val clamped = (data.goalPercent / 100f).coerceIn(0f, 1f)
                            val filled = if (clamped > 0f) (count * clamped).roundToInt().coerceIn(1, count) else 0
                            val gap = 3.dp.toPx()
                            val segW = (size.width - gap * (count - 1)) / count.toFloat()
                            val r = minOf(segW * 0.45f, size.height * 0.45f)
                            for (i in 0 until count) {
                                val cx = i * (segW + gap) + segW / 2f
                                val col = when {
                                    filled > 0 && i == filled - 1 -> accent
                                    i < filled -> Color.White
                                    else -> Color.White.copy(alpha = 0.16f)
                                }
                                drawCircle(
                                    color = col,
                                    radius = if (i == filled - 1) r * 1.15f else r,
                                    center = Offset(cx, size.height / 2f)
                                )
                            }
                        }
                    }
                }

                if (options.showStreakAndBest) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(panelColor)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("STREAK", fontFamily = JetBrainsMono, fontSize = 9.sp, color = Color(0xFF9B9BA6))
                            Text(
                                text = "${data.currentStreak} ${if (data.currentStreak == 1) "DAY" else "DAYS"}",
                                fontFamily = DotGothic16,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(panelColor)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("BEST SESSION", fontFamily = JetBrainsMono, fontSize = 9.sp, color = Color(0xFF9B9BA6))
                            Text(
                                text = if (data.longestSessionMs > 0L) {
                                    ShareRecapCardRenderer.formatDurationHeadline(data.longestSessionMs).uppercase(Locale.US)
                                } else "—",
                                fontFamily = DotGothic16,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (options.showMiniHeatmap) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(panelColor)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "35-DAY CONSISTENCY",
                            fontFamily = JetBrainsMono,
                            fontSize = 9.sp,
                            color = Color(0xFF9B9BA6)
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            val cols = 7
                            val rows = 5
                            val cw = size.width / cols
                            val ch = size.height / rows
                            val r = minOf(cw, ch) * 0.28f
                            for (i in data.miniHeatmapLevels.indices) {
                                val col = i % cols
                                val row = i / cols
                                val lvl = data.miniHeatmapLevels[i]
                                val c = when (lvl) {
                                    3 -> accent
                                    2 -> Color.White
                                    1 -> Color.White.copy(alpha = 0.45f)
                                    else -> Color.White.copy(alpha = 0.14f)
                                }
                                drawCircle(c, r, Offset(col * cw + cw / 2f, row * ch + ch / 2f))
                            }
                        }
                    }
                }
            }

            Text(
                text = "FOCUS LOCK · DEEP FOCUS RECAP",
                fontFamily = JetBrainsMono,
                fontSize = 9.5.sp,
                color = Color(0xFF9B9BA6),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
