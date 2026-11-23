package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BackupPreviewSummary
import com.example.data.BackupValidationResult
import com.example.data.FocusBackupManager
import com.example.data.FocusStatsRepository
import com.example.ui.components.SanctuaryMonolithCard
import com.example.ui.components.ScenicPageVariant
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemeScenicBackdropOverlay
import com.example.ui.components.ThemeScenicVignetteFooter
import com.example.ui.components.ThemedDivider
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    onRestoreApplied: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    var refreshTick by remember { mutableIntStateOf(0) }
    val snapshot = remember(refreshTick) { FocusBackupManager.getCurrentSnapshot(context) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }
    var showPasteArea by remember { mutableStateOf(false) }
    var pastedJson by remember { mutableStateOf("") }
    var pendingRestorePreview by remember { mutableStateOf<BackupPreviewSummary?>(null) }

    fun handleIncomingBackupText(rawText: String) {
        when (val result = FocusBackupManager.validateBackupJson(rawText)) {
            is BackupValidationResult.Valid -> {
                statusMessage = null
                isErrorStatus = false
                pendingRestorePreview = result.summary
            }
            is BackupValidationResult.Invalid -> {
                haptics.reject()
                isErrorStatus = true
                statusMessage = result.reason
            }
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val writeResult = withContext(Dispatchers.IO) {
                runCatching {
                    val json = FocusBackupManager.exportToJson(context)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: error("Could not open selected file for writing.")
                }
            }
            writeResult.onSuccess {
                haptics.confirm()
                isErrorStatus = false
                statusMessage = "Backup file saved successfully."
            }.onFailure { err ->
                haptics.reject()
                isErrorStatus = true
                statusMessage = err.message ?: "Failed to save backup file."
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val readResult = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("Could not read selected file.")
                }
            }
            readResult.onSuccess { text ->
                handleIncomingBackupText(text)
            }.onFailure { err ->
                haptics.reject()
                isErrorStatus = true
                statusMessage = err.message ?: "Could not open backup file."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("backup_restore_screen")
    ) {
        ThemeBackdropLayer(palette = palette)
        ThemeScenicBackdropOverlay(
            variant = ScenicPageVariant.BACKUP,
            palette = palette
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        haptics.tap()
                        onBack()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("backup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BACKUP & RESTORE",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                        color = palette.primary
                    )
                    Text(
                        text = "On-Device Data Archive",
                        fontFamily = palette.displayFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status feedback banner
                    AnimatedVisibility(visible = statusMessage != null) {
                        val accent = if (isErrorStatus) palette.error else palette.success
                        val bannerShape = RoundedCornerShape(18.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(bannerShape)
                                .background(accent.copy(alpha = 0.14f), bannerShape)
                                .border(0.75.dp, accent.copy(alpha = 0.42f), bannerShape)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("backup_status_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isErrorStatus) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                color = palette.textPrimary
                            )
                        }
                    }

                    // 1. Current On-Device Archive Summary Card
                    SanctuaryMonolithCard(
                        palette = palette,
                        cornerRadius = 28.dp,
                        contentPaddingHorizontal = 18.dp,
                        contentPaddingVertical = 18.dp,
                        verticalSpacing = 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INCLUDED IN BACKUP",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp,
                                color = palette.textSecondary
                            )
                            Text(
                                text = "SCHEMA v${FocusBackupManager.SCHEMA_VERSION}",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = palette.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ArchiveMetricChip(
                                value = "${snapshot.sessionCount}",
                                label = "SESSIONS",
                                modifier = Modifier.weight(1f)
                            )
                            ArchiveMetricChip(
                                value = FocusStatsRepository.formatDurationShort(snapshot.totalFocusMs),
                                label = "FOCUSED",
                                modifier = Modifier.weight(1f)
                            )
                            ArchiveMetricChip(
                                value = "${snapshot.scheduleCount}",
                                label = "SCHEDULES",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ArchiveMetricChip(
                                value = "${snapshot.allowedAppsCount}",
                                label = "APPS",
                                modifier = Modifier.weight(1f)
                            )
                            ArchiveMetricChip(
                                value = "${snapshot.totalXp}",
                                label = "XP",
                                modifier = Modifier.weight(1f)
                            )
                            ArchiveMetricChip(
                                value = "${snapshot.quizHistoryCount}",
                                label = "QUIZ SEEN",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Exports all settings, focus stats, XP & streaks, schedules, widget configs, blocked notification logs, and no-repeat quiz history.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = palette.textSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 2. Export Backup Card
                    SanctuaryMonolithCard(
                        palette = palette,
                        cornerRadius = 28.dp,
                        contentPaddingHorizontal = 18.dp,
                        contentPaddingVertical = 18.dp,
                        verticalSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "EXPORT BACKUP",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.6.sp,
                            color = palette.textSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                haptics.tap()
                                exportFileLauncher.launch(FocusBackupManager.suggestedFileName())
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("export_save_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Backup (.json)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = withContext(Dispatchers.IO) {
                                        FocusBackupManager.exportToJson(context)
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("FocusLock Backup", json))
                                    haptics.confirm()
                                    isErrorStatus = false
                                    statusMessage = "Full backup JSON copied to clipboard."
                                }
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_copy_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Copy Backup JSON",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    // 3. Import & Restore Card
                    SanctuaryMonolithCard(
                        palette = palette,
                        cornerRadius = 28.dp,
                        contentPaddingHorizontal = 18.dp,
                        contentPaddingVertical = 18.dp,
                        verticalSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "IMPORT & RESTORE",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.6.sp,
                            color = palette.textSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                haptics.tap()
                                importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.surfaceElevated,
                                contentColor = palette.textPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("import_pick_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.UploadFile,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Choose Backup File (.json)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                showPasteArea = !showPasteArea
                                if (showPasteArea && pastedJson.isBlank()) {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                    if (!clip.isNullOrBlank() && clip.trim().startsWith("{")) {
                                        pastedJson = clip
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("import_toggle_paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showPasteArea) "Hide Paste JSON" else "Paste Backup JSON",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            )
                        }

                        AnimatedVisibility(visible = showPasteArea) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThemedDivider(palette = palette)
                                OutlinedTextField(
                                    value = pastedJson,
                                    onValueChange = { pastedJson = it },
                                    placeholder = {
                                        Text(
                                            text = "Paste exported FocusLock JSON here...",
                                            fontSize = 12.sp,
                                            color = palette.textSecondary.copy(alpha = 0.6f)
                                        )
                                    },
                                    minLines = 4,
                                    maxLines = 8,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = palette.primary,
                                        unfocusedBorderColor = palette.borderHighlight,
                                        focusedTextColor = palette.textPrimary,
                                        unfocusedTextColor = palette.textPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("import_paste_input")
                                )

                                Button(
                                    onClick = { handleIncomingBackupText(pastedJson) },
                                    enabled = pastedJson.isNotBlank(),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primary,
                                        contentColor = palette.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("import_validate_paste_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Validate & Restore JSON",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    ThemeScenicVignetteFooter(
                        caption = "PRIVATE VAULT",
                        subtitle = "All settings, streaks & sessions kept on your device",
                        palette = palette
                    )
                }
            }
        }
    }

    // Pre-import validation preview confirmation dialog
    val preview = pendingRestorePreview
    if (preview != null) {
        val exportedDateLabel = remember(preview.exportedAtMs) {
            if (preview.exportedAtMs > 0L) {
                SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US).format(Date(preview.exportedAtMs))
            } else {
                "Unknown date"
            }
        }
        AlertDialog(
            onDismissRequest = { pendingRestorePreview = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "Restore FocusLock Backup?",
                    fontFamily = palette.displayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will replace your current settings, stats, XP, schedules, and quiz memory with the backup below:",
                        fontSize = 13.sp,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "• Exported: $exportedDateLabel (v${preview.appVersion})\n" +
                            "• Sessions: ${preview.sessionCount} (${FocusStatsRepository.formatDurationShort(preview.totalFocusMs)})\n" +
                            "• Schedules: ${preview.scheduleCount}\n" +
                            "• Allowed Apps: ${preview.allowedAppsCount}\n" +
                            "• Total XP: ${preview.totalXp}\n" +
                            "• Quiz Memory: ${preview.quizHistoryCount} questions",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                        color = palette.textPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestorePreview = null
                        scope.launch {
                            val applyResult = withContext(Dispatchers.IO) {
                                FocusBackupManager.applyValidatedBackup(context, preview)
                            }
                            applyResult.onSuccess {
                                haptics.confirm()
                                isErrorStatus = false
                                statusMessage = "Backup restored! All settings, stats, and schedules updated."
                                pastedJson = ""
                                showPasteArea = false
                                refreshTick++
                                onRestoreApplied?.invoke()
                            }.onFailure { err ->
                                haptics.reject()
                                isErrorStatus = true
                                statusMessage = err.message ?: "Could not apply backup."
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier.testTag("confirm_restore_backup_button")
                ) {
                    Text("Restore Now", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRestorePreview = null },
                    modifier = Modifier.testTag("cancel_restore_backup_button")
                ) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ArchiveMetricChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.background.copy(alpha = 0.45f), shape)
            .border(0.75.dp, palette.border.copy(alpha = 0.55f), shape)
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
            fontSize = 8.5.sp,
            letterSpacing = 0.9.sp,
            color = palette.textSecondary,
            maxLines = 1
        )
    }
}
