package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockedAppGroup
import com.example.data.BlockedNotificationItem
import com.example.data.FocusSettingsRepository
import com.example.data.NotificationBlockerState
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val COLLAPSED_NOTIFICATION_LIMIT = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedSummaryBottomSheet(
    blockerState: NotificationBlockerState,
    onDoneClearLog: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groups = blockerState.groupedLastSummary
    val totalCount = blockerState.totalBlockedCount
    var expandedPackages by remember { mutableStateOf(emptySet<String>()) }

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
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "While You Were Focused",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "$totalCount distraction${if (totalCount == 1) "" else "s"} silenced across ${groups.size} app${if (groups.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.primaryContainer
                ) {
                    Text(
                        text = "$totalCount",
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary,
                        modifier = Modifier
                            .padding(horizontal = 11.dp, vertical = 4.dp)
                            .testTag("blocked_summary_total_badge")
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = palette.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneInTalk,
                        contentDescription = null,
                        tint = palette.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Incoming phone calls and alarms remained allowed.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
            }

            if (groups.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No blocked notifications logged from your last session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = groups,
                        key = { it.packageName }
                    ) { group ->
                        val isExpanded = group.packageName in expandedPackages
                        BlockedAppGroupCard(
                            group = group,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                haptics.tick()
                                expandedPackages = if (isExpanded) {
                                    expandedPackages - group.packageName
                                } else {
                                    expandedPackages + group.packageName
                                }
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    haptics.confirm()
                    onDoneClearLog()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("dismiss_blocked_summary_button")
            ) {
                Text("Done", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun BlockedAppGroupCard(
    group: BlockedAppGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val iconBitmap by produceState<ImageBitmap?>(
        initialValue = FocusSettingsRepository.getCachedAppIconBitmap(group.packageName),
        key1 = group.packageName
    ) {
        if (value == null) {
            value = FocusSettingsRepository.loadAppIconBitmapAsync(context, group.packageName)
        }
    }

    val previewItems = remember(group.items) { group.items.take(COLLAPSED_NOTIFICATION_LIMIT) }
    val extraItems = remember(group.items) {
        if (group.items.size > COLLAPSED_NOTIFICATION_LIMIT) {
            group.items.drop(COLLAPSED_NOTIFICATION_LIMIT)
        } else {
            emptyList()
        }
    }

    val cardShape = remember { RoundedCornerShape(26.dp) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusGlassPanel(shape = cardShape, palette = palette, elevated = true)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap!!,
                            contentDescription = group.appLabel,
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.tint(palette.primary, BlendMode.SrcIn),
                            modifier = Modifier.size(22.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = group.appLabel,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = group.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.5.sp,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.primaryContainer
                ) {
                    Text(
                        text = "${group.count}",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            ThemedDivider(palette = palette)

            previewItems.forEach { item ->
                NotificationSummaryItemRow(item = item, timeFormat = timeFormat)
            }

            if (extraItems.isNotEmpty()) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        extraItems.forEach { item ->
                            NotificationSummaryItemRow(item = item, timeFormat = timeFormat)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .background(palette.primaryContainer.copy(alpha = 0.78f))
                        .clickable(onClick = onToggleExpand)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("expand_notifications_toggle_${group.packageName}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isExpanded) {
                            "Show less"
                        } else {
                            "+${extraItems.size} more notification${if (extraItems.size == 1) "" else "s"}"
                        },
                        fontFamily = JetBrainsMono,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primary
                    )
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Outlined.KeyboardArrowUp
                        } else {
                            Icons.Outlined.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) {
                            "Collapse notifications"
                        } else {
                            "Expand all notifications"
                        },
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSummaryItemRow(
    item: BlockedNotificationItem,
    timeFormat: SimpleDateFormat
) {
    val palette = LocalFocusTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.5.sp,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeFormat.format(Date(item.timestampMs)),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.textSecondary
            )
        }
        if (item.snippet.isNotBlank() && item.snippet != item.title) {
            Text(
                text = item.snippet,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
