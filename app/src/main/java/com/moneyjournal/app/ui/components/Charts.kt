package com.moneyjournal.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyjournal.app.data.Analytics
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.AccentDeep
import com.moneyjournal.app.ui.theme.AccentSoft
import com.moneyjournal.app.ui.theme.CategoryPalette
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.Surface3
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextTertiary

fun sliceColor(index: Int): Color =
    CategoryPalette[((index % CategoryPalette.size) + CategoryPalette.size) % CategoryPalette.size]

@Composable
fun MoneyHealthRing(
    score: Int,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
) {
    val progress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "ring",
    )
    val startAngle = 138f
    val maxSweep = 264f

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(size)) {
            val stroke = 15.dp.toPx()
            val inset = stroke / 2 + 6.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = Surface2,
                startAngle = startAngle,
                sweepAngle = maxSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // soft outer glow of the filled portion
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(AccentDeep.copy(alpha = 0.0f), AccentSoft.copy(alpha = 0.30f), Accent.copy(alpha = 0.0f))
                ),
                startAngle = startAngle,
                sweepAngle = maxSweep * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke + 12.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.linearGradient(listOf(AccentSoft, Accent, AccentDeep)),
                startAngle = startAngle,
                sweepAngle = maxSweep * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "/100",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Accent,
            )
        }
    }
}

@Composable
fun DonutChart(
    slices: List<Analytics.Slice>,
    modifier: Modifier = Modifier,
    stroke: Dp = 20.dp,
    selectedIndex: Int? = null,
) {
    val animation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "donut",
    )
    val gap = if (slices.size > 1) 2.4f else 0f
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp)
    ) {
        // Always draw a true circle, centred in whatever space the card gives us.
        val strokeWidth = stroke.toPx()
        val diameter = (minOf(size.width, size.height) - strokeWidth - 16.dp.toPx())
            .coerceAtLeast(24.dp.toPx())
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset(
            x = (size.width - diameter) / 2,
            y = (size.height - diameter) / 2,
        )
        drawArc(
            color = Surface2,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke.toPx()),
        )
        var cursor = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = (slice.share * 360f - gap) * animation
            if (sweep > 0.5f) {
                val color = sliceColor(slice.colorIndex)
                val isDimmed = selectedIndex != null && selectedIndex != index
                val width = if (selectedIndex == index) strokeWidth + 8.dp.toPx() else strokeWidth
                drawArc(
                    color = if (isDimmed) color.copy(alpha = 0.35f) else color,
                    startAngle = cursor,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = width, cap = StrokeCap.Round),
                )
            }
            cursor += slice.share * 360f * animation
        }
    }
}

@Composable
fun DailyBars(
    days: List<Analytics.DayTotal>,
    modifier: Modifier = Modifier,
    highlightDay: Int? = null,
) {
    val animation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "bars",
    )
    val measurer = rememberTextMeasurer()
    // One huge day (rent, festival shopping) must not flatten every other bar, so the
    // scale is capped just above the second largest day and taller bars get clipped.
    val scale = remember(days) {
        val sorted = days.map { it.expensePaise }.sortedDescending()
        val top = sorted.getOrNull(0) ?: 0L
        val second = sorted.getOrNull(1) ?: 0L
        if (second > 0L && top > (second * 2.2).toLong()) (second * 1.7).toLong() else top
    }
    val maxValue = scale.coerceAtLeast(1L)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
    ) {
        if (days.isEmpty()) return@Canvas
        val labelStyle = TextStyle(fontSize = 10.sp, color = TextTertiary)
        val axisHeight = 22.dp.toPx()
        val topPad = 16.dp.toPx()
        val chartHeight = size.height - axisHeight - topPad
        val slot = size.width / days.size
        val barWidth = (slot * 0.6f).coerceAtLeast(2.5f)

        days.forEachIndexed { index, day ->
            val centreX = slot * index + slot / 2
            val rawRatio = day.expensePaise.toFloat() / maxValue
            val ratio = rawRatio.coerceAtMost(1f)
            val clipped = rawRatio > 1f
            val barHeight = (chartHeight * ratio * animation).coerceAtLeast(if (day.expensePaise > 0) 3.dp.toPx() else 0f)
            val isHighlight = highlightDay != null && day.day == highlightDay
            if (barHeight > 0f) {
                val brush = when {
                    clipped || isHighlight -> Brush.verticalGradient(listOf(AccentSoft, Accent))
                    ratio > 0.6f -> Brush.verticalGradient(listOf(Accent.copy(alpha = 0.9f), AccentDeep.copy(alpha = 0.55f)))
                    else -> Brush.verticalGradient(listOf(Accent.copy(alpha = 0.55f), AccentDeep.copy(alpha = 0.28f)))
                }
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(centreX - barWidth / 2, topPad + chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2),
                )
                if (clipped) {
                    // notch marks a bar that runs off the top of the scale
                    drawRoundRect(
                        color = Color(0xFF04120C),
                        topLeft = Offset(centreX - barWidth / 2, topPad + chartHeight - barHeight + 3.dp.toPx()),
                        size = Size(barWidth, 2.5.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f),
                    )
                }
            } else {
                drawRoundRect(
                    color = Surface3.copy(alpha = 0.7f),
                    topLeft = Offset(centreX - barWidth / 2, topPad + chartHeight - 3.dp.toPx()),
                    size = Size(barWidth, 3.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f),
                )
            }

            if (day.day == 1 || day.day % 5 == 0) {
                val label = measurer.measure("${day.day}", labelStyle)
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(centreX - label.size.width / 2, topPad + chartHeight + 6.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun WeekdayBars(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
) {
    val animation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "weekday",
    )
    val maxValue = remember(values) { values.maxOrNull() ?: 0L }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(104.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { index, value ->
                val ratio = if (maxValue <= 0L) 0f else value.toFloat() / maxValue
                val heightFraction = (0.08f + ratio * 0.92f) * animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((96 * heightFraction).dp.coerceAtLeast(6.dp))
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                highlightIndex == index ->
                                    Brush.verticalGradient(listOf(AccentSoft, Accent))
                                else ->
                                    Brush.verticalGradient(
                                        listOf(Accent.copy(alpha = 0.5f), AccentDeep.copy(alpha = 0.22f))
                                    )
                            }
                        )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlightIndex == index) Accent else TextTertiary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun Sparkline(
    values: List<Long>,
    modifier: Modifier = Modifier,
    color: Color = Accent,
) {
    val animation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "spark",
    )
    Canvas(modifier = modifier.fillMaxWidth().height(38.dp)) {
        if (values.size < 2) return@Canvas
        val maxValue = values.maxOrNull() ?: 0L
        if (maxValue <= 0L) return@Canvas
        val step = size.width / (values.size - 1)
        var previous: Offset? = null
        values.forEachIndexed { index, value ->
            val point = Offset(
                x = step * index,
                y = size.height - (value.toFloat() / maxValue) * (size.height - 4.dp.toPx()) * animation - 2.dp.toPx(),
            )
            previous?.let { prev ->
                drawLine(
                    color = color.copy(alpha = 0.9f),
                    start = prev,
                    end = point,
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            previous = point
        }
    }
}

@Composable
fun LegendRow(
    name: String,
    emoji: String,
    amountPaise: Long,
    share: Float,
    color: Color,
    hideAmounts: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Surface2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = "$emoji  $name",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Surface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(share.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.85f))
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = if (hideAmounts) "\u2022\u2022\u2022" else Money.formatPaise(amountPaise),
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
            )
            Text(
                text = "${(share * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
    }
}
