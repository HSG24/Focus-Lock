package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Session
import com.example.data.FocusSettingsState
import com.example.data.RangePreset
import com.example.data.ShareLinkResult
import com.example.data.ShareMode
import com.example.data.ShareRepository
import com.example.data.SharedLinkItem
import com.example.data.SharedStatsSummary
import com.example.fetchAllSessions
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalView
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private enum class ShareStep {
    CHOOSE_MODE,
    CONFIGURE,
    LINK_READY,
    YOUR_LINKS
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
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(ShareStep.CHOOSE_MODE) }
    var selectedMode by remember { mutableStateOf(ShareMode.LIVE) }

    // Sessions loaded for preview and computation
    var allSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var isLoadingSessions by remember { mutableStateOf(true) }

    // Live Share configuration
    val todayStartMs = remember { ShareRepository.startOfDayMs(System.currentTimeMillis()) }
    var liveUseCustomDate by remember { mutableStateOf(false) }
    var liveStartDateMs by remember {
        mutableLongStateOf(initialDateMs?.let { ShareRepository.startOfDayMs(it) } ?: todayStartMs)
    }

    // Range Share configuration
    var selectedPreset by remember { mutableStateOf<RangePreset?>(RangePreset.TODAY) }
    var rangeStartDateMs by remember {
        mutableLongStateOf(initialDateMs?.let { ShareRepository.startOfDayMs(it) } ?: todayStartMs)
    }
    var rangeEndDateMs by remember {
        mutableLongStateOf(initialDateMs?.let { ShareRepository.startOfDayMs(it) } ?: todayStartMs)
    }

    // Pickers
    var showLiveStartPicker by remember { mutableStateOf(false) }
    var showRangeStartPicker by remember { mutableStateOf(false) }
    var showRangeEndPicker by remember { mutableStateOf(false) }

    // Submission & Link state
    var isCreatingLink by remember { mutableStateOf(false) }
    var isStoppingLive by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var createdResult by remember { mutableStateOf<ShareLinkResult?>(null) }
    var linkStoppedInResult by remember { mutableStateOf(false) }
    var copiedBanner by remember { mutableStateOf(false) }
    var copiedActiveBanner by remember { mutableStateOf(false) }

    // Your Links state
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

    val handleStepBack = {
        errorMessage = null
        when (currentStep) {
            ShareStep.CHOOSE_MODE -> onBack()
            ShareStep.CONFIGURE -> currentStep = ShareStep.CHOOSE_MODE
            ShareStep.LINK_READY -> onBack()
            ShareStep.YOUR_LINKS -> currentStep = ShareStep.CHOOSE_MODE
        }
    }

    BackHandler(onBack = handleStepBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            ShareStep.CHOOSE_MODE -> "Share Focus Stats"
                            ShareStep.CONFIGURE -> if (selectedMode == ShareMode.LIVE) "Live Share" else "Range Share"
                            ShareStep.LINK_READY -> "Share Link"
                            ShareStep.YOUR_LINKS -> "Your Links"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = handleStepBack,
                        modifier = Modifier.testTag("share_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (currentStep == ShareStep.YOUR_LINKS) {
                        IconButton(
                            onClick = refreshSharedLinks,
                            enabled = !isLoadingLinks,
                            modifier = Modifier.testTag("refresh_your_links_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh Links",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                errorMessage = null
                                currentStep = ShareStep.YOUR_LINKS
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("your_links_topbar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Your Links",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!settingsState.activeLiveShareId.isNullOrBlank()) {
                            val activeUrl = ShareRepository.buildShareUrl(settingsState.activeLiveShareId)
                            ActiveLiveShareCard(
                                startDateMs = settingsState.activeLiveShareStartDateMs,
                                url = activeUrl,
                                isCopied = copiedActiveBanner,
                                isStopping = isStoppingLive,
                                onCopy = {
                                    copyUrlToClipboard(context, activeUrl)
                                    copiedActiveBanner = true
                                },
                                onOpen = { openUrlInBrowser(context, activeUrl) },
                                onStop = {
                                    isStoppingLive = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val res = ShareRepository.stopLiveShare(context)
                                        isStoppingLive = false
                                        if (res.isFailure) {
                                            errorMessage = "Could not stop live share. Check your internet connection."
                                        }
                                    }
                                }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CHOOSE SHARE TYPE",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Pick whether your link updates live or stays frozen to past dates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ShareModeOptionCard(
                            title = "Live Share",
                            subtitle = "Updates automatically every time a new session starts and ends until you stop sharing.",
                            badgeText = "REAL-TIME",
                            badgeColor = FocusEmerald,
                            icon = Icons.Outlined.Sensors,
                            iconTint = FocusEmerald,
                            iconBg = FocusEmerald.copy(alpha = 0.12f),
                            testTag = "share_mode_live_card",
                            onClick = {
                                selectedMode = ShareMode.LIVE
                                liveUseCustomDate = false
                                liveStartDateMs = todayStartMs
                                errorMessage = null
                                currentStep = ShareStep.CONFIGURE
                            }
                        )

                        ShareModeOptionCard(
                            title = "Range Share",
                            subtitle = "Shares a fixed snapshot of previously recorded stats for your selected dates. Does not update.",
                            badgeText = "SNAPSHOT",
                            badgeColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.Outlined.DateRange,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBg = MaterialTheme.colorScheme.primaryContainer,
                            testTag = "share_mode_range_card",
                            onClick = {
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

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        ShareModeOptionCard(
                            title = "Your Links",
                            subtitle = "View all generated links, check creation times, disable live sharing, or delete links.",
                            badgeText = "MANAGE",
                            badgeColor = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Outlined.Link,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            iconBg = MaterialTheme.colorScheme.secondaryContainer,
                            testTag = "your_links_card_button",
                            onClick = {
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
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
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
                                onSelectStartDate = {
                                    showRangeStartPicker = true
                                },
                                onSelectEndDate = {
                                    showRangeEndPicker = true
                                }
                            )
                        }

                        // Preview of the 4 shared stats (Total Focus Time, Sessions, Average, Longest)
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
                                            endDateMs = rangeEndDateMs
                                        )
                                    }
                                    isCreatingLink = false
                                    result.onSuccess { linkResult ->
                                        createdResult = linkResult
                                        linkStoppedInResult = false
                                        copiedBanner = false
                                        currentStep = ShareStep.LINK_READY
                                    }.onFailure {
                                        errorMessage = "Could not create share link. Check your internet connection."
                                    }
                                }
                            },
                            enabled = !isCreatingLink && !isLoadingSessions,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("create_share_link_button")
                        ) {
                            if (isCreatingLink) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = if (selectedMode == ShareMode.LIVE) {
                                        "Create Live Link"
                                    } else {
                                        "Create Snapshot Link"
                                    },
                                    fontSize = 16.sp,
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
                                copyUrlToClipboard(context, result.url)
                                copiedBanner = true
                            },
                            onOpenBrowser = { openUrlInBrowser(context, result.url) },
                            onSystemShare = { shareUrlViaChooser(context, result.url) },
                            onStopLiveSharing = {
                                isStoppingLive = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val res = ShareRepository.stopLiveShare(context)
                                    isStoppingLive = false
                                    if (res.isSuccess) {
                                        linkStoppedInResult = true
                                    } else {
                                        errorMessage = "Could not stop live share. Check your internet connection."
                                    }
                                }
                            },
                            onDone = onBack
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
                            copyUrlToClipboard(context, item.url)
                            copiedLinkId = item.shareId
                        },
                        onViewLink = { item ->
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
                        onRequestDelete = { item ->
                            linkPendingDelete = item
                        }
                    )
                }
            }
        }
    }

    val pendingDelete = linkPendingDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { linkPendingDelete = null },
            containerColor = ObsidianSurface,
            titleContentColor = PureWhite,
            textContentColor = SlateGray,
            title = { Text("Delete Share Link?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This permanently removes the link so anyone opening it will no longer be able to view these stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateGray
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonPrimary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { linkPendingDelete = null }) {
                    Text("Cancel", color = SlateGray)
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
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayUtcMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year <= currentYear
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showLiveStartPicker = false },
            colors = DatePickerDefaults.colors(containerColor = ObsidianSurface),
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
                    Text("Select", color = CrimsonPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveStartPicker = false }) {
                    Text("Cancel", color = SlateGray)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(containerColor = ObsidianSurface)
            )
        }
    }

    if (showRangeStartPicker) {
        val startPickerState = rememberDatePickerState(
            initialSelectedDateMillis = ShareRepository.localStartOfDayToUtcPickerMillis(rangeStartDateMs),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayUtcMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year <= currentYear
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = {
                showRangeStartPicker = false
            },
            colors = DatePickerDefaults.colors(containerColor = ObsidianSurface),
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
                    Text("Select", color = CrimsonPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRangeStartPicker = false
                    }
                ) {
                    Text("Cancel", color = SlateGray)
                }
            }
        ) {
            DatePicker(
                state = startPickerState,
                colors = DatePickerDefaults.colors(containerColor = ObsidianSurface),
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
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in minEndUtcMillis..todayUtcMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year <= currentYear
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showRangeEndPicker = false },
            colors = DatePickerDefaults.colors(containerColor = ObsidianSurface),
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
                    Text("Select", color = CrimsonPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangeEndPicker = false }) {
                    Text("Cancel", color = SlateGray)
                }
            }
        ) {
            DatePicker(
                state = endPickerState,
                colors = DatePickerDefaults.colors(containerColor = ObsidianSurface),
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
private fun ActiveLiveShareCard(
    startDateMs: Long,
    url: String,
    isCopied: Boolean,
    isStopping: Boolean,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, FocusEmerald.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FocusEmerald)
                    )
                    Text(
                        text = "LIVE SHARE ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = FocusEmerald
                    )
                }
                if (startDateMs > 0L) {
                    Text(
                        text = "Since ${ShareRepository.formatSingleDate(startDateMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = url,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCopy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) FocusEmerald else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("active_live_copy_button")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCopied) "Copied" else "Copy Link",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("active_live_open_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open Link",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = onStop,
                    enabled = !isStopping,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("active_live_stop_button")
                ) {
                    if (isStopping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.StopCircle,
                            contentDescription = "Stop Live Sharing",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Stop",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareModeOptionCard(
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "START DATE",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Choose when your live stats start. New sessions will stream to this link until you stop sharing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val todaySelected = !useCustomDate &&
                startDateMs == ShareRepository.startOfDayMs(System.currentTimeMillis())

            DateOptionTile(
                title = "Today",
                subtitle = "From 12:00 AM today",
                selected = todaySelected,
                onClick = onSelectToday,
                testTag = "live_start_today_button",
                modifier = Modifier.weight(1f)
            )

            DateOptionTile(
                title = "Custom Date",
                subtitle = if (!todaySelected) {
                    ShareRepository.formatSingleDate(startDateMs)
                } else {
                    "Pick previous date"
                },
                selected = !todaySelected,
                onClick = onSelectCustomDate,
                testTag = "live_start_custom_button",
                modifier = Modifier.weight(1f)
            )
        }

        if (hasExistingActiveLive) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Creating a new Live Link will automatically close your current active Live Share.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DateOptionTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .heightIn(min = 68.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    val presetWeights = remember {
        mapOf(
            RangePreset.TODAY to 0.74f,
            RangePreset.YESTERDAY to 1.04f,
            RangePreset.LAST_7_DAYS to 1.07f,
            RangePreset.LAST_30_DAYS to 1.15f
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "SELECT DATES",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Range Share captures a static snapshot of recorded sessions and never updates later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RangePreset.entries.forEach { preset ->
                val isSelected = selectedPreset == preset
                Surface(
                    onClick = { onSelectPreset(preset) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(presetWeights[preset] ?: 1f)
                        .heightIn(min = 40.dp)
                        .testTag("range_preset_${preset.name.lowercase()}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DateOptionTile(
                title = "Start Date",
                subtitle = ShareRepository.formatSingleDate(startDateMs),
                selected = selectedPreset == null,
                onClick = onSelectStartDate,
                testTag = "range_start_date_button",
                modifier = Modifier.weight(1f)
            )
            DateOptionTile(
                title = "End Date",
                subtitle = ShareRepository.formatSingleDate(endDateMs),
                selected = selectedPreset == null,
                onClick = onSelectEndDate,
                testTag = "range_end_date_button",
                modifier = Modifier.weight(1f)
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLive) "PREVIEW · LIVE" else "PREVIEW · SNAPSHOT",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.1.sp,
                    color = if (isLive) FocusEmerald else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TOTAL FOCUS TIME",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ShareRepository.formatDurationReadable(summary.totalFocusMs),
                        fontFamily = JetBrainsMono,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PreviewMiniStat(
                        label = "Sessions",
                        value = "${summary.sessionCount}",
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStat(
                        label = "Average",
                        value = ShareRepository.formatDurationCompact(summary.averageMs),
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStat(
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
private fun PreviewMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = value,
            fontFamily = JetBrainsMono,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val isLiveActive = result.mode == ShareMode.LIVE && !isStopped

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
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
                        color = when {
                            isLiveActive -> FocusEmerald.copy(alpha = 0.14f)
                            result.mode == ShareMode.LIVE -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
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
                                        .background(FocusEmerald)
                                )
                            }
                            Text(
                                text = when {
                                    isLiveActive -> "LIVE · RECORDING"
                                    result.mode == ShareMode.LIVE -> "LIVE · STOPPED"
                                    else -> "SNAPSHOT"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isLiveActive -> FocusEmerald
                                    result.mode == ShareMode.LIVE -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }

                    Text(
                        text = ShareRepository.formatDateRangeLabel(result.startDateMs, result.endDateMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Your link is ready",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            isLiveActive -> "Anyone with this link can view your stats and watch live updates as sessions start and end."
                            result.mode == ShareMode.LIVE -> "Live recording has been stopped. The link now shows your final recorded stats."
                            else -> "Anyone with this link can view this fixed stats snapshot in any browser."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Compact 4-stat summary
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PreviewMiniStat(
                            label = "Focus",
                            value = ShareRepository.formatDurationCompact(result.stats.totalFocusMs),
                            modifier = Modifier.weight(1f)
                        )
                        PreviewMiniStat(
                            label = "Sessions",
                            value = "${result.stats.sessionCount}",
                            modifier = Modifier.weight(1f)
                        )
                        PreviewMiniStat(
                            label = "Average",
                            value = ShareRepository.formatDurationCompact(result.stats.averageMs),
                            modifier = Modifier.weight(1f)
                        )
                        PreviewMiniStat(
                            label = "Longest",
                            value = ShareRepository.formatDurationCompact(result.stats.longestMs),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // URL display box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.url,
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                // Primary Copy Link button
                Button(
                    onClick = onCopyLink,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) FocusEmerald else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("copy_share_link_button")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCopied) "Link Copied" else "Copy Link",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Secondary Open & System Share actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenBrowser,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
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

                    OutlinedButton(
                        onClick = onSystemShare,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
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
            OutlinedButton(
                onClick = onStopLiveSharing,
                enabled = !isStopping,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stop Live Sharing",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (errorMessage != null) {
            ErrorBanner(message = errorMessage)
        }

        TextButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("share_done_button")
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (errorMessage != null) {
            ErrorBanner(message = errorMessage)
        }

        if (isLoading && links.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (links.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "No shared links yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Create a Live Share or Range Share link to manage and view it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (errorMessage != null) {
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinksFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        LinksFilter.ALL -> links.size
                        LinksFilter.LIVE -> links.count { it.mode == ShareMode.LIVE }
                        LinksFilter.SNAPSHOT -> links.count { it.mode == ShareMode.RANGE }
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = "${filter.label} ($count)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("links_filter_${filter.name.lowercase()}")
                    )
                }
            }

            if (filteredLinks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No links in this category.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                val view = LocalView.current
                filteredLinks.forEach { item ->
                    key(item.shareId) {
                        val swipeState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        view.performKeyTapHaptic()
                                        onRequestDelete(item)
                                        false // Keep card until confirmation dialog deletes it
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        view.performKeyTapHaptic()
                                        onCopyLink(item)
                                        false // Snap back after copying
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
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            when {
                                                isCopy -> FocusEmerald.copy(alpha = 0.18f)
                                                isDelete -> CrimsonContainer
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
                                                tint = FocusEmerald
                                            )
                                            Text(
                                                text = "Copy Link",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = FocusEmerald
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
                                                color = CrimsonPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Delete Link",
                                                tint = CrimsonPrimary
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
        }

        Spacer(modifier = Modifier.height(12.dp))
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
    val isLiveActive = item.mode == ShareMode.LIVE && item.status == "active"
    val isLiveStopped = item.mode == ShareMode.LIVE && item.status == "stopped"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isLiveActive) FocusEmerald.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shared_link_card_${item.shareId}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when {
                        isLiveActive -> FocusEmerald.copy(alpha = 0.14f)
                        isLiveStopped -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.primaryContainer
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
                                    .background(FocusEmerald)
                            )
                        }
                        Text(
                            text = when {
                                isLiveActive -> "LIVE · ACTIVE"
                                isLiveStopped -> "LIVE · STOPPED"
                                else -> "SNAPSHOT"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isLiveActive -> FocusEmerald
                                isLiveStopped -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("delete_link_${item.shareId}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete Link",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Created ${ShareRepository.formatCreatedTimestamp(item.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLiveStopped && item.endDateMs != null) {
                    Text(
                        text = "Live stopped ${ShareRepository.formatCreatedTimestamp(item.endDateMs)} · Viewable as frozen stats",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PreviewMiniStat(
                        label = "Focus",
                        value = ShareRepository.formatDurationCompact(item.stats.totalFocusMs),
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStat(
                        label = "Sessions",
                        value = "${item.stats.sessionCount}",
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStat(
                        label = "Average",
                        value = ShareRepository.formatDurationCompact(item.stats.averageMs),
                        modifier = Modifier.weight(1f)
                    )
                    PreviewMiniStat(
                        label = "Longest",
                        value = ShareRepository.formatDurationCompact(item.stats.longestMs),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.url,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) FocusEmerald else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("copy_link_${item.shareId}")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCopied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = onView,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("view_link_${item.shareId}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (isLiveActive) {
                    OutlinedButton(
                        onClick = onDisableLive,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("disable_live_${item.shareId}")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.StopCircle,
                                contentDescription = "Disable Live Sharing",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Disable Live",
                                style = MaterialTheme.typography.labelMedium
                            )
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
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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
