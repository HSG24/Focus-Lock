package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.LocalFocusTheme
import kotlin.math.roundToInt

/**
 * Shared exact lock-screen tray width metrics used by ManualLockScreen,
 * ScheduledLockScreen, and the Settings 1:1 Live Preview so pill wrapping
 * matches reality down to the pixel.
 */
object LockAppsTrayMetrics {
    const val OUTER_HORIZONTAL_PADDING_DP = 22f
    const val MAX_CONTENT_WIDTH_DP = 400f
    const val TRAY_INNER_HORIZONTAL_PADDING_DP = 16f

    fun computeLockTrayContentWidthDp(screenWidthDp: Float): Float {
        val containerW = minOf(screenWidthDp - (OUTER_HORIZONTAL_PADDING_DP * 2f), MAX_CONTENT_WIDTH_DP)
        return (containerW - (TRAY_INNER_HORIZONTAL_PADDING_DP * 2f)).coerceAtLeast(180f)
    }
}

@Immutable
data class LockPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val accentContent: Color
) {
    companion object {
        val StealthDark = LockPalette(
            background = Color(0xFF000000),
            surface = Color(0xFF141416),
            surfaceElevated = Color(0xFF1B1B1D),
            border = Color(0xFF28282C),
            primaryText = Color(0xFFFFFFFF),
            secondaryText = Color(0xFF9A9AA3),
            accent = Color(0xFFFF1E27),
            accentContent = Color(0xFFFFFFFF)
        )

        fun fromFocusTheme(theme: FocusThemePalette): LockPalette = LockPalette(
            background = theme.background,
            surface = theme.surface,
            surfaceElevated = theme.surfaceElevated,
            border = theme.border,
            primaryText = theme.textPrimary,
            secondaryText = theme.textSecondary,
            accent = theme.primary,
            accentContent = theme.onPrimary
        )

        @Composable
        fun fromMaterialTheme(): LockPalette = fromFocusTheme(LocalFocusTheme.current)
    }
}

