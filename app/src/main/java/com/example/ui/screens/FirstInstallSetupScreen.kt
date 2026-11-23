package com.example.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.FocusSettingsRepository
import com.example.data.FocusSettingsState
import com.example.service.FocusNotificationBlockerService
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.SanctuaryAmbientHalo
import com.example.ui.components.SanctuaryMonolithCard
import com.example.ui.components.ScenicPageVariant
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemeMiniArtPreview
import com.example.ui.components.ThemeScenicBackdropOverlay
import com.example.ui.components.ThemedDivider
import com.example.ui.components.ThemedHeroOrbitFrame
import com.example.ui.components.ThemedProgressSegments
import com.example.ui.components.ThemedTimerRing
import com.example.ui.components.focusGlassPanel
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.launch
import java.util.Locale

private const val TOTAL_SETUP_STEPS = 4

/**
 * First-Install Sanctuary Setup experience.
 *
 * Shown ONLY on brand-new installations (never on updates to existing installs).
 * Guides the user through welcome/restore, live visual theme selection,
 * daily focus goal calibration, and essential Android permissions.
 */
@Composable
fun FirstInstallSetupScreen(
    settingsState: FocusSettingsState,
    onOpenRestoreBackup: () -> Unit,
    onCompleteSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) }
    var hasOverlay by remember { mutableStateOf(false) }
    var hasUsage by remember { mutableStateOf(false) }
    var hasExactAlarm by remember { mutableStateOf(true) }
    var hasNotificationAccess by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        scope.launch {
            hasOverlay = FocusSettingsRepository.canDrawOverlaysAsync(context)
            hasUsage = FocusSettingsRepository.hasUsageStatsPermissionAsync(context)
            hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                am?.canScheduleExactAlarms() == true
            } else {
                true
            }
            hasNotificationAccess = FocusNotificationBlockerService.isNotificationListenerEnabled(context)
        }
    }

    LaunchedEffect(Unit) { refreshPermissions() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = step > 0) {
        haptics.tick()
        step -= 1
    }

    fun launchSafe(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("first_install_setup_screen")
    ) {
        ThemeBackdropLayer(palette = palette)
        ThemeScenicBackdropOverlay(
            variant = ScenicPageVariant.SETUP,
            palette = palette
        )
        SanctuaryAmbientHalo(
            accent = palette.primary,
            alpha = if (palette.isLiquidGlass) 0.10f else 0.13f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top progress bar & step navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 0) {
                    IconButton(
                        onClick = {
                            haptics.tick()
                            step -= 1
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("setup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous step",
                            tint = palette.textPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }

                // Theme-crafted step progress segments
                ThemedProgressSegments(
                    total = TOTAL_SETUP_STEPS,
                    completed = step,
                    activeIndex = step,
                    palette = palette,
                    modifier = Modifier
                        .width(124.dp)
                        .testTag("setup_step_dots")
                )

                Text(
                    text = "${step + 1}/$TOTAL_SETUP_STEPS",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textSecondary,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End
                )
            }

            // Center scrollable content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState >= initialState
                        (fadeIn(tween(240)) + slideInHorizontally(tween(240)) { if (forward) it / 5 else -it / 5 }) togetherWith
                            (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { if (forward) -it / 5 else it / 5 })
                    },
                    label = "setup_step_content"
                ) { activeStep ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        when (activeStep) {
                            0 -> SetupWelcomeStep()
                            1 -> SetupThemeStep(
                                selectedTheme = settingsState.appTheme,
                                onSelectTheme = { chosen ->
                                    haptics.tap()
                                    FocusSettingsRepository.setAppTheme(context, chosen)
                                }
                            )
                            2 -> SetupDailyGoalStep(
                                goalMinutes = settingsState.dailyGoalMinutes,
                                onSelectGoal = { mins ->
                                    haptics.tick()
                                    FocusSettingsRepository.setDailyGoalMinutes(context, mins)
                                }
                            )
                            else -> SetupPermissionsStep(
                                hasOverlay = hasOverlay,
                                hasUsage = hasUsage,
                                hasExactAlarm = hasExactAlarm,
                                hasNotificationAccess = hasNotificationAccess,
                                onRequestOverlay = {
                                    haptics.tap()
                                    launchSafe(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                },
                                onRequestUsage = {
                                    haptics.tap()
                                    launchSafe(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                },
                                onRequestExactAlarm = {
                                    haptics.tap()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        launchSafe(
                                            Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }
                                },
                                onRequestNotification = {
                                    haptics.tap()
                                    launchSafe(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }
                            )
                        }
                    }
                }
            }

            // Bottom action dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val primaryInteraction = remember { MutableInteractionSource() }
                val primaryScale = rememberSpringPressScale(primaryInteraction)

                Button(
                    onClick = {
                        haptics.confirm()
                        if (step < TOTAL_SETUP_STEPS - 1) {
                            step += 1
                        } else {
                            onCompleteSetup()
                        }
                    },
                    interactionSource = primaryInteraction,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer {
                            scaleX = primaryScale
                            scaleY = primaryScale
                        }
                        .testTag(if (step < TOTAL_SETUP_STEPS - 1) "setup_next_button" else "setup_finish_button")
                ) {
                    Text(
                        text = when (step) {
                            0 -> "Get Started"
                            TOTAL_SETUP_STEPS - 1 -> "Enter FocusLock"
                            else -> "Continue"
                        },
                        fontFamily = palette.bodyFont,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (step == TOTAL_SETUP_STEPS - 1) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (step == 0) {
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            onOpenRestoreBackup()
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("setup_restore_backup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Restore Existing Backup",
                            color = palette.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupWelcomeStep() {
    val palette = LocalFocusTheme.current

    FocusLockEmblem(
        size = 118.dp,
        showAmbientGlow = true
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "BUILT FOR DEEP FOCUS",
            fontFamily = JetBrainsMono,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            color = palette.primary
        )
        Text(
            text = "Welcome to FocusLock",
            fontFamily = palette.displayFont,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            color = palette.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Quiet the noise, stay locked into what matters, and keep every stat private on your device.",
            fontFamily = palette.bodyFont,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

    SanctuaryMonolithCard(
        palette = palette,
        cornerRadius = 28.dp,
        contentPaddingHorizontal = 18.dp,
        contentPaddingVertical = 18.dp,
        verticalSpacing = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        SetupPillarRow(
            icon = Icons.Outlined.Psychology,
            title = "10-Question Mindful Unlock",
            subtitle = "Infinite non-repeating mental challenges replace passwords"
        )
        ThemedDivider(palette = palette)
        SetupPillarRow(
            icon = Icons.Outlined.Schedule,
            title = "Automated Focus Schedules",
            subtitle = "Lock in automatically on your daily routine and release on time"
        )
        ThemedDivider(palette = palette)
        SetupPillarRow(
            icon = Icons.Outlined.Shield,
            title = "100% On-Device & Portable",
            subtitle = "Zero cloud accounts — export or restore your full archive anytime"
        )
    }
}

@Composable
private fun SetupPillarRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val palette = LocalFocusTheme.current
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
                imageVector = icon,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontFamily = palette.bodyFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                fontFamily = palette.bodyFont,
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun SetupThemeStep(
    selectedTheme: AppVisualTheme,
    onSelectTheme: (AppVisualTheme) -> Unit
) {
    val palette = LocalFocusTheme.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(palette.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "CHOOSE YOUR ATMOSPHERE",
            fontFamily = JetBrainsMono,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = palette.primary
        )
        Text(
            text = "Select Visual Theme",
            fontFamily = palette.displayFont,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        Text(
            text = "Every lock screen, ring, and keypad adapts to your chosen craft.",
            fontSize = 13.5.sp,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppVisualTheme.entries.forEach { theme ->
            val preview = remember(theme) { FocusThemePalette.from(theme) }
            val isSelected = theme == selectedTheme
            val cardShape = RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGlassPanel(
                        shape = cardShape,
                        palette = palette,
                        elevated = isSelected,
                        highlightAccent = if (isSelected) preview.primary else null,
                        showOrnaments = false
                    )
                    .clickable { onSelectTheme(theme) }
                    .padding(12.dp)
                    .testTag("setup_theme_${theme.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ThemeMiniArtPreview(
                    theme = theme,
                    isSelected = isSelected,
                    modifier = Modifier
                        .width(92.dp)
                        .height(58.dp)
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = theme.displayName,
                            fontFamily = preview.displayFont,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = theme.badgeTag,
                            fontFamily = JetBrainsMono,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = preview.primary
                        )
                    }
                    Text(
                        text = theme.subtitle,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Selected",
                        tint = palette.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupDailyGoalStep(
    goalMinutes: Long,
    onSelectGoal: (Long) -> Unit
) {
    val palette = LocalFocusTheme.current
    val presets = remember { listOf(30L, 60L, 120L, 180L, 240L) }
    val formattedGoal = remember(goalMinutes) {
        val h = goalMinutes / 60L
        val m = goalMinutes % 60L
        when {
            h > 0L && m > 0L -> "${h}h ${m}m"
            h > 0L -> "${h}h"
            else -> "${m}m"
        }
    }
    val ringFraction = (goalMinutes.toFloat() / 360f).coerceIn(0.10f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "DAILY CADENCE",
            fontFamily = JetBrainsMono,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = palette.primary
        )
        Text(
            text = "Set Your Daily Goal",
            fontFamily = palette.displayFont,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        Text(
            text = "You can refine this anytime in Settings.",
            fontSize = 13.5.sp,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
    }

    SanctuaryMonolithCard(
        palette = palette,
        cornerRadius = 30.dp,
        contentPaddingHorizontal = 20.dp,
        contentPaddingVertical = 22.dp,
        verticalSpacing = 18.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        ThemedHeroOrbitFrame(
            modifier = Modifier.size(204.dp),
            palette = palette
        ) {
            ThemedTimerRing(
                progress = ringFraction,
                size = 176.dp,
                palette = palette
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = formattedGoal,
                        fontFamily = palette.numeralFont,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        modifier = Modifier.testTag("setup_goal_value_text")
                    )
                    Text(
                        text = "PER DAY",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.8.sp,
                        color = palette.primary
                    )
                }
            }
        }

        // Fine stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = { onSelectGoal((goalMinutes - 15L).coerceAtLeast(15L)) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceElevated)
                    .testTag("setup_goal_minus")
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease goal by 15 minutes",
                    tint = palette.textPrimary
                )
            }
            Text(
                text = "15m steps",
                fontFamily = JetBrainsMono,
                fontSize = 11.5.sp,
                color = palette.textSecondary
            )
            IconButton(
                onClick = { onSelectGoal((goalMinutes + 15L).coerceAtMost(720L)) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceElevated)
                    .testTag("setup_goal_plus")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase goal by 15 minutes",
                    tint = palette.textPrimary
                )
            }
        }

        // Quick preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                val active = goalMinutes == preset
                val chipShape = RoundedCornerShape(50)
                val label = if (preset >= 60L) "${preset / 60L}h" else "${preset}m"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(chipShape)
                        .background(if (active) palette.primary else palette.background.copy(alpha = 0.5f), chipShape)
                        .border(
                            width = 0.75.dp,
                            color = if (active) palette.primary else palette.borderHighlight.copy(alpha = 0.5f),
                            shape = chipShape
                        )
                        .clickable { onSelectGoal(preset) }
                        .testTag("setup_goal_preset_$preset"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) palette.onPrimary else palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupPermissionsStep(
    hasOverlay: Boolean,
    hasUsage: Boolean,
    hasExactAlarm: Boolean,
    hasNotificationAccess: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestUsage: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotification: () -> Unit
) {
    val palette = LocalFocusTheme.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "SYSTEM GUARDRAILS",
            fontFamily = JetBrainsMono,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = palette.primary
        )
        Text(
            text = "Essential Permissions",
            fontFamily = palette.displayFont,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        Text(
            text = "Grant Overlay & Usage Access so FocusLock can hold your focus screen reliably.",
            fontSize = 13.5.sp,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
    }

    SanctuaryMonolithCard(
        palette = palette,
        cornerRadius = 28.dp,
        contentPaddingHorizontal = 16.dp,
        contentPaddingVertical = 16.dp,
        verticalSpacing = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        SetupPermissionRow(
            icon = Icons.Outlined.Layers,
            title = "Display Over Other Apps",
            subtitle = "Required to show the focus lock screen",
            granted = hasOverlay,
            required = true,
            onGrant = onRequestOverlay,
            testTag = "setup_perm_overlay"
        )
        ThemedDivider(palette = palette)
        SetupPermissionRow(
            icon = Icons.Outlined.QueryStats,
            title = "Usage Access",
            subtitle = "Required to detect non-allowed app switches",
            granted = hasUsage,
            required = true,
            onGrant = onRequestUsage,
            testTag = "setup_perm_usage"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ThemedDivider(palette = palette)
            SetupPermissionRow(
                icon = Icons.Outlined.Alarm,
                title = "Exact Alarms",
                subtitle = "Starts scheduled focus locks on the exact minute",
                granted = hasExactAlarm,
                required = false,
                onGrant = onRequestExactAlarm,
                testTag = "setup_perm_alarms"
            )
        }
        ThemedDivider(palette = palette)
        SetupPermissionRow(
            icon = Icons.Outlined.NotificationsOff,
            title = "Notification Shield",
            subtitle = "Optional — silences distracting alerts while locked",
            granted = hasNotificationAccess,
            required = false,
            onGrant = onRequestNotification,
            testTag = "setup_perm_notifications"
        )
    }
}

@Composable
private fun SetupPermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    required: Boolean,
    onGrant: () -> Unit,
    testTag: String
) {
    val palette = LocalFocusTheme.current
    val pillShape = RoundedCornerShape(50)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (granted) palette.success.copy(alpha = 0.16f) else palette.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Outlined.CheckCircle else icon,
                contentDescription = null,
                tint = if (granted) palette.success else palette.primary,
                modifier = Modifier.size(19.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (required && !granted) {
                    Text(
                        text = "REQUIRED",
                        fontFamily = JetBrainsMono,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                }
            }
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = palette.textSecondary,
                maxLines = 2
            )
        }

        if (granted) {
            Text(
                text = "Active",
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.success,
                modifier = Modifier
                    .clip(pillShape)
                    .background(palette.success.copy(alpha = 0.14f), pillShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(palette.primary, pillShape)
                    .clickable(onClick = onGrant)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Allow",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.onPrimary
                )
            }
        }
    }
}
