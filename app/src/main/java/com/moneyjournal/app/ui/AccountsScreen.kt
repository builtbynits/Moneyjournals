package com.moneyjournal.app.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyjournal.app.data.Account
import com.moneyjournal.app.data.AccountKind
import com.moneyjournal.app.data.Analytics
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.DateFormats
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.data.TxType
import com.moneyjournal.app.ui.components.AnimatedSegmented
import com.moneyjournal.app.ui.components.EmptyState
import com.moneyjournal.app.ui.components.SheetGrabber
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.AccentDeep
import com.moneyjournal.app.ui.theme.AccentSoft
import com.moneyjournal.app.ui.theme.AccountGradients
import com.moneyjournal.app.ui.theme.Danger
import com.moneyjournal.app.ui.theme.OutlineFaint
import com.moneyjournal.app.ui.theme.Surface1
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextSecondary
import com.moneyjournal.app.ui.theme.TextTertiary
import java.time.YearMonth

@Composable
fun AccountsScreen(
    vm: JournalViewModel,
    data: AppData,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Account?>(null) }
    var creating by remember { mutableStateOf(false) }
    val hide = data.settings.hideAmounts
    val balances = remember(data) { Analytics.balances(data) }
    val netWorth = remember(data) { Analytics.netWorth(data) }
    val liability = remember(data) { Analytics.liability(data) }
    val monthTxns = remember(data.transactions) {
        Analytics.inMonth(data.transactions, YearMonth.now())
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("head") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACCOUNTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        text = "Where your money lives",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Accent)
                        .clickable { creating = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add account",
                        tint = Color(0xFF04120C),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        item("net") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF14352A), Color(0xFF10222A), Color(0xFF111A26))
                        )
                    )
                    .border(1.dp, Accent.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
                    .padding(20.dp),
            ) {
                Column {
                    Text(
                        text = "TOTAL BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentSoft,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (hide) "\u20B9 \u2022\u2022\u2022\u2022\u2022\u2022\u2022"
                        else Money.formatPaise(netWorth),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryPill(
                            label = "Accounts",
                            value = "${data.accounts.size}",
                            modifier = Modifier.weight(1f),
                        )
                        SummaryPill(
                            label = "Card dues",
                            value = if (hide) "\u20B9\u2022\u2022\u2022" else Money.compact(liability),
                            modifier = Modifier.weight(1f),
                        )
                        SummaryPill(
                            label = "Entries",
                            value = "${monthTxns.size}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (data.accounts.isEmpty()) {
            item("empty") {
                EmptyState(
                    title = "No accounts yet",
                    subtitle = "Add your bank, cash or card account so entries know where they belong.",
                    emoji = "\uD83C\uDFE6",
                    action = "Add account",
                    onAction = { creating = true },
                )
            }
        }

        items(data.accounts, key = { it.id }) { account ->
            AccountCard(
                account = account,
                balancePaise = balances[account.id] ?: 0L,
                entriesThisMonth = monthTxns.count {
                    it.accountId == account.id || it.toAccountId == account.id
                },
                hideAmounts = hide,
                onClick = { editing = account },
            )
        }

        item("note") {
            Text(
                text = "Balances are calculated from your opening balance plus every entry. Nothing leaves this device.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
            )
        }
    }

    if (creating || editing != null) {
        AccountSheet(
            vm = vm,
            existing = editing,
            canDelete = data.accounts.size > 1,
            onDismiss = {
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AccountCard(
    account: Account,
    balancePaise: Long,
    entriesThisMonth: Int,
    hideAmounts: Boolean,
    onClick: () -> Unit,
) {
    val gradient = AccountGradients[((account.colorIndex % AccountGradients.size) + AccountGradients.size) % AccountGradients.size]
    val icon = when (account.kind) {
        AccountKind.BANK -> Icons.Rounded.AccountBalance
        AccountKind.CASH -> Icons.Rounded.Payments
        AccountKind.WALLET -> Icons.Rounded.Savings
        AccountKind.CARD -> Icons.Rounded.CreditCard
    }
    val isLiability = account.kind == AccountKind.CARD
    val balanceColor = when {
        isLiability && balancePaise < 0L -> Danger
        balancePaise < 0L -> Danger
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(gradient))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = account.kind.label +
                            if (account.last4.isNotBlank()) " \u00B7 \u2022\u2022\u2022\u2022 ${account.last4}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f),
                    )
                }
                if (isLiability) {
                    Text(
                        text = "DUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (hideAmounts) "\u20B9 \u2022\u2022\u2022\u2022\u2022\u2022" else Money.formatPaise(balancePaise),
                style = MaterialTheme.typography.headlineMedium,
                color = balanceColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (entriesThisMonth == 1) "1 entry this month" else "$entriesThisMonth entries this month",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Tap to edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.42f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(
    vm: JournalViewModel,
    existing: Account?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var kind by remember { mutableStateOf(existing?.kind ?: AccountKind.BANK) }
    var opening by remember {
        mutableStateOf(existing?.let { Money.paiseToInput(it.openingPaise) } ?: "")
    }
    var last4 by remember { mutableStateOf(existing?.last4.orEmpty()) }
    var colorIndex by remember { mutableStateOf(existing?.colorIndex ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (existing == null) "New account" else "Edit account",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Balances update automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
                if (existing != null && canDelete) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Danger.copy(alpha = 0.12f))
                            .clickable { confirmDelete = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete account",
                            tint = Danger,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FieldText("ACCOUNT NAME")
            Spacer(Modifier.height(8.dp))
            InputBox(
                value = name,
                onValueChange = { name = it.take(28) },
                placeholder = "e.g. SBI Savings",
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(16.dp))
            FieldText("TYPE")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountKind.entries.forEach { option ->
                    val selected = option == kind
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color(0xFF04120C) else TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Accent else Surface2)
                            .border(
                                1.dp,
                                if (selected) Accent else OutlineFaint,
                                RoundedCornerShape(50),
                            )
                            .clickable { kind = option }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            FieldText(if (existing == null) "OPENING BALANCE" else "OPENING BALANCE (RECALCULATES HISTORY)")
            Spacer(Modifier.height(8.dp))
            InputBox(
                value = opening,
                onValueChange = { input ->
                    opening = input.filter { it.isDigit() || it == '.' }.take(12)
                },
                placeholder = "0",
                keyboardType = KeyboardType.Decimal,
                prefix = "\u20B9",
            )

            Spacer(Modifier.height(16.dp))
            FieldText("LAST 4 DIGITS (OPTIONAL)")
            Spacer(Modifier.height(8.dp))
            InputBox(
                value = last4,
                onValueChange = { last4 = it.filter { ch -> ch.isDigit() }.take(4) },
                placeholder = "4821",
                keyboardType = KeyboardType.Number,
            )

            Spacer(Modifier.height(16.dp))
            FieldText("CARD COLOUR")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountGradients.forEachIndexed { index, gradient ->
                    val selected = index == colorIndex
                    Box(
                        modifier = Modifier
                            .size(if (selected) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(gradient))
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) TextPrimary else OutlineFaint,
                                CircleShape,
                            )
                            .clickable { colorIndex = index },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (name.isBlank()) Brush.horizontalGradient(listOf(Surface2, Surface2))
                        else Brush.horizontalGradient(listOf(AccentSoft, Accent, AccentDeep))
                    )
                    .clickable(enabled = name.isNotBlank()) {
                        vm.saveAccount(
                            id = existing?.id,
                            name = name,
                            kind = kind,
                            openingPaise = Money.parseToPaise(opening) ?: 0L,
                            last4 = last4,
                            colorIndex = colorIndex,
                        )
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (existing == null) "Add account" else "Save account",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (name.isBlank()) TextTertiary else Color(0xFF04120C),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete && existing != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Surface1,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Delete ${existing.name}?") },
            text = { Text("Its entries stay in your journal and move to your first remaining account.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    vm.deleteAccount(existing.id)
                    confirmDelete = false
                    onDismiss()
                }) { Text("Delete", color = Danger) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                    Text("Keep", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun FieldText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    prefix: String? = null,
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
        if (prefix != null) {
            Text(
                text = prefix,
                style = MaterialTheme.typography.bodyLarge,
                color = Accent,
            )
            Spacer(Modifier.width(6.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
