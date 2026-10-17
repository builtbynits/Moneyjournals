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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyjournal.app.data.Analytics
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.DateFormats
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.data.TxType
import com.moneyjournal.app.ui.components.DailyBars
import com.moneyjournal.app.ui.components.DonutChart
import com.moneyjournal.app.ui.components.EmptyState
import com.moneyjournal.app.ui.components.LegendRow
import com.moneyjournal.app.ui.components.MonthSelector
import com.moneyjournal.app.ui.components.MoneyHealthRing
import com.moneyjournal.app.ui.components.Panel
import com.moneyjournal.app.ui.components.SectionHeader
import com.moneyjournal.app.ui.components.WeekdayBars
import com.moneyjournal.app.ui.components.sliceColor
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.AccentDeep
import com.moneyjournal.app.ui.theme.AccentSoft
import com.moneyjournal.app.ui.theme.Danger
import com.moneyjournal.app.ui.theme.OutlineFaint
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextSecondary
import com.moneyjournal.app.ui.theme.TextTertiary
import com.moneyjournal.app.ui.theme.Warning
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun InsightsScreen(
    vm: JournalViewModel,
    data: AppData,
    modifier: Modifier = Modifier,
    onManageCategories: () -> Unit,
) {
    val month by vm.selectedMonth.collectAsState()
    val hide = data.settings.hideAmounts
    val transactions = data.transactions
    val monthTxns = remember(transactions, month) { Analytics.inMonth(transactions, month) }
    val summary = remember(monthTxns) { Analytics.summary(monthTxns) }
    val slices = remember(monthTxns, data.categories) { Analytics.categoryBreakdown(monthTxns, data) }
    val days = remember(monthTxns, month) { Analytics.dailyTotals(monthTxns, month) }
    val score = remember(transactions, month) { Analytics.healthScore(transactions, month) }
    val delta = remember(transactions, month) { Analytics.monthOverMonth(transactions, month) }
    val weekdayProfile = remember(monthTxns) { Analytics.weekdayProfile(monthTxns) }
    val busiest = remember(monthTxns) { Analytics.busiestWeekday(monthTxns) }
    val dailyAverage = remember(monthTxns, month) { Analytics.dailyAverage(monthTxns, month) }
    val highestDay = remember(monthTxns, month) { Analytics.highestDay(monthTxns, month) }
    val topMerchants = remember(monthTxns) { Analytics.topMerchants(monthTxns, 4) }
    val today = LocalDate.now()
    val isCurrentMonth = YearMonth.from(today) == month
    val projection = remember(dailyAverage, month) {
        if (!isCurrentMonth) summary.expensePaise else dailyAverage * month.lengthOfMonth()
    }

    var selectedSlice by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("head") {
            Column {
                Text(
                    text = "INSIGHTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    letterSpacing = 1.6.sp,
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Your money story",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                MonthSelector(
                    month = month,
                    onPrevious = { vm.shiftMonth(-1) },
                    onNext = { vm.shiftMonth(1) },
                    onPick = { },
                    canGoNext = month.isBefore(YearMonth.now()),
                )
            }
        }

        item("health") {
            Panel(padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyHealthRing(
                        score = score,
                        label = Analytics.healthLabel(score),
                        size = 150.dp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Money health",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = healthAdvice(score, summary.savingsRate),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                        Spacer(Modifier.height(12.dp))
                        MiniStat(
                            label = "Savings rate",
                            value = "${(summary.savingsRate.coerceAtLeast(0f) * 100).toInt()}%",
                            color = Accent,
                        )
                        if (delta != null) {
                            MiniStat(
                                label = "vs ${DateFormats.monthShortLabel(month.minusMonths(1))}",
                                value = (if (delta > 0) "+" else "\u2212") +
                                    "${(kotlin.math.abs(delta) * 100).toInt()}%",
                                color = if (delta > 0) Warning else Accent,
                            )
                        }
                        MiniStat(
                            label = "Avg / day",
                            value = if (hide) "\u20B9\u2022\u2022\u2022" else Money.compact(dailyAverage),
                            color = TextPrimary,
                        )
                    }
                }
            }
        }

        item("tiles") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Earned",
                        paise = summary.incomePaise,
                        hide = hide,
                        accent = Accent,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Spent",
                        paise = summary.expensePaise,
                        hide = hide,
                        accent = Warning,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Saved",
                        paise = summary.netPaise,
                        hide = hide,
                        accent = if (summary.netPaise >= 0) AccentSoft else Danger,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Entries",
                        count = summary.txCount,
                        hide = false,
                        accent = TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (isCurrentMonth) {
            item("pace") {
                Panel(background = Surface2, padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Warning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "On pace for ${
                                    if (hide) "\u20B9\u2022\u2022\u2022\u2022" else Money.formatPaise(projection)
                                } this month",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Based on ${DateFormats.fullDate(today)} at ${
                                    if (hide) "\u20B9\u2022\u2022\u2022" else Money.compact(dailyAverage)
                                }/day",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        if (slices.isEmpty()) {
            item("empty") {
                EmptyState(
                    title = "No spending yet",
                    subtitle = "Once you log entries for ${DateFormats.monthLabel(month)}, your charts fill in here.",
                    emoji = "\uD83D\uDCC9",
                    action = "Add an entry",
                    onAction = { vm.openNewEntry() },
                )
            }
        } else {
            item("donut") {
                Panel(padding = 18.dp) {
                    SectionHeader(
                        title = "Where it went",
                        subtitle = "${slices.size} categories \u00B7 ${
                            if (hide) "hidden" else Money.formatPaise(summary.expensePaise)
                        } spent",
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(contentAlignment = Alignment.Center) {
                        DonutChart(slices = slices, selectedIndex = selectedSlice)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val active = selectedSlice?.let { slices.getOrNull(it) }
                            Text(
                                text = (active?.emoji ?: "\uD83D\uDCB8") + "  " +
                                    (active?.name ?: "Total"),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (hide) "\u20B9 \u2022\u2022\u2022\u2022"
                                else Money.formatPaise(active?.totalPaise ?: summary.expensePaise),
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (active != null) "${(active.share * 100).toInt()}% of spend"
                                else "${summary.txCount} entries",
                                style = MaterialTheme.typography.labelSmall,
                                color = Accent,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    slices.take(7).forEachIndexed { index, slice ->
                        LegendRow(
                            name = slice.name,
                            emoji = slice.emoji,
                            amountPaise = slice.totalPaise,
                            share = slice.share,
                            color = sliceColor(slice.colorIndex),
                            hideAmounts = hide,
                            selected = selectedSlice == index,
                            onClick = {
                                selectedSlice = if (selectedSlice == index) null else index
                            },
                        )
                    }
                }
            }
        }

        item("daily") {
            Panel(padding = 18.dp) {
                SectionHeader(
                    title = "Daily rhythm",
                    subtitle = "Every day of ${DateFormats.monthShortLabel(month)}",
                )
                Spacer(Modifier.height(14.dp))
                DailyBars(
                    days = days,
                    highlightDay = if (isCurrentMonth) today.dayOfMonth else null,
                )
                Spacer(Modifier.height(10.dp))
                if (highestDay != null) {
                    Text(
                        text = "Biggest day: ${DateFormats.fullDate(highestDay.first)} \u00B7 ${
                            if (hide) "\u20B9\u2022\u2022\u2022" else Money.formatPaise(highestDay.second)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
            }
        }

        item("weekday") {
            Panel(padding = 18.dp) {
                SectionHeader(
                    title = "Weekday spending",
                    subtitle = "Average per entry",
                )
                Spacer(Modifier.height(16.dp))
                WeekdayBars(
                    values = weekdayProfile,
                    labels = DateFormats.weekdayLabels(),
                    highlightIndex = busiest,
                )
                if (busiest != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "You tend to spend the most on ${DateFormats.weekdayFullNames()[busiest]}s \u00B7 average ${
                            if (hide) "\u20B9\u2022\u2022\u2022" else Money.formatPaise(weekdayProfile[busiest])
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
            }
        }

        if (topMerchants.isNotEmpty()) {
            item("merchants") {
                Panel(padding = 18.dp) {
                    SectionHeader(title = "Top merchants", subtitle = "Where the money repeats")
                    Spacer(Modifier.height(8.dp))
                    topMerchants.forEach { (merchant, amount) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = merchant.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Accent,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = merchant,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (hide) "\u20B9\u2022\u2022\u2022" else Money.formatPaise(amount),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        item("manage") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface2)
                    .border(1.dp, OutlineFaint, RoundedCornerShape(20.dp))
                    .clickable(onClick = onManageCategories)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Category,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manage categories",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "${data.categories.size} categories \u00B7 add, rename, recolour",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun healthAdvice(score: Int, savingsRate: Float): String = when {
    score >= 80 -> "Superb balance. Keep this pacing and your savings compound nicely."
    score >= 65 -> "Healthy month \u2014 ${(savingsRate * 100).toInt()}% of income stayed with you."
    score >= 45 -> "Steady, but trimming one big category would lift this."
    score >= 25 -> "Spending is close to income this month."
    else -> "Expenses outweigh income \u2014 review the largest categories."
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
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
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    paise: Long = 0L,
    count: Int? = null,
    hide: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(1.dp, OutlineFaint, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (count != null) "$count" else if (hide) "\u20B9 \u2022\u2022\u2022\u2022\u2022" else Money.formatPaise(paise),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun InsightArrow(up: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (up) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
        contentDescription = null,
        tint = if (up) Warning else Accent,
        modifier = modifier.size(14.dp),
    )
}

@Composable
fun GradientStrip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(AccentSoft, Accent, AccentDeep)))
    )
}
