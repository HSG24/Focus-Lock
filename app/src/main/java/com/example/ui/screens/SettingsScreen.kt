package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.ThemeMode
import com.example.ui.components.AllowedAppIconBadge
import com.example.ui.components.LockAllowedAppsSection
import com.example.ui.components.LockPalette
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: FocusSettingsState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Appearance / Theme Section
            item {
                SettingsSection(title = "APPEARANCE") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "App Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                val selected = settingsState.themeMode == mode
                                val icon = when (mode) {
                                    ThemeMode.SYSTEM -> Icons.Outlined.PhoneAndroid
                                    ThemeMode.LIGHT -> Icons.Outlined.LightMode
                                    ThemeMode.DARK -> Icons.Outlined.DarkMode
                                }
                                Surface(
                                    onClick = { FocusSettingsRepository.setThemeMode(context, mode) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("theme_${mode.name.lowercase()}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = mode.displayName,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsSwitchRow(
                            title = "Lock Screen Follows Theme",
                            subtitle = "Use light or dark palette on the lock overlay",
                            checked = settingsState.lockScreenFollowsTheme,
                            onCheckedChange = {
                                FocusSettingsRepository.setLockScreenFollowsTheme(context, it)
                            },
                            testTag = "switch_lock_follows_theme"
                        )
                    }
                }
            }

            // 2. Locked State Allowed Apps Section
            item {
                SettingsSection(
                    title = "ALLOWED APPS IN LOCK MODE",
                    trailingAction = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { FocusSettingsRepository.resetAllowedAppsToDefaults(context) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("reset_apps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Defaults", fontSize = 12.sp)
                            }
                        }
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (settingsState.allowedApps.isEmpty()) {
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "No apps allowed during focus sessions. Add essential apps below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            settingsState.allowedApps.forEachIndexed { index, app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AllowedAppIconBadge(
                                        app = app,
                                        tint = MaterialTheme.colorScheme.primary,
                                        badgeBackground = MaterialTheme.colorScheme.surfaceVariant,
                                        iconSize = 18.dp,
                                        containerSize = 36.dp,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (app.isPreset) "Built-in essential" else app.packageName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            FocusSettingsRepository.removeAllowedApp(context, app.id)
                                        },
                                        modifier = Modifier.testTag("remove_app_${app.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove ${app.label}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (index < settingsState.allowedApps.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddAppDialog = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("add_allowed_app_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Allowed App",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 3. Lock Screen App Layout Section (5 Sleek Layouts + Live Preview)
            item {
                SettingsSection(title = "LOCK SCREEN APPS LAYOUT") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Live Lock Screen Mini-Preview
                        val previewPalette = if (settingsState.lockScreenFollowsTheme) {
                            LockPalette.fromMaterialTheme()
                        } else {
                            LockPalette.StealthDark
                        }
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
                                    color = MaterialTheme.colorScheme.outlineVariant,
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
                                    color = previewPalette.secondaryText
                                )
                                Text(
                                    text = settingsState.lockAppLayout.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = previewPalette.accent
                                )
                            }

                            LockAllowedAppsSection(
                                apps = previewApps,
                                layout = settingsState.lockAppLayout,
                                palette = previewPalette,
                                onAppClick = {},
                                isPreview = true
                            )
                        }

                        // 5 Layout Selector Options
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LockAppLayout.entries.forEach { layout ->
                                val selected = settingsState.lockAppLayout == layout
                                Surface(
                                    onClick = {
                                        FocusSettingsRepository.setLockAppLayout(context, layout)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    },
                                    border = if (selected) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = layout.subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        RadioButton(
                                            selected = selected,
                                            onClick = {
                                                FocusSettingsRepository.setLockAppLayout(context, layout)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Focus & Lock Behavior Section
            item {
                SettingsSection(title = "FOCUS & LOCK BEHAVIOR") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Daily Goal Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Target study duration per day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${settingsState.dailyGoalMinutes} min",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsSwitchRow(
                            title = "Live Session Timer",
                            subtitle = "Display elapsed focus time on the lock screen",
                            checked = settingsState.showSessionTimer,
                            onCheckedChange = {
                                FocusSettingsRepository.setShowSessionTimer(context, it)
                            },
                            testTag = "switch_session_timer"
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsSwitchRow(
                            title = "Keypad Haptic Feedback",
                            subtitle = "Subtle tactile pulse when entering unlock PIN",
                            checked = settingsState.hapticKeypad,
                            onCheckedChange = {
                                FocusSettingsRepository.setHapticKeypad(context, it)
                            },
                            testTag = "switch_haptic_keypad"
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsSwitchRow(
                            title = "Auto-Clear Wrong PIN",
                            subtitle = "Reset digits automatically after an incorrect attempt",
                            checked = settingsState.autoClearWrongPin,
                            onCheckedChange = {
                                FocusSettingsRepository.setAutoClearWrongPin(context, it)
                            },
                            testTag = "switch_auto_clear_pin"
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        // App Switch Grace Period
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "App Launch Grace Period",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Delay before re-locking if you leave an allowed app",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1, 2, 3, 5).forEach { sec ->
                                    val selected = settingsState.appSwitchGraceSeconds == sec
                                    Surface(
                                        onClick = {
                                            FocusSettingsRepository.setAppSwitchGraceSeconds(context, sec)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                        border = if (selected) {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        } else {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${sec}s",
                                                fontFamily = JetBrainsMono,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. System Permissions Status
            item {
                SettingsSection(title = "PERMISSIONS") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PermissionStatusRow(
                            title = "Display Over Other Apps",
                            granted = hasOverlayPermission,
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        PermissionStatusRow(
                            title = "Usage Access Monitoring",
                            granted = hasUsagePermission,
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showAddAppDialog) {
        AddAllowedAppDialog(
            currentlyAllowed = settingsState.allowedApps,
            onAddApp = { app ->
                FocusSettingsRepository.addAllowedApp(context, app)
                showAddAppDialog = false
            },
            onDismiss = { showAddAppDialog = false }
        )
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Daily Focus Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose a preset or enter custom minutes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30L, 60L, 90L, 120L).forEach { preset ->
                            Surface(
                                onClick = { goalInputText = preset.toString() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (goalInputText == preset.toString()) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
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
                                        color = MaterialTheme.colorScheme.onSurface
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
                        val parsed = goalInputText.toLongOrNull() ?: 60L
                        if (parsed > 0) {
                            FocusSettingsRepository.setDailyGoalMinutes(context, parsed)
                        }
                        showGoalDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    trailingAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            trailingAction?.invoke()
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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
                tint = if (granted) FocusEmerald else MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = if (granted) {
                FocusEmerald.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
        ) {
            Text(
                text = if (granted) "Granted" else "Configure",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                color = if (granted) FocusEmerald else MaterialTheme.colorScheme.onTertiaryContainer,
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
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        badgeBackground = MaterialTheme.colorScheme.primaryContainer,
                                        iconSize = 18.dp,
                                        containerSize = 34.dp,
                                        shape = CircleShape
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Quick essential preset",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add ${preset.label}",
                                        tint = MaterialTheme.colorScheme.primary,
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(filteredInstalled, key = { it.packageName }) { info ->
                                val iconBitmap = remember(info.packageName) {
                                    FocusSettingsRepository.loadAppIconBitmap(context, info.packageName)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onAddApp(
                                                AllowedApp(
                                                    id = "pkg_${info.packageName}",
                                                    label = info.label,
                                                    packageName = info.packageName,
                                                    isPreset = false
                                                )
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (iconBitmap != null) {
                                            Image(
                                                bitmap = iconBitmap,
                                                contentDescription = info.label,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = info.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = info.packageName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add ${info.label}",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else if (missingPresets.isEmpty()) {
                            item {
                                Text(
                                    text = "No matching apps available to add.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Text("Done")
            }
        }
    )
}
