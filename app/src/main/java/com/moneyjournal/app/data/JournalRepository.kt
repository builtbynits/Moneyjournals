package com.moneyjournal.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.util.UUID

sealed interface ImportResult {
    data class Success(val transactions: Int, val accounts: Int, val categories: Int) : ImportResult
    data class Failure(val message: String) : ImportResult
}

/**
 * Single source of truth for the journal. Everything lives in one JSON document in
 * internal storage — no network, no cloud, no database server.
 */
class JournalRepository(
    private val context: Context,
    storageDir: File? = null,
) {

    private val filesDir: File = storageDir ?: context.filesDir

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val storeFile = File(filesDir, STORE_FILE)
    private val safetyFile = File(filesDir, SAFETY_FILE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /* ---------------- boot ---------------- */

    fun bootstrap() {
        scope.launch {
            gate.withLock {
                val onDisk = readJournal()
                val next = when {
                    onDisk == null -> DemoData.seed()
                    onDisk.accounts.isEmpty() -> onDisk.copy(accounts = DemoData.defaultAccounts)
                    onDisk.categories.isEmpty() -> onDisk.copy(categories = DemoData.defaultCategories)
                    else -> onDisk
                }
                persist(next)
                _data.value = next
                _loaded.value = true
            }
        }
    }

    /* ---------------- transactions ---------------- */

    fun saveTransaction(txn: Txn) = mutate { current ->
        val existing = current.transactions.any { it.id == txn.id }
        val list = if (existing) {
            current.transactions.map { if (it.id == txn.id) txn else it }
        } else {
            current.transactions + txn
        }
        current.copy(transactions = sort(list))
    }

    fun deleteTransaction(id: String) = mutate { current ->
        current.copy(transactions = current.transactions.filterNot { it.id == id })
    }

    fun deleteTransactions(ids: Set<String>) = mutate { current ->
        current.copy(transactions = current.transactions.filterNot { it.id in ids })
    }

    /* ---------------- accounts ---------------- */

    fun saveAccount(account: Account) = mutate { current ->
        val exists = current.accounts.any { it.id == account.id }
        val accounts = if (exists) current.accounts.map { if (it.id == account.id) account else it }
        else current.accounts + account
        val settings = if (current.settings.defaultAccountId == null) {
            current.settings.copy(defaultAccountId = account.id)
        } else {
            current.settings
        }
        current.copy(accounts = accounts, settings = settings)
    }

    fun deleteAccount(id: String) = mutate { current ->
        val remaining = current.accounts.filterNot { it.id == id }
        if (remaining.isEmpty()) return@mutate current
        val fallback = remaining.first().id
        val transactions = current.transactions.mapNotNull { txn ->
            val accountId = if (txn.accountId == id) fallback else txn.accountId
            val toAccountId = txn.toAccountId?.let { if (it == id) fallback else it }
            if (txn.type == TxType.TRANSFER && accountId == toAccountId) {
                null
            } else {
                txn.copy(accountId = accountId, toAccountId = toAccountId)
            }
        }
        val settings = if (current.settings.defaultAccountId == id) {
            current.settings.copy(defaultAccountId = fallback)
        } else {
            current.settings
        }
        current.copy(accounts = remaining, transactions = transactions, settings = settings)
    }

    /* ---------------- categories ---------------- */

    fun saveCategory(category: Category) = mutate { current ->
        val exists = current.categories.any { it.id == category.id }
        val categories = if (exists) current.categories.map { if (it.id == category.id) category else it }
        else current.categories + category
        current.copy(categories = categories)
    }

    fun deleteCategory(id: String) = mutate { current ->
        val target = current.category(id) ?: return@mutate current
        val fallback = if (target.type == TxType.INCOME) DemoData.OTHER_INCOME else DemoData.OTHER_EXPENSE
        val categories = current.categories.filterNot { it.id == id }
        val transactions = current.transactions.map { txn ->
            if (txn.categoryId == id) txn.copy(categoryId = fallback) else txn
        }
        current.copy(categories = categories, transactions = transactions)
    }

    /* ---------------- settings ---------------- */

    fun updateSettings(transform: (Settings) -> Settings) = mutate { current ->
        current.copy(settings = transform(current.settings))
    }

    fun setAppLock(pin: String) = updateSettings { settings ->
        val salt = PinSecurity.newSalt()
        settings.copy(appLock = true, pinSalt = salt, pinHash = PinSecurity.hash(pin, salt))
    }

    fun clearAppLock() = updateSettings { it.copy(appLock = false, pinHash = "", pinSalt = "") }

    fun verifyPin(pin: String): Boolean {
        val settings = _data.value.settings
        if (!settings.appLock || settings.pinHash.isEmpty()) return true
        return PinSecurity.hash(pin, settings.pinSalt) == settings.pinHash
    }

    /* ---------------- data lifecycle ---------------- */

    fun resetToSampleData() = mutate { DemoData.seed() }

    fun eraseEverything() = mutate { DemoData.freshStart() }

    fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().take(8)}"

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        gate.withLock {
            val stamped = _data.value.copy(
                settings = _data.value.settings.copy(lastBackupAt = System.currentTimeMillis())
            )
            persist(stamped)
            _data.value = stamped
            json.encodeToString(AppData.serializer(), stamped)
        }
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.Default) {
        val current = _data.value
        val categories = current.categories.associateBy { it.id }
        val accounts = current.accounts.associateBy { it.id }
        val header = "date,type,amount_inr,merchant,category,account,mode,note"
        val rows = current.transactions.sortedByDescending { it.epochDay }.map { txn ->
            val date = LocalDate.ofEpochDay(txn.epochDay).toString()
            val amount = Money.formatPaise(txn.amountPaise, withSymbol = false, forceDecimals = true)
            listOf(
                date,
                txn.type.name.lowercase(),
                amount,
                txn.merchant,
                categories[txn.categoryId]?.name ?: "",
                accounts[txn.accountId]?.name ?: "",
                txn.mode.label,
                txn.note,
            ).joinToString(",") { csvEscape(it) }
        }
        (listOf(header) + rows).joinToString("\n")
    }

    suspend fun importJson(text: String): ImportResult = withContext(Dispatchers.IO) {
        val parsed = runCatching { json.decodeFromString<AppData>(text) }.getOrNull()
            ?: return@withContext ImportResult.Failure("That file is not a Money Journal backup.")
        if (parsed.accounts.isEmpty() && parsed.transactions.isEmpty()) {
            return@withContext ImportResult.Failure("The backup looks empty — nothing to restore.")
        }
        val accounts = parsed.accounts.ifEmpty { DemoData.defaultAccounts }
        val categories = parsed.categories.ifEmpty { DemoData.defaultCategories }
        val validAccounts = accounts.map { it.id }.toSet()
        val validCategories = categories.map { it.id }.toSet()
        val fallbackAccount = accounts.first().id
        val transactions = parsed.transactions.map { txn ->
            txn.copy(
                accountId = if (txn.accountId in validAccounts) txn.accountId else fallbackAccount,
                toAccountId = txn.toAccountId?.takeIf { it in validAccounts },
                categoryId = txn.categoryId?.takeIf { it in validCategories },
            )
        }
        val restored = AppData(
            schemaVersion = parsed.schemaVersion,
            accounts = accounts,
            categories = categories,
            transactions = sort(transactions),
            settings = parsed.settings.copy(
                appLock = _data.value.settings.appLock,
                pinHash = _data.value.settings.pinHash,
                pinSalt = _data.value.settings.pinSalt,
                defaultAccountId = parsed.settings.defaultAccountId?.takeIf { it in validAccounts }
                    ?: fallbackAccount,
            ),
        )
        gate.withLock {
            persist(restored)
            _data.value = restored
        }
        ImportResult.Success(restored.transactions.size, accounts.size, categories.size)
    }

    /* ---------------- internals ---------------- */

    private fun mutate(block: (AppData) -> AppData) {
        scope.launch {
            gate.withLock {
                val next = block(_data.value)
                // Write to disk first: a mutation is only visible once it is durable, so a
                // second reader (restore, backup, another instance) never sees a stale file.
                persist(next)
                _data.value = next
            }
        }
    }

    private fun sort(list: List<Txn>): List<Txn> =
        list.sortedWith(compareByDescending<Txn> { it.epochDay }.thenByDescending { it.createdAt })

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /** Falls back to the safety copy if the primary file is missing or corrupt. */
    private fun readJournal(): AppData? {
        val primary = runCatching {
            json.decodeFromString(AppData.serializer(), storeFile.readText())
        }.getOrNull()
        if (primary != null) return primary
        val safety = runCatching {
            json.decodeFromString(AppData.serializer(), safetyFile.readText())
        }.getOrNull()
        if (safety != null) {
            android.util.Log.w("MoneyJournal", "Primary journal unreadable - restored from safety copy")
        }
        return safety
    }

    private fun persist(next: AppData) {
        runCatching {
            if (storeFile.exists()) {
                storeFile.copyTo(safetyFile, overwrite = true)
            }
            val temp = File(filesDir, "$STORE_FILE.tmp")
            temp.writeText(json.encodeToString(AppData.serializer(), next))
            if (!temp.renameTo(storeFile)) {
                storeFile.writeText(temp.readText())
                temp.delete()
            }
        }.onFailure { error ->
            android.util.Log.w("MoneyJournal", "Couldn't write journal to disk", error)
        }
    }

    companion object {
        private const val STORE_FILE = "money_journal.json"
        private const val SAFETY_FILE = "money_journal.safety.json"
        const val BACKUP_FILE_NAME = "money-journal-backup"
    }
}

object PinSecurity {
    private val random = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray())
        digest.update(pin.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
