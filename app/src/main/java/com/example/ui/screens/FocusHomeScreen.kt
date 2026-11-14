package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LockService
import com.example.MainActivity
import com.example.data.FocusSettingsRepository
import com.example.data.FocusXpRepository
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemedTimerRing
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import com.example.uploadPinToFirebase
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHomeScreen(
    onOpenShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val palette = LocalFocusTheme.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val settingsState by FocusSettingsRepository.observe(context).collectAsStateWithLifecycle()
    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var hasOverlayPermission by remember { mutableStateOf(true) }
    var hasUsagePermission by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        hasOverlayPermission = FocusSettingsRepository.canDrawOverlaysAsync(context)
        hasUsagePermission = FocusSettingsRepository.hasUsageStatsPermissionAsync(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    hasOverlayPermission = FocusSettingsRepository.canDrawOverlaysAsync(context)
                    hasUsagePermission = FocusSettingsRepository.hasUsageStatsPermissionAsync(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsReady = hasOverlayPermission && hasUsagePermission
    val goalMs = settingsState.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
    val goalProgress = (xpState.todayFocusMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
    val goalMet = xpState.todayFocusMs >= goalMs

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        ThemeBackdropLayer(palette = palette)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 68.dp, bottom = 104.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hero Emblem framed by the active Theme's Timer / Goal Progress Ring
                ThemedTimerRing(
                    progress = if (goalProgress > 0f) goalProgress else 0.12f,
                    size = 180.dp,
                    palette = palette
                ) {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(CircleShape)
                            .background(palette.surface)
                            .border(1.dp, palette.primaryBorderTint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        FocusLockEmblem(
                            size = 86.dp,
                            showAmbientGlow = true
                        )
                    }
                }

                // Editorial Headline & Subtitle styled with Theme Typography
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Deep Focus",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = palette.displayFont,
                        fontSize = 32.sp,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Silence distractions and keep only essential tools within reach.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // Compact Rank, Focus XP & Daily Goal Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.testTag("home_rank_xp_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = palette.primary
                        )
                        Text(
                            text = "${xpState.rank.title.uppercase(Locale.US)} · ${xpState.totalXp} XP",
                            fontFamily = palette.numeralFont,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (goalMet) "· SAFE" else "· ${(goalProgress * 100).toInt()}% GOAL",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (goalMet) palette.success else palette.primary
                        )
                    }
                }

                // Primary Spring-Animated Theme CTA
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(38.dp),
                        strokeWidth = 3.dp,
                        color = palette.primary
                    )
                } else {
                    val ctaInteraction = remember { MutableInteractionSource() }
                    val ctaScale = rememberSpringPressScale(ctaInteraction, pressedScale = 0.96f)

                    Button(
                        onClick = {
                            view.performKeyTapHaptic()
                            val mainActivity = context as? MainActivity
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else if (mainActivity?.hasUsageStatsPermission(context) == false) {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } else {
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
                                        context.startService(serviceIntent)
                                        NothingFocusWidgetUpdater.scheduleUpdateAll(context)
                                        (context as? ComponentActivity)?.moveTaskToBack(true)
                                    } else {
                                        errorMessage = "Could not sync unlock PIN. Check your internet connection."
                                    }
                                }
                            }
                        },
                        interactionSource = ctaInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .graphicsLayer {
                                scaleX = ctaScale
                                scaleY = ctaScale
                            }
                            .testTag("start_focus_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = palette.textPrimary.copy(alpha = 0.14f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = if (permissionsReady) "Start Focus Mode" else "Grant Permission & Start",
                            fontFamily = palette.displayFont,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Permission Readiness Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (permissionsReady) Icons.Outlined.VerifiedUser else Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (permissionsReady) palette.success else palette.primary
                        )
                        Text(
                            text = if (permissionsReady) {
                                "Overlay & usage protection ready"
                            } else {
                                "Overlay or usage access permission needed"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = palette.primary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Top Bar with Matching Geometric Brand Emblem + Share Action
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusLockEmblem(
                        size = 30.dp,
                        showAmbientGlow = false
                    )
                    Text(
                        text = "Focus Lock",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = palette.displayFont,
                        color = palette.textPrimary
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        view.performKeyTapHaptic()
                        onOpenShare()
                    },
                    modifier = Modifier.testTag("home_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Stats",
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
