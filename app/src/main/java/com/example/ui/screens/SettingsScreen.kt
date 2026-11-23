package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.QuizDifficulty
import com.example.data.UpdateUiState
import com.example.service.FocusNotificationBlockerService
import com.example.service.FocusPowerBlockerAccessibilityService
import com.example.ui.components.AllowedAppIconBadge
import com.example.ui.components.AppearanceAndThemesSection
import com.example.ui.components.DailyStudyGoalSection
import com.example.ui.components.ElasticMorphingSegmentedPill
import com.example.ui.components.ElasticPillSegment
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.HomeScreenWidgetSection
import com.example.ui.components.LockAllowedAppsSection
import com.example.ui.components.LockAppsTrayMetrics
import com.example.ui.components.LockPalette
import com.example.ui.components.ProtectionAndBlockersSection
import com.example.ui.components.ScenicPageVariant
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemeScenicBackdropOverlay
import com.example.ui.components.ThemeScenicVignetteFooter
import com.example.ui.components.ThemedDivider
import com.example.ui.components.ThemedSettingsSectionCard
import com.example.ui.components.ThemedSettingsSwitchRow
import com.example.ui.components.WhatsNewBottomSheet
import com.example.ui.components.performClockTickHaptic
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: FocusSettingsState,
    installedVersion: String,
    updateUiState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateSheet: () -> Unit,
    onOpenWhatsNew: (() -> Unit)? = null,
    onOpenBackupRestore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = rememberFocusHaptics()
    val palette = LocalFocusTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val iconCache by FocusSettingsRepository.appIconsState.collectAsStateWithLifecycle()
    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()
    val blockerState by NotificationBlockerRepository.observe(context).collectAsStateWithLifecycle()

    var showAddAppDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showLocalWhatsNewSheet by remember { mutableStateOf(false) }
    var showLocalBackupScreen by remember { mutableStateOf(false) }
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
    val onSelectPillSize = remember(context, haptics) {
        { sizeDp: Int ->
            if (sizeDp != FocusSettingsRepository.getCurrent(context).minimalPillButtonSizeDp) {
                haptics.tick()
                FocusSettingsRepository.setMinimalPillButtonSizeDp(context, sizeDp)
            }
        }
    }
    val onOpenGoalDialog = remember(haptics, settingsState.dailyGoalMinutes) {
        {
            haptics.tap()
            goalInputText = settingsState.dailyGoalMinutes.toString()
            showGoalDialog = true
        }
    }
    val onToggleSessionTimer = remember(context) {
        { enabled: Boolean ->
            FocusSettingsRepository.setShowSessionTimer(context, enabled)
        }
    }
    val onToggleHapticKeypad = remember(context) {
        { enabled: Boolean ->
            FocusSettingsRepository.setHapticKeypad(context, enabled)
        }
    }
    val onToggleAutoClearWrongPin = remember(context) {
        { enabled: Boolean ->
            FocusSettingsRepository.setAutoClearWrongPin(context, enabled)
        }
    }
    val onSelectQuizDifficulty = remember(context, haptics) {
        { difficulty: QuizDifficulty ->
            haptics.tick()
            FocusSettingsRepository.setQuizDifficulty(context, difficulty)
        }
    }
    val onSelectGraceSeconds = remember(context, haptics) {
        { sec: Int ->
            haptics.tick()
            FocusSettingsRepository.setAppSwitchGraceSeconds(context, sec)
        }
    }
    val onOpenOverlaySettings = remember(context, haptics) {
        {
            haptics.tap()
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }.onFailure {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }
            Unit
        }
    }
    val onOpenUsageSettings = remember(context, haptics) {
        {
            haptics.tap()
            runCatching {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }.onFailure {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }
            Unit
        }
    }
    val onOpenNotificationListenerSettings = remember(context, haptics) {
        {
            haptics.tap()
            runCatching {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            Unit
        }
    }
    val onOpenAccessibilitySettings = remember(context, haptics) {
        {
            haptics.tap()
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
        ThemeScenicBackdropOverlay(
            variant = ScenicPageVariant.SETTINGS,
            palette = palette
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = palette.displayFont,
                    fontSize = 22.sp,
                    color = palette.textPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
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
                    pillButtonSizeDp = settingsState.minimalPillButtonSizeDp,
                    iconCache = iconCache,
                    onSelectLayout = onSelectLayout,
                    onSelectPillSize = onSelectPillSize
                )

                FocusBehaviorSection(
                    showSessionTimer = settingsState.showSessionTimer,
                    hapticKeypad = settingsState.hapticKeypad,
                    autoClearWrongPin = settingsState.autoClearWrongPin,
                    quizDifficulty = settingsState.quizDifficulty,
                    appSwitchGraceSeconds = settingsState.appSwitchGraceSeconds,
                    onToggleSessionTimer = onToggleSessionTimer,
                    onToggleHapticKeypad = onToggleHapticKeypad,
                    onToggleAutoClearWrongPin = onToggleAutoClearWrongPin,
                    onSelectQuizDifficulty = onSelectQuizDifficulty,
                    onSelectGraceSeconds = onSelectGraceSeconds
                )

                BackupRestoreSettingsSection(
                    onOpenBackupRestore = {
                        view.performKeyTapHaptic()
                        if (onOpenBackupRestore != null) {
                            onOpenBackupRestore()
                        } else {
                            showLocalBackupScreen = true
                        }
                    }
                )

                AppUpdatesSection(
                    installedVersion = installedVersion,
                    updateUiState = updateUiState,
                    onCheckForUpdates = onCheckForUpdates,
                    onOpenUpdateSheet = onOpenUpdateSheet,
                    onOpenWhatsNew = {
                        view.performKeyTapHaptic()
                        if (onOpenWhatsNew != null) {
                            onOpenWhatsNew()
                        } else {
                            showLocalWhatsNewSheet = true
                        }
                    }
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

                ThemeScenicVignetteFooter(
                    caption = "FOCUS SANCTUARY",
                    subtitle = "Crafted for calm, distraction-free sessions",
                    palette = palette
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

    if (showLocalWhatsNewSheet) {
        WhatsNewBottomSheet(
            installedVersion = installedVersion,
            onDismiss = { showLocalWhatsNewSheet = false }
        )
    }

    if (showLocalBackupScreen) {
        BackupRestoreScreen(
            onBack = { showLocalBackupScreen = false }
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
    val iconShape = CircleShape

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
                        val rowHaptics = rememberFocusHaptics()
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    rowHaptics.reject()
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
                                    .background(if (palette.isLiquidGlass) Color(0xFF0C0F15) else palette.surface)
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
                                    preloadedIcon = iconCache[FocusSettingsRepository.iconCacheKey(app.packageName, palette.theme)],
                                    loadIfMissing = true
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
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
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
    pillButtonSizeDp: Int,
    iconCache: Map<String, ImageBitmap>,
    onSelectLayout: (LockAppLayout) -> Unit,
    onSelectPillSize: (Int) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val previewShape = remember { RoundedCornerShape(24.dp) }
    val optionShape = remember { RoundedCornerShape(22.dp) }
    val noopPreviewClick = remember<((AllowedApp) -> Unit)> { {} }
    val previewPalette = remember(palette) { LockPalette.fromFocusTheme(palette) }
    val previewApps = remember(allowedApps) {
        allowedApps.ifEmpty { FocusSettingsRepository.DEFAULT_ALLOWED_APPS }
    }
    var measuredPillRows by remember(previewApps.size, pillButtonSizeDp) { mutableIntStateOf(1) }

    // Exact lock-screen tray width on this device so pill wrapping matches reality 1:1
    val realLockTrayWidthDp = remember(context) {
        val dm = context.resources.displayMetrics
        val screenWidthDp = dm.widthPixels / dm.density.coerceAtLeast(1f)
        LockAppsTrayMetrics.computeLockTrayContentWidthDp(screenWidthDp)
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
                        text = "LIVE 1:1 PREVIEW",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary
                    )
                    if (selectedLayout == LockAppLayout.PILL_ROW) {
                        val isSingleRow = measuredPillRows <= 1
                        val badgeAccent = if (isSingleRow) palette.success else palette.warning
                        Text(
                            text = if (isSingleRow) {
                                "1 ROW · ${pillButtonSizeDp}dp"
                            } else {
                                "$measuredPillRows ROWS · ${pillButtonSizeDp}dp"
                            },
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(badgeAccent.copy(alpha = 0.14f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("pill_preview_row_status_badge")
                        )
                    } else {
                        Text(
                            text = selectedLayout.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.primary
                        )
                    }
                }

                // Render PILL_ROW at exact realLockTrayWidthDp and scale uniformly only if card is narrower
                if (selectedLayout == LockAppLayout.PILL_ROW) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val availableDp = maxWidth.value.coerceAtLeast(160f)
                        val fitScale = (availableDp / realLockTrayWidthDp).coerceIn(0.60f, 1f)
                        Box(
                            modifier = Modifier
                                .requiredWidth(realLockTrayWidthDp.dp)
                                .graphicsLayer {
                                    scaleX = fitScale
                                    scaleY = fitScale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LockAllowedAppsSection(
                                apps = previewApps,
                                layout = selectedLayout,
                                palette = previewPalette,
                                onAppClick = noopPreviewClick,
                                isPreview = true,
                                pillButtonSizeDp = pillButtonSizeDp,
                                iconCache = iconCache,
                                readFromCacheOnly = true,
                                onRowCountMeasured = { rows -> measuredPillRows = rows }
                            )
                        }
                    }
                } else {
                    LockAllowedAppsSection(
                        apps = previewApps,
                        layout = selectedLayout,
                        palette = previewPalette,
                        onAppClick = noopPreviewClick,
                        isPreview = true,
                        pillButtonSizeDp = pillButtonSizeDp,
                        iconCache = iconCache,
                        readFromCacheOnly = true
                    )
                }
            }

            AnimatedVisibility(visible = selectedLayout == LockAppLayout.PILL_ROW) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(optionShape)
                        .background(palette.surfaceElevated.copy(alpha = 0.72f), optionShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("pill_size_control_card"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PILL BUTTON SIZE",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primaryContainer
                        ) {
                            Text(
                                text = "${pillButtonSizeDp}dp",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                    .testTag("pill_size_value_label")
                            )
                        }
                    }

                    Slider(
                        value = pillButtonSizeDp.toFloat(),
                        onValueChange = { raw -> onSelectPillSize(raw.roundToInt()) },
                        valueRange = FocusSettingsRepository.MIN_PILL_BUTTON_SIZE_DP.toFloat()..
                            FocusSettingsRepository.MAX_PILL_BUTTON_SIZE_DP.toFloat(),
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary,
                            inactiveTrackColor = palette.borderHighlight,
                            activeTickColor = palette.onPrimary.copy(alpha = 0.55f),
                            inactiveTickColor = palette.textSecondary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pill_size_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${FocusSettingsRepository.MIN_PILL_BUTTON_SIZE_DP}dp Compact",
                            fontFamily = JetBrainsMono,
                            fontSize = 9.5.sp,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "${FocusSettingsRepository.MAX_PILL_BUTTON_SIZE_DP}dp Large",
                            fontFamily = JetBrainsMono,
                            fontSize = 9.5.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LockAppLayout.entries.forEach { layout ->
                    val selected = selectedLayout == layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(optionShape)
                            .background(
                                color = if (selected) {
                                    if (palette.isLiquidGlass) Color(0xFF1F2634) else palette.primaryContainer
                                } else {
                                    palette.surfaceElevated.copy(alpha = 0.6f)
                                },
                                shape = optionShape
                            )
                            .clickable { onSelectLayout(layout) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
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
    quizDifficulty: QuizDifficulty,
    appSwitchGraceSeconds: Int,
    onToggleSessionTimer: (Boolean) -> Unit,
    onToggleHapticKeypad: (Boolean) -> Unit,
    onToggleAutoClearWrongPin: (Boolean) -> Unit,
    onSelectQuizDifficulty: (QuizDifficulty) -> Unit,
    onSelectGraceSeconds: (Int) -> Unit
) {
    val palette = LocalFocusTheme.current

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
                subtitle = "Subtle tactile pulse during the unlock challenge",
                checked = hapticKeypad,
                onCheckedChange = onToggleHapticKeypad,
                testTag = "switch_haptic_keypad"
            )

            ThemedDivider(palette = palette)

            ThemedSettingsSwitchRow(
                title = "Auto-Clear Wrong Answer",
                subtitle = "Reset input automatically after an incorrect attempt",
                checked = autoClearWrongPin,
                onCheckedChange = onToggleAutoClearWrongPin,
                testTag = "switch_auto_clear_pin"
            )

            ThemedDivider(palette = palette)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text(
                        text = "Question Difficulty",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        color = palette.textPrimary
                    )
                    Text(
                        text = quizDifficulty.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
                val difficultyOptions = remember {
                    listOf(
                        QuizDifficulty.EASY,
                        QuizDifficulty.DEFAULT,
                        QuizDifficulty.HARD
                    )
                }
                val difficultySegments = remember {
                    listOf(
                        ElasticPillSegment(QuizDifficulty.EASY.label),
                        ElasticPillSegment(QuizDifficulty.DEFAULT.label),
                        ElasticPillSegment(QuizDifficulty.HARD.label)
                    )
                }
                ElasticMorphingSegmentedPill(
                    segments = difficultySegments,
                    selectedIndex = difficultyOptions.indexOf(quizDifficulty).takeIf { it >= 0 } ?: 1,
                    onSelectIndex = { idx ->
                        difficultyOptions.getOrNull(idx)?.let(onSelectQuizDifficulty)
                    },
                    height = 42.dp,
                    fontSize = 13.sp,
                    useElevatedContainer = true,
                    modifier = Modifier.testTag("quiz_difficulty_selector")
                )
            }

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
                val graceOptions = remember { listOf(1, 2, 3, 5) }
                val graceSegments = remember {
                    listOf(
                        ElasticPillSegment("1s"),
                        ElasticPillSegment("2s"),
                        ElasticPillSegment("3s"),
                        ElasticPillSegment("5s")
                    )
                }
                ElasticMorphingSegmentedPill(
                    segments = graceSegments,
                    selectedIndex = graceOptions.indexOf(appSwitchGraceSeconds).coerceAtLeast(0),
                    onSelectIndex = { idx ->
                        graceOptions.getOrNull(idx)?.let(onSelectGraceSeconds)
                    },
                    height = 42.dp,
                    fontSize = 13.sp,
                    useElevatedContainer = true
                )
            }
        }
    }
}

@Composable
private fun BackupRestoreSettingsSection(
    onOpenBackupRestore: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val pillShape = remember { RoundedCornerShape(50) }

    ThemedSettingsSectionCard(title = "DATA & BACKUP") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = "Backup & Restore",
                        tint = palette.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Export & Import Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Save or restore all settings, stats, XP, schedules & quiz history",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
            }

            Button(
                onClick = onOpenBackupRestore,
                shape = pillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryContainer,
                    contentColor = palette.textPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("open_backup_restore_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Backup & Restore",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AppUpdatesSection(
    installedVersion: String,
    updateUiState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onOpenUpdateSheet: () -> Unit,
    onOpenWhatsNew: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val view = LocalView.current
    val pillShape = remember { RoundedCornerShape(50) }

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
                            .clip(CircleShape)
                            .background(palette.primaryContainer),
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
                            .clip(pillShape)
                            .background(palette.success.copy(alpha = 0.12f), pillShape)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
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
                shape = pillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.surfaceElevated,
                    contentColor = palette.textPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .graphicsLayer {
                        scaleX = btnScale
                        scaleY = btnScale
                    }
                    .testTag("check_updates_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp,
                        color = palette.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking GitHub...", fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (updateUiState is UpdateUiState.UpdateAvailable) {
                            "View Update (${updateUiState.release.tagName})"
                        } else {
                            "Check for Updates"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onOpenWhatsNew,
                shape = pillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryContainer,
                    contentColor = palette.textPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("whats_new_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "What's New",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
    val chipShape = remember { RoundedCornerShape(50) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
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
                                .height(38.dp)
                                .clip(chipShape)
                                .background(
                                    color = if (isChosen) {
                                        if (palette.isLiquidGlass) Color(0xFF283040) else palette.primaryContainer
                                    } else {
                                        palette.surfaceElevated
                                    },
                                    shape = chipShape
                                )
                                .clickable {
                                    view.performClockTickHaptic()
                                    onGoalInputChange(preset.toString())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset / 60}h",
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isChosen) {
                                    if (palette.isLiquidGlass) palette.textPrimary else palette.primary
                                } else {
                                    palette.textPrimary
                                }
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
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(50),
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
            .clip(pillShape)
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
        shape = RoundedCornerShape(30.dp),
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
                    shape = RoundedCornerShape(50),
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
                                        .clip(RoundedCornerShape(22.dp))
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
                                        .clip(RoundedCornerShape(22.dp))
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
                                        shape = CircleShape,
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
