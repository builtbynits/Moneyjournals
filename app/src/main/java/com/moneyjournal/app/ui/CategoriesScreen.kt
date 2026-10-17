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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyjournal.app.data.Analytics
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.Category
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.data.TxType
import com.moneyjournal.app.ui.components.AnimatedSegmented
import com.moneyjournal.app.ui.components.EmptyState
import com.moneyjournal.app.ui.components.IconBubble
import com.moneyjournal.app.ui.components.SheetGrabber
import com.moneyjournal.app.ui.components.sliceColor
import com.moneyjournal.app.ui.theme.Accent
import com.moneyjournal.app.ui.theme.AccentDeep
import com.moneyjournal.app.ui.theme.AccentSoft
import com.moneyjournal.app.ui.theme.CategoryPalette
import com.moneyjournal.app.ui.theme.Danger
import com.moneyjournal.app.ui.theme.Ink
import com.moneyjournal.app.ui.theme.OutlineFaint
import com.moneyjournal.app.ui.theme.Surface1
import com.moneyjournal.app.ui.theme.Surface2
import com.moneyjournal.app.ui.theme.TextPrimary
import com.moneyjournal.app.ui.theme.TextSecondary
import com.moneyjournal.app.ui.theme.TextTertiary

private val emojiChoices = listOf(
    "\uD83C\uDF5C", "\uD83D\uDED2", "\uD83D\uDE95", "\uD83D\uDECD", "\uD83D\uDCA1", "\uD83C\uDFE0",
    "\uD83C\uDFAC", "\uD83D\uDC8A", "\uD83C\uDF93", "\uD83D\uDCC8", "\uD83C\uDFE7", "\uD83D\uDD01",
    "\uD83D\uDCBC", "\uD83E\uDDD1\u200D\uD83D\uDCBB", "\uD83C\uDFE6", "\uD83D\uDCB8", "\uD83C\uDF81", "\u2708\uFE0F",
    "\uD83D\uDC36", "\uD83C\uDFCB\uFE0F", "\uD83D\uDCF1", "\uD83D\uDC8D", "\uD83C\uDFAE", "\u2728",
)

@Composable
fun CategoriesScreen(
    vm: JournalViewModel,
    data: AppData,
    onBack: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    val type = if (tab == 0) TxType.EXPENSE else TxType.INCOME
    val categories = remember(data.categories, type) { data.categoriesOf(type) }
    val monthTxns = remember(data.transactions) { Analytics.inMonth(data.transactions, java.time.YearMonth.now()) }

    Column(modifier = Modifier.fillMaxSize().background(Ink)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Text(
                    text = "${data.categories.size} in total \u00B7 tap to edit",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Accent)
                    .clickable { creating = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color(0xFF04120C),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "New",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF04120C),
                )
            }
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            AnimatedSegmented(
                options = listOf("Expense", "Income"),
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        }

        if (categories.isEmpty()) {
            EmptyState(
                title = "No categories",
                subtitle = "Create your first ${type.label.lowercase()} category to start grouping entries.",
                emoji = "\uD83C\uDFF7\uFE0F",
                action = "New category",
                onAction = { creating = true },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 14.dp, bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    val count = monthTxns.count { it.categoryId == category.id }
                    val total = monthTxns.filter { it.categoryId == category.id }
                        .sumOf { it.amountPaise }
                    val color = sliceColor(category.colorIndex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Surface1)
                            .border(1.dp, OutlineFaint, RoundedCornerShape(20.dp))
                            .clickable { editing = category }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBubble(emoji = category.emoji, color = color, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (count == 0) "No entries this month"
                                else if (count == 1) "1 entry \u00B7 ${
                                    if (data.settings.hideAmounts) "\u20B9\u2022\u2022\u2022"
                                    else Money.formatPaise(total)
                                } this month"
                                else "$count entries \u00B7 ${
                                    if (data.settings.hideAmounts) "\u20B9\u2022\u2022\u2022"
                                    else Money.formatPaise(total)
                                } this month",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                            )
                        }
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        CategorySheet(
            vm = vm,
            data = data,
            existing = editing,
            defaultType = type,
            onDismiss = {
                creating = false
                editing = null
            },
            onRequestDelete = { category ->
                pendingDelete = category
            },
        )
    }

    pendingDelete?.let { category ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Surface1,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Delete ${category.name}?") },
            text = {
                Text("Entries in this category move to ${if (category.type == TxType.INCOME) "Other Income" else "Other"}. This cannot be undone.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    vm.deleteCategory(category.id)
                    pendingDelete = null
                    editing = null
                    creating = false
                }) { Text("Delete", color = Danger) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) {
                    Text("Keep", color = TextSecondary)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(
    vm: JournalViewModel,
    data: AppData,
    existing: Category?,
    defaultType: TxType,
    onDismiss: () -> Unit,
    onRequestDelete: (Category) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "\u2728") }
    var colorIndex by remember { mutableStateOf(existing?.colorIndex ?: 0) }
    var type by remember { mutableStateOf(existing?.type ?: defaultType) }

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
                        text = if (existing == null) "New category" else "Edit category",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Used for ${type.label.lowercase()} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                    )
                }
                if (existing != null && !existing.system) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Danger.copy(alpha = 0.12f))
                            .clickable { onRequestDelete(existing) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Danger,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            AnimatedSegmented(
                options = listOf("Expense", "Income"),
                selectedIndex = if (type == TxType.EXPENSE) 0 else 1,
                onSelect = { type = if (it == 0) TxType.EXPENSE else TxType.INCOME },
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "NAME",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(8.dp))
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
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (name.isEmpty()) {
                                Text(
                                    text = "e.g. Pet care",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextTertiary,
                                )
                            }
                            inner()
                        }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "ICON",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gridItems(emojiChoices) { choice ->
                    val selected = choice == emoji
                    Box(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Accent.copy(alpha = 0.20f) else Surface2)
                            .border(
                                1.dp,
                                if (selected) Accent else OutlineFaint,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { emoji = choice },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = choice, fontSize = 20.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "COLOUR",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryPalette.forEachIndexed { index, color ->
                    val selected = index == colorIndex
                    Box(
                        modifier = Modifier
                            .size(if (selected) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (selected) 2.dp else 0.dp,
                                if (selected) TextPrimary else Color.Transparent,
                                CircleShape,
                            )
                            .clickable { colorIndex = index },
                        contentAlignment = Alignment.Center,
                    ) {}
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
                        vm.saveCategory(existing?.id, name, emoji, colorIndex, type)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (existing == null) "Add category" else "Save changes",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (name.isBlank()) TextTertiary else Color(0xFF04120C),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
