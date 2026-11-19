package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LockService
import com.example.MainActivity
import com.example.data.FocusSettingsRepository
import com.example.data.FocusStatsRepository
import com.example.data.FocusXpRepository
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.RankXpBottomSheet
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemedTimerRing
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import com.example.uploadPinToFirebase
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun FocusHomeScreen(
    onOpenShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberFocusHaptics()
    val palette = LocalFocusTheme.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val settingsState by FocusSettingsRepository.observe(context).collectAsStateWithLifecycle()
    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRankSheet by remember { mutableStateOf(false) }

    var permissionsChecked by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(true) }
    var hasUsagePermission by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        hasOverlayPermission = FocusSettingsRepository.canDrawOverlaysAsync(context)
        hasUsagePermission = FocusSettingsRepository.hasUsageStatsPermissionAsync(context)
        permissionsChecked = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    hasOverlayPermission = FocusSettingsRepository.canDrawOverlaysAsync(context)
                    hasUsagePermission = FocusSettingsRepository.hasUsageStatsPermissionAsync(context)
                    permissionsChecked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsReady = hasOverlayPermission && hasUsagePermission
    val goalMs = settingsState.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
    val goalProgress = (xpState.todayFocusMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
    val animatedRingProgress by animateFloatAsState(
        targetValue = if (goalProgress > 0f) goalProgress else 0.04f,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessLow),
        label = "home_goal_ring_progress"
    )
    val goalMet = xpState.todayFocusMs >= goalMs
    val todayFormatted = remember(xpState.todayFocusMs) {
        if (xpState.todayFocusMs > 0L) {
            FocusStatsRepository.formatDurationShort(xpState.todayFocusMs)
        } else {
            "0m"
        }
    }
    val goalFormatted = remember(settingsState.dailyGoalMinutes) {
        val mins = settingsState.dailyGoalMinutes.coerceAtLeast(1L)
        val h = mins / 60L
        val m = mins % 60L
        when {
            h > 0L && m > 0L -> "${h}h ${m}m"
            h > 0L -> "${h}h"
            else -> "${m}m"
        }
    }

    fun launchSafeSettings(intent: Intent) {
        runCatching {
            context.startActivity(intent)
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
    }

    fun startFocusAction() {
        if (isLoading) return
        val mainActivity = context as? MainActivity
        if (!Settings.canDrawOverlays(context)) {
            haptics.tap()
            launchSafeSettings(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
            return
        }
        if (mainActivity?.hasUsageStatsPermission(context) == false) {
            haptics.tap()
            launchSafeSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        haptics.confirm()
        val pin = String.format(Locale.US, "%06d", Random.nextInt(1000000))
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            val success = uploadPinToFirebase(pin)
            isLoading = false
            if (success) {
                val serviceIntent = Intent(context, LockService::class.java).apply {
                    putExtra("PIN", pin)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
                NothingFocusWidgetUpdater.scheduleUpdateAll(context)
                (context as? ComponentActivity)?.moveTaskToBack(true)
            } else {
                haptics.reject()
                errorMessage = "Could not sync unlock PIN. Check your connection."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ThemeBackdropLayer(palette = palette)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Minimal, airy header (no duplicate emblem, no hard bar divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusLockEmblem(
                        size = 24.dp,
                        showAmbientGlow = false
                    )
                    Text(
                        text = "FOCUS LOCK",
                        fontFamily = palette.displayFont,
                        fontSize = 15.sp,
                        letterSpacing = 1.6.sp,
                        color = palette.textPrimary
                    )
                }

                IconButton(
                    onClick = {
                        haptics.tap()
                        onOpenShare()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Stats",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Centered serene hero sanctuary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 104.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Minimal Progress Halo + Quiet Readout
                    ThemedTimerRing(
                        progress = animatedRingProgress,
                        size = 224.dp,
                        palette = palette
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                letterSpacing = 1.8.sp,
                                color = palette.textSecondary
                            )
                            Text(
                                text = todayFormatted,
                                fontFamily = palette.numeralFont,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (goalMet) "GOAL MET" else "${(goalProgress * 100).toInt()}% OF $goalFormatted",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                letterSpacing = 0.6.sp,
                                color = if (goalMet) palette.success else palette.textSecondary
                            )
                        }
                    }

                    // Quiet Tappable Rank & XP Chip (opens RankXpBottomSheet)
                    val rankInteraction = remember { MutableInteractionSource() }
                    val rankScale = rememberSpringPressScale(rankInteraction, pressedScale = 0.96f)
                    Surface(
                        onClick = {
                            haptics.tap()
                            showRankSheet = true
                        },
                        interactionSource = rankInteraction,
                        shape = RoundedCornerShape(50),
                        color = palette.surface,
                        border = BorderStroke(0.75.dp, palette.border),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = rankScale
                                scaleY = rankScale
                            }
                            .testTag("home_rank_xp_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = palette.primary
                            )
                            Text(
                                text = xpState.rank.title.uppercase(Locale.US),
                                fontFamily = palette.numeralFont,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.8.sp,
                                color = palette.textPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(palette.borderHighlight)
                            )
                            Text(
                                text = "${xpState.totalXp} XP",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textSecondary
                            )
                        }
                    }

                    // Signature Interactive Slide-or-Tap Focus Pill
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = palette.primary
                            )
                        }
                    } else {
                        SlideOrTapFocusPill(
                            permissionsReady = permissionsReady,
                            onTrigger = { startFocusAction() }
                        )
                    }

                    // Only show permission notice when actually missing (zero clutter when ready)
                    AnimatedVisibility(
                        visible = permissionsChecked && !permissionsReady,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.primaryContainer.copy(alpha = 0.55f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = palette.primary
                            )
                            Text(
                                text = "Overlay & usage permissions required to lock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = palette.textPrimary.copy(alpha = 0.88f)
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = palette.primary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        if (showRankSheet) {
            RankXpBottomSheet(
                xpState = xpState,
                dailyGoalMinutes = settingsState.dailyGoalMinutes,
                onDismiss = { showRankSheet = false }
            )
        }
    }
}

/**
 * Tactile minimal pill supporting both 1-tap launch and horizontal slide-to-lock
 * with progressive detent haptics along the track.
 */
@Composable
private fun SlideOrTapFocusPill(
    permissionsReady: Boolean,
    onTrigger: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnTrigger by rememberUpdatedState(onTrigger)

    val thumbSizeDp = 52.dp
    val trackPaddingDp = 6.dp
    val pillShape = RoundedCornerShape(32.dp)
    val offsetX = remember { Animatable(0f) }
    var lastDetentBucket by remember { mutableIntStateOf(0) }

    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.97f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(pillShape)
            .background(palette.surface)
            .border(0.75.dp, palette.borderHighlight.copy(alpha = 0.7f), pillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { currentOnTrigger() }
            )
            .testTag("start_focus_button")
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }
        val thumbSizePx = with(density) { thumbSizeDp.toPx() }
        val padPx = with(density) { trackPaddingDp.toPx() }
        val maxDragPx = (trackWidthPx - thumbSizePx - padPx * 2f).coerceAtLeast(1f)
        val fraction = (offsetX.value / maxDragPx).coerceIn(0f, 1f)

        // Subtle glowing fill trailing behind the sliding thumb
        if (fraction > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.18f, 1f))
                    .height(64.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.28f),
                                palette.primary.copy(alpha = 0.10f)
                            )
                        )
                    )
            )
        }

        // Centered subtle prompt label that gently fades as you slide
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 34.dp, end = 16.dp)
                .graphicsLayer { alpha = (1f - fraction * 1.4f).coerceIn(0f, 1f) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (permissionsReady) "Slide or tap to focus" else "Grant permission",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = palette.textSecondary.copy(alpha = 0.75f),
                modifier = Modifier.size(15.dp)
            )
        }

        // Draggable primary lock thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = trackPaddingDp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(palette.primary)
                .pointerInput(maxDragPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            lastDetentBucket = 0
                            haptics.gestureStart()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val next = (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                            val nextFrac = next / maxDragPx
                            val bucket = when {
                                nextFrac >= 0.82f -> 3
                                nextFrac >= 0.55f -> 2
                                nextFrac >= 0.28f -> 1
                                else -> 0
                            }
                            if (bucket != lastDetentBucket) {
                                lastDetentBucket = bucket
                                if (bucket == 3) haptics.segment() else haptics.tick()
                            }
                            scope.launch {
                                offsetX.snapTo(next)
                            }
                        },
                        onDragEnd = {
                            val triggered = offsetX.value >= maxDragPx * 0.78f
                            scope.launch {
                                if (triggered) {
                                    offsetX.animateTo(
                                        maxDragPx,
                                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
                                    )
                                    currentOnTrigger()
                                    offsetX.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                } else {
                                    haptics.gestureEnd()
                                    offsetX.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Start Focus Mode",
                tint = palette.onPrimary,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}