@Composable
fun AllowedAppIconBadge(
    app: AllowedApp,
    tint: Color,
    badgeBackground: Color,
    iconSize: Dp = 18.dp,
    containerSize: Dp = 32.dp,
    shape: RoundedCornerShape = CircleShape,
    preloadedIcon: ImageBitmap? = null,
    loadIfMissing: Boolean = false
) {
    val resolvedIcon: ImageBitmap? = if (loadIfMissing) {
        val context = LocalContext.current
        val initial = preloadedIcon ?: FocusSettingsRepository.getCachedAppIconBitmap(app.packageName)
        val asyncLoaded by produceState<ImageBitmap?>(
            initialValue = initial,
            key1 = app.packageName,
            key2 = preloadedIcon
        ) {
            if (value == null && !app.packageName.startsWith("__builtin_")) {
                value = FocusSettingsRepository.loadAppIconBitmapAsync(context, app.packageName)
            }
        }
        asyncLoaded
    } else {
        preloadedIcon ?: FocusSettingsRepository.getCachedAppIconBitmap(app.packageName)
    }

    Box(
        modifier = Modifier
            .size(containerSize)
            .clip(shape)
            .background(badgeBackground, shape),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedIcon != null) {
            Image(
                bitmap = resolvedIcon,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(tint, BlendMode.SrcIn),
                modifier = Modifier.size(iconSize * 1.12f)
            )
        } else {
            Icon(
                imageVector = presetIconFor(app.packageName),
                contentDescription = app.label,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

fun presetIconFor(packageName: String): ImageVector = when (packageName) {
    FocusSettingsRepository.PRESET_PHONE_PKG -> Icons.Outlined.Phone
    FocusSettingsRepository.PRESET_GOOGLE_PKG -> Icons.Outlined.Search
    FocusSettingsRepository.PRESET_XIAOMI_PKG -> Icons.Outlined.Home
    else -> Icons.Outlined.Apps
}

private fun testTagForApp(app: AllowedApp): String = when (app.packageName) {
    FocusSettingsRepository.PRESET_PHONE_PKG -> "phone_button"
    FocusSettingsRepository.PRESET_GOOGLE_PKG -> "google_button"
    FocusSettingsRepository.PRESET_XIAOMI_PKG -> "xiaomi_home_button"
    else -> "allowed_app_${app.packageName}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LockAllowedAppsSection(
    apps: List<AllowedApp>,
    layout: LockAppLayout,
    palette: LockPalette,
    onAppClick: (AllowedApp) -> Unit,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    pillButtonSizeDp: Int = FocusSettingsRepository.DEFAULT_PILL_BUTTON_SIZE_DP,
    iconCache: Map<String, ImageBitmap> = emptyMap(),
    readFromCacheOnly: Boolean = true,
    onRowCountMeasured: ((Int) -> Unit)? = null
) {
    if (apps.isEmpty()) return
    val themePalette = LocalFocusTheme.current
    val activeTheme = themePalette.theme
    val haptics = rememberFocusHaptics()

    when (layout) {
        LockAppLayout.PILL_ROW -> {
            val pillShape = RoundedCornerShape(50)
            val clampedSize = pillButtonSizeDp.coerceIn(
                FocusSettingsRepository.MIN_PILL_BUTTON_SIZE_DP,
                FocusSettingsRepository.MAX_PILL_BUTTON_SIZE_DP
            )
            val sizeRatio = (clampedSize - FocusSettingsRepository.MIN_PILL_BUTTON_SIZE_DP).toFloat() /
                (FocusSettingsRepository.MAX_PILL_BUTTON_SIZE_DP - FocusSettingsRepository.MIN_PILL_BUTTON_SIZE_DP).toFloat()

            // Exact 1:1 dimensions in both real lock screen and Settings Live Preview
            val pillHeight = clampedSize.dp
            val containerDp = (clampedSize * 0.62f).dp
            val iconDp = (clampedSize * 0.38f).dp
            val gapBetweenPills = (4f + 5f * sizeRatio).dp
            val innerGap = (4f + 4f * sizeRatio).dp
            val startPad = (4f + 6f * sizeRatio).dp
            val endPad = (7f + 7f * sizeRatio).dp
            val labelSp = (9.5f + 4f * sizeRatio).sp
            val maxLabelW = (54f + 48f * sizeRatio).dp

            val itemRowBuckets = remember(apps.size, clampedSize) { mutableStateMapOf<Int, Int>() }
            if (onRowCountMeasured != null) {
                val distinctRows = itemRowBuckets.values.distinct().size
                LaunchedEffect(distinctRows, apps.size) {
                    if (itemRowBuckets.size >= apps.size && distinctRows > 0) {
                        onRowCountMeasured(distinctRows)
                    }
                }
            }

            FlowRow(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gapBetweenPills, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(gapBetweenPills)
            ) {
                apps.forEachIndexed { idx, app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(innerGap),
                        modifier = Modifier
                            .then(
                                if (onRowCountMeasured != null) {
                                    Modifier.onGloballyPositioned { coords ->
                                        // Bucket y into 8px bands so sub-pixel rounding never inflates row count
                                        val bucket = (coords.positionInParent().y / 8f).roundToInt()
                                        if (itemRowBuckets[idx] != bucket) {
                                            itemRowBuckets[idx] = bucket
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .height(pillHeight)
                            .focusGlassPanel(shape = pillShape, palette = themePalette)
                            .clickable(enabled = !isPreview) {
                                haptics.tap()
                                onAppClick(app)
                            }
                            .padding(start = startPad, end = endPad)
                            .testTag(testTagForApp(app))
                    ) {
                        AllowedAppIconBadge(
                            app = app,
                            tint = palette.accent,
                            badgeBackground = palette.surfaceElevated,
                            iconSize = iconDp,
                            containerSize = containerDp,
                            preloadedIcon = iconCache[FocusSettingsRepository.iconCacheKey(app.packageName, activeTheme)],
                            loadIfMissing = !readFromCacheOnly
                        )
                        Text(
                            text = app.label,
                            fontSize = labelSp,
                            fontWeight = FontWeight.Medium,
                            color = palette.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = maxLabelW)
                        )
                    }
                }
            }
        }

        LockAppLayout.ICON_DOCK -> {
            val dockShape = RoundedCornerShape(50)
            Row(
                modifier = modifier
                    .focusGlassPanel(shape = dockShape, palette = themePalette)
                    .horizontalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (isPreview) 10.dp else 14.dp,
                        vertical = if (isPreview) 6.dp else 8.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(if (isPreview) 10.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                apps.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = !isPreview) {
                                haptics.tap()
                                onAppClick(app)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag(testTagForApp(app))
                    ) {
                        AllowedAppIconBadge(
                            app = app,
                            tint = palette.accent,
                            badgeBackground = palette.surfaceElevated,
                            iconSize = if (isPreview) 18.dp else 22.dp,
                            containerSize = if (isPreview) 34.dp else 44.dp,
                            shape = CircleShape,
                            preloadedIcon = iconCache[FocusSettingsRepository.iconCacheKey(app.packageName, activeTheme)],
                            loadIfMissing = !readFromCacheOnly
                        )
                        Text(
                            text = app.label,
                            fontSize = if (isPreview) 9.sp else 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 56.dp)
                        )
                    }
                }
            }
        }

        LockAppLayout.COMPACT_LIST -> {
            val visibleApps = if (isPreview) apps.take(3) else apps
            val cardShape = RoundedCornerShape(if (isPreview) 22.dp else 28.dp)
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .focusGlassPanel(shape = cardShape, palette = themePalette)
            ) {
                visibleApps.forEachIndexed { index, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isPreview) {
                                haptics.tap()
                                onAppClick(app)
                            }
                            .height(if (isPreview) 34.dp else 46.dp)
                            .padding(horizontal = 14.dp)
                            .testTag(testTagForApp(app)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AllowedAppIconBadge(
                            app = app,
                            tint = palette.accent,
                            badgeBackground = palette.surfaceElevated,
                            iconSize = if (isPreview) 14.dp else 16.dp,
                            containerSize = if (isPreview) 24.dp else 30.dp,
                            shape = CircleShape,
                            preloadedIcon = iconCache[FocusSettingsRepository.iconCacheKey(app.packageName, activeTheme)],
                            loadIfMissing = !readFromCacheOnly
                        )
                        Text(
                            text = app.label,
                            fontSize = if (isPreview) 11.sp else 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open ${app.label}",
                            tint = palette.secondaryText,
                            modifier = Modifier.size(if (isPreview) 16.dp else 18.dp)
                        )
                    }
                    if (index < visibleApps.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = palette.border.copy(alpha = 0.45f),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            }
        }

        LockAppLayout.SPLIT_CHIPS -> {
            val rows = apps.chunked(2)
            val chipShape = RoundedCornerShape(50)
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isPreview) 34.dp else 44.dp)
                                    .focusGlassPanel(shape = chipShape, palette = themePalette, elevated = true)
                                    .clickable(enabled = !isPreview) {
                                        haptics.tap()
                                        onAppClick(app)
                                    }
                                    .padding(horizontal = 10.dp)
                                    .testTag(testTagForApp(app))
                            ) {
                                AllowedAppIconBadge(
                                    app = app,
                                    tint = palette.accent,
                                    badgeBackground = palette.surface,
                                    iconSize = if (isPreview) 14.dp else 16.dp,
                                    containerSize = if (isPreview) 22.dp else 28.dp,
                                    shape = CircleShape,
                                    preloadedIcon = iconCache[FocusSettingsRepository.iconCacheKey(app.packageName, activeTheme)],
                                    loadIfMissing = !readFromCacheOnly
                                )
                                Text(
                                    text = app.label,
                                    fontSize = if (isPreview) 11.sp else 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
