package com.moneyjournal.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moneyjournal.app.data.Account
import com.moneyjournal.app.data.AccountKind
import com.moneyjournal.app.data.Analytics
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.Category
import com.moneyjournal.app.data.ImportResult
import com.moneyjournal.app.data.JournalRepository
import com.moneyjournal.app.data.Money
import com.moneyjournal.app.data.PayMode
import com.moneyjournal.app.data.ParsedEntry
import com.moneyjournal.app.data.Settings
import com.moneyjournal.app.data.SmsParser
import com.moneyjournal.app.data.Txn
import com.moneyjournal.app.data.TxnSource
import com.moneyjournal.app.data.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class EditorState(
    val id: String?,
    val type: TxType,
    val amountInput: String,
    val merchant: String,
    val categoryId: String?,
    val accountId: String,
    val toAccountId: String?,
    val mode: PayMode,
    val date: LocalDate,
    val note: String,
    val parseNote: String? = null,
    val source: TxnSource = TxnSource.MANUAL,
) {
    val amountPaise: Long get() = Money.parseToPaise(amountInput) ?: 0L
}

class JournalViewModel(private val repo: JournalRepository) : ViewModel() {

    val data: StateFlow<AppData> = repo.data
    val loaded: StateFlow<Boolean> = repo.loaded

    val selectedMonth = MutableStateFlow(YearMonth.now())
    val query = MutableStateFlow("")
    val typeFilter = MutableStateFlow<TxType?>(null)
    val modeFilter = MutableStateFlow<PayMode?>(null)
    val accountFilter = MutableStateFlow<String?>(null)
    val categoryFilter = MutableStateFlow<String?>(null)

