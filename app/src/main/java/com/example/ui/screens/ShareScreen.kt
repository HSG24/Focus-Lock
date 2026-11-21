package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.Session
import com.example.data.FocusSettingsState
import com.example.data.FocusXpRepository
import com.example.data.RangePreset
import com.example.data.ShareLinkResult
import com.example.data.ShareMode
import com.example.data.ShareRepository
import com.example.data.SharedLinkItem
import com.example.data.SharedStatsSummary
import com.example.fetchAllSessions
import com.example.ui.components.ElasticMorphingSegmentedPill
import com.example.ui.components.ElasticPillSegment
import com.example.ui.components.RollingMetricText
import com.example.ui.components.ShareRecapCardStudio
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.focusGlassPanel
import com.example.ui.components.performFocusConfirm
import com.example.ui.components.performFocusReject
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private enum class ShareStep(val depth: Int) {
    CHOOSE_MODE(0),
    CONFIGURE(1),
    LINK_READY(2),
    YOUR_LINKS(1),
    RECAP_CARD(1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    settingsState: FocusSettingsState,
    initialDateMs: Long? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(ShareStep.CHOOSE_MODE) }
    var selectedMode by remember { mutableStateOf(ShareMode.LIVE) }

    var allSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var isLoadingSessions by remember { mutableStateOf(true) }

    val todayStartMs = remember { ShareRepository.startOfDayMs(System.currentTimeMillis()) }
    var liveUseCustomDate by remember { mutableStateOf(false) }
    var liveStartDateMs by remember {
        mutableLongStateOf(initialDateMs?.let { ShareRepository.startOfDayMs(it) } ?: todayStartMs)
    }

    var selectedPreset by remember { mutableStateOf<RangePreset?>(RangePreset.TODAY) }
    var rangeStartDateMs by remember {
        mutableLongStateOf(initialDateMs?.let { ShareRepository.startOfDayMs(it) } ?: todayStartMs)
    }
    var rangeEndDateMs by remember {
        mutableLongStateOf(initialDateMs?.let { ShareRepository.startOfDayMs(it) } ?: todayStartMs)
    }

    var showLiveStartPicker by remember { mutableStateOf(false) }
    var showRangeStartPicker by remember { mutableStateOf(false) }
    var showRangeEndPicker by remember { mutableStateOf(false) }

    var isCreatingLink by remember { mutableStateOf(false) }
    var isStoppingLive by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var createdResult by remember { mutableStateOf<ShareLinkResult?>(null) }
    var linkStoppedInResult by remember { mutableStateOf(false) }
    var copiedBanner by remember { mutableStateOf(false) }
    var copiedActiveBanner by remember { mutableStateOf(false) }

    var sharedLinks by remember { mutableStateOf<List<SharedLinkItem>>(emptyList()) }
    var isLoadingLinks by remember { mutableStateOf(false) }
    var linksErrorMessage by remember { mutableStateOf<String?>(null) }
    var busyLinkId by remember { mutableStateOf<String?>(null) }
    var copiedLinkId by remember { mutableStateOf<String?>(null) }
    var linkPendingDelete by remember { mutableStateOf<SharedLinkItem?>(null) }

    val refreshSharedLinks: () -> Unit = {
        coroutineScope.launch {
            isLoadingLinks = true
            linksErrorMessage = null
            ShareRepository.fetchAllSharedLinks()
                .onSuccess { sharedLinks = it }
                .onFailure { linksErrorMessage = "Could not load your shared links. Check your connection." }
            isLoadingLinks = false
        }
    }

    LaunchedEffect(Unit) {
        isLoadingSessions = true
        allSessions = fetchAllSessions()
        isLoadingSessions = false
    }

    LaunchedEffect(currentStep) {
        if (currentStep == ShareStep.YOUR_LINKS) {
            refreshSharedLinks()
        }
    }

    LaunchedEffect(copiedBanner) {
        if (copiedBanner) {
            delay(2000L)
            copiedBanner = false
        }
    }

    LaunchedEffect(copiedActiveBanner) {
        if (copiedActiveBanner) {
            delay(2000L)
            copiedActiveBanner = false
        }
    }

    LaunchedEffect(copiedLinkId) {
        if (copiedLinkId != null) {
            delay(2000L)
            copiedLinkId = null
        }
    }

    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()

    val handleStepBack = {
        errorMessage = null
        when (currentStep) {
            ShareStep.CHOOSE_MODE -> onBack()
            ShareStep.CONFIGURE -> currentStep = ShareStep.CHOOSE_MODE
            ShareStep.LINK_READY -> onBack()
            ShareStep.YOUR_LINKS -> currentStep = ShareStep.CHOOSE_MODE
            ShareStep.RECAP_CARD -> currentStep = ShareStep.CHOOSE_MODE
        }
    }

    BackHandler(onBack = handleStepBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ThemeBackdropLayer(palette = palette)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentStep) {
                                ShareStep.CHOOSE_MODE -> "Share"
                                ShareStep.CONFIGURE -> if (selectedMode == ShareMode.LIVE) "Live Share" else "Range Share"
                                ShareStep.LINK_READY -> "Link Ready"
                                ShareStep.YOUR_LINKS -> "Your Links"
                                ShareStep.RECAP_CARD -> "Recap Card"
                            },
                            fontFamily = palette.displayFont,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    },
                    navigationIcon = {
                        ShareCircleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = {
                                haptics.tick()
                                handleStepBack()
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .testTag("share_back_button")
                        )
                    },
                    actions = {
                        if (currentStep == ShareStep.YOUR_LINKS) {
                            ShareCircleIconButton(
                                icon = Icons.Outlined.Refresh,
                                contentDescription = "Refresh Links",
                                enabled = !isLoadingLinks,
                                onClick = {
                                    haptics.tick()
                                    refreshSharedLinks()
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("refresh_your_links_button")
                            )
                        } else {
                            val pillInteraction = remember { MutableInteractionSource() }
                            val pillScale = rememberSpringPressScale(pillInteraction, pressedScale = 0.94f)
                            Box(
                                modifier = Modifier
                                    .padding(end = 14.dp)
                                    .heightIn(min = 48.dp)
                                    .graphicsLayer {
                                        scaleX = pillScale
                                        scaleY = pillScale
                                    }
                                    .clickable(
                                        interactionSource = pillInteraction,
                                        indication = null
                                    ) {
                                        haptics.segment()
                                        errorMessage = null
                                        currentStep = ShareStep.YOUR_LINKS
                                    }
                                    .testTag("your_links_topbar_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .focusGlassPanel(shape = RoundedCornerShape(50), palette = palette)
                                        .padding(horizontal = 13.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = palette.primary
                                    )
                                    Text(
                                        text = "Links",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    val forward = targetState.depth >= initialState.depth
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    ) { w -> (w * 0.16f * dir).toInt() } + fadeIn(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium)
                    ) { w -> (-w * 0.12f * dir).toInt() } + fadeOut(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ))
                },
                label = "share_step_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { step ->
                when (step) {
                    ShareStep.CHOOSE_MODE -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!settingsState.activeLiveShareId.isNullOrBlank()) {
                                val activeUrl = ShareRepository.buildShareUrl(settingsState.activeLiveShareId)
                                ActiveLiveShareCard(
                                    startDateMs = settingsState.activeLiveShareStartDateMs,
                                    url = activeUrl,
                                    isCopied = copiedActiveBanner,
                                    isStopping = isStoppingLive,
                                    onCopy = {
                                        haptics.confirm()
                                        copyUrlToClipboard(context, activeUrl)
                                        copiedActiveBanner = true
                                    },
                                    onOpen = {
                                        haptics.tap()
                                        openUrlInBrowser(context, activeUrl)
                                    },
                                    onStop = {
                                        haptics.tap()
                                        isStoppingLive = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            val res = ShareRepository.stopLiveShare(context)
                                            isStoppingLive = false
                                            if (res.isFailure) {
                                                haptics.reject()
                                                errorMessage = "Could not stop live share. Check your internet connection."
                                            } else {
                                                haptics.confirm()
                                            }
                                        }
                                    }
                                )
                            }

                            ShareModePillCard(
                                title = "Live Share",
                                subtitle = "Streams focus updates in real time",
                                badgeText = "LIVE",
                                badgeColor = palette.success,
                                icon = Icons.Outlined.Sensors,
                                iconTint = palette.success,
                                iconBg = palette.success.copy(alpha = 0.14f),
                                testTag = "share_mode_live_card",
                                onClick = {
                                    haptics.segment()
                                    selectedMode = ShareMode.LIVE
                                    liveUseCustomDate = false
                                    liveStartDateMs = todayStartMs
                                    errorMessage = null
                                    currentStep = ShareStep.CONFIGURE
                                }
                            )

                            ShareModePillCard(
                                title = "Range Share",
                                subtitle = "Static snapshot for chosen dates",
                                badgeText = "SNAPSHOT",
                                badgeColor = palette.primary,
                                icon = Icons.Outlined.DateRange,
                                iconTint = palette.primary,
                                iconBg = palette.primaryContainer,
                                testTag = "share_mode_range_card",
                                onClick = {
                                    haptics.segment()
                                    selectedMode = ShareMode.RANGE
                                    if (initialDateMs != null && ShareRepository.startOfDayMs(initialDateMs) != todayStartMs) {
                                        selectedPreset = null
                                        rangeStartDateMs = ShareRepository.startOfDayMs(initialDateMs)
                                        rangeEndDateMs = ShareRepository.startOfDayMs(initialDateMs)
                                    } else {
                                        selectedPreset = RangePreset.TODAY
                                        rangeStartDateMs = todayStartMs
                                        rangeEndDateMs = todayStartMs
                                    }
                                    errorMessage = null
                                    currentStep = ShareStep.CONFIGURE
                                }
                            )

                            ShareModePillCard(
                                title = "Recap Image Card",
                                subtitle = "Export themed Nothing/Glass PNG card",
                                badgeText = "IMAGE",
                                badgeColor = palette.primary,
                                icon = Icons.Outlined.Image,
                                iconTint = palette.primary,
                                iconBg = palette.primaryContainer,
                                testTag = "share_mode_recap_card",
                                onClick = {
                                    haptics.segment()
                                    errorMessage = null
                                    currentStep = ShareStep.RECAP_CARD
                                }
                            )

                            ShareModePillCard(
                                title = "Your Links",
                                subtitle = "Manage, copy, stop, or delete links",
                                badgeText = "MANAGE",
                                badgeColor = palette.primarySecondary,
                                icon = Icons.Outlined.Link,
                                iconTint = palette.primarySecondary,
                                iconBg = palette.surfaceElevated,
                                testTag = "your_links_card_button",
                                onClick = {
                                    haptics.segment()
                                    errorMessage = null
                                    currentStep = ShareStep.YOUR_LINKS
                                }
                            )

                            if (errorMessage != null) {
                                ErrorBanner(message = errorMessage!!)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    ShareStep.CONFIGURE -> {
                        val previewSummary = remember(
                            allSessions,
                            selectedMode,
                            liveStartDateMs,
                            rangeStartDateMs,
                            rangeEndDateMs
                        ) {
                            if (selectedMode == ShareMode.LIVE) {
                                ShareRepository.computeStatsForRange(allSessions, liveStartDateMs, null)
                            } else {
                                ShareRepository.computeStatsForRange(allSessions, rangeStartDateMs, rangeEndDateMs)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (selectedMode == ShareMode.LIVE) {
                                LiveShareConfigSection(
                                    useCustomDate = liveUseCustomDate,
                                    startDateMs = liveStartDateMs,
                                    hasExistingActiveLive = !settingsState.activeLiveShareId.isNullOrBlank(),
                                    onSelectToday = {
                                        liveUseCustomDate = false
                                        liveStartDateMs = todayStartMs
                                    },
                                    onSelectCustomDate = {
                                        showLiveStartPicker = true
                                    }
                                )
                            } else {
                                RangeShareConfigSection(
                                    selectedPreset = selectedPreset,
                                    startDateMs = rangeStartDateMs,
                                    endDateMs = rangeEndDateMs,
                                    onSelectPreset = { preset ->
                                        selectedPreset = preset
                                        val today = Calendar.getInstance()
                                        when (preset) {
                                            RangePreset.TODAY -> {
                                                rangeStartDateMs = todayStartMs
                                                rangeEndDateMs = todayStartMs
                                            }
                                            RangePreset.YESTERDAY -> {
                                                val yest = (today.clone() as Calendar).apply { add(Calendar.DATE, -1) }.timeInMillis
                                                val yestDay = ShareRepository.startOfDayMs(yest)
                                                rangeStartDateMs = yestDay
                                                rangeEndDateMs = yestDay
                                            }
                                            RangePreset.LAST_7_DAYS -> {
                                                val start = (today.clone() as Calendar).apply { add(Calendar.DATE, -6) }.timeInMillis
                                                rangeStartDateMs = ShareRepository.startOfDayMs(start)
                                                rangeEndDateMs = todayStartMs
                                            }
                                            RangePreset.LAST_30_DAYS -> {
                                                val start = (today.clone() as Calendar).apply { add(Calendar.DATE, -29) }.timeInMillis
                                                rangeStartDateMs = ShareRepository.startOfDayMs(start)
                                                rangeEndDateMs = todayStartMs
                                            }
                                        }
                                    },
                                    onSelectStartDate = { showRangeStartPicker = true },
                                    onSelectEndDate = { showRangeEndPicker = true }
                                )
                            }

                            StatsPreviewCard(
                                summary = previewSummary,
                                isLoading = isLoadingSessions,
                                subtitle = if (selectedMode == ShareMode.LIVE) {
                                    ShareRepository.formatDateRangeLabel(liveStartDateMs, null)
                                } else {
                                    ShareRepository.formatDateRangeLabel(rangeStartDateMs, rangeEndDateMs)
                                },
                                isLive = selectedMode == ShareMode.LIVE
                            )

                            if (errorMessage != null) {
                                ErrorBanner(message = errorMessage!!)
                            }

                            Button(
                                onClick = {
                                    haptics.tap()
                                    isCreatingLink = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val latestSessions = fetchAllSessions()
                                        allSessions = latestSessions
                                        val result = if (selectedMode == ShareMode.LIVE) {
                                            ShareRepository.createLiveShare(
                                                context = context,
                                                sessions = latestSessions,
                                                startDateMs = liveStartDateMs
                                            )
                                        } else {
                                            ShareRepository.createRangeShare(
                                                sessions = latestSessions,
                                                startDateMs = rangeStartDateMs,
                                                endDateMs = rangeEndDateMs,
                                                xpState = FocusXpRepository.getCurrent(context)
                                            )
                                        }
                                        isCreatingLink = false
                                        result.onSuccess { linkResult ->
                                            haptics.confirm()
                                            createdResult = linkResult
                                            linkStoppedInResult = false
                                            copiedBanner = false
                                            currentStep = ShareStep.LINK_READY
                                        }.onFailure {
                                            haptics.reject()
                                            errorMessage = "Could not create share link. Check your internet connection."
                                        }
                                    }
                                },
                                enabled = !isCreatingLink && !isLoadingSessions,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("create_share_link_button")
                            ) {
                                if (isCreatingLink) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = palette.onPrimary
                                    )
                                } else {
                                    Text(
                                        text = if (selectedMode == ShareMode.LIVE) "Create Live Link" else "Create Snapshot Link",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    ShareStep.LINK_READY -> {
                        val result = createdResult
                        if (result != null) {
                            LinkReadySection(
                                result = result,
                                isCopied = copiedBanner,
                                isStopped = linkStoppedInResult,
                                isStopping = isStoppingLive,
                                errorMessage = errorMessage,
                                onCopyLink = {
                                    haptics.confirm()
                                    copyUrlToClipboard(context, result.url)
                                    copiedBanner = true
                                },
                                onOpenBrowser = {
                                    haptics.tap()
                                    openUrlInBrowser(context, result.url)
                                },
                                onSystemShare = {
                                    haptics.tap()
                                    shareUrlViaChooser(context, result.url)
                                },
                                onStopLiveSharing = {
                                    haptics.tap()
                                    isStoppingLive = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val res = ShareRepository.stopLiveShare(context)
                                        isStoppingLive = false
                                        if (res.isSuccess) {
                                            haptics.confirm()
                                            linkStoppedInResult = true
                                        } else {
                                            haptics.reject()
                                            errorMessage = "Could not stop live share. Check your internet connection."
                                        }
                                    }
                                },
                                onDone = {
                                    haptics.tick()
                                    onBack()
                                }
                            )
                        }
                    }

                    ShareStep.YOUR_LINKS -> {
                        YourLinksSection(
                            links = sharedLinks,
                            isLoading = isLoadingLinks,
                            errorMessage = linksErrorMessage,
                            busyLinkId = busyLinkId,
                            copiedLinkId = copiedLinkId,
                            onRetry = refreshSharedLinks,
                            onCopyLink = { item ->
                                haptics.confirm()
                                copyUrlToClipboard(context, item.url)
                                copiedLinkId = item.shareId
                            },
                            onViewLink = { item ->
                                haptics.tap()
                                openUrlInBrowser(context, item.url)
                            },
                            onDisableLive = { item ->
                                busyLinkId = item.shareId
                                linksErrorMessage = null
                                coroutineScope.launch {
                                    ShareRepository.stopLiveShareById(context, item.shareId, item.startDateMs)
                                        .onSuccess { updated ->
                                            sharedLinks = sharedLinks.map { existing ->
                                                if (existing.shareId == item.shareId) {
                                                    updated.copy(createdAt = existing.createdAt)
                                                } else {
                                                    existing
                                                }
                                            }
                                        }
                                        .onFailure {
                                            linksErrorMessage = "Could not disable live sharing. Check your connection."
                                        }
                                    busyLinkId = null
                                }
                            },
                            onRequestDelete = { item -> linkPendingDelete = item }
                        )
                    }

                    ShareStep.RECAP_CARD -> {
                        ShareRecapCardStudio(
                            allSessions = allSessions,
                            xpState = xpState,
                            dailyGoalMinutes = settingsState.dailyGoalMinutes
                        )
                    }
                }
            }
        }
    }

    val pendingDelete = linkPendingDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { linkPendingDelete = null },
            shape = RoundedCornerShape(30.dp),
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Delete Share Link?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Anyone opening this link will no longer be able to view these stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pendingDelete
                        linkPendingDelete = null
                        busyLinkId = target.shareId
                        linksErrorMessage = null
                        coroutineScope.launch {
                            ShareRepository.deleteSharedLink(context, target.shareId)
                                .onSuccess {
                                    sharedLinks = sharedLinks.filterNot { it.shareId == target.shareId }
                                }
                                .onFailure {
                                    linksErrorMessage = "Could not delete link. Check your connection."
                                }
                            busyLinkId = null
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { linkPendingDelete = null }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val todayUtcMillis = remember(todayStartMs) {
        ShareRepository.localStartOfDayToUtcPickerMillis(todayStartMs)
    }

    if (showLiveStartPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = ShareRepository.localStartOfDayToUtcPickerMillis(liveStartDateMs),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis
                override fun isSelectableYear(year: Int): Boolean = year <= currentYear
            }
        )
        DatePickerDialog(
            onDismissRequest = { showLiveStartPicker = false },
            colors = DatePickerDefaults.colors(containerColor = palette.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMs ->
                            val localStart = ShareRepository.utcDatePickerMillisToLocalStartOfDay(utcMs)
                            liveStartDateMs = localStart
                            liveUseCustomDate = localStart != todayStartMs
                        }
                        showLiveStartPicker = false
                    }
                ) {
                    Text("Select", color = palette.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveStartPicker = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(containerColor = palette.surface)
            )
        }
    }

    if (showRangeStartPicker) {
        val startPickerState = rememberDatePickerState(
            initialSelectedDateMillis = ShareRepository.localStartOfDayToUtcPickerMillis(rangeStartDateMs),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis
                override fun isSelectableYear(year: Int): Boolean = year <= currentYear
            }
        )
        DatePickerDialog(
            onDismissRequest = { showRangeStartPicker = false },
            colors = DatePickerDefaults.colors(containerColor = palette.surface),
            confirmButton = {
                TextButton(
                    enabled = startPickerState.selectedDateMillis != null,
                    onClick = {
                        startPickerState.selectedDateMillis?.let { utcMs ->
                            val localStart = ShareRepository.utcDatePickerMillisToLocalStartOfDay(utcMs)
                            rangeStartDateMs = localStart
                            if (rangeEndDateMs < localStart) {
                                rangeEndDateMs = localStart
                            }
                            selectedPreset = null
                        }
                        showRangeStartPicker = false
                    }
                ) {
                    Text("Select", color = palette.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangeStartPicker = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        ) {
            DatePicker(
                state = startPickerState,
                colors = DatePickerDefaults.colors(containerColor = palette.surface),
                title = {
                    Text(
                        text = "Select Start Date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
                    )
                }
            )
        }
    }

    if (showRangeEndPicker) {
        val minEndUtcMillis = ShareRepository.localStartOfDayToUtcPickerMillis(rangeStartDateMs)
        val initialEndLocal = maxOf(rangeEndDateMs, rangeStartDateMs)
        val endPickerState = rememberDatePickerState(
            initialSelectedDateMillis = ShareRepository.localStartOfDayToUtcPickerMillis(initialEndLocal),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in minEndUtcMillis..todayUtcMillis
                override fun isSelectableYear(year: Int): Boolean = year <= currentYear
            }
        )
        DatePickerDialog(
            onDismissRequest = { showRangeEndPicker = false },
            colors = DatePickerDefaults.colors(containerColor = palette.surface),
            confirmButton = {
                TextButton(
                    enabled = endPickerState.selectedDateMillis != null,
                    onClick = {
                        endPickerState.selectedDateMillis?.let { utcMs ->
                            val localEnd = ShareRepository.utcDatePickerMillisToLocalStartOfDay(utcMs)
                            rangeEndDateMs = maxOf(localEnd, rangeStartDateMs)
                            selectedPreset = null
                        }
                        showRangeEndPicker = false
                    }
                ) {
                    Text("Select", color = palette.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangeEndPicker = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        ) {
            DatePicker(
                state = endPickerState,
                colors = DatePickerDefaults.colors(containerColor = palette.surface),
                title = {
                    Text(
                        text = "Select End Date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun ShareCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalFocusTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.90f)

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .focusGlassPanel(shape = CircleShape, palette = palette),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun ActiveLiveShareCard(
    startDateMs: Long,
    url: String,
    isCopied: Boolean,
    isStopping: Boolean,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onStop: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val cardShape = remember { RoundedCornerShape(30.dp) }
    val pillShape = remember { RoundedCornerShape(50) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusGlassPanel(
                shape = cardShape,
                palette = palette,
                highlightAccent = palette.success.copy(alpha = 0.55f)
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .clip(pillShape)
                        .background(palette.success.copy(alpha = 0.14f), pillShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(palette.success)
                    )
                    Text(
                        text = "LIVE ACTIVE",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.success
                    )
                }
                if (startDateMs > 0L) {
                    Text(
                        text = "Since ${ShareRepository.formatSingleDate(startDateMs)}",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = palette.textSecondary
                    )
                }
            }

            Surface(
                shape = pillShape,
                color = palette.background.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = url,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCopy,
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) palette.success else palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("active_live_copy_button")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCopied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Button(
                    onClick = onOpen,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.surfaceElevated,
                        contentColor = palette.textPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("active_live_open_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open Link",
                        modifier = Modifier.size(17.dp)
                    )
                }

                Button(
                    onClick = onStop,
                    enabled = !isStopping,
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .testTag("active_live_stop_button")
                ) {
                    if (isStopping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.StopCircle,
                            contentDescription = "Stop Live Sharing",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareModePillCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val cardShape = remember { RoundedCornerShape(30.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.97f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusGlassPanel(
                shape = cardShape,
                palette = palette,
                isPressed = isPressed
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
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
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = badgeText,
                            fontFamily = JetBrainsMono,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.5.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceElevated.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveShareConfigSection(
    useCustomDate: Boolean,
    startDateMs: Long,
    hasExistingActiveLive: Boolean,
    onSelectToday: () -> Unit,
    onSelectCustomDate: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val todaySelected = !useCustomDate &&
        startDateMs == ShareRepository.startOfDayMs(System.currentTimeMillis())

    val segments = remember {
        listOf(
            ElasticPillSegment("Today", testTag = "live_start_today_button"),
            ElasticPillSegment("Custom Date", testTag = "live_start_custom_button")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElasticMorphingSegmentedPill(
            segments = segments,
            selectedIndex = if (todaySelected) 0 else 1,
            onSelectIndex = { idx ->
                if (idx == 0) onSelectToday() else onSelectCustomDate()
            },
            height = 42.dp,
            fontSize = 13.sp,
            fontFamily = palette.displayFont
        )

        AnimatedVisibility(
            visible = !todaySelected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            DatePillRowButton(
                label = "Start Date",
                value = ShareRepository.formatSingleDate(startDateMs),
                onClick = onSelectCustomDate
            )
        }

        if (hasExistingActiveLive) {
            Surface(
                shape = RoundedCornerShape(50),
                color = palette.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Replaces your currently active live link",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = palette.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun RangeShareConfigSection(
    selectedPreset: RangePreset?,
    startDateMs: Long,
    endDateMs: Long,
    onSelectPreset: (RangePreset) -> Unit,
    onSelectStartDate: () -> Unit,
    onSelectEndDate: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val segments = remember {
        listOf(
            ElasticPillSegment("Today", testTag = "range_preset_today", weight = 0.85f),
            ElasticPillSegment("Yesterday", testTag = "range_preset_yesterday", weight = 1.15f),
            ElasticPillSegment("7d", testTag = "range_preset_last_7_days", weight = 0.72f),
            ElasticPillSegment("30d", testTag = "range_preset_last_30_days", weight = 0.78f)
        )
    }
    val selectedIdx = selectedPreset?.let { RangePreset.entries.indexOf(it) } ?: -1

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElasticMorphingSegmentedPill(
            segments = segments,
            selectedIndex = selectedIdx,
            onSelectIndex = { idx ->
                RangePreset.entries.getOrNull(idx)?.let(onSelectPreset)
            },
            height = 42.dp,
            fontSize = 12.sp,
            fontFamily = palette.displayFont
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DatePillRowButton(
                label = "From",
                value = ShareRepository.formatSingleDate(startDateMs),
                onClick = onSelectStartDate,
                modifier = Modifier
                    .weight(1f)
                    .testTag("range_start_date_button")
            )
            DatePillRowButton(
                label = "To",
                value = ShareRepository.formatSingleDate(endDateMs),
                onClick = onSelectEndDate,
                modifier = Modifier
                    .weight(1f)
                    .testTag("range_end_date_button")
            )
        }
    }
}

@Composable
private fun DatePillRowButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.96f)
    val shape = remember { RoundedCornerShape(24.dp) }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusGlassPanel(shape = shape, palette = palette, isPressed = isPressed)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(palette.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = label,
                tint = palette.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.textSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatsPreviewCard(
    summary: SharedStatsSummary,
    isLoading: Boolean,
    subtitle: String,
    isLive: Boolean
) {
    val palette = LocalFocusTheme.current
    val cardShape = remember { RoundedCornerShape(30.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusGlassPanel(shape = cardShape, palette = palette)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isLive) palette.success.copy(alpha = 0.14f) else palette.primaryContainer
                ) {
                    Text(
                        text = if (isLive) "LIVE PREVIEW" else "SNAPSHOT",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive) palette.success else palette.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = subtitle,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = palette.primary
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    RollingMetricText(
                        text = ShareRepository.formatDurationReadable(summary.totalFocusMs),
                        style = TextStyle(
                            fontFamily = palette.numeralFont,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Total focus",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviewMiniStatPill(
                        label = "Sessions",
                        value = "${summary.sessionCount}",
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStatPill(
                        label = "Average",
                        value = ShareRepository.formatDurationCompact(summary.averageMs),
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStatPill(
                        label = "Longest",
                        value = ShareRepository.formatDurationCompact(summary.longestMs),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewMiniStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surfaceElevated.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            fontFamily = palette.numeralFont,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            color = palette.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun LinkReadySection(
    result: ShareLinkResult,
    isCopied: Boolean,
    isStopped: Boolean,
    isStopping: Boolean,
    errorMessage: String?,
    onCopyLink: () -> Unit,
    onOpenBrowser: () -> Unit,
    onSystemShare: () -> Unit,
    onStopLiveSharing: () -> Unit,
    onDone: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val isLiveActive = result.mode == ShareMode.LIVE && !isStopped
    val cardShape = remember { RoundedCornerShape(30.dp) }
    val pillShape = remember { RoundedCornerShape(50) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .focusGlassPanel(shape = cardShape, palette = palette)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = pillShape,
                        color = when {
                            isLiveActive -> palette.success.copy(alpha = 0.14f)
                            result.mode == ShareMode.LIVE -> palette.surfaceElevated
                            else -> palette.primaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isLiveActive) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(palette.success)
                                )
                            }
                            Text(
                                text = when {
                                    isLiveActive -> "LIVE · ACTIVE"
                                    result.mode == ShareMode.LIVE -> "LIVE · STOPPED"
                                    else -> "SNAPSHOT"
                                },
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isLiveActive -> palette.success
                                    result.mode == ShareMode.LIVE -> palette.textSecondary
                                    else -> palette.primary
                                }
                            )
                        }
                    }

                    Text(
                        text = ShareRepository.formatDateRangeLabel(result.startDateMs, result.endDateMs),
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = palette.textSecondary
                    )
                }

                Text(
                    text = "Your link is ready",
                    fontFamily = palette.displayFont,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PreviewMiniStatPill(
                        label = "Focus",
                        value = ShareRepository.formatDurationCompact(result.stats.totalFocusMs),
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStatPill(
                        label = "Sessions",
                        value = "${result.stats.sessionCount}",
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStatPill(
                        label = "Average",
                        value = ShareRepository.formatDurationCompact(result.stats.averageMs),
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStatPill(
                        label = "Longest",
                        value = ShareRepository.formatDurationCompact(result.stats.longestMs),
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = pillShape,
                    color = palette.background.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.url,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                Button(
                    onClick = onCopyLink,
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) palette.success else palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_share_link_button")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCopied) "Link Copied" else "Copy Link",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenBrowser,
                        shape = pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.surfaceElevated,
                            contentColor = palette.textPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("open_in_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onSystemShare,
                        shape = pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.surfaceElevated,
                            contentColor = palette.textPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("system_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (isLiveActive) {
            Button(
                onClick = onStopLiveSharing,
                enabled = !isStopping,
                shape = pillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("stop_live_sharing_button")
            ) {
                if (isStopping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.StopCircle,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Live Sharing", style = MaterialTheme.typography.labelMedium, fontSize = 14.sp)
                }
            }
        }

        if (errorMessage != null) {
            ErrorBanner(message = errorMessage)
        }

        TextButton(
            onClick = onDone,
            shape = pillShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("share_done_button")
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textSecondary
            )
        }
    }
}

private enum class LinksFilter(val label: String) {
    ALL("All"),
    LIVE("Live"),
    SNAPSHOT("Snapshot")
}

@Composable
private fun YourLinksSection(
    links: List<SharedLinkItem>,
    isLoading: Boolean,
    errorMessage: String?,
    busyLinkId: String?,
    copiedLinkId: String?,
    onRetry: () -> Unit,
    onCopyLink: (SharedLinkItem) -> Unit,
    onViewLink: (SharedLinkItem) -> Unit,
    onDisableLive: (SharedLinkItem) -> Unit,
    onRequestDelete: (SharedLinkItem) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(LinksFilter.ALL) }
    val filteredLinks = remember(links, selectedFilter) {
        when (selectedFilter) {
            LinksFilter.ALL -> links
            LinksFilter.LIVE -> links.filter { it.mode == ShareMode.LIVE }
            LinksFilter.SNAPSHOT -> links.filter { it.mode == ShareMode.RANGE }
        }
    }

    val palette = LocalFocusTheme.current
    val view = LocalView.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (errorMessage != null) {
            item(key = "links_error", contentType = "error_banner") {
                ErrorBanner(message = errorMessage)
            }
        }

        if (isLoading && links.isEmpty()) {
            item(key = "links_loading", contentType = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = palette.primary
                    )
                }
            }
        } else if (links.isEmpty()) {
            item(key = "links_empty", contentType = "empty_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGlassPanel(shape = RoundedCornerShape(30.dp), palette = palette)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(palette.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "No shared links yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Create a Live or Snapshot link to see it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        if (errorMessage != null) {
                            TextButton(onClick = onRetry) {
                                Text("Retry", color = palette.primary)
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "links_filters", contentType = "filters_row") {
                val allCount = links.size
                val liveCount = links.count { it.mode == ShareMode.LIVE }
                val snapCount = links.count { it.mode == ShareMode.RANGE }
                val filterSegments = remember(allCount, liveCount, snapCount) {
                    listOf(
                        ElasticPillSegment("All ($allCount)", testTag = "links_filter_all"),
                        ElasticPillSegment("Live ($liveCount)", testTag = "links_filter_live"),
                        ElasticPillSegment("Snapshot ($snapCount)", testTag = "links_filter_snapshot")
                    )
                }
                ElasticMorphingSegmentedPill(
                    segments = filterSegments,
                    selectedIndex = LinksFilter.entries.indexOf(selectedFilter).coerceAtLeast(0),
                    onSelectIndex = { idx ->
                        LinksFilter.entries.getOrNull(idx)?.let { selectedFilter = it }
                    },
                    height = 40.dp,
                    fontSize = 11.5.sp,
                    fontFamily = palette.displayFont
                )
            }

            if (filteredLinks.isEmpty()) {
                item(key = "links_category_empty", contentType = "empty_category") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusGlassPanel(shape = RoundedCornerShape(26.dp), palette = palette)
                    ) {
                        Text(
                            text = "No links in this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(
                    items = filteredLinks,
                    key = { it.shareId },
                    contentType = { "shared_link_card" }
                ) { item ->
                    val swipeState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.EndToStart -> {
                                    view.performFocusReject()
                                    onRequestDelete(item)
                                    false
                                }
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    view.performFocusConfirm()
                                    onCopyLink(item)
                                    false
                                }
                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = swipeState,
                        backgroundContent = {
                            val direction = swipeState.dismissDirection
                            val isCopy = direction == SwipeToDismissBoxValue.StartToEnd
                            val isDelete = direction == SwipeToDismissBoxValue.EndToStart
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(
                                        when {
                                            isCopy -> palette.success.copy(alpha = 0.16f)
                                            isDelete -> palette.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (isCopy) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                if (isCopy) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy Link",
                                            tint = palette.success
                                        )
                                        Text(
                                            text = "Copy",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.success
                                        )
                                    }
                                } else if (isDelete) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Delete",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete Link",
                                            tint = palette.primary
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        SharedLinkItemCard(
                            item = item,
                            isBusy = busyLinkId == item.shareId,
                            isCopied = copiedLinkId == item.shareId,
                            onCopy = { onCopyLink(item) },
                            onView = { onViewLink(item) },
                            onDisableLive = { onDisableLive(item) },
                            onDelete = { onRequestDelete(item) }
                        )
                    }
                }
            }
        }

        item(key = "links_bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SharedLinkItemCard(
    item: SharedLinkItem,
    isBusy: Boolean,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onView: () -> Unit,
    onDisableLive: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val isLiveActive = item.mode == ShareMode.LIVE && item.status == "active"
    val isLiveStopped = item.mode == ShareMode.LIVE && item.status == "stopped"
    val cardShape = remember { RoundedCornerShape(30.dp) }
    val pillShape = remember { RoundedCornerShape(50) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusGlassPanel(
                shape = cardShape,
                palette = palette,
                highlightAccent = if (isLiveActive) palette.success.copy(alpha = 0.5f) else null
            )
            .testTag("shared_link_card_${item.shareId}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = pillShape,
                    color = when {
                        isLiveActive -> palette.success.copy(alpha = 0.14f)
                        isLiveStopped -> palette.surfaceElevated
                        else -> palette.primaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLiveActive) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(palette.success)
                            )
                        }
                        Text(
                            text = when {
                                isLiveActive -> "LIVE · ACTIVE"
                                isLiveStopped -> "LIVE · STOPPED"
                                else -> "SNAPSHOT"
                            },
                            fontFamily = JetBrainsMono,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isLiveActive -> palette.success
                                isLiveStopped -> palette.textSecondary
                                else -> palette.primary
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .clickable(enabled = !isBusy, onClick = onDelete)
                        .testTag("delete_link_${item.shareId}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete Link",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Created ${ShareRepository.formatCreatedTimestamp(item.createdAt)}",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )
                if (isLiveStopped && item.endDateMs != null) {
                    Text(
                        text = "Stopped ${ShareRepository.formatCreatedTimestamp(item.endDateMs)}",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.5.sp,
                        color = palette.textSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PreviewMiniStatPill(
                    label = "Focus",
                    value = ShareRepository.formatDurationCompact(item.stats.totalFocusMs),
                    modifier = Modifier.weight(1f)
                )
                PreviewMiniStatPill(
                    label = "Sessions",
                    value = "${item.stats.sessionCount}",
                    modifier = Modifier.weight(1f)
                )
                PreviewMiniStatPill(
                    label = "Average",
                    value = ShareRepository.formatDurationCompact(item.stats.averageMs),
                    modifier = Modifier.weight(1f)
                )
                PreviewMiniStatPill(
                    label = "Longest",
                    value = ShareRepository.formatDurationCompact(item.stats.longestMs),
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                shape = pillShape,
                color = palette.background.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.url,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCopy,
                    enabled = !isBusy,
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) palette.success else palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("copy_link_${item.shareId}")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isCopied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Button(
                    onClick = onView,
                    enabled = !isBusy,
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.surfaceElevated,
                        contentColor = palette.textPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("view_link_${item.shareId}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("View", style = MaterialTheme.typography.labelMedium)
                }

                if (isLiveActive) {
                    Button(
                        onClick = onDisableLive,
                        enabled = !isBusy,
                        shape = pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryContainer,
                            contentColor = palette.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("disable_live_${item.shareId}")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = palette.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.StopCircle,
                                contentDescription = "Disable Live Sharing",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

private fun copyUrlToClipboard(context: Context, url: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("Focus Lock Stats Link", url))
}

private fun openUrlInBrowser(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun shareUrlViaChooser(context: Context, url: String) {
    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Focus Stats").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
