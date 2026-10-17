package com.moneyjournal.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.moneyjournal.app.ui.JournalViewModel
import com.moneyjournal.app.ui.MoneyJournalRoot
import com.moneyjournal.app.ui.theme.MoneyJournalTheme

class MainActivity : ComponentActivity() {

    private val vm: JournalViewModel by viewModels {
        JournalViewModel.Factory((application as MoneyJournalApp).repository)
    }

    private var pausedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !vm.loaded.value }
        setContent {
            MoneyJournalTheme {
                MoneyJournalRoot(vm = vm)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        pausedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (pausedAt > 0L && SystemClock.elapsedRealtime() - pausedAt > LOCK_GRACE_MS) {
            vm.lockNow()
        }
    }

    private companion object {
        const val LOCK_GRACE_MS = 20_000L
    }
}
