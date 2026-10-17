package com.moneyjournal.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class TxType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer");

    companion object {
        fun fromName(value: String?): TxType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPENSE
    }
}

@Serializable
enum class PayMode(val label: String, val short: String) {
    UPI("UPI", "UPI"),
    CASH("Cash", "Cash"),
    DEBIT_CARD("Debit Card", "Debit"),
    CREDIT_CARD("Credit Card", "Credit"),
    NET_BANKING("Net Banking", "NetBank");

    companion object {
        fun fromName(value: String?): PayMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UPI
    }
}

@Serializable
enum class AccountKind(val label: String) {
    BANK("Bank Account"),
    CASH("Cash"),
    WALLET("Wallet"),
    CARD("Credit Card");

    companion object {
        fun fromName(value: String?): AccountKind =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BANK
    }
}

@Serializable
data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind = AccountKind.BANK,
    val openingPaise: Long = 0L,
    val last4: String = "",
    val colorIndex: Int = 0,
    val archived: Boolean = false,
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val colorIndex: Int = 0,
    val type: TxType = TxType.EXPENSE,
    val system: Boolean = false,
)

@Serializable
enum class TxnSource { MANUAL, PASTED, SEED }

@Serializable
data class Txn(
    val id: String,
    val type: TxType,
    val amountPaise: Long,
    val merchant: String = "",
    val categoryId: String? = null,
    val accountId: String,
    val toAccountId: String? = null,
    val mode: PayMode = PayMode.UPI,
    val epochDay: Long,
    val note: String = "",
    val createdAt: Long = 0L,
    val source: TxnSource = TxnSource.MANUAL,
)

@Serializable
data class Settings(
    val name: String = "",
    val defaultAccountId: String? = null,
    val defaultMode: PayMode = PayMode.UPI,
    val appLock: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val hideAmounts: Boolean = false,
    val haptics: Boolean = true,
    val seeded: Boolean = false,
    val lastBackupAt: Long = 0L,
    val installStamp: Long = 0L,
)

@Serializable
data class AppData(
    val schemaVersion: Int = 1,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Txn> = emptyList(),
    val settings: Settings = Settings(),
) {
    fun account(id: String?): Account? = accounts.firstOrNull { it.id == id }
    fun category(id: String?): Category? = categories.firstOrNull { it.id == id }

    fun categoriesOf(type: TxType): List<Category> =
        categories.filter { it.type == type }.sortedBy { it.name }
}
