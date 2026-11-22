package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AllowedApp
import com.example.data.FocusQuizEngine
import com.example.data.FocusQuizQuestion
import com.example.data.FocusSettingsRepository
import com.example.data.LockAppLayout
import com.example.data.NotificationBlockerRepository
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class ManualLockStage {
    LOCKED,
    QUIZ,
    UNLOCKING
}

/**
 * Redesigned Manual Focus Lock Screen ("Sanctuary Monolith").
 *
 * Flows seamlessly across three integrated stages inside the same overlay:
 * 1. LOCKED: Serene monolith with live elapsed focus ring / Precision Aperture Iris,
 *    concentric telemetry bento, optional Essential Access tray, and Unlock Session pill.
 * 2. QUIZ: 10-question procedural infinite challenge (zero repeats ever) with a
 *    theme-matched Adaptive Built-In Keypad.
 * 3. UNLOCKING: Smooth Precision Aperture Iris opening animation before releasing lock.
 */
@Composable
fun ManualLockScreen(
    dailyGoalPercent: Int,
    currentStreak: Int,
    allowedApps: List<AllowedApp>,
    appLayout: LockAppLayout,
    minimalPillButtonSizeDp: Int = FocusSettingsRepository.DEFAULT_PILL_BUTTON_SIZE_DP,
    showSessionTimer: Boolean = true,
    hapticKeypad: Boolean = true,
    autoClearWrongAnswer: Boolean = true,
    sessionStartTimeMs: Long = System.currentTimeMillis(),
    iconCache: Map<String, ImageBitmap> = emptyMap(),
    onLaunchApp: (AllowedApp) -> Unit = {},
    onUnlock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val lockPalette = remember(palette) { LockPalette.fromFocusTheme(palette) }
    val blockerState by NotificationBlockerRepository.observe(context).collectAsState()
    val haptics = rememberFocusHaptics()

    var stage by remember { mutableStateOf(ManualLockStage.LOCKED) }
    var quizQuestions by remember { mutableStateOf<List<FocusQuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var nowMs by remember(sessionStartTimeMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sessionStartTimeMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val elapsedMs = (nowMs - sessionStartTimeMs).coerceAtLeast(0L)
    val elapsedSec = elapsedMs / 1000L
    val hours = elapsedSec / 3600L
    val mins = (elapsedSec % 3600L) / 60L
    val secs = elapsedSec % 60L
    val formattedElapsed = if (hours > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
    val liveClockText = remember(nowMs / 30_000L) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(nowMs))
    }
    val liveDateText = remember(nowMs / 3_600_000L) {
        SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(nowMs)).uppercase(Locale.US)
    }
    val silencedCount = blockerState.activeSessionBlocked.size

    val haloAccent by animateColorAsState(
        targetValue = if (stage == ManualLockStage.UNLOCKING) palette.success else palette.primary,
        animationSpec = tween(420),
        label = "manual_lock_halo_accent"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("manual_lock_screen_root")
    ) {
        ThemeBackdropLayer(palette = palette)

        SanctuaryAmbientHalo(
            accent = haloAccent,
            alpha = if (stage == ManualLockStage.UNLOCKING) 0.16f else if (palette.isLiquidGlass) 0.085f else 0.11f
        )

        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260))) togetherWith
                    fadeOut(tween(180))
            },
            label = "manual_lock_stage"
        ) { activeStage ->
            when (activeStage) {
                ManualLockStage.LOCKED -> {
                    ManualLockedOverview(
                        palette = palette,
                        lockPalette = lockPalette,
                        liveClockText = liveClockText,
                        liveDateText = liveDateText,
                        formattedElapsed = formattedElapsed,
                        elapsedSec = elapsedSec,
                        showSessionTimer = showSessionTimer,
                        dailyGoalPercent = dailyGoalPercent,
                        currentStreak = currentStreak,
                        silencedCount = silencedCount,
                        allowedApps = allowedApps,
                        appLayout = appLayout,
                        minimalPillButtonSizeDp = minimalPillButtonSizeDp,
                        iconCache = iconCache,
                        onLaunchApp = onLaunchApp,
                        onStartUnlockQuiz = {
                            if (hapticKeypad) haptics.confirm()
                            if (quizQuestions.isEmpty()) {
                                quizQuestions = FocusQuizEngine.generateQuizSet(
                                    context = context,
                                    count = FocusQuizEngine.QUESTIONS_PER_UNLOCK
                                )
                                currentIndex = 0
                            }
                            userInput = ""
                            errorMessage = null
                            stage = ManualLockStage.QUIZ
                        }
                    )
                }

                ManualLockStage.QUIZ -> {
                    val activeQuestion = quizQuestions.getOrNull(currentIndex)
                    if (activeQuestion != null) {
                        ManualUnlockQuizStage(
                            palette = palette,
                            question = activeQuestion,
                            questionIndex = currentIndex,
                            totalQuestions = quizQuestions.size.coerceAtLeast(FocusQuizEngine.QUESTIONS_PER_UNLOCK),
                            userInput = userInput,
                            errorMessage = errorMessage,
                            hapticKeypad = hapticKeypad,
                            autoClearWrongAnswer = autoClearWrongAnswer,
                            onBackToLock = {
                                if (hapticKeypad) haptics.tick()
                                userInput = ""
                                errorMessage = null
                                stage = ManualLockStage.LOCKED
                            },
                            onInputChange = { next ->
                                userInput = next
                                if (errorMessage != null) errorMessage = null
                            },
                            onWrongAnswer = {
                                errorMessage = "Not quite — try again"
                                if (autoClearWrongAnswer) {
                                    userInput = ""
                                }
                            },
                            onCorrectAnswer = {
                                errorMessage = null
                                userInput = ""
                                if (currentIndex + 1 >= quizQuestions.size) {
                                    stage = ManualLockStage.UNLOCKING
                                } else {
                                    currentIndex += 1
                                }
                            }
                        )
                    }
                }

                ManualLockStage.UNLOCKING -> {
                    ManualUnlockApertureStage(
                        palette = palette,
                        formattedElapsed = formattedElapsed,
                        onAnimationFinished = onUnlock
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualLockedOverview(
    palette: FocusThemePalette,
    lockPalette: LockPalette,
    liveClockText: String,
    liveDateText: String,
    formattedElapsed: String,
    elapsedSec: Long,
    showSessionTimer: Boolean,
    dailyGoalPercent: Int,
    currentStreak: Int,
    silencedCount: Int,
    allowedApps: List<AllowedApp>,
    appLayout: LockAppLayout,
    minimalPillButtonSizeDp: Int,
    iconCache: Map<String, ImageBitmap>,
    onLaunchApp: (AllowedApp) -> Unit,
    onStartUnlockQuiz: () -> Unit
) {
    val breathTransition = rememberInfiniteTransition(label = "iris_breath")
    val breathScale by breathTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iris_breath_scale"
    )
    // Gentle 60-second minute sweep on the theme ring for a calm sense of live progress
    val minuteProgress = remember(elapsedSec) {
        val secInMin = (elapsedSec % 60L).toFloat() / 60f
        (0.08f + secInMin * 0.92f).coerceIn(0.08f, 1f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Borderless Editorial Ambient Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .testTag("manual_lock_header")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    StatusPulseDot(color = palette.primary, scale = 1f)
                    Text(
                        text = "DEEP FOCUS",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                        color = palette.primary
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(palette.textSecondary.copy(alpha = 0.45f))
                    )
                    Text(
                        text = "$liveClockText · $liveDateText",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.5.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Focus Sanctuary",
                    fontFamily = palette.displayFont,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // 2. Unified Sanctuary Monolith Card
            SanctuaryMonolithCard(
                palette = palette,
                cornerRadius = 32.dp,
                contentPaddingHorizontal = 20.dp,
                contentPaddingVertical = 24.dp,
                verticalSpacing = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_lock_monolith_card")
            ) {
                if (showSessionTimer) {
                    ThemedTimerRing(
                        progress = minuteProgress,
                        size = 208.dp,
                        palette = palette
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FocusLockEmblem(
                                size = 34.dp,
                                showAmbientGlow = false
                            )
                            Text(
                                text = formattedElapsed,
                                fontFamily = palette.numeralFont,
                                fontSize = if (elapsedSec >= 3600L) 29.sp else 36.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                color = palette.textPrimary,
                                maxLines = 1,
                                modifier = Modifier.testTag("manual_lock_elapsed_text")
                            )
                            Text(
                                text = "ELAPSED",
                                fontFamily = JetBrainsMono,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.9.sp,
                                color = palette.primary
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        FocusLockEmblem(
                            size = 112.dp,
                            showAmbientGlow = true,
                            modifier = Modifier.graphicsLayer {
                                scaleX = breathScale
                                scaleY = breathScale
                            }
                        )
                        Text(
                            text = "STAY PRESENT",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.2.sp,
                            color = palette.primary
                        )
                    }
                }

                ThemedDivider(palette = palette)

                // Concentric 3-Tile Bento Telemetry Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_lock_telemetry_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val goalAchieved = dailyGoalPercent >= 100
                    SanctuaryBentoTile(
                        icon = if (goalAchieved) Icons.Outlined.CheckCircle else Icons.Outlined.Flag,
                        iconTint = if (goalAchieved) palette.success else palette.primary,
                        value = "$dailyGoalPercent%",
                        valueColor = if (goalAchieved) palette.success else palette.textPrimary,
                        label = "GOAL",
                        scale = 1f,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    SanctuaryBentoTile(
                        icon = Icons.Outlined.LocalFireDepartment,
                        iconTint = palette.warning,
                        value = "${currentStreak}d",
                        valueColor = palette.textPrimary,
                        label = "STREAK",
                        scale = 1f,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    SanctuaryBentoTile(
                        icon = Icons.Outlined.NotificationsOff,
                        iconTint = if (silencedCount > 0) palette.primary else palette.textSecondary,
                        value = "$silencedCount",
                        valueColor = palette.textPrimary,
                        label = "SILENCED",
                        scale = 1f,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Matching Companion Essential Apps Tray
            if (allowedApps.isNotEmpty()) {
                SanctuaryMonolithCard(
                    palette = palette,
                    cornerRadius = 26.dp,
                    contentPaddingHorizontal = 16.dp,
                    contentPaddingVertical = 14.dp,
                    verticalSpacing = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_lock_apps_tray")
                ) {
                    Text(
                        text = "ESSENTIAL ACCESS",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.8.sp,
                        color = palette.textSecondary.copy(alpha = 0.72f)
                    )
                    LockAllowedAppsSection(
                        apps = allowedApps,
                        layout = appLayout,
                        palette = lockPalette,
                        onAppClick = onLaunchApp,
                        isPreview = false,
                        pillButtonSizeDp = minimalPillButtonSizeDp.coerceIn(
                            FocusSettingsRepository.MIN_PILL_BUTTON_SIZE_DP,
                            FocusSettingsRepository.MAX_PILL_BUTTON_SIZE_DP
                        ),
                        iconCache = iconCache,
                        readFromCacheOnly = false
                    )
                }
            }

            // 4. Tactile Unlock Session Pill Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                val unlockInteraction = remember { MutableInteractionSource() }
                val unlockScale = rememberSpringPressScale(unlockInteraction)
                Button(
                    onClick = onStartUnlockQuiz,
                    interactionSource = unlockInteraction,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer {
                            scaleX = unlockScale
                            scaleY = unlockScale
                        }
                        .testTag("manual_lock_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock Session",
                        fontFamily = palette.bodyFont,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )
                }

                Text(
                    text = "COMPLETE 10 QUICK CHALLENGES TO UNLOCK",
                    fontFamily = JetBrainsMono,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.4.sp,
                    color = palette.textSecondary.copy(alpha = 0.68f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ManualUnlockQuizStage(
    palette: FocusThemePalette,
    question: FocusQuizQuestion,
    questionIndex: Int,
    totalQuestions: Int,
    userInput: String,
    errorMessage: String?,
    hapticKeypad: Boolean,
    autoClearWrongAnswer: Boolean,
    onBackToLock: () -> Unit,
    onInputChange: (String) -> Unit,
    onWrongAnswer: () -> Unit,
    onCorrectAnswer: () -> Unit
) {
    val haptics = rememberFocusHaptics()
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    fun triggerWrongFeedback() {
        if (hapticKeypad) haptics.reject()
        onWrongAnswer()
        scope.launch {
            shakeOffset.snapTo(0f)
            val offsets = listOf(-14f, 14f, -10f, 10f, -5f, 0f)
            for (target in offsets) {
                shakeOffset.animateTo(target, animationSpec = tween(42))
            }
        }
    }

    fun submitAnswer() {
        if (userInput.isBlank()) return
        if (FocusQuizEngine.checkAnswer(question, userInput)) {
            if (hapticKeypad) haptics.confirm()
            onCorrectAnswer()
        } else {
            triggerWrongFeedback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("manual_unlock_quiz_stage"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar: Return to Lock pill + Step Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val backShape = RoundedCornerShape(50)
                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(backShape)
                        .focusGlassPanel(shape = backShape, palette = palette)
                        .clickable(onClick = onBackToLock)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("quiz_back_to_lock_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to lock screen",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Lock",
                        fontFamily = palette.bodyFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusLockEmblem(size = 24.dp, showAmbientGlow = false)
                    Text(
                        text = "${questionIndex + 1} / $totalQuestions",
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = palette.primary,
                        modifier = Modifier.testTag("quiz_progress_counter")
                    )
                }
            }

            // 10-Segment Precision Progress Track
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_segment_track"),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (i in 0 until totalQuestions) {
                    val segColor by animateColorAsState(
                        targetValue = when {
                            i < questionIndex -> palette.success
                            i == questionIndex -> palette.primary
                            else -> palette.borderHighlight.copy(alpha = 0.42f)
                        },
                        animationSpec = tween(220),
                        label = "quiz_seg_$i"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(segColor)
                    )
                }
            }

            // Question Monolith Card
            AnimatedContent(
                targetState = question,
                transitionSpec = {
                    (fadeIn(tween(210)) + slideInHorizontally(tween(210)) { it / 6 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 6 })
                },
                label = "quiz_question_card"
            ) { q ->
                SanctuaryMonolithCard(
                    palette = palette,
                    cornerRadius = 28.dp,
                    contentPaddingHorizontal = 20.dp,
                    contentPaddingVertical = 22.dp,
                    verticalSpacing = 14.dp,
                    accentRim = if (errorMessage != null) palette.error else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_question_card")
                ) {
                    // Category eyebrow pill
                    val catShape = RoundedCornerShape(50)
                    Box(
                        modifier = Modifier
                            .clip(catShape)
                            .background(palette.primary.copy(alpha = 0.14f), catShape)
                            .border(0.75.dp, palette.primary.copy(alpha = 0.34f), catShape)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = q.category.uppercase(Locale.US),
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.6.sp,
                            color = palette.primary
                        )
                    }

                    Text(
                        text = q.hint,
                        fontFamily = palette.bodyFont,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("quiz_question_hint")
                    )

                    Text(
                        text = q.prompt,
                        fontFamily = JetBrainsMono,
                        fontSize = if (q.prompt.length > 22) 19.sp else 23.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("quiz_question_prompt")
                    )

                    ThemedDivider(palette = palette)

                    // Live Answer Display Capsule (with spring shake on wrong answer)
                    val answerShape = RoundedCornerShape(18.dp)
                    val answerBorder = when {
                        errorMessage != null -> palette.error.copy(alpha = 0.85f)
                        userInput.isNotEmpty() -> palette.primary.copy(alpha = 0.72f)
                        else -> palette.borderHighlight.copy(alpha = 0.45f)
                    }
                    val answerBg = if (palette.isLiquidGlass) {
                        Color(0x6606080E)
                    } else {
                        palette.background.copy(alpha = 0.55f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .clip(answerShape)
                            .background(answerBg, answerShape)
                            .border(width = 1.25.dp, color = answerBorder, shape = answerShape)
                            .padding(horizontal = 16.dp)
                            .testTag("quiz_answer_display"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userInput.isEmpty()) {
                            Text(
                                text = "TAP ANSWER BELOW",
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                letterSpacing = 1.8.sp,
                                color = palette.textSecondary.copy(alpha = 0.46f)
                            )
                        } else {
                            Text(
                                text = userInput,
                                fontFamily = JetBrainsMono,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                color = if (errorMessage != null) palette.error else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = errorMessage ?: if (autoClearWrongAnswer) {
                            "Answer accurately to advance"
                        } else {
                            "Tap Submit to verify"
                        },
                        fontFamily = palette.bodyFont,
                        fontSize = 12.sp,
                        fontWeight = if (errorMessage != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (errorMessage != null) palette.error else palette.textSecondary.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("quiz_feedback_text")
                    )
                }
            }
        }

        // Docked Adaptive Built-In Keypad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(top = 12.dp)
        ) {
            QuizAdaptiveKeypad(
                inputMode = question.inputMode,
                canSubmit = userInput.isNotBlank(),
                hapticEnabled = hapticKeypad,
                palette = palette,
                onChar = { ch ->
                    if (userInput.length < 14) {
                        onInputChange(userInput + ch)
                    }
                },
                onBackspace = {
                    if (userInput.isNotEmpty()) {
                        onInputChange(userInput.dropLast(1))
                    }
                },
                onClear = {
                    if (userInput.isNotEmpty()) {
                        onInputChange("")
                    }
                },
                onSubmit = { submitAnswer() }
            )
        }
    }
}

@Composable
private fun ManualUnlockApertureStage(
    palette: FocusThemePalette,
    formattedElapsed: String,
    onAnimationFinished: () -> Unit
) {
    val haptics = rememberFocusHaptics()
    var triggered by remember { mutableStateOf(false) }

    val openProgress by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing),
        label = "aperture_open_progress"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (triggered) 1f else 0.90f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow),
        label = "unlock_card_scale"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(durationMillis = 480, delayMillis = 160),
        label = "unlock_text_alpha"
    )

    LaunchedEffect(Unit) {
        haptics.confirm()
        delay(80L)
        triggered = true
        delay(1220L)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("manual_unlock_animation_stage"),
        contentAlignment = Alignment.Center
    ) {
        SanctuaryMonolithCard(
            palette = palette,
            cornerRadius = 34.dp,
            contentPaddingHorizontal = 26.dp,
            contentPaddingVertical = 34.dp,
            verticalSpacing = 18.dp,
            accentRim = palette.success,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                }
        ) {
            FocusLockEmblem(
                size = 128.dp,
                showAmbientGlow = true,
                openProgress = openProgress,
                accentOverride = palette.success
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer { alpha = textAlpha }
            ) {
                Text(
                    text = "SESSION UNLOCKED",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.2.sp,
                    color = palette.success
                )
                Text(
                    text = "Aperture Open",
                    fontFamily = palette.displayFont,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Focused for $formattedElapsed",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.5.sp,
                    color = palette.textSecondary
                )
            }
        }
    }
}
