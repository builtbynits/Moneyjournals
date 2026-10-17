package com.moneyjournal.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyjournal.app.BuildConfig
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.DateFormats
import com.moneyjournal.app.data.PayMode
import com.moneyjournal.app.ui.components.Panel
import com.moneyjournal.app.ui.components.SectionHeader
import com.moneyjournal.app.ui.components.SheetGrabber
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.Danger
import com.moneyjournal.app.ui.theme.OutlineFaint
import com.moneyjournal.app.ui.theme.Surface1
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextSecondary
import com.moneyjournal.app.ui.theme.TextTertiary
import java.time.Instant
import java.time.ZoneId

@Composable
fun SettingsScreen(
    vm: JournalViewModel,
    data: AppData,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = data.settings
    var showNameSheet by remember { mutableStateOf(false) }
    var showPinSheet by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var pendingJson by remember { mutableStateOf<String?>(null) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }

    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val payload = pendingJson
        if (uri != null && payload != null) {
            writeToUri(context, uri, payload)
            vm.updateSettings { it.copy(lastBackupAt = System.currentTimeMillis()) }
            vm.snackbar.value = "Backup saved"
        }
        pendingJson = null
    }

    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val payload = pendingCsv
        if (uri != null && payload != null) {
            writeToUri(context, uri, payload)
            vm.snackbar.value = "CSV exported"
        }
        pendingCsv = null
    }

    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text.isNullOrBlank()) {
                vm.snackbar.value = "Couldn't read that file"
            } else {
                vm.importJson(text) { }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("head") {
            Column {
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    letterSpacing = 1.6.sp,
                )
                Text(
                    text = "Your journal, your rules",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        item("profile") {
            Panel(padding = 18.dp, onClick = { showNameSheet = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.16f))
                            .border(1.dp, Accent.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = settings.name.trim().take(1).uppercase().ifBlank { "\u20B9" },
                            style = MaterialTheme.typography.titleLarge,
                            color = Accent,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.name.ifBlank { "Add your name" },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${data.transactions.size} entries \u00B7 ${data.accounts.size} accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                    }
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        item("privacy") {
            Panel {
                SectionHeader(title = "Privacy", subtitle = "Nothing leaves this phone")
                Spacer(Modifier.height(6.dp))
                SettingToggle(
                    title = "Hide amounts",
                    subtitle = "Blur every figure while you're in public",
                    checked = settings.hideAmounts,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(hideAmounts = value) }
                    },
                )
                SettingToggle(
                    title = "App lock",
                    subtitle = if (settings.appLock) "4-digit PIN required to open" else "Protect the journal with a PIN",
                    checked = settings.appLock,
                    onCheckedChange = { value ->
                        if (value) showPinSheet = true else vm.disableAppLock()
                    },
                )
                if (settings.appLock) {
                    SettingAction(
                        icon = Icons.Rounded.Lock,
                        title = "Lock now",
                        subtitle = "Require the PIN immediately",
                        onClick = { vm.lockNow() },
                    )
                }
                SettingToggle(
                    title = "Haptic feedback",
                    subtitle = "Subtle vibration on taps and saves",
                    checked = settings.haptics,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(haptics = value) }
                    },
                )
                Spacer(Modifier.height(10.dp))
                PrivacyNote()
            }
        }

        item("defaults") {
            Panel {
                SectionHeader(title = "Defaults", subtitle = "What new entries start with")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "DEFAULT ACCOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(8.dp))
                ChipFlow(
                    options = data.accounts.map { it.id to it.name },
                    selectedId = settings.defaultAccountId,
                    onSelect = { id -> vm.updateSettings { it.copy(defaultAccountId = id) } },
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "DEFAULT MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(8.dp))
                ChipFlow(
                    options = PayMode.entries.map { it.name to it.label },
                    selectedId = settings.defaultMode.name,
                    onSelect = { id ->
                        vm.updateSettings { it.copy(defaultMode = PayMode.fromName(id)) }
                    },
                )
            }
        }

        item("backup") {
            Panel {
                SectionHeader(
                    title = "Backup & restore",
                    subtitle = "Keep a copy in Files, Drive or anywhere you like",
                )
                Spacer(Modifier.height(8.dp))
                SettingAction(
                    icon = Icons.Rounded.Download,
                    title = "Export backup (.json)",
                    subtitle = "Complete journal \u2014 accounts, categories, entries",
                    onClick = { vm.exportJson { payload -> pendingJson = payload; createJson.launch("money-journal-backup.json") } },
                )
                SettingAction(
                    icon = Icons.Rounded.Backup,
                    title = "Export spreadsheet (.csv)",
                    subtitle = "Open in Sheets or Excel",
                    onClick = { vm.exportCsv { payload -> pendingCsv = payload; createCsv.launch("money-journal.csv") } },
                )
                SettingAction(
                    icon = Icons.Rounded.Upload,
                    title = "Restore from backup",
                    subtitle = "Replaces your current journal with the file",
                    onClick = { openBackup.launch(arrayOf("application/json", "text/plain", "*/*")) },
                )
                if (settings.lastBackupAt > 0L) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Last backup: ${formatStamp(settings.lastBackupAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
            }
        }

        item("data") {
            Panel {
                SectionHeader(title = "Journal data", subtitle = "Start fresh whenever you want")
                Spacer(Modifier.height(8.dp))
                SettingAction(
                    icon = Icons.Rounded.RestartAlt,
                    title = "Reload sample journal",
                    subtitle = "Brings back the demo entries for this month",
                    onClick = { confirmReset = true },
                )
                SettingAction(
                    icon = Icons.Rounded.DeleteForever,
                    title = "Clear all entries",
                    subtitle = "Keeps accounts and categories, removes every entry",
                    danger = true,
                    onClick = { confirmClear = true },
                )
            }
        }

        item("install") {
            Panel {
                SectionHeader(title = "About this install", subtitle = "Native Android app \u00B7 no browser involved")
                Spacer(Modifier.height(12.dp))
                InfoRow("App", "Money Journal")
                InfoRow("Version", "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
                InfoRow("Package", BuildConfig.APPLICATION_ID)
                InfoRow("Storage", "On-device JSON journal file")
                InfoRow("Permissions", "None \u2014 no internet access requested")
                InfoRow("Entries", "${data.transactions.size}")
                InfoRow("First installed", formatStamp(settings.installStamp))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Money Journal runs completely offline. Updates are installed by opening a newer APK from Files \u2014 your data stays exactly as it is.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Made for Indian money habits \u00B7 \u20B9",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }
        }

        item("footnote") {
            Text(
                text = "Money Journal v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }

    if (showNameSheet) {
        NameSheet(
            initial = settings.name,
            onDismiss = { showNameSheet = false },
            onSave = { name ->
                vm.updateSettings { it.copy(name = name) }
                showNameSheet = false
            },
        )
    }

    if (showPinSheet) {
        PinSetupSheet(
            onDismiss = { showPinSheet = false },
            onSave = { pin ->
                vm.setAppLock(pin)
                showPinSheet = false
            },
        )
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reload sample journal?",
            body = "Your current entries are replaced with the demo journal for this month.",
            confirm = "Reload",
            onConfirm = {
                vm.resetToSampleData()
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear all entries?",
            body = "Every transaction is deleted. Accounts and categories stay. Export a backup first if you're unsure.",
            confirm = "Clear entries",
            onConfirm = {
                vm.eraseEverything()
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

private fun writeToUri(context: Context, uri: Uri, text: String) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray())
        }
    }
}

private fun formatStamp(millis: Long): String {
    if (millis <= 0L) return "Not yet"
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return DateFormats.fullDate(date)
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 11.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF04120C),
                checkedTrackColor = Accent,
                checkedBorderColor = Accent,
                uncheckedThumbColor = Color(0xFFB9C6D4),
                uncheckedTrackColor = Surface2,
                uncheckedBorderColor = OutlineFaint,
            ),
        )
    }
}

@Composable
private fun SettingAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (danger) Danger.copy(alpha = 0.12f) else Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) Danger else Accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (danger) Danger else TextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Accent.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "The app ships without the INTERNET permission, so your journal physically cannot be uploaded. Backups are written only where you choose.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            val selected = id == selectedId
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color(0xFF04120C) else TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Accent else Surface2)
                    .border(1.dp, if (selected) Accent else OutlineFaint, RoundedCornerShape(50))
                    .clickable { onSelect(id) }
                    .padding(horizontal = 13.dp, vertical = 9.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameSheet(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            SheetGrabber()
            Text(
                text = "Your name",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = "Used for the greeting on the Journal screen",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface2)
                    .border(1.dp, OutlineFaint, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (name.isEmpty()) {
                                Text("e.g. Aarav", style = MaterialTheme.typography.bodyLarge, color = TextTertiary)
                            }
                            inner()
                        }
                    },
                )
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Accent)
                    .clickable { onSave(name.trim()) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF04120C),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupSheet(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    fun onDigit(digit: Char) {
        error = null
        if (stage == 0) {
            first += digit
            if (first.length == 4) stage = 1
        } else {
            second += digit
            if (second.length == 4) {
                if (second == first) onSave(second) else {
                    error = "PINs didn't match \u2014 start again"
                    first = ""
                    second = ""
                    stage = 0
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetGrabber()
            Text(
                text = if (stage == 0) "Choose a 4-digit PIN" else "Confirm your PIN",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = error ?: "Stored as a salted hash on this device only",
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) Danger else TextTertiary,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val current = if (stage == 0) first else second
                (0 until 4).forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (index < current.length) Accent else Surface2)
                            .border(1.dp, OutlineFaint, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "\u232B"),
            ).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (key.isEmpty()) Color.Transparent else Surface2)
                                .clickable(enabled = key.isNotEmpty()) {
                                    when (key) {
                                        "\u232B" -> {
                                            if (stage == 0) first = first.dropLast(1)
                                            else second = second.dropLast(1)
                                        }
                                        else -> onDigit(key[0])
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (key.isNotEmpty()) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(confirm, color = Danger)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

