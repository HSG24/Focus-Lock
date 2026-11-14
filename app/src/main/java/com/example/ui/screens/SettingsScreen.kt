package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AllowedApp
import com.example.data.FocusSettingsRepository
import com.example.data.FocusSettingsState
import com.example.data.FocusXpRepository
import com.example.data.InstalledAppInfo
import com.example.data.LockAppLayout
import com.example.data.NotificationBlockerRepository
import com.example.data.UpdateUiState
import com.example.service.FocusNotificationBlockerService
import com.example.service.FocusPowerBlockerAccessibilityService
import com.example.ui.components.AllowedAppIconBadge
import com.example.ui.components.AppearanceAndThemesSection
import com.example.ui.components.DailyStudyGoalSection
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.HomeScreenWidgetSection
import com.example.ui.components.LockAllowedAppsSection
import com.example.ui.components.LockPalette
import com.example.ui.components.ProtectionAndBlockersSection
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemedDivider
import com.example.ui.components.ThemedSettingsSectionCard
import com.example.ui.components.ThemedSettingsSwitchRow
import com.example.ui.components.performClockTickHaptic
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.launch

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
    val palette = LocalFocusTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val iconCache by FocusSettingsRepository.appIconsState.collectAsStateWithLifecycle()
    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()
    val blockerState by NotificationBlockerRepository.observe(context).collectAsStateWithLifecycle()

    var showAddAppDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInputText by remember(settingsState.dailyGoalMinutes) {
        mutableStateOf(settingsState.dailyGoalMinutes.toString())
    }

    var hasOverlayPermission by remember { mutableStateOf(true) }
    var hasUsagePermission by remember { mutableStateOf(true) }
    var hasNotificationAccess by remember { mutableStateOf(false) }
    var hasAccessibilityGuard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasOverlayPermission = FocusSettingsRepository.canDrawOverlaysAsync(context)
        hasUsagePermission = FocusSettingsRepository.hasUsageStatsPermissionAsync(context)
        hasNotificationAccess = FocusNotificationBlockerService.isNotificationListenerEnabled(context)
        hasAccessibilityGuard = FocusPowerBlockerAccessibilityService.isPowerBlockerAccessibilityEnabled(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    hasOverlayPermission = FocusSettingsRepository.canDrawOverlaysAsync(context)
                    hasUsagePermission = FocusSettingsRepository.hasUsageStatsPermissionAsync(context)
                    hasNotificationAccess = FocusNotificationBlockerService.isNotificationListenerEnabled(context)
                    hasAccessibilityGuard = FocusPowerBlockerAccessibilityService.isPowerBlockerAccessibilityEnabled(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settingsState.allowedApps) {
        FocusSettingsRepository.preloadAppIcons(context, settingsState.allowedApps)
    }

    val onSelectTheme = remember(context, view) {
        { theme: AppVisualTheme ->
            view.performClockTickHaptic()
            FocusSettingsRepository.setAppTheme(context, theme)
        }
    }
    val onResetDefaults = remember(context, view) {
        {
            view.performKeyTapHaptic()
            FocusSettingsRepository.resetAllowedAppsToDefaults(context)
        }
    }
    val onRemoveApp = remember(context, view) {
        { appId: String ->
            view.performKeyTapHaptic()
            FocusSettingsRepository.removeAllowedApp(context, appId)
        }
    }
    val onOpenAddApp = remember(view) {
        {
            view.performKeyTapHaptic()
            showAddAppDialog = true
        }
    }
    val onSelectLayout = remember(context, view) {
        { layout: LockAppLayout ->
            view.performClockTickHaptic()
            FocusSettingsRepository.setLockAppLayout(context, layout)
        }
    }
    val onOpenGoalDialog = remember(view, settingsState.dailyGoalMinutes) {
        {
            view.performKeyTapHaptic()
            goalInputText = settingsState.dailyGoalMinutes.toString()
            showGoalDialog = true
        }
    }
    val onToggleSessionTimer = remember(context, view) {
        { enabled: Boolean ->
            view.performClockTickHaptic()
            FocusSettingsRepository.setShowSessionTimer(context, enabled)
        }
    }
    val onToggleHapticKeypad = remember(context, view) {
        { enabled: Boolean ->
            view.performClockTickHaptic()
            FocusSettingsRepository.setHapticKeypad(context, enabled)
        }
    }
    val onToggleAutoClearWrongPin = remember(context, view) {
        { enabled: Boolean ->
            view.performClockTickHaptic()
            FocusSettingsRepository.setAutoClearWrongPin(context, enabled)
        }
    }
    val onSelectGraceSeconds = remember(context, view) {
        { sec: Int ->
            view.performClockTickHaptic()
            FocusSettingsRepository.setAppSwitchGraceSeconds(context, sec)
        }
    }
    val onOpenOverlaySettings = remember(context, view) {
        {
            view.performKeyTapHaptic()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }
    val onOpenUsageSettings = remember(context, view) {
        {
            view.performKeyTapHaptic()
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }
    val onOpenNotificationListenerSettings = remember(context, view) {
        {
            view.performKeyTapHaptic()
            runCatching {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            Unit
        }
    }
    val onOpenAccessibilitySettings = remember(context, view) {
        {
            view.performKeyTapHaptic()
            runCatching {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .navigationBarsPadding()
    ) {
        ThemeBackdropLayer(palette = palette)

        Column(modifier = Modifier.fillMaxSize()) {
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
                            fontFamily = palette.displayFont,
                            color = palette.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AppearanceAndThemesSection(
                    selectedTheme = settingsState.appTheme,
                    onSelectTheme = onSelectTheme
                )

                HomeScreenWidgetSection(
                    settingsState = settingsState,
                    xpState = xpState,
                    onToggleDottedRing = { enabled ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setWidgetShowDottedRing(context, enabled)
                    },
                    onToggleStudyHours = { enabled ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setWidgetShowStudyHours(context, enabled)
                    },
                    onToggleGoalPercent = { enabled ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setWidgetShowGoalPercent(context, enabled)
                    },
                    onToggleStreakBadge = { enabled ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setWidgetShowStreakBadge(context, enabled)
                    },
                    onToggleRankXpTag = { enabled ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setWidgetShowRankXpTag(context, enabled)
                    },
                    onToggleQuickLockButton = { enabled ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setWidgetShowQuickLockButton(context, enabled)
                    }
                )

                DailyStudyGoalSection(
                    dailyGoalMinutes = settingsState.dailyGoalMinutes,
                    onSelectGoalMinutes = { mins ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setDailyGoalMinutes(context, mins)
                    },
                    onOpenCustomGoalDialog = onOpenGoalDialog
                )

                ProtectionAndBlockersSection(
                    blockNotifications = settingsState.blockNotificationsDuringLock,
                    notificationAccessGranted = hasNotificationAccess,
                    hasBlockedLog = blockerState.lastSessionSummary.isNotEmpty(),
                    blockPowerMenu = settingsState.blockPowerMenuDuringLock,
                    accessibilityGranted = hasAccessibilityGuard,
                    onToggleBlockNotifications = { enabled ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setBlockNotificationsDuringLock(context, enabled)
                    },
                    onOpenNotificationAccessSettings = onOpenNotificationListenerSettings,
                    onOpenLastUnlockSummary = {
                        view.performKeyTapHaptic()
                        NotificationBlockerRepository.openLastSummarySheet(context)
                    },
                    onToggleBlockPowerMenu = { enabled ->
                        view.performClockTickHaptic()
                        FocusSettingsRepository.setBlockPowerMenuDuringLock(context, enabled)
                    },
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings
                )

                AllowedAppsSection(
                    allowedApps = settingsState.allowedApps,
                    iconCache = iconCache,
                    onResetDefaults = onResetDefaults,
                    onRemoveApp = onRemoveApp,
                    onOpenAddApp = onOpenAddApp
                )

                LockScreenLayoutSection(
                    allowedApps = settingsState.allowedApps,
                    selectedLayout = settingsState.lockAppLayout,
                    iconCache = iconCache,
                    onSelectLayout = onSelectLayout
                )

                FocusBehaviorSection(
                    showSessionTimer = settingsState.showSessionTimer,
                    hapticKeypad = settingsState.hapticKeypad,
                    autoClearWrongPin = settingsState.autoClearWrongPin,
                    appSwitchGraceSeconds = settingsState.appSwitchGraceSeconds,
                    onToggleSessionTimer = onToggleSessionTimer,
                    onToggleHapticKeypad = onToggleHapticKeypad,
                    onToggleAutoClearWrongPin = onToggleAutoClearWrongPin,
                    onSelectGraceSeconds = onSelectGraceSeconds
                )

                AppUpdatesSection(
                    installedVersion = installedVersion,
                    updateUiState = updateUiState,
                    onCheckForUpdates = onCheckForUpdates,
                    onOpenUpdateSheet = onOpenUpdateSheet
                )

                PermissionsSection(
                    hasOverlayPermission = hasOverlayPermission,
                    hasUsagePermission = hasUsagePermission,
                    hasNotificationAccess = hasNotificationAccess,
                    hasAccessibilityGuard = hasAccessibilityGuard,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenUsageSettings = onOpenUsageSettings,
                    onOpenNotificationSettings = onOpenNotificationListenerSettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings
                )
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
        DailyGoalDialog(
            goalInputText = goalInputText,
            onGoalInputChange = { goalInputText = it },
            onSave = {
                view.performKeyTapHaptic()
                val parsed = goalInputText.toLongOrNull() ?: 240L
                if (parsed > 0) {
                    FocusSettingsRepository.setDailyGoalMinutes(context, parsed)
                }
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
        )
    }
}

@Composable
private fun AllowedAppsSection(
    allowedApps: List<AllowedApp>,
    iconCache: Map<String, ImageBitmap>,
    onResetDefaults: () -> Unit,
    onRemoveApp: (String) -> Unit,
    onOpenAddApp: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val iconShape = remember { RoundedCornerShape(10.dp) }

    ThemedSettingsSectionCard(
        title = "ALLOWED APPS IN LOCK MODE",
        trailingAction = {
            TextButton(
                onClick = onResetDefaults,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("reset_apps_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Defaults", fontSize = 12.sp, color = palette.primary)
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (allowedApps.isEmpty()) {
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
                        color = palette.textPrimary
                    )
                    Text(
                        text = "No apps allowed during focus sessions. Add essential apps below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            } else {
                allowedApps.forEachIndexed { index, app ->
                    key(app.id) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onRemoveApp(app.id)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(palette.primaryContainer)
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
                                            color = palette.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remove ${app.label}",
                                            tint = palette.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.surface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AllowedAppIconBadge(
                                    app = app,
                                    tint = palette.primary,
                                    badgeBackground = palette.surfaceElevated,
                                    iconSize = 18.dp,
                                    containerSize = 36.dp,
                                    shape = iconShape,
                                    preloadedIcon = iconCache[app.packageName],
                                    loadIfMissing = false
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 14.sp,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (app.isPreset) {
                                            "Built-in essential · Swipe left to remove"
                                        } else {
                                            app.packageName
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 11.sp,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveApp(app.id) },
                                    modifier = Modifier.testTag("remove_app_${app.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove ${app.label}",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (index < allowedApps.lastIndex) {
                            ThemedDivider(
                                palette = palette,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }

            ThemedDivider(palette = palette)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                    .clickable(onClick = onOpenAddApp)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("add_allowed_app_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Allowed App",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 13.sp,
                    color = palette.primary
                )
            }
        }
    }
}

@Composable
private fun LockScreenLayoutSection(
    allowedApps: List<AllowedApp>,
    selectedLayout: LockAppLayout,
    iconCache: Map<String, ImageBitmap>,
    onSelectLayout: (LockAppLayout) -> Unit
) {
    val palette = LocalFocusTheme.current
    val previewShape = remember { RoundedCornerShape(16.dp) }
    val optionShape = remember { RoundedCornerShape(12.dp) }
    val noopPreviewClick = remember<((AllowedApp) -> Unit)> { {} }
    val previewPalette = remember(palette) { LockPalette.fromFocusTheme(palette) }
    val previewApps = remember(allowedApps) {
        allowedApps.ifEmpty { FocusSettingsRepository.DEFAULT_ALLOWED_APPS }
    }

    ThemedSettingsSectionCard(title = "LOCK SCREEN APPS LAYOUT") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(previewShape)
                    .background(previewPalette.background, previewShape)
                    .border(1.dp, palette.border, previewShape)
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
                        color = palette.textSecondary
                    )
                    Text(
                        text = selectedLayout.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primary
                    )
                }

                LockAllowedAppsSection(
                    apps = previewApps,
                    layout = selectedLayout,
                    palette = previewPalette,
                    onAppClick = noopPreviewClick,
                    isPreview = true,
                    iconCache = iconCache,
                    readFromCacheOnly = true
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LockAppLayout.entries.forEach { layout ->
                    val selected = selectedLayout == layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(optionShape)
                            .background(
                                color = if (selected) palette.primaryContainer else palette.surfaceElevated.copy(alpha = 0.55f),
                                shape = optionShape
                            )
                            .then(
                                if (selected) {
                                    Modifier.border(1.dp, palette.primaryBorderTint, optionShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectLayout(layout) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("layout_option_${layout.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = layout.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 14.sp,
                                color = palette.textPrimary
                            )
                            Text(
                                text = layout.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = palette.textSecondary
                            )
                        }
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectLayout(layout) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = palette.primary,
                                unselectedColor = palette.textSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusBehaviorSection(
    showSessionTimer: Boolean,
    hapticKeypad: Boolean,
    autoClearWrongPin: Boolean,
    appSwitchGraceSeconds: Int,
    onToggleSessionTimer: (Boolean) -> Unit,
    onToggleHapticKeypad: (Boolean) -> Unit,
    onToggleAutoClearWrongPin: (Boolean) -> Unit,
    onSelectGraceSeconds: (Int) -> Unit
) {
    val palette = LocalFocusTheme.current
    val chipShape = remember { RoundedCornerShape(10.dp) }

    ThemedSettingsSectionCard(title = "FOCUS & LOCK BEHAVIOR") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemedSettingsSwitchRow(
                title = "Live Session Timer",
                subtitle = "Display elapsed focus time on the lock screen",
                checked = showSessionTimer,
                onCheckedChange = onToggleSessionTimer,
                testTag = "switch_session_timer"
            )

            ThemedDivider(palette = palette)

            ThemedSettingsSwitchRow(
                title = "Keypad Haptic Feedback",
                subtitle = "Subtle tactile pulse when entering unlock PIN",
                checked = hapticKeypad,
                onCheckedChange = onToggleHapticKeypad,
                testTag = "switch_haptic_keypad"
            )

            ThemedDivider(palette = palette)

            ThemedSettingsSwitchRow(
                title = "Auto-Clear Wrong PIN",
                subtitle = "Reset digits automatically after an incorrect attempt",
                checked = autoClearWrongPin,
                onCheckedChange = onToggleAutoClearWrongPin,
                testTag = "switch_auto_clear_pin"
            )

            ThemedDivider(palette = palette)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text(
                        text = "App Launch Grace Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Delay before re-locking if you leave an allowed app",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 5).forEach { sec ->
                        val selected = appSwitchGraceSeconds == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(chipShape)
                                .background(
                                    color = if (selected) palette.primaryContainer else palette.surfaceElevated,
                                    shape = chipShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) palette.primaryBorderTint else palette.border,
                                    shape = chipShape
                                )
                                .clickable { onSelectGraceSeconds(sec) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sec}s",
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) palette.primary else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdatesSection(
    installedVersion: String,
    updateUiState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateSheet: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val view = LocalView.current
    val pillShape = remember { RoundedCornerShape(50) }
    val bannerShape = remember { RoundedCornerShape(12.dp) }
    val iconShape = remember { RoundedCornerShape(10.dp) }

    ThemedSettingsSectionCard(title = "APP UPDATES") {
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
                            .clip(iconShape)
                            .background(palette.primaryContainer, iconShape)
                            .border(1.dp, palette.primaryBorderTint, iconShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = "Updates",
                            tint = palette.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Focus Lock Releases",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Installed: v$installedVersion",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                when (updateUiState) {
                    is UpdateUiState.UpToDate -> {
                        Text(
                            text = "Latest",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.success,
                            modifier = Modifier
                                .clip(pillShape)
                                .background(palette.success.copy(alpha = 0.14f), pillShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    is UpdateUiState.UpdateAvailable -> {
                        Text(
                            text = updateUiState.release.tagName,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary,
                            modifier = Modifier
                                .clip(pillShape)
                                .background(palette.primaryContainer, pillShape)
                                .border(1.dp, palette.primaryBorderTint, pillShape)
                                .clickable { onOpenUpdateSheet() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    else -> Unit
                }
            }

            when (updateUiState) {
                is UpdateUiState.UpToDate -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(bannerShape)
                            .background(palette.success.copy(alpha = 0.10f), bannerShape)
                            .border(1.dp, palette.success.copy(alpha = 0.28f), bannerShape)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("latest_version_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = palette.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "You are using the latest version",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                    }
                }
                is UpdateUiState.Error -> {
                    Text(
                        text = updateUiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = palette.primary
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
                    containerColor = palette.surfaceElevated,
                    contentColor = palette.textPrimary
                ),
                border = BorderStroke(1.dp, palette.primaryBorderTint),
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
                        color = palette.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking GitHub...", fontSize = 13.5.sp)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = palette.primary,
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

@Composable
private fun PermissionsSection(
    hasOverlayPermission: Boolean,
    hasUsagePermission: Boolean,
    hasNotificationAccess: Boolean,
    hasAccessibilityGuard: Boolean,
    onOpenOverlaySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val palette = LocalFocusTheme.current
    ThemedSettingsSectionCard(title = "PERMISSIONS") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PermissionStatusRow(
                title = "Display Over Other Apps",
                granted = hasOverlayPermission,
                onClick = onOpenOverlaySettings
            )
            ThemedDivider(palette = palette)
            PermissionStatusRow(
                title = "Usage Access Monitoring",
                granted = hasUsagePermission,
                onClick = onOpenUsageSettings
            )
            ThemedDivider(palette = palette)
            PermissionStatusRow(
                title = "Notification Listener Access",
                granted = hasNotificationAccess,
                onClick = onOpenNotificationSettings
            )
            ThemedDivider(palette = palette)
            PermissionStatusRow(
                title = "Accessibility Power Guard",
                granted = hasAccessibilityGuard,
                onClick = onOpenAccessibilitySettings
            )
        }
    }
}

@Composable
private fun DailyGoalDialog(
    goalInputText: String,
    onGoalInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val view = LocalView.current
    val chipShape = remember { RoundedCornerShape(8.dp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = { Text("Daily Focus Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose a preset or enter custom minutes:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(60L, 120L, 240L, 360L).forEach { preset ->
                        val isChosen = goalInputText == preset.toString()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(chipShape)
                                .background(
                                    color = if (isChosen) palette.primaryContainer else palette.surfaceElevated,
                                    shape = chipShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isChosen) palette.primaryBorderTint else palette.border,
                                    shape = chipShape
                                )
                                .clickable {
                                    view.performClockTickHaptic()
                                    onGoalInputChange(preset.toString())
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset / 60}h",
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = goalInputText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            onGoalInputChange(newValue)
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
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textSecondary)
            }
        }
    )
}

@Composable
private fun PermissionStatusRow(
    title: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val pillShape = remember { RoundedCornerShape(50) }
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
                tint = if (granted) palette.success else palette.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                color = palette.textPrimary
            )
        }
        Text(
            text = if (granted) "Granted" else "Configure",
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            color = if (granted) palette.success else palette.primary,
            modifier = Modifier
                .clip(pillShape)
                .background(
                    color = if (granted) palette.success.copy(alpha = 0.14f) else palette.primaryContainer,
                    shape = pillShape
                )
                .then(
                    if (granted) Modifier else Modifier.border(1.dp, palette.primaryBorderTint, pillShape)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AddAllowedAppDialog(
    currentlyAllowed: List<AllowedApp>,
    onAddApp: (AllowedApp) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFocusTheme.current
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
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
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
                            color = palette.primary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (missingPresets.isNotEmpty() && searchQuery.isBlank()) {
                            item(key = "presets_header", contentType = "dialog_header") {
                                Text(
                                    text = "BUILT-IN ESSENTIALS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(
                                items = missingPresets,
                                key = { it.id },
                                contentType = { "preset_row" }
                            ) { preset ->
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
                                        tint = palette.primary,
                                        badgeBackground = palette.primaryContainer,
                                        iconSize = 18.dp,
                                        containerSize = 34.dp,
                                        shape = CircleShape,
                                        loadIfMissing = false
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = "Quick essential preset",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 11.sp,
                                            color = palette.textSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add ${preset.label}",
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (filteredInstalled.isNotEmpty()) {
                            item(key = "installed_apps_header", contentType = "dialog_header") {
                                Text(
                                    text = "INSTALLED APPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(
                                items = filteredInstalled,
                                key = { it.packageName },
                                contentType = { "installed_app_row" }
                            ) { info ->
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
                                        tint = palette.primary,
                                        badgeBackground = palette.surfaceElevated,
                                        iconSize = 18.dp,
                                        containerSize = 34.dp,
                                        shape = RoundedCornerShape(10.dp),
                                        loadIfMissing = true
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = info.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                            color = palette.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = info.packageName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 11.sp,
                                            color = palette.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add ${info.label}",
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else if (missingPresets.isEmpty()) {
                            item(key = "no_matching_apps") {
                                Text(
                                    text = "No matching apps available to add.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
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
                Text("Done", color = palette.primary)
            }
        }
    )
}
