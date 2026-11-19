package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusWhatsNewCatalog
import com.example.data.WhatsNewCategory
import com.example.data.WhatsNewItem
import com.example.data.WhatsNewRelease
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(
    installedVersion: String,
    releases: List<WhatsNewRelease> = FocusWhatsNewCatalog.releases,
    onDismiss: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val latestRelease = releases.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp)
                .testTag("whats_new_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    palette.primary.copy(alpha = 0.24f),
                                    palette.primaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "What's New",
                        tint = palette.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "What's New",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = palette.displayFont,
                            color = palette.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primaryContainer
                        ) {
                            Text(
                                text = "v$installedVersion",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (latestRelease != null) {
                        Text(
                            text = latestRelease.headline,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.5.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            if (latestRelease != null) {
                val newItems = remember(latestRelease) {
                    latestRelease.items.filter { it.category == WhatsNewCategory.NEW }
                }
                val fixItems = remember(latestRelease) {
                    latestRelease.items.filter { it.category == WhatsNewCategory.FIX }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (newItems.isNotEmpty()) {
                        WhatsNewCategoryCard(
                            sectionTitle = "NEW",
                            count = newItems.size,
                            accentColor = palette.primary,
                            badgeBackground = palette.primaryContainer,
                            headerIcon = Icons.Outlined.AutoAwesome,
                            itemIcon = Icons.Outlined.AutoAwesome,
                            items = newItems,
                            headerTestTag = "whats_new_section_new"
                        )
                    }

                    if (fixItems.isNotEmpty()) {
                        WhatsNewCategoryCard(
                            sectionTitle = "FIXES",
                            count = fixItems.size,
                            accentColor = palette.success,
                            badgeBackground = palette.success.copy(alpha = 0.14f),
                            headerIcon = Icons.Outlined.Build,
                            itemIcon = Icons.Outlined.CheckCircle,
                            items = fixItems,
                            headerTestTag = "whats_new_section_fixes"
                        )
                    }
                }
            }

            // Primary CTA
            val btnInteraction = remember { MutableInteractionSource() }
            val btnScale = rememberSpringPressScale(btnInteraction)
            Button(
                onClick = {
                    haptics.confirm()
                    onDismiss()
                },
                interactionSource = btnInteraction,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .graphicsLayer {
                        scaleX = btnScale
                        scaleY = btnScale
                    }
                    .testTag("whats_new_got_it_button")
            ) {
                Text(
                    text = "Got It",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WhatsNewCategoryCard(
    sectionTitle: String,
    count: Int,
    accentColor: Color,
    badgeBackground: Color,
    headerIcon: ImageVector,
    itemIcon: ImageVector,
    items: List<WhatsNewItem>,
    headerTestTag: String
) {
    val palette = LocalFocusTheme.current
    val cardShape = remember { RoundedCornerShape(28.dp) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusGlassPanel(shape = cardShape, palette = palette, elevated = true)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(headerTestTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = badgeBackground
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = sectionTitle,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = accentColor
                    )
                }
            }

            Text(
                text = "$count ${if (count == 1) "update" else "updates"}",
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                color = palette.textSecondary
            )
        }

        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(badgeBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = itemIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.2.sp,
                        lineHeight = 17.sp,
                        color = palette.textSecondary
                    )
                }
            }

            if (index < items.lastIndex) {
                ThemedDivider(palette = palette)
            }
        }
    }
}
