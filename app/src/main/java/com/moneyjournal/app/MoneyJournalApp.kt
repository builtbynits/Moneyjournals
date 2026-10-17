package com.moneyjournal.app

import android.app.Application
import com.moneyjournal.app.data.JournalRepository

class MoneyJournalApp : Application() {

    lateinit var repository: JournalRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = JournalRepository(this)
        repository.bootstrap()
    }
}
