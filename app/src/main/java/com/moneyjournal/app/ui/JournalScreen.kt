package com.moneyjournal.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyjournal.app.data.Analytics
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.DateFormats
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.data.TxType
import com.moneyjournal.app.ui.components.AmountText
import com.moneyjournal.app.ui.components.EmptyState
import com.moneyjournal.app.ui.components.FilterChip
import com.moneyjournal.app.ui.components.MonthPickerDialog
import com.moneyjournal.app.ui.components.MonthSelector
import com.moneyjournal.app.ui.components.Panel
import com.moneyjournal.app.ui.components.SectionHeader
import com.moneyjournal.app.ui.components.TransactionRow
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.AccentDeep
import com.moneyjournal.app.ui.theme.AccentSoft
import com.moneyjournal.app.ui.theme.Danger
import com.moneyjournal.app.ui.theme.Ink
import com.moneyjournal.app.ui.theme.OutlineFaint
import com.moneyjournal.app.ui.theme.Surface1
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextSecondary
import com.moneyjournal.app.ui.theme.TextTertiary
import com.moneyjournal.app.ui.theme.Transfer
import com.moneyjournal.app.ui.theme.Warning
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    vm: JournalViewModel,
    data: AppData,
    modifier: Modifier = Modifier,
    onOpenInsights: () -> Unit = {},
) {
    val month by vm.selectedMonth.collectAsState()
    val query by vm.query.collectAsState()
    val activeType by vm.typeFilter.collectAsState()
    val filtersActive by vm.filtersActive.collectAsState()
    val listState = rememberLazyListState()
    val filtered = vm.filtered(month)
    val summary = Analytics.summary(filtered)
    val hideAmounts = data.settings.hideAmounts

    var showMonthPicker by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    val groups = remember(filtered) {
        filtered.groupBy { it.epochDay }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item("header") {
            JournalHeader(
                data = data,
                hideAmounts = hideAmounts,
                onToggleHide = { vm.updateSettings { s -> s.copy(hideAmounts = !s.hideAmounts) } },
            )
        }

        item("month") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonthSelector(
                    month = month,
                    onPrevious = { vm.shiftMonth(-1) },
                    onNext = { vm.shiftMonth(1) },
                    onPick = { showMonthPicker = true },
                    canGoNext = month.isBefore(YearMonth.now()),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${filtered.size} entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        }

        item("hero") {
            SpendHero(
                data = data,
                month = month,
                summary = summary,
                hideAmounts = hideAmounts,
                onOpenInsights = onOpenInsights,
            )
        }

        item("quickadd") {
            QuickAddRow(
                vm = vm,
                data = data,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        item("search") {
            SearchField(
                query = query,
                onQueryChange = { vm.setSearch(it) },
                filtersActive = filtersActive,
                onOpenFilters = { showFilters = true },
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        item("typechips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    label = "All",
                    selected = activeType == null,
                    onClick = { vm.toggleTypeFilter(null) },
                )
                FilterChip(
                    label = "Expense",
                    selected = activeType == TxType.EXPENSE,
                    onClick = { vm.toggleTypeFilter(TxType.EXPENSE) },
                )
                FilterChip(
                    label = "Income",
                    selected = activeType == TxType.INCOME,
                    onClick = { vm.toggleTypeFilter(TxType.INCOME) },
                )
                FilterChip(
                    label = "Transfer",
                    selected = activeType == TxType.TRANSFER,
                    onClick = { vm.toggleTypeFilter(TxType.TRANSFER) },
                )
            }
        }

        if (groups.isEmpty()) {
            item("empty") {
                EmptyState(
                    title = if (query.isBlank()) "Nothing here yet" else "No matches",
                    subtitle = if (query.isBlank()) {
                        "Tap the + button to log your first entry for ${DateFormats.monthLabel(month)}."
                    } else {
                        "Try a different merchant, category or amount."
                    },
                    emoji = "\uD83E\uDDFE",
                    action = "Add an entry",
                    onAction = { vm.openNewEntry() },
                )
            }
        }

        groups.forEach { (epochDay, txns) ->
            val date = LocalDate.ofEpochDay(epochDay)
            val daySpend = txns.filter { it.type == TxType.EXPENSE }.sumOf { it.amountPaise }
            val dayIncome = txns.filter { it.type == TxType.INCOME }.sumOf { it.amountPaise }
            stickyHeader(key = "day_$epochDay") {
                DayHeader(
                    label = DateFormats.dayHeader(date),
                    spend = daySpend,
                    income = dayIncome,
                    hideAmounts = hideAmounts,
                )
            }
            items(txns, key = { it.id }) { txn ->
                TransactionRow(
                    txn = txn,
                    data = data,
                    hideAmounts = hideAmounts,
                    onClick = { vm.openEdit(txn) },
                    onLongClick = { vm.deleteTransaction(txn.id) },
                )
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            selected = month,
            onDismiss = { showMonthPicker = false },
            onSelect = { vm.setMonth(it) },
        )
    }

    if (showFilters) {
        FilterSheet(vm = vm, data = data, onDismiss = { showFilters = false })
    }
}

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
private fun JournalHeader(
    data: AppData,
    hideAmounts: Boolean,
    onToggleHide: () -> Unit,
) {
    val streak = remember(data.transactions) { Analytics.streakOfRecordedDays(data.transactions) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MONEY JOURNAL",
                style = MaterialTheme.typography.labelSmall,
                color = Accent,
                letterSpacing = 1.6.sp,
            )
            Text(
                text = if (data.settings.name.isBlank()) greeting() else "${greeting()}, ${data.settings.name}",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = if (streak > 0) "$streak day logging streak" else "Log an entry to start a streak",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Surface2)
                .border(1.dp, OutlineFaint, CircleShape)
                .clickable(onClick = onToggleHide),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (hideAmounts) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = "Hide amounts",
                tint = if (hideAmounts) Accent else TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SpendHero(
    data: AppData,
    month: YearMonth,
    summary: Analytics.Summary,
    hideAmounts: Boolean,
    onOpenInsights: () -> Unit,
) {
    val delta = remember(data.transactions, month) { Analytics.monthOverMonth(data.transactions, month) }
    val savingsRate = summary.savingsRate.coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = savingsRate,
        animationSpec = tween(900),
        label = "heroProgress",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF122A22), Color(0xFF0F1D1E), Color(0xFF111A26))
                )
            )
            .border(1.dp, Accent.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
            .clickable(onClick = onOpenInsights)
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (month == YearMonth.now()) "SPENT THIS MONTH"
                        else "SPENT IN ${DateFormats.monthLabel(month).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentSoft,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (hideAmounts) "\u20B9 \u2022\u2022\u2022\u2022\u2022\u2022\u2022"
                        else Money.formatPaise(summary.expensePaise),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                    )
                }
                if (delta != null) {
                    val up = delta > 0
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (up) Warning.copy(alpha = 0.14f) else Accent.copy(alpha = 0.16f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (up) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (up) Warning else Accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${(kotlin.math.abs(delta) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (up) Warning else Accent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.07f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(AccentSoft, AccentDeep)))
                )
            }
            Text(
                text = if (summary.incomePaise > 0)
                    "You kept ${(savingsRate * 100).toInt()}% of what came in"
                else "No income logged this month yet",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(
                    label = "Earned",
                    paise = summary.incomePaise,
                    color = Accent,
                    hideAmounts = hideAmounts,
                    modifier = Modifier.weight(1f),
                )
                HeroStat(
                    label = "Saved",
                    paise = summary.netPaise,
                    color = if (summary.netPaise >= 0) AccentSoft else Danger,
                    hideAmounts = hideAmounts,
                    modifier = Modifier.weight(1f),
                )
                HeroStat(
                    label = "Transfers",
                    paise = summary.transferPaise,
                    color = Transfer,
                    hideAmounts = hideAmounts,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    paise: Long,
    color: Color,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (hideAmounts) "\u20B9 \u2022\u2022\u2022\u2022" else Money.compact(paise),
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
    }
}