    val filtersActive: StateFlow<Boolean> = combine(
        typeFilter, modeFilter, accountFilter, categoryFilter
    ) { type, mode, account, category ->
        type != null || mode != null || account != null || category != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val editor = MutableStateFlow<EditorState?>(null)
    val snackbar = MutableStateFlow<String?>(null)

    val locked = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repo.loaded.collect { ready ->
                if (ready && locked.value.not() && repo.data.value.settings.appLock) {
                    locked.value = true
                }
            }
        }
        viewModelScope.launch {
            selectedMonth.value = YearMonth.now()
        }
    }

    /* ---------------- filters ---------------- */

    fun setSearch(value: String) {
        query.value = value
    }

    fun toggleTypeFilter(type: TxType?) {
        typeFilter.value = if (typeFilter.value == type) null else type
    }

    fun clearFilters() {
        typeFilter.value = null
        modeFilter.value = null
        accountFilter.value = null
        categoryFilter.value = null
    }

    fun shiftMonth(delta: Long) {
        val next = selectedMonth.value.plusMonths(delta)
        if (next.isAfter(YearMonth.now().plusMonths(0))) return
        selectedMonth.value = next
    }

    fun setMonth(month: YearMonth) {
        if (!month.isAfter(YearMonth.now())) selectedMonth.value = month
    }

    fun stepMonth(direction: Int) = shiftMonth(direction.toLong())

    fun filtered(month: YearMonth): List<Txn> {
        val snapshot = data.value
        val q = query.value.trim().lowercase()
        return Analytics.inMonth(snapshot.transactions, month)
            .filter { txn ->
                val typeOk = typeFilter.value?.let { it == txn.type } ?: true
                val modeOk = modeFilter.value?.let { it == txn.mode } ?: true
                val accountOk = accountFilter.value?.let {
                    it == txn.accountId || it == txn.toAccountId
                } ?: true
                val categoryOk = categoryFilter.value?.let { it == txn.categoryId } ?: true
                typeOk && modeOk && accountOk && categoryOk
            }
            .filter { txn ->
                if (q.isEmpty()) return@filter true
                val category = snapshot.category(txn.categoryId)?.name.orEmpty()
                val account = snapshot.account(txn.accountId)?.name.orEmpty()
                txn.merchant.lowercase().contains(q) ||
                    txn.note.lowercase().contains(q) ||
                    category.lowercase().contains(q) ||
                    account.lowercase().contains(q) ||
                    Money.formatPaise(txn.amountPaise, withSymbol = false).contains(q)
            }
    }

    fun quickCategories(limit: Int = 6): List<Category> {
        val snapshot = data.value
        val frequency = snapshot.transactions
            .filter { it.type == TxType.EXPENSE }
            .groupingBy { it.categoryId }.eachCount()
        return snapshot.categoriesOf(TxType.EXPENSE)
            .sortedByDescending { frequency[it.id] ?: 0 }
            .take(limit)
    }

    /* ---------------- editor ---------------- */

    fun openNewEntry(type: TxType = TxType.EXPENSE, prefillCategory: String? = null) {
        val snapshot = data.value
        val defaultAccount = snapshot.settings.defaultAccountId?.takeIf { snapshot.account(it) != null }
            ?: snapshot.accounts.firstOrNull()?.id
            ?: ""
        val secondAccount = snapshot.accounts.firstOrNull { it.id != defaultAccount }?.id
        editor.value = EditorState(
            id = null,
            type = type,
            amountInput = "",
            merchant = "",
            categoryId = prefillCategory,
            accountId = defaultAccount,
            toAccountId = if (type == TxType.TRANSFER) secondAccount else null,
            mode = snapshot.settings.defaultMode,
            date = LocalDate.now(),
            note = "",
        )
    }

    fun openEdit(txn: Txn) {
        editor.value = EditorState(
            id = txn.id,
            type = txn.type,
            amountInput = Money.paiseToInput(txn.amountPaise),
            merchant = txn.merchant,
            categoryId = txn.categoryId,
            accountId = txn.accountId,
            toAccountId = txn.toAccountId,
            mode = txn.mode,
            date = LocalDate.ofEpochDay(txn.epochDay),
            note = txn.note,
            source = txn.source,
        )
    }

    fun closeEditor() {
        editor.value = null
    }

    fun updateEditor(transform: (EditorState) -> EditorState) {
        editor.value = editor.value?.let(transform)
    }

    fun setType(type: TxType) = updateEditor { state ->
        val snapshot = data.value
        val category = snapshot.category(state.categoryId)
        val keepCategory = category != null && category.type == type
        val transferTarget = if (type == TxType.TRANSFER && state.toAccountId == null) {
            snapshot.accounts.firstOrNull { it.id != state.accountId }?.id
        } else {
            state.toAccountId
        }
        state.copy(
            type = type,
            categoryId = if (keepCategory) state.categoryId else null,
            toAccountId = if (type == TxType.TRANSFER) transferTarget else null,
        )
    }

    fun appendAmount(digit: Char) = updateEditor { state ->
        val current = state.amountInput
        val next = when {
            digit == '.' && current.contains('.') -> current
            digit == '.' && current.isEmpty() -> "0."
            current == "0" && digit != '.' -> digit.toString()
            current.length >= 12 -> current
            else -> {
                val decimals = current.substringAfter('.', "")
                if (current.contains('.')) {
                    if (decimals.length >= 2) current else current + digit
                } else {
                    current + digit
                }
            }
        }
        state.copy(amountInput = next)
    }

    fun backspaceAmount() = updateEditor { state ->
        state.copy(amountInput = state.amountInput.dropLast(1))
    }

    fun clearAmount() = updateEditor { state -> state.copy(amountInput = "") }

    fun setAmountInput(value: String) = updateEditor { state ->
        val cleaned = value.filter { it.isDigit() || it == '.' }
        state.copy(amountInput = cleaned.take(12))
    }

    fun setMerchant(value: String) = updateEditor { state -> state.copy(merchant = value.take(48)) }

    fun setNote(value: String) = updateEditor { state -> state.copy(note = value.take(160)) }

    fun setCategory(categoryId: String?) = updateEditor { state -> state.copy(categoryId = categoryId) }

    fun setAccount(accountId: String) = updateEditor { state ->
        val toAccount = if (state.toAccountId == accountId) {
            data.value.accounts.firstOrNull { it.id != accountId }?.id
        } else {
            state.toAccountId
        }
        state.copy(accountId = accountId, toAccountId = toAccount)
    }

    fun setToAccount(accountId: String) = updateEditor { state -> state.copy(toAccountId = accountId) }

    fun setMode(mode: PayMode) = updateEditor { state -> state.copy(mode = mode) }

    fun setDate(date: LocalDate) = updateEditor { state -> state.copy(date = date) }

    fun applyPastedText(text: String): Boolean {
        val parsed = SmsParser.parse(text) ?: return false
        val snapshot = data.value
        val current = editor.value ?: run {
            openNewEntry(parsed.type)
            editor.value
        } ?: return false
        val account = parsed.accountLast4?.let { tail ->
            snapshot.accounts.firstOrNull { it.last4 == tail && it.kind != AccountKind.CARD }
                ?: snapshot.accounts.firstOrNull { it.last4 == tail }
        }
        val category = parsed.categoryHint?.let { hint ->
            snapshot.categoriesOf(parsed.type).firstOrNull { it.name.equals(hint, true) }
                ?: snapshot.categoriesOf(parsed.type).firstOrNull {
                    it.name.contains(hint, true) || hint.contains(it.name, true)
                }
        }
        editor.value = current.copy(
            type = parsed.type,
            amountInput = Money.paiseToInput(parsed.amountPaise),
            merchant = parsed.merchant.ifBlank { current.merchant },
            categoryId = category?.id ?: current.categoryId,
            accountId = account?.id ?: current.accountId,
            mode = parsed.mode,
            date = parsed.epochDay?.let { LocalDate.ofEpochDay(it) } ?: current.date,
            note = parsed.note.ifBlank { current.note },
            parseNote = buildParseNote(parsed),
            source = TxnSource.PASTED,
        )
        return true
    }

    private fun buildParseNote(parsed: ParsedEntry): String {
        val bits = mutableListOf<String>()
        bits += parsed.mode.label
        parsed.accountLast4?.let { bits += "A/c \u2022\u2022$it" }
        bits += "${(parsed.confidence * 100).toInt()}% match"
        return "Read from clipboard \u2014 " + bits.joinToString(" \u00b7 ")
    }

    fun saveEditor(): Boolean {
        val state = editor.value ?: return false
        val amount = state.amountPaise
        if (amount <= 0L) {
            snackbar.value = "Enter an amount first"
            return false
        }
        if (state.accountId.isEmpty()) {
            snackbar.value = "Add an account first"
            return false
        }
        if (state.type != TxType.TRANSFER && state.categoryId == null) {
            snackbar.value = "Pick a category"
            return false
        }
        if (state.type == TxType.TRANSFER && (state.toAccountId == null || state.toAccountId == state.accountId)) {
            snackbar.value = "Choose two different accounts"
            return false
        }
        val existing = state.id?.let { id -> data.value.transactions.firstOrNull { it.id == id } }
        val txn = Txn(
            id = existing?.id ?: repo.newId("txn"),
            type = state.type,
            amountPaise = amount,
            merchant = state.merchant.trim(),
            categoryId = if (state.type == TxType.TRANSFER) null else state.categoryId,
            accountId = state.accountId,
            toAccountId = if (state.type == TxType.TRANSFER) state.toAccountId else null,
            mode = state.mode,
            epochDay = state.date.toEpochDay(),
            note = state.note.trim(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            source = state.source,
        )
        repo.saveTransaction(txn)
        snackbar.value = if (existing != null) "Entry updated" else "Entry saved"
        editor.value = null
        if (selectedMonth.value != YearMonth.from(state.date)) {
            selectedMonth.value = YearMonth.from(state.date)
        }
        return true
    }

    private var lastDeleted: Txn? = null

    fun deleteTransaction(id: String) {
        lastDeleted = data.value.transactions.firstOrNull { it.id == id }
        repo.deleteTransaction(id)
        snackbar.value = "Entry deleted"
        if (editor.value?.id == id) editor.value = null
    }

    fun undoDelete() {
        val restored = lastDeleted ?: return
        lastDeleted = null
        repo.saveTransaction(restored)
        snackbar.value = "Entry restored"
    }

    /* ---------------- accounts & categories ---------------- */

    fun saveAccount(
        id: String?,
        name: String,
        kind: AccountKind,
        openingPaise: Long,
        last4: String,
        colorIndex: Int,
    ) {
        if (name.isBlank()) {
            snackbar.value = "Give the account a name"
            return
        }
        repo.saveAccount(
            Account(
                id = id ?: repo.newId("acc"),
                name = name.trim(),
                kind = kind,
                openingPaise = openingPaise,
                last4 = last4.filter { it.isDigit() }.take(4),
                colorIndex = colorIndex,
            )
        )
        snackbar.value = if (id == null) "Account added" else "Account updated"
    }

    fun deleteAccount(id: String) {
        if (data.value.accounts.size <= 1) {
            snackbar.value = "Keep at least one account"
            return
        }
        repo.deleteAccount(id)
        snackbar.value = "Account removed"
    }

    fun saveCategory(id: String?, name: String, emoji: String, colorIndex: Int, type: TxType) {
        if (name.isBlank()) {
            snackbar.value = "Name the category"
            return
        }
        repo.saveCategory(
            Category(
                id = id ?: repo.newId("cat"),
                name = name.trim().take(24),
                emoji = emoji.ifBlank { "\u2728" },
                colorIndex = colorIndex,
                type = type,
                system = false,
            )
        )
        snackbar.value = if (id == null) "Category added" else "Category updated"
    }

    fun deleteCategory(id: String) {
        repo.deleteCategory(id)
        snackbar.value = "Category removed \u2014 entries moved to Other"
    }

    /* ---------------- settings ---------------- */

    fun updateSettings(transform: (Settings) -> Settings) = repo.updateSettings(transform)

    fun setAppLock(pin: String) {
        repo.setAppLock(pin)
        locked.value = false
        snackbar.value = "App lock enabled"
    }

    fun disableAppLock() {
        repo.clearAppLock()
        locked.value = false
        snackbar.value = "App lock disabled"
    }

    fun unlock(pin: String): Boolean {
        val ok = repo.verifyPin(pin)
        if (ok) locked.value = false
        return ok
    }

    fun lockNow() {
        if (data.value.settings.appLock) locked.value = true
    }

    fun resetToSampleData() {
        repo.resetToSampleData()
        selectedMonth.value = YearMonth.now()
        snackbar.value = "Sample journal restored"
    }

    fun eraseEverything() {
        repo.eraseEverything()
        selectedMonth.value = YearMonth.now()
        snackbar.value = "All entries cleared"
    }

    fun exportJson(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(repo.exportJson()) }
    }

    fun exportCsv(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(repo.exportCsv()) }
    }

    fun importJson(text: String, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = repo.importJson(text)
            if (result is ImportResult.Success) {
                selectedMonth.value = YearMonth.now()
                snackbar.value = "Restored ${result.transactions} entries"
            } else if (result is ImportResult.Failure) {
                snackbar.value = result.message
            }
            onResult(result)
        }
    }

    fun consumeSnackbar() {
        snackbar.value = null
    }

    fun suggestions(limit: Int = 4): List<String> {
        val snapshot = data.value
        return snapshot.transactions
            .filter { it.type == TxType.EXPENSE && it.merchant.isNotBlank() }
            .groupingBy { it.merchant }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit)
    }

    class Factory(private val repo: JournalRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = JournalViewModel(repo) as T
    }
}
