package com.moneyjournal.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.moneyjournal.app.data.DateFormats
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.OutlineFaint
import com.moneyjournal.app.ui.theme.Surface1
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.Surface3
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextSecondary
import com.moneyjournal.app.ui.theme.TextTertiary
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    shape: Dp = 24.dp,
    background: Color = Surface1,
    borderColor: Color = OutlineFaint,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(shape))
            .background(background)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(shape))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (action != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingEmoji: String? = null,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Accent else Surface2,
        animationSpec = tween(220),
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color(0xFF04120C) else TextSecondary,
        animationSpec = tween(220),
        label = "chipFg",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                BorderStroke(1.dp, if (selected) Accent else OutlineFaint),
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingEmoji != null) {
            Text(text = leadingEmoji, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
        )
    }
}

@Composable
fun AnimatedSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeTextColor: Color = Color(0xFF04120C),
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface2)
            .border(1.dp, OutlineFaint, RoundedCornerShape(18.dp))
            .padding(5.dp)
    ) {
        val segmentWidth = (maxWidth - 10.dp) / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(260, easing = FastOutSlowInEasing),
            label = "segOffset",
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .height(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Accent)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    val color by animateColorAsState(
                        targetValue = if (index == selectedIndex) activeTextColor else TextSecondary,
                        animationSpec = tween(200),
                        label = "segText",
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun AmountText(
    paise: Long,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    hide: Boolean = false,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    showSymbol: Boolean = true,
) {
    Text(
        text = if (hide) "\u20B9 \u2022\u2022\u2022\u2022\u2022\u2022" else Money.formatPaise(paise, withSymbol = showSymbol),
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun MonthSelector(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit,
    canGoNext: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Surface2)
            .border(1.dp, OutlineFaint, RoundedCornerShape(50))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChevronButton(icon = Icons.Rounded.ChevronLeft, contentDescription = "Previous month", onClick = onPrevious)
        Text(
            text = DateFormats.monthLabel(month),
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onPick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        ChevronButton(
            icon = Icons.Rounded.ChevronRight,
            contentDescription = "Next month",
            onClick = { if (canGoNext) onNext() },
            enabled = canGoNext,
        )
    }
}

@Composable
private fun ChevronButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) TextSecondary else TextTertiary.copy(alpha = 0.35f),
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
fun MonthPickerDialog(
    selected: YearMonth,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit,
) {
    val today = YearMonth.now()
    val months = remember { (0..23).map { today.minusMonths(it.toLong()) } }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Surface1,
            border = BorderStroke(1.dp, OutlineFaint),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Jump to month",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(330.dp)) {
                    items(months) { month ->
                        val isSelected = month == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    onSelect(month)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = DateFormats.monthLabel(month),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) Accent else TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextTertiary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    emoji: String = "\uD83E\uDDFE",
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 38.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Accent.copy(alpha = 0.14f))
                    .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(action, style = MaterialTheme.typography.labelMedium, color = Accent)
            }
        }
    }
}

@Composable
fun IconBubble(
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    emojiSize: androidx.compose.ui.unit.TextUnit = MaterialTheme.typography.titleMedium.fontSize,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.36f))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.26f), color.copy(alpha = 0.10f))))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(size * 0.36f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = emojiSize)
    }
}

@Composable
fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Surface3)
        )
    }
}

@Composable
fun VerticalGlow(modifier: Modifier = Modifier, color: Color = Accent) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.10f), Color.Transparent),
                    endY = 520f,
                )
            )
    )
}

@Composable
fun LabelValue(label: String, value: String, valueColor: Color = TextPrimary) {
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
            color = valueColor,
        )
    }
}

@Composable
fun DatePill(date: LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Surface2)
            .border(1.dp, OutlineFaint, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DateFormats.fullDate(date),
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
