package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
            surface = Color(0xFF0D0D0F),
            surfaceElevated = Color(0xFF141418),
            border = Color(0xFF222228),
            primaryText = Color(0xFFFFFFFF),
            secondaryText = Color(0xFF8E8E98),
            accent = Color(0xFFFF1E27),
            accentContent = Color(0xFFFFFFFF)
        )

        @Composable
        fun fromMaterialTheme(): LockPalette = StealthDark
    }
}

@Composable
fun AllowedAppIconBadge(
    app: AllowedApp,
    tint: Color,
    badgeBackground: Color,
    iconSize: Dp = 18.dp,
    containerSize: Dp = 32.dp,
    shape: RoundedCornerShape = CircleShape
) {
    val context = LocalContext.current
    val cachedBitmap = FocusSettingsRepository.getCachedAppIconBitmap(app.packageName)
    val bitmap by produceState<ImageBitmap?>(
        initialValue = cachedBitmap,
        key1 = app.packageName
    ) {
        if (value == null && !app.packageName.startsWith("__builtin_")) {
            value = FocusSettingsRepository.loadAppIconBitmapAsync(context, app.packageName)
        }
    }

    Box(
        modifier = Modifier
            .size(containerSize)
            .clip(shape)
            .background(badgeBackground),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(containerSize * 0.72f)
                    .clip(RoundedCornerShape(6.dp))
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
    isPreview: Boolean = false
) {
    if (apps.isEmpty()) return

    when (layout) {
        LockAppLayout.PILL_ROW -> {
            FlowRow(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                apps.forEach { app ->
                    Surface(
                        onClick = { onAppClick(app) },
                        shape = RoundedCornerShape(50),
                        color = palette.surface,
                        contentColor = palette.primaryText,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .height(if (isPreview) 36.dp else 48.dp)
                            .testTag(testTagForApp(app))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(
                                start = if (isPreview) 8.dp else 10.dp,
                                end = if (isPreview) 12.dp else 14.dp
                            )
                        ) {
                            AllowedAppIconBadge(
                                app = app,
                                tint = palette.accent,
                                badgeBackground = palette.surfaceElevated,
                                iconSize = if (isPreview) 14.dp else 16.dp,
                                containerSize = if (isPreview) 22.dp else 28.dp
                            )
                            Text(
                                text = app.label,
                                fontSize = if (isPreview) 11.sp else 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 100.dp)
                            )
                        }
                    }
                }
            }
        }

        LockAppLayout.GLASS_GRID -> {
            val columns = if (apps.size <= 2) apps.size else 3
            val rows = apps.chunked(columns)
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowApps.forEach { app ->
                            Surface(
                                onClick = { onAppClick(app) },
                                shape = RoundedCornerShape(if (isPreview) 14.dp else 18.dp),
                                color = palette.surface,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag(testTagForApp(app))
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(if (isPreview) 5.dp else 7.dp),
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = if (isPreview) 8.dp else 12.dp
                                    )
                                ) {
                                    AllowedAppIconBadge(
                                        app = app,
                                        tint = palette.accent,
                                        badgeBackground = palette.surfaceElevated,
                                        iconSize = if (isPreview) 16.dp else 20.dp,
                                        containerSize = if (isPreview) 28.dp else 36.dp,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Text(
                                        text = app.label,
                                        fontSize = if (isPreview) 10.sp else 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        repeat(columns - rowApps.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        LockAppLayout.ICON_DOCK -> {
            Surface(
                shape = RoundedCornerShape(50),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier
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
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onAppClick(app) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag(testTagForApp(app))
                        ) {
                            AllowedAppIconBadge(
                                app = app,
                                tint = palette.primaryText,
                                badgeBackground = palette.surfaceElevated,
                                iconSize = if (isPreview) 18.dp else 22.dp,
                                containerSize = if (isPreview) 34.dp else 44.dp,
                                shape = CircleShape
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
        }

        LockAppLayout.COMPACT_LIST -> {
            val visibleApps = if (isPreview) apps.take(3) else apps
            Surface(
                shape = RoundedCornerShape(if (isPreview) 14.dp else 18.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    visibleApps.forEachIndexed { index, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app) }
                                .height(if (isPreview) 36.dp else 48.dp)
                                .padding(horizontal = 12.dp)
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
                                shape = RoundedCornerShape(8.dp)
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
                                color = palette.border
                            )
                        }
                    }
                }
            }
        }

        LockAppLayout.SPLIT_CHIPS -> {
            val rows = apps.chunked(2)
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
                            Surface(
                                onClick = { onAppClick(app) },
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceElevated,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isPreview) 36.dp else 48.dp)
                                    .testTag(testTagForApp(app))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    AllowedAppIconBadge(
                                        app = app,
                                        tint = palette.accent,
                                        badgeBackground = palette.surface,
                                        iconSize = if (isPreview) 14.dp else 16.dp,
                                        containerSize = if (isPreview) 22.dp else 28.dp,
                                        shape = RoundedCornerShape(8.dp)
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
