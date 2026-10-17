package com.moneyjournal.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyjournal.app.data.Account
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.DateFormats
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.data.PayMode
import com.moneyjournal.app.data.TxType
import com.moneyjournal.app.ui.components.AnimatedSegmented
import com.moneyjournal.app.ui.components.DatePill
import com.moneyjournal.app.ui.components.SheetGrabber
import com.moneyjournal.app.ui.components.sliceColor
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.AccentSoft
import com.moneyjournal.app.ui.theme.Danger
import com.moneyjournal.app.ui.theme.OutlineFaint
import com.moneyjournal.app.ui.theme.Surface1
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextSecondary
import com.moneyjournal.app.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    vm: JournalViewModel,
    data: AppData,
) {
    val state by vm.editor.collectAsState()
    val editor = state ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var clipboardMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val tabs = listOf("Expense", "Income", "Transfer")
    val selectedTab = when (editor.type) {
        TxType.EXPENSE -> 0
        TxType.INCOME -> 1
        TxType.TRANSFER -> 2
    }
    val categories = remember(data.categories, editor.type) { data.categoriesOf(editor.type) }

    ModalBottomSheet(
        onDismissRequest = { vm.closeEditor() },
        sheetState = sheetState,
        containerColor = Surface1,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            SheetGrabber()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editor.id == null) "New entry" else "Edit entry",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = when (editor.type) {
                            TxType.EXPENSE -> "Money going out"
                            TxType.INCOME -> "Money coming in"
                            TxType.TRANSFER -> "Between your accounts"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
                PasteButton(onClick = {
                    val text = clipboard.getText()?.text
                    val parsed = text != null && vm.applyPastedText(text)
                    clipboardMessage = if (parsed) null else "Couldn't find a transaction in the clipboard"
                })
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .clickable { vm.closeEditor() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            AnimatedSegmented(
                options = tabs,
                selectedIndex = selectedTab,
                onSelect = { index ->
                    vm.setType(
                        when (index) {
                            0 -> TxType.EXPENSE
                            1 -> TxType.INCOME
                            else -> TxType.TRANSFER
                        }
                    )
                },
            )

            AnimatedVisibility(
                visible = editor.parseNote != null || clipboardMessage != null,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 2 },
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    val text = editor.parseNote ?: clipboardMessage.orEmpty()
                    val isError = editor.parseNote == null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isError) Danger.copy(alpha = 0.12f) else Accent.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isError) "\u26A0\uFE0F" else "\u2728",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) Danger else AccentSoft,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            FieldLabel("Amount")
            Spacer(Modifier.height(8.dp))
            AmountField(
                value = editor.amountInput,
                onValueChange = { vm.setAmountInput(it) },
                onClear = { vm.clearAmount() },
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(100, 500, 1000, 2000)) { rupees ->
                    QuickAmountChip(
                        label = "+ \u20B9$rupees",
                        onClick = {
                            val current = editor.amountPaise + rupees.toLong() * 100
                            vm.setAmountInput(Money.paiseToInput(current))
                        },
                    )
                }
                item {
                    QuickAmountChip(
                        label = "Round off",
                        onClick = {
                            val rupees = editor.amountPaise / 100
                            val rounded = ((rupees + 50) / 100) * 100
                            vm.setAmountInput(rounded.toString())
                        },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            FieldLabel(
                when (editor.type) {
                    TxType.INCOME -> "Source"
                    TxType.TRANSFER -> "Description"
                    TxType.EXPENSE -> "Merchant"
                }
            )
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = editor.merchant,
                onValueChange = { vm.setMerchant(it) },
                placeholder = when (editor.type) {
                    TxType.INCOME -> "Where it came from"
                    TxType.TRANSFER -> "e.g. Sweep to savings"
                    TxType.EXPENSE -> "Who you paid"
                },
            )
            val suggestions = remember(data.transactions) { vm.suggestions() }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { suggestion ->
                        QuickAmountChip(
                            label = suggestion,
                            onClick = { vm.setMerchant(suggestion) },
                        )
                    }
                }
            }

            if (editor.type != TxType.TRANSFER) {
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldLabel("Category")
                    Spacer(Modifier.weight(1f))
                    if (editor.categoryId != null) {
                        val selected = categories.firstOrNull { it.id == editor.categoryId }
                        Text(
                            text = selected?.name.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Accent,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                CategoryGrid(
                    categories = categories,
                    selectedId = editor.categoryId,
                    onSelect = { vm.setCategory(it) },
                )
            }

            Spacer(Modifier.height(18.dp))
            FieldLabel(if (editor.type == TxType.TRANSFER) "From account" else "Account")
            Spacer(Modifier.height(8.dp))
            AccountChips(
                accounts = data.accounts,
                selectedId = editor.accountId,
                onSelect = { vm.setAccount(it) },
            )

            if (editor.type == TxType.TRANSFER) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    FieldLabel("To account")
                }
                Spacer(Modifier.height(8.dp))
                AccountChips(
                    accounts = data.accounts.filter { it.id != editor.accountId },
                    selectedId = editor.toAccountId,
                    onSelect = { vm.setToAccount(it) },
                )
            }

            Spacer(Modifier.height(18.dp))
            FieldLabel("Payment mode")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PayMode.entries.toList()) { mode ->
                    SelectableChip(
                        label = mode.label,
                        selected = editor.mode == mode,
                        onClick = { vm.setMode(mode) },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel("Date")
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAmountChip(
                        label = "Today",
                        onClick = { vm.setDate(LocalDate.now()) },
                    )
                    QuickAmountChip(
                        label = "Yesterday",
                        onClick = { vm.setDate(LocalDate.now().minusDays(1)) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DatePill(date = editor.date, onClick = { showDatePicker = true })
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = DateFormats.dayHeader(editor.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.height(18.dp))
            FieldLabel("Note")
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = editor.note,
                onValueChange = { vm.setNote(it) },
                placeholder = "Optional detail (split with, project, bill no.)",
                singleLine = false,
            )

            Spacer(Modifier.height(22.dp))
            SaveButton(
                enabled = editor.amountPaise > 0L,
                label = if (editor.id == null) "Save entry" else "Update entry",
                amountPaise = editor.amountPaise,
                onClick = { vm.saveEditor() },
            )
            if (editor.id != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { vm.deleteTransaction(editor.id!!) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Delete entry",
                        style = MaterialTheme.typography.labelLarge,
                        color = Danger,
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = editor.date
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        vm.setDate(
                            java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("Apply", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun PasteButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Accent.copy(alpha = 0.14f))
            .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.ContentPaste,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Paste", style = MaterialTheme.typography.labelMedium, color = Accent)
    }
}

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(1.dp, OutlineFaint, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\u20B9",
            style = MaterialTheme.typography.displayMedium,
            color = Accent,
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextTertiary.copy(alpha = 0.55f),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.displayMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear amount",
                    tint = TextTertiary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface2)
            .border(1.dp, OutlineFaint, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = if (singleLine) androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Default,
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextTertiary,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun QuickAmountChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Surface2)
            .border(1.dp, OutlineFaint, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) Accent else Surface2,
        animationSpec = tween(200),
        label = "selBg",
    )
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color(0xFF04120C) else TextSecondary,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (selected) Accent else OutlineFaint, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun AccountChips(
    accounts: List<Account>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accounts, key = { it.id }) { account ->
            val selected = account.id == selectedId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Accent.copy(alpha = 0.16f) else Surface2)
                    .border(
                        1.dp,
                        if (selected) Accent else OutlineFaint,
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(account.id) }
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (selected) Accent else TextTertiary)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = account.kind.label + if (account.last4.isNotBlank()) " \u2022\u2022${account.last4}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<com.moneyjournal.app.data.Category>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(258.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        gridItems(categories, key = { it.id }) { category ->
            val selected = category.id == selectedId
            val color = sliceColor(category.colorIndex)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) color.copy(alpha = 0.20f) else Surface2)
                    .border(
                        1.dp,
                        if (selected) color.copy(alpha = 0.75f) else OutlineFaint,
                        RoundedCornerShape(18.dp),
                    )
                    .clickable { onSelect(category.id) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = category.emoji, fontSize = 20.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = if (selected) TextPrimary else TextSecondary,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
                    modifier = Modifier
                        .height(26.dp)
                        .padding(horizontal = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    label: String,
    amountPaise: Long,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(AccentSoft, Accent, com.moneyjournal.app.ui.theme.AccentDeep))
                else Brush.horizontalGradient(listOf(Surface2, Surface2))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color(0xFF04120C) else TextTertiary,
                fontWeight = FontWeight.Bold,
            )
            if (enabled) {
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF04120C).copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = Money.formatPaise(amountPaise),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF04120C),
                    )
                }
            }
        }
    }
}
