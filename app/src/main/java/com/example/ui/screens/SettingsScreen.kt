package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.MainActivity
import com.example.data.AllowedApp
import com.example.data.FocusSettingsRepository
import com.example.data.FocusSettingsState
import com.example.data.InstalledAppInfo
import com.example.data.LockAppLayout
import com.example.data.UpdateUiState
import com.example.ui.components.AllowedAppIconBadge
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.LockAllowedAppsSection
import com.example.ui.components.LockPalette
import com.example.ui.components.performClockTickHaptic
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.CrimsonBorderTint
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: FocusSettingsState,
    installedVersion: String,
    updateUiState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showAddAppDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInputText by remember(settingsState.dailyGoalMinutes) {
        mutableStateOf(settingsState.dailyGoalMinutes.toString())
    }

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasUsagePermission by remember {
        mutableStateOf((context as? MainActivity)?.hasUsageStatsPermission(context) ?: true)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasUsagePermission = (context as? MainActivity)?.hasUsageStatsPermission(context) ?: true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settingsState.allowedApps) {
        FocusSettingsRepository.preloadAppIcons(context, settingsState.allowedApps)
    }

    val noopPreviewClick = remember<((AllowedApp) -> Unit)> { {} }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusLockEmblem(
                        size = 26.dp,
                        showAmbientGlow = false
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = OledBlack
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp)
        ) {
            // 1. Locked State Allowed Apps Section (flattened & keyed for smooth 120Hz scroll)
            item(key = "allowed_apps_header", contentType = "section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ALLOWED APPS IN LOCK MODE",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        color = SlateGray
                    )
                    TextButton(
                        onClick = {
                            view.performKeyTapHaptic()
                            FocusSettingsRepository.resetAllowedAppsToDefaults(context)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("reset_apps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Defaults", fontSize = 12.sp, color = CrimsonPrimary)
                    }
                }
            }

            if (settingsState.allowedApps.isEmpty()) {
                item(key = "allowed_apps_empty", contentType = "empty_card") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Strict Lock Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 14.sp,
                                color = PureWhite
                            )
                            Text(
                                text = "No apps allowed during focus sessions. Add essential apps below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateGray
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = settingsState.allowedApps,
                    key = { _, app -> "allowed_app_${app.id}" },
                    contentType = { _, _ -> "allowed_app_row" }
                ) { index, app ->
                    val isFirst = index == 0
                    val cardShape = if (isFirst) {
                        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    } else {
                        RoundedCornerShape(0.dp)
                    }
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                view.performKeyTapHaptic()
                                FocusSettingsRepository.removeAllowedApp(context, app.id)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(ObsidianSurface)
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(CrimsonContainer)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Remove",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CrimsonPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remove ${app.label}",
                                            tint = CrimsonPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ObsidianSurface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AllowedAppIconBadge(
                                    app = app,
                                    tint = CrimsonPrimary,
                                    badgeBackground = ObsidianElevated,
                                    iconSize = 18.dp,
                                    containerSize = 36.dp,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 14.sp,
                                        color = PureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (app.isPreset) "Built-in essential · Swipe left to remove" else app.packageName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 11.sp,
                                        color = SlateGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        view.performKeyTapHaptic()
                                        FocusSettingsRepository.removeAllowedApp(context, app.id)
                                    },
                                    modifier = Modifier.testTag("remove_app_${app.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove ${app.label}",
                                        tint = SlateGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (index < settingsState.allowedApps.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = ObsidianBorder,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }

            item(key = "allowed_apps_add_footer", contentType = "footer_button") {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(0.5.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                view.performKeyTapHaptic()
                                showAddAppDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                            .testTag("add_allowed_app_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Allowed App",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 13.sp,
                            color = CrimsonPrimary
                        )
                    }
                }
            }

            // 2. Lock Screen App Layout Section (OLED Stealth Dark Live Preview)
            item(key = "lock_apps_layout_section", contentType = "layout_section") {
                SettingsSection(
                    title = "LOCK SCREEN APPS LAYOUT",
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val previewPalette = LockPalette.StealthDark
                        val previewApps = settingsState.allowedApps.ifEmpty {
                            FocusSettingsRepository.DEFAULT_ALLOWED_APPS
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(previewPalette.background)
                                .border(
                                    width = 1.dp,
                                    color = ObsidianBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE PREVIEW",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateGray
                                )
                                Text(
                                    text = settingsState.lockAppLayout.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CrimsonPrimary
                                )
                            }

                            LockAllowedAppsSection(
                                apps = previewApps,
                                layout = settingsState.lockAppLayout,
                                palette = previewPalette,
                                onAppClick = noopPreviewClick,
                                isPreview = true
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LockAppLayout.entries.forEach { layout ->
                                val selected = settingsState.lockAppLayout == layout
                                Surface(
                                    onClick = {
                                        view.performClockTickHaptic()
                                        FocusSettingsRepository.setLockAppLayout(context, layout)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) CrimsonContainer else ObsidianElevated.copy(alpha = 0.55f),
                                    border = if (selected) {
                                        BorderStroke(1.dp, CrimsonBorderTint)
                                    } else {
                                        BorderStroke(1.dp, Color.Transparent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("layout_option_${layout.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = layout.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontSize = 14.sp,
                                                color = PureWhite
                                            )
                                            Text(
                                                text = layout.subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = 12.sp,
                                                color = SlateGray
                                            )
                                        }
                                        RadioButton(
                                            selected = selected,
                                            onClick = {
                                                view.performClockTickHaptic()
                                                FocusSettingsRepository.setLockAppLayout(context, layout)
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = CrimsonPrimary,
                                                unselectedColor = SlateGray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Focus & Lock Behavior Section
            item(key = "focus_behavior_section", contentType = "behavior_section") {
                SettingsSection(
                    title = "FOCUS & LOCK BEHAVIOR",
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    view.performKeyTapHaptic()
                                    goalInputText = settingsState.dailyGoalMinutes.toString()
                                    showGoalDialog = true
                                }
                                .padding(vertical = 4.dp)
                                .testTag("settings_daily_goal_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Focus Goal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Target study duration per day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = SlateGray
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CrimsonContainer,
                                border = BorderStroke(1.dp, CrimsonBorderTint)
                            ) {
                                Text(
                                    text = "${settingsState.dailyGoalMinutes} min",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = ObsidianBorder)

                        SettingsSwitchRow(
                            title = "Live Session Timer",
                            subtitle = "Display elapsed focus time on the lock screen",
                            checked = settingsState.showSessionTimer,
                            onCheckedChange = {
                                view.performClockTickHaptic()
                                FocusSettingsRepository.setShowSessionTimer(context, it)
                            },
                            testTag = "switch_session_timer"
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = ObsidianBorder)

                        SettingsSwitchRow(
                            title = "Keypad Haptic Feedback",
                            subtitle = "Subtle tactile pulse when entering unlock PIN",
                            checked = settingsState.hapticKeypad,
                            onCheckedChange = {
                                view.performClockTickHaptic()
                                FocusSettingsRepository.setHapticKeypad(context, it)
                            },
                            testTag = "switch_haptic_keypad"
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = ObsidianBorder)

                        SettingsSwitchRow(
                            title = "Auto-Clear Wrong PIN",
                            subtitle = "Reset digits automatically after an incorrect attempt",
                            checked = settingsState.autoClearWrongPin,
                            onCheckedChange = {
                                view.performClockTickHaptic()
                                FocusSettingsRepository.setAutoClearWrongPin(context, it)
                            },
                            testTag = "switch_auto_clear_pin"
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = ObsidianBorder)

                        // App Switch Grace Period
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column {
                                Text(
                                    text = "App Launch Grace Period",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Delay before re-locking if you leave an allowed app",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = SlateGray
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1, 2, 3, 5).forEach { sec ->
                                    val selected = settingsState.appSwitchGraceSeconds == sec
                                    Surface(
                                        onClick = {
                                            view.performClockTickHaptic()
                                            FocusSettingsRepository.setAppSwitchGraceSeconds(context, sec)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) CrimsonContainer else ObsidianElevated,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) CrimsonBorderTint else ObsidianBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${sec}s",
                                                fontFamily = JetBrainsMono,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selected) CrimsonPrimary else SlateGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Private GitHub In-App Updates Section
            item(key = "app_updates_section", contentType = "updates_section") {
                SettingsSection(
                    title = "APP UPDATES",
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CrimsonContainer)
                                        .border(1.dp, CrimsonBorderTint, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SystemUpdate,
                                        contentDescription = "Updates",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Focus Lock Releases",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 15.sp,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "Installed: v$installedVersion",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 12.sp,
                                        color = SlateGray
                                    )
                                }
                            }

                            when (updateUiState) {
                                is UpdateUiState.UpToDate -> {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = FocusEmerald.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "Latest",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FocusEmerald,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                is UpdateUiState.UpdateAvailable -> {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = CrimsonContainer,
                                        border = BorderStroke(1.dp, CrimsonBorderTint),
                                        modifier = Modifier.clickable { onOpenUpdateSheet() }
                                    ) {
                                        Text(
                                            text = updateUiState.release.tagName,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        }

                        when (updateUiState) {
                            is UpdateUiState.UpToDate -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = FocusEmerald.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, FocusEmerald.copy(alpha = 0.28f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("latest_version_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = FocusEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "You are using the latest version",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PureWhite
                                        )
                                    }
                                }
                            }
                            is UpdateUiState.Error -> {
                                Text(
                                    text = updateUiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = CrimsonPrimary
                                )
                            }
                            else -> Unit
                        }

                        val isChecking = updateUiState is UpdateUiState.Checking
                        val btnInteraction = remember { MutableInteractionSource() }
                        val btnScale = rememberSpringPressScale(btnInteraction)

                        Button(
                            onClick = {
                                view.performKeyTapHaptic()
                                if (updateUiState is UpdateUiState.UpdateAvailable) {
                                    onOpenUpdateSheet()
                                } else {
                                    onCheckForUpdates()
                                }
                            },
                            enabled = !isChecking,
                            interactionSource = btnInteraction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianElevated,
                                contentColor = PureWhite
                            ),
                            border = BorderStroke(1.dp, CrimsonBorderTint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .graphicsLayer {
                                    scaleX = btnScale
                                    scaleY = btnScale
                                }
                                .testTag("check_updates_button")
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = CrimsonPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking GitHub...", fontSize = 13.5.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (updateUiState is UpdateUiState.UpdateAvailable) {
                                        "View Update (${updateUiState.release.tagName})"
                                    } else {
                                        "Check for Updates"
                                    },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 5. System Permissions Status
            item(key = "permissions_section", contentType = "permissions_section") {
                SettingsSection(title = "PERMISSIONS") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PermissionStatusRow(
                            title = "Display Over Other Apps",
                            granted = hasOverlayPermission,
                            onClick = {
                                view.performKeyTapHaptic()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = ObsidianBorder)
                        PermissionStatusRow(
                            title = "Usage Access Monitoring",
                            granted = hasUsagePermission,
                            onClick = {
                                view.performKeyTapHaptic()
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddAppDialog) {
        AddAllowedAppDialog(
            currentlyAllowed = settingsState.allowedApps,
            onAddApp = { app ->
                view.performKeyTapHaptic()
                FocusSettingsRepository.addAllowedApp(context, app)
                showAddAppDialog = false
            },
            onDismiss = { showAddAppDialog = false }
        )
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = PureWhite,
            textContentColor = SlateGray,
            title = { Text("Daily Focus Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose a preset or enter custom minutes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30L, 60L, 90L, 120L).forEach { preset ->
                            val isChosen = goalInputText == preset.toString()
                            Surface(
                                onClick = {
                                    view.performClockTickHaptic()
                                    goalInputText = preset.toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) CrimsonContainer else ObsidianElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isChosen) CrimsonBorderTint else ObsidianBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${preset}m",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PureWhite
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = goalInputText,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                goalInputText = newValue
                            }
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        view.performKeyTapHaptic()
                        val parsed = goalInputText.toLongOrNull() ?: 60L
                        if (parsed > 0) {
                            FocusSettingsRepository.setDailyGoalMinutes(context, parsed)
                        }
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonPrimary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = SlateGray)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                color = SlateGray
            )
            trailingAction?.invoke()
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
                color = PureWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = SlateGray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = CrimsonPrimary,
                uncheckedThumbColor = SlateGray,
                uncheckedTrackColor = ObsidianElevated,
                uncheckedBorderColor = ObsidianBorder
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (granted) FocusEmerald else CrimsonPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                color = PureWhite
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = if (granted) FocusEmerald.copy(alpha = 0.14f) else CrimsonContainer,
            border = if (granted) null else BorderStroke(1.dp, CrimsonBorderTint)
        ) {
            Text(
                text = if (granted) "Granted" else "Configure",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                color = if (granted) FocusEmerald else CrimsonPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AddAllowedAppDialog(
    currentlyAllowed: List<AllowedApp>,
    onAddApp: (AllowedApp) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        installedApps = FocusSettingsRepository.loadLaunchableApps(context)
        isLoadingApps = false
    }

    val existingPackages = remember(currentlyAllowed) {
        currentlyAllowed.map { it.packageName }.toSet()
    }

    val missingPresets = remember(currentlyAllowed) {
        FocusSettingsRepository.DEFAULT_ALLOWED_APPS.filter { it.packageName !in existingPackages }
    }

    val filteredInstalled = remember(installedApps, existingPackages, searchQuery) {
        installedApps.filter { app ->
            app.packageName !in existingPackages &&
                (searchQuery.isBlank() ||
                    app.label.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = PureWhite,
        textContentColor = SlateGray,
        title = {
            Text("Add Allowed App", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoadingApps) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = CrimsonPrimary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (missingPresets.isNotEmpty() && searchQuery.isBlank()) {
                            item {
                                Text(
                                    text = "BUILT-IN ESSENTIALS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateGray,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(missingPresets, key = { it.id }) { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onAddApp(preset) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AllowedAppIconBadge(
                                        app = preset,
                                        tint = CrimsonPrimary,
                                        badgeBackground = CrimsonContainer,
                                        iconSize = 18.dp,
                                        containerSize = 34.dp,
                                        shape = CircleShape
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                            color = PureWhite
                                        )
                                        Text(
                                            text = "Quick essential preset",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 11.sp,
                                            color = SlateGray
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add ${preset.label}",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (filteredInstalled.isNotEmpty()) {
                            item {
                                Text(
                                    text = "INSTALLED APPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateGray,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(filteredInstalled, key = { it.packageName }) { info ->
                                val previewAllowedApp = remember(info) {
                                    AllowedApp(
                                        id = "pkg_${info.packageName}",
                                        label = info.label,
                                        packageName = info.packageName,
                                        isPreset = false
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onAddApp(previewAllowedApp) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AllowedAppIconBadge(
                                        app = previewAllowedApp,
                                        tint = CrimsonPrimary,
                                        badgeBackground = ObsidianElevated,
                                        iconSize = 18.dp,
                                        containerSize = 34.dp,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = info.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                            color = PureWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = info.packageName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 11.sp,
                                            color = SlateGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add ${info.label}",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else if (missingPresets.isEmpty()) {
                            item {
                                Text(
                                    text = "No matching apps available to add.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateGray,
                                    modifier = Modifier.padding(vertical = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = CrimsonPrimary)
            }
        }
    )
}
