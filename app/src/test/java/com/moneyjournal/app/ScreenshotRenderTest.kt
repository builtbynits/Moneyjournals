package com.moneyjournal.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.moneyjournal.app.data.JournalRepository
import com.moneyjournal.app.ui.AccountsScreen
import com.moneyjournal.app.ui.CategoriesScreen
import com.moneyjournal.app.ui.InsightsScreen
import com.moneyjournal.app.ui.JournalScreen
import com.moneyjournal.app.ui.JournalViewModel
import com.moneyjournal.app.ui.MoneyJournalRoot
import com.moneyjournal.app.ui.SettingsScreen
import com.moneyjournal.app.ui.theme.Ink
import com.moneyjournal.app.ui.theme.MoneyJournalTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun readyViewModel(): JournalViewModel {
        val context = ApplicationProvider.getApplicationContext<MoneyJournalApp>()
        val repo = JournalRepository(context)
        repo.bootstrap()
        runBlocking {
            withTimeout(10_000) { repo.loaded.first { it } }
            delay(400)
        }
        return JournalViewModel(repo)
    }

    @Test
    fun journal() {
        val vm = readyViewModel()
        captureRoboImage("build/screenshots/01-journal.png") {
            MoneyJournalTheme {
                Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
                    JournalScreen(vm = vm, data = vm.data.value)
                }
            }
        }
    }

    @Test
    fun insights() {
        val vm = readyViewModel()
        captureRoboImage("build/screenshots/02-insights.png") {
            MoneyJournalTheme {
                Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
                    InsightsScreen(vm = vm, data = vm.data.value, onManageCategories = {})
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp-xxhdpi")
    fun insightsTall() {
        val vm = readyViewModel()
        captureRoboImage("build/screenshots/03-insights-full.png") {
            MoneyJournalTheme {
                Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
                    InsightsScreen(vm = vm, data = vm.data.value, onManageCategories = {})
                }
            }
        }
    }

    @Test
    fun accounts() {
        val vm = readyViewModel()
        captureRoboImage("build/screenshots/04-accounts.png") {
            MoneyJournalTheme {
                Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
                    AccountsScreen(vm = vm, data = vm.data.value)
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h1600dp-xxhdpi")
    fun settings() {
        val vm = readyViewModel()
        captureRoboImage("build/screenshots/05-settings.png") {
            MoneyJournalTheme {
                Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
                    SettingsScreen(vm = vm, data = vm.data.value)
                }
            }
        }
    }

    @Test
    fun appShellWithAddSheet() {
        val vm = readyViewModel()
        composeRule.setContent {
            MoneyJournalTheme {
                MoneyJournalRoot(vm = vm)
            }
        }
        composeRule.waitForIdle()
        vm.openNewEntry()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1200)
        captureScreenRoboImage("build/screenshots/07-add-entry.png")
    }

    @Test
    fun appShellAfterPaste() {
        val vm = readyViewModel()
        composeRule.setContent {
            MoneyJournalTheme {
                MoneyJournalRoot(vm = vm)
            }
        }
        composeRule.waitForIdle()
        vm.openNewEntry()
        vm.applyPastedText(
            "Rs.1,250.00 debited from A/c XX4821 on 12-10-26 to VPA swiggy@axl (Swiggy). " +
                "UPI Ref 123456789012. -HDFC Bank"
        )
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1200)
        captureScreenRoboImage("build/screenshots/08-add-entry-pasted.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun addSheetLowerHalf() {
        val vm = readyViewModel()
        composeRule.setContent {
            MoneyJournalTheme {
                MoneyJournalRoot(vm = vm)
            }
        }
        composeRule.waitForIdle()
        vm.openNewEntry(com.moneyjournal.app.data.TxType.TRANSFER)
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(1200)
        captureScreenRoboImage("build/screenshots/09-add-entry-transfer.png")
    }

    @Test
    fun categories() {
        val vm = readyViewModel()
        captureRoboImage("build/screenshots/06-categories.png") {
            MoneyJournalTheme {
                Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
                    CategoriesScreen(vm = vm, data = vm.data.value, onBack = {})
                }
            }
        }
    }
}
