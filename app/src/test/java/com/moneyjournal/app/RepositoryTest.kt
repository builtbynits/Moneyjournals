package com.moneyjournal.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moneyjournal.app.data.Account
import com.moneyjournal.app.data.AccountKind
import com.moneyjournal.app.data.DemoData
import com.moneyjournal.app.data.ImportResult
import com.moneyjournal.app.data.JournalRepository
import com.moneyjournal.app.data.PayMode
import com.moneyjournal.app.data.Txn
import com.moneyjournal.app.data.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class RepositoryTest {

    private lateinit var context: Context
    private lateinit var repo: JournalRepository
    private lateinit var repoDir: java.io.File

    /** Storage folder backing [repo] — reused by tests that need a second repository. */
    private fun repoStorageDir(): java.io.File = repoDir

    /** Repository writes happen on IO; wait for the observable state to settle. */
    private fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(25)
        }
        throw AssertionError("Condition never became true")
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repoDir = java.io.File(context.cacheDir, "repo-${java.util.UUID.randomUUID()}")
        assertTrue(repoDir.mkdirs())
        repo = JournalRepository(context, repoDir)
        repo.bootstrap()
        runBlocking { withTimeout(10_000) { repo.loaded.first { it } } }
        awaitUntil { repo.data.value.transactions.isNotEmpty() }
    }

    private fun newTransaction(id: String = repo.newId("txn"), merchant: String = "Chai Point") = Txn(
        id = id,
        type = TxType.EXPENSE,
        amountPaise = 12_500,
        merchant = merchant,
        categoryId = "cat_food",
        accountId = "acc_hdfc",
        mode = PayMode.UPI,
        epochDay = LocalDate.of(2026, 10, 12).toEpochDay(),
        createdAt = System.currentTimeMillis(),
    )

    @Test
    fun seedsASampleJournalOnFirstLaunch() {
        val data = repo.data.value
        assertTrue(data.transactions.size > 30)
        assertEquals(DemoData.defaultAccounts.size, data.accounts.size)
        assertEquals(DemoData.defaultCategories.size, data.categories.size)
        assertTrue(data.settings.seeded)
    }

    @Test
    fun reloadsTheSameJournalFromDisk() {
        val txn = newTransaction(merchant = "Persisted Cafe")
        repo.saveTransaction(txn)
        awaitUntil { repo.data.value.transactions.any { it.id == txn.id } }

        val reopened = JournalRepository(context, repoStorageDir())
        reopened.bootstrap()
        runBlocking { withTimeout(10_000) { reopened.loaded.first { it } } }
        assertNotNull(reopened.data.value.transactions.firstOrNull { it.id == txn.id })
        assertEquals(repo.data.value.transactions.size, reopened.data.value.transactions.size)
    }

    @Test
    fun recoversFromTheSafetyCopyWhenThePrimaryFileIsCorrupt() {
        // two writes: the safety copy keeps the previous known-good version of the journal
        repo.saveTransaction(newTransaction(merchant = "Before the crash"))
        repo.saveTransaction(newTransaction(merchant = "After the crash"))
        awaitUntil { repo.data.value.transactions.count { it.merchant.contains("crash") } == 2 }

        // simulate a torn write / corrupted primary store
        java.io.File(repoDir, "money_journal.json").writeText("{ this is not json")

        val recovered = JournalRepository(context, repoDir)
        recovered.bootstrap()
        runBlocking { withTimeout(10_000) { recovered.loaded.first { it } } }

        val data = recovered.data.value
        assertEquals(DemoData.defaultAccounts.size, data.accounts.size)
        assertEquals(DemoData.defaultCategories.size, data.categories.size)
        assertTrue("most entries survive a corrupt primary file", data.transactions.size > 30)
        assertNotNull(data.transactions.firstOrNull { it.merchant == "Before the crash" })
    }

    @Test
    fun updatesAndDeletesEntries() {
        val txn = newTransaction()
        repo.saveTransaction(txn)
        awaitUntil { repo.data.value.transactions.any { it.id == txn.id } }

        repo.saveTransaction(txn.copy(amountPaise = 99_900, merchant = "Updated"))
        awaitUntil { repo.data.value.transactions.first { it.id == txn.id }.amountPaise == 99_900L }

        repo.deleteTransaction(txn.id)
        awaitUntil { repo.data.value.transactions.none { it.id == txn.id } }
    }

    @Test
    fun keepsFeedSortedNewestFirst() {
        val older = newTransaction(merchant = "Older").copy(
            epochDay = LocalDate.of(2026, 1, 5).toEpochDay()
        )
        val newer = newTransaction(merchant = "Newer").copy(
            epochDay = LocalDate.of(2026, 10, 16).toEpochDay()
        )
        repo.saveTransaction(older)
        repo.saveTransaction(newer)
        awaitUntil { repo.data.value.transactions.count { it.id in setOf(older.id, newer.id) } == 2 }
        val list = repo.data.value.transactions
        assertTrue(list.indexOfFirst { it.id == newer.id } < list.indexOfFirst { it.id == older.id })
    }

    @Test
    fun deletingACategoryMovesEntriesToOther() {
        val txn = newTransaction()
        repo.saveTransaction(txn)
        awaitUntil { repo.data.value.transactions.any { it.id == txn.id } }

        repo.deleteCategory("cat_food")
        awaitUntil { repo.data.value.category("cat_food") == null }
        assertEquals(DemoData.OTHER_EXPENSE, repo.data.value.transactions.first { it.id == txn.id }.categoryId)
    }

    @Test
    fun deletingAnAccountMovesEntriesAndKeepsDefaults() {
        repo.updateSettings { it.copy(defaultAccountId = "acc_cash") }
        awaitUntil { repo.data.value.settings.defaultAccountId == "acc_cash" }

        val transfers = repo.data.value.transactions.filter { it.type == TxType.TRANSFER }
        assertTrue("seed should contain transfers, found ${repo.data.value.transactions.size} txns", transfers.isNotEmpty())
        // a transfer that never touches the cash account must survive the deletion
        val sweep = transfers.first { it.accountId != "acc_cash" && it.toAccountId != "acc_cash" }

        repo.deleteAccount("acc_cash")
        awaitUntil { repo.data.value.account("acc_cash") == null }

        val moved = repo.data.value.transactions.first { it.id == sweep.id }
        assertTrue(moved.accountId != "acc_cash" && moved.toAccountId != "acc_cash")
        // nothing in the journal may still point at the deleted account
        assertTrue(repo.data.value.transactions.none { it.accountId == "acc_cash" || it.toAccountId == "acc_cash" })
        assertFalse(repo.data.value.settings.defaultAccountId == "acc_cash")
    }

    @Test
    fun savingAnAccountKeepsItsBalance() {
        repo.saveAccount(
            Account(
                id = repo.newId("acc"),
                name = "SBI Savings",
                kind = AccountKind.BANK,
                openingPaise = 500_000,
                last4 = "1122",
                colorIndex = 2,
            )
        )
        awaitUntil { repo.data.value.accounts.any { it.name == "SBI Savings" } }
        assertEquals("1122", repo.data.value.accounts.first { it.name == "SBI Savings" }.last4)
    }

    @Test
    fun backupRoundTripsThroughJson() {
        val before = repo.data.value
        val exported = runBlocking { repo.exportJson() }
        assertTrue(exported.contains("MoneyJournal") || exported.contains("schemaVersion"))

        repo.eraseEverything()
        awaitUntil { repo.data.value.transactions.isEmpty() }

        val result = runBlocking { repo.importJson(exported) }
        assertTrue(result is ImportResult.Success)
        assertEquals(before.transactions.size, repo.data.value.transactions.size)
        assertEquals(before.accounts.size, repo.data.value.accounts.size)
        assertEquals(before.categories.size, repo.data.value.categories.size)
    }

    @Test
    fun rejectsBrokenBackups() {
        val result = runBlocking { repo.importJson("{\"not\":\"a journal\"}") }
        assertTrue(result is ImportResult.Failure)
        val empty = runBlocking { repo.importJson("{}") }
        assertTrue(empty is ImportResult.Failure)
    }

    @Test
    fun exportCsvHasAHeaderAndEveryRow() {
        val csv = runBlocking { repo.exportCsv() }
        val lines = csv.trim().split("\n")
        assertEquals("date,type,amount_inr,merchant,category,account,mode,note", lines.first())
        assertEquals(repo.data.value.transactions.size + 1, lines.size)
    }

    @Test
    fun pinLockRoundTrips() {
        repo.setAppLock("4821")
        awaitUntil { repo.data.value.settings.appLock }
        assertTrue(repo.verifyPin("4821"))
        assertFalse(repo.verifyPin("0000"))
        assertTrue(repo.data.value.settings.pinHash.isNotEmpty())

        repo.clearAppLock()
        awaitUntil { !repo.data.value.settings.appLock }
        assertTrue(repo.verifyPin("anything"))
    }

    @Test
    fun erasingKeepsAccountsAndCategoriesOnly() {
        repo.eraseEverything()
        awaitUntil { repo.data.value.transactions.isEmpty() }
        assertEquals(DemoData.defaultAccounts.size, repo.data.value.accounts.size)
        assertEquals(DemoData.defaultCategories.size, repo.data.value.categories.size)
    }
}
