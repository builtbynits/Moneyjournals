package com.moneyjournal.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.DateFormats
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.data.Txn
import com.moneyjournal.app.data.TxType
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.Expense
import com.moneyjournal.app.ui.theme.Income
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextSecondary
import com.moneyjournal.app.ui.theme.TextTertiary
import com.moneyjournal.app.ui.theme.Transfer
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    txn: Txn,
    data: AppData,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val category = data.category(txn.categoryId)
    val account = data.account(txn.accountId)
    val toAccount = data.account(txn.toAccountId)
    val accentColor = when (txn.type) {
        TxType.EXPENSE -> sliceColor(category?.colorIndex ?: 7)
        TxType.INCOME -> Income
        TxType.TRANSFER -> Transfer
    }
    val amountColor = when (txn.type) {
        TxType.EXPENSE -> Expense
        TxType.INCOME -> Income
        TxType.TRANSFER -> Transfer
    }
    val prefix = when (txn.type) {
        TxType.EXPENSE -> "\u2212"
        TxType.INCOME -> "+"
        TxType.TRANSFER -> ""
    }
    val emoji = when (txn.type) {
        TxType.TRANSFER -> "\uD83D\uDD01"
        TxType.INCOME -> category?.emoji ?: "\uD83D\uDCB0"
        TxType.EXPENSE -> category?.emoji ?: "\u2728"
    }
    val title = txn.merchant.ifBlank {
        category?.name ?: when (txn.type) {
            TxType.EXPENSE -> "Expense"
            TxType.INCOME -> "Income"
            TxType.TRANSFER -> "Transfer"
        }
    }
    val subtitle = buildString {
        if (txn.type == TxType.TRANSFER) {
            append("${account?.name ?: "Account"} \u2192 ${toAccount?.name ?: "Account"}")
        } else {
            append(category?.name ?: "Uncategorised")
            append(" \u00B7 ")
            append(txn.mode.label)
        }
        if (txn.note.isNotBlank()) append(" \u00B7 ${txn.note}")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(emoji = emoji, color = accentColor, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (hideAmounts) "\u20B9 \u2022\u2022\u2022\u2022"
                else prefix + Money.formatPaise(txn.amountPaise),
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                maxLines = 1,
            )
            Text(
                text = DateFormats.shortDate(LocalDate.ofEpochDay(txn.epochDay)),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
fun TransferBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Surface2)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.SwapHoriz,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("Transfer", style = MaterialTheme.typography.labelSmall, color = Accent)
    }
}
