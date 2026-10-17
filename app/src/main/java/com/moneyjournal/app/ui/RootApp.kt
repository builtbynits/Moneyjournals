package com.moneyjournal.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import kotlinx.coroutines.delay

private const val ROUTE_JOURNAL = "journal"
private const val ROUTE_INSIGHTS = "insights"
private const val ROUTE_ACCOUNTS = "accounts"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CATEGORIES = "categories"

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(ROUTE_JOURNAL, "Journal", Icons.AutoMirrored.Rounded.MenuBook),
    NavItem(ROUTE_INSIGHTS, "Insights", Icons.Rounded.Insights),
    NavItem(ROUTE_ACCOUNTS, "Accounts", Icons.Rounded.AccountBalanceWallet),
    NavItem(ROUTE_SETTINGS, "Settings", Icons.Rounded.Settings),
)

@Composable
fun MoneyJournalRoot(vm: JournalViewModel) {
    val data by vm.data.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val locked by vm.locked.collectAsState()
    val editor by vm.editor.collectAsState()
    val message by vm.snackbar.collectAsState()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: ROUTE_JOURNAL

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = text,
            actionLabel = if (text.startsWith("Entry deleted")) "Undo" else null,
            withDismissAction = true,
        )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            vm.undoDelete()
        }
        vm.consumeSnackbar()
    }

    when {
        !loaded -> LoadingScreen()
        locked -> LockScreen(
            onUnlock = { pin -> vm.unlock(pin) },
            onForgot = {
                vm.disableAppLock()
                vm.eraseEverything()
            },
        )
        else -> {
            Scaffold(
                containerColor = Ink,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { hostData ->
                        androidx.compose.material3.Snackbar(
                            snackbarData = hostData,
                            containerColor = Surface2,
                            contentColor = TextPrimary,
                            actionColor = Accent,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                },
                bottomBar = {
                    BottomBar(
                        currentRoute = currentRoute,
                        onSelect = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(ROUTE_JOURNAL) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                },
                floatingActionButton = {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(listOf(AccentSoft, Accent, AccentDeep))
                            )
                            .border(4.dp, Ink, CircleShape)
                            .clickable { vm.openNewEntry() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add entry",
                            tint = Color(0xFF04120C),
                            modifier = Modifier.size(30.dp),
                        )
                    }
                },
                floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center,
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_JOURNAL,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding()),
                    enterTransition = {
                        fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it / 14 }
                    },
                    exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = {
                        fadeIn(tween(200)) + slideInHorizontally(tween(260)) { -it / 14 }
                    },
                    popExitTransition = {
                        fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 14 }
                    },
                ) {
                    composable(ROUTE_JOURNAL) {
                        JournalScreenWithInsets(
                            vm = vm,
                            onOpenInsights = { navController.navigate(ROUTE_INSIGHTS) },
                        )
                    }
                    composable(ROUTE_INSIGHTS) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            InsightsScreen(
                                vm = vm,
                                data = data,
                                onManageCategories = { navController.navigate(ROUTE_CATEGORIES) },
                            )
                        }
                    }
                    composable(ROUTE_ACCOUNTS) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            AccountsScreen(vm = vm, data = data)
                        }
                    }
                    composable(ROUTE_SETTINGS) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            SettingsScreen(vm = vm, data = data)
                        }
                    }
                    composable(ROUTE_CATEGORIES) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            CategoriesScreen(
                                vm = vm,
                                data = data,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = editor != null, enter = fadeIn(), exit = fadeOut()) {
                AddEntrySheet(vm = vm, data = data)
            }
        }
    }
}

@Composable
private fun JournalScreenWithInsets(
    vm: JournalViewModel,
    onOpenInsights: () -> Unit,
) {
    val data by vm.data.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        JournalScreen(
            vm = vm,
            data = data,
            onOpenInsights = onOpenInsights,
        )
    }
}

@Composable
private fun BottomBar(
    currentRoute: String,
    onSelect: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1)
            .border(1.dp, OutlineFaint, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomSlot(
                item = navItems[0],
                selected = currentRoute == navItems[0].route,
                onClick = { onSelect(navItems[0].route) },
                modifier = Modifier.weight(1f),
            )
            BottomSlot(
                item = navItems[1],
                selected = currentRoute == navItems[1].route,
                onClick = { onSelect(navItems[1].route) },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(72.dp))
            BottomSlot(
                item = navItems[2],
                selected = currentRoute == navItems[2].route,
                onClick = { onSelect(navItems[2].route) },
                modifier = Modifier.weight(1f),
            )
            BottomSlot(
                item = navItems[3],
                selected = currentRoute == navItems[3].route,
                onClick = { onSelect(navItems[3].route) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BottomSlot(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) Accent else TextTertiary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Accent)
            )
            Spacer(Modifier.height(4.dp))
        }
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier
                .size(23.dp)
                .offset(y = if (selected) 0.dp else 2.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontSize = 10.5.sp,
        )
    }
}

@Composable
private fun LoadingScreen() {
    val transition = rememberInfiniteTransition(label = "load")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Accent.copy(alpha = 0.22f), Accent.copy(alpha = 0.06f))
                        )
                    )
                    .border(1.dp, Accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u20B9",
                    style = MaterialTheme.typography.displayMedium,
                    color = Accent,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Money Journal",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Offline \u00B7 private \u00B7 made for \u20B9",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun LockScreen(
    onUnlock: (String) -> Boolean,
    onForgot: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error) {
            delay(900)
            error = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.14f))
                    .border(1.dp, Accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "\u20B9", style = MaterialTheme.typography.headlineSmall, color = Accent)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Money Journal is locked",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (error) "Wrong PIN \u2014 try again" else "Enter your 4-digit PIN",
                style = MaterialTheme.typography.bodySmall,
                color = if (error) Danger else TextTertiary,
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                (0 until 4).forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (index < pin.length) Accent else Surface2)
                            .border(1.dp, OutlineFaint, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "back"),
            ).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (key.isEmpty()) Color.Transparent else Surface1)
                                .border(
                                    1.dp,
                                    if (key.isEmpty()) Color.Transparent else OutlineFaint,
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable(enabled = key.isNotEmpty()) {
                                    when (key) {
                                        "back" -> pin = pin.dropLast(1)
                                        else -> {
                                            val next = (pin + key).take(4)
                                            pin = next
                                            if (next.length == 4) {
                                                val ok = onUnlock(next)
                                                if (!ok) {
                                                    error = true
                                                    pin = ""
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (key == "back") {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Delete",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else if (key.isNotEmpty()) {
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
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Forgot your PIN?",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { showForgot = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }

    if (showForgot) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showForgot = false },
            containerColor = Surface1,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Reset the journal?") },
            text = {
                Text("The PIN can't be recovered. To get back in, app lock is turned off and all entries are cleared. Accounts and categories stay.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showForgot = false
                    onForgot()
                }) { Text("Reset", color = Danger) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showForgot = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}