@Composable
private fun QuickAddRow(
    vm: JournalViewModel,
    data: AppData,
    modifier: Modifier = Modifier,
) {
    val categories = remember(data.transactions, data.categories) { vm.quickCategories() }
    Column(modifier = modifier) {
        SectionHeader(
            title = "Quick add",
            subtitle = "One tap to log the usuals",
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Accent)
                        .clickable { vm.openNewEntry() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color(0xFF04120C),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "New entry",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF04120C),
                    )
                }
            }
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    label = category.name,
                    leadingEmoji = category.emoji,
                    selected = false,
                    onClick = { vm.openNewEntry(TxType.EXPENSE, category.id) },
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    filtersActive: Boolean,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(1.dp, OutlineFaint, RoundedCornerShape(18.dp))
            .padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = "Search merchant, category, note\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (filtersActive) Accent.copy(alpha = 0.16f) else Color.Transparent)
                .clickable(onClick = onOpenFilters),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = "Filters",
                tint = if (filtersActive) Accent else TextSecondary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun DayHeader(
    label: String,
    spend: Long,
    income: Long,
    hideAmounts: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .padding(top = 14.dp, bottom = 6.dp, start = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        if (income > 0) {
            Text(
                text = if (hideAmounts) "+ \u20B9\u2022\u2022\u2022" else "+${Money.formatPaise(income)}",
                style = MaterialTheme.typography.labelSmall,
                color = Accent,
            )
            Spacer(Modifier.width(10.dp))
        }
        if (spend > 0) {
            Text(
                text = if (hideAmounts) "\u2212 \u20B9\u2022\u2022\u2022" else "\u2212${Money.formatPaise(spend)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    vm: JournalViewModel,
    data: AppData,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Filter entries",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { vm.clearFilters() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            FilterSection(title = "Account") {
                FilterChip(
                    label = "Any",
                    selected = vm.accountFilter.value == null,
                    onClick = { vm.accountFilter.value = null },
                )
                data.accounts.forEach { account ->
                    FilterChip(
                        label = account.name,
                        selected = vm.accountFilter.value == account.id,
                        onClick = {
                            vm.accountFilter.value =
                                if (vm.accountFilter.value == account.id) null else account.id
                        },
                    )
                }
            }

            FilterSection(title = "Payment mode") {
                FilterChip(
                    label = "Any",
                    selected = vm.modeFilter.value == null,
                    onClick = { vm.modeFilter.value = null },
                )
                com.moneyjournal.app.data.PayMode.entries.forEach { mode ->
                    FilterChip(
                        label = mode.label,
                        selected = vm.modeFilter.value == mode,
                        onClick = {
                            vm.modeFilter.value = if (vm.modeFilter.value == mode) null else mode
                        },
                    )
                }
            }

            FilterSection(title = "Category") {
                FilterChip(
                    label = "Any",
                    selected = vm.categoryFilter.value == null,
                    onClick = { vm.categoryFilter.value = null },
                )
                data.categories.sortedBy { it.name }.forEach { category ->
                    FilterChip(
                        label = "${category.emoji} ${category.name}",
                        selected = vm.categoryFilter.value == category.id,
                        onClick = {
                            vm.categoryFilter.value =
                                if (vm.categoryFilter.value == category.id) null else category.id
                        },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Accent)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Show results",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF04120C),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
fun HiddenAmountNotice(modifier: Modifier = Modifier) {
    Panel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Amounts are hidden while you're in public.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
