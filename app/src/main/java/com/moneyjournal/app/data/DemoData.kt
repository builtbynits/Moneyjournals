package com.moneyjournal.app.data

import java.time.LocalDate
import kotlin.random.Random

object DemoData {

    const val OTHER_EXPENSE = "cat_other_expense"
    const val OTHER_INCOME = "cat_other_income"

    val defaultCategories: List<Category> = listOf(
        Category("cat_food", "Food & Dining", "\uD83C\uDF5C", 1, TxType.EXPENSE, true),
        Category("cat_groceries", "Groceries", "\uD83D\uDED2", 0, TxType.EXPENSE, true),
        Category("cat_transport", "Transport", "\uD83D\uDE95", 9, TxType.EXPENSE, true),
        Category("cat_shopping", "Shopping", "\uD83D\uDECD", 3, TxType.EXPENSE, true),
        Category("cat_bills", "Bills & Utilities", "\uD83D\uDCA1", 8, TxType.EXPENSE, true),
        Category("cat_rent", "Rent", "\uD83C\uDFE0", 4, TxType.EXPENSE, true),
        Category("cat_fun", "Entertainment", "\uD83C\uDFAC", 6, TxType.EXPENSE, true),
        Category("cat_health", "Health", "\uD83D\uDC8A", 5, TxType.EXPENSE, true),
        Category("cat_education", "Education", "\uD83C\uDF93", 2, TxType.EXPENSE, true),
        Category("cat_invest", "Investments", "\uD83D\uDCC8", 0, TxType.EXPENSE, true),
        Category("cat_cash", "Cash Withdrawal", "\uD83C\uDFE7", 7, TxType.EXPENSE, true),
        Category("cat_transfer", "Transfers", "\uD83D\uDD01", 2, TxType.EXPENSE, true),
        Category(OTHER_EXPENSE, "Other", "\u2728", 7, TxType.EXPENSE, true),
        Category("cat_salary", "Salary", "\uD83D\uDCBC", 0, TxType.INCOME, true),
        Category("cat_freelance", "Freelance", "\uD83E\uDDD1\u200D\uD83D\uDCBB", 2, TxType.INCOME, true),
        Category("cat_interest", "Interest", "\uD83C\uDFE6", 5, TxType.INCOME, true),
        Category("cat_refund", "Refunds", "\uD83D\uDCB8", 4, TxType.INCOME, true),
        Category("cat_gift", "Gifts", "\uD83C\uDF81", 3, TxType.INCOME, true),
        Category(OTHER_INCOME, "Other Income", "\u2728", 7, TxType.INCOME, true),
    )

    val defaultAccounts: List<Account> = listOf(
        Account("acc_hdfc", "HDFC Savings", AccountKind.BANK, 42_500_00L, "4821", 0),
        Account("acc_icici", "ICICI Salary", AccountKind.BANK, 1_56_000_00L, "7733", 1),
        Account("acc_cash", "Cash Wallet", AccountKind.CASH, 4_500_00L, "", 3),
        Account("acc_card", "HDFC Credit Card", AccountKind.CARD, 0L, "9014", 2),
    )

    private data class Template(
        val merchant: String,
        val category: String,
        val mode: PayMode,
        val account: String,
        val minRupees: Int,
        val maxRupees: Int,
        val weight: Int = 3,
    )

    private val templates = listOf(
        Template("Swiggy", "cat_food", PayMode.UPI, "acc_hdfc", 180, 720, 5),
        Template("Zomato", "cat_food", PayMode.UPI, "acc_hdfc", 220, 890, 4),
        Template("Blinkit", "cat_groceries", PayMode.UPI, "acc_hdfc", 240, 1400, 4),
        Template("BigBasket", "cat_groceries", PayMode.UPI, "acc_icici", 700, 3200, 2),
        Template("DMart", "cat_groceries", PayMode.DEBIT_CARD, "acc_hdfc", 900, 4200, 2),
        Template("Chai Point", "cat_food", PayMode.UPI, "acc_cash", 40, 180, 3),
        Template("Uber", "cat_transport", PayMode.UPI, "acc_hdfc", 90, 480, 4),
        Template("Ola Cabs", "cat_transport", PayMode.UPI, "acc_cash", 80, 420, 3),
        Template("IndianOil Petrol", "cat_transport", PayMode.CREDIT_CARD, "acc_card", 1500, 3000, 2),
        Template("Metro Recharge", "cat_transport", PayMode.UPI, "acc_hdfc", 100, 500, 2),
        Template("Amazon", "cat_shopping", PayMode.CREDIT_CARD, "acc_card", 350, 5200, 3),
        Template("Flipkart", "cat_shopping", PayMode.UPI, "acc_hdfc", 400, 3800, 2),
        Template("Myntra", "cat_shopping", PayMode.CREDIT_CARD, "acc_card", 700, 3400, 2),
        Template("Airtel Postpaid", "cat_bills", PayMode.NET_BANKING, "acc_icici", 499, 999, 2),
        Template("BESCOM Electricity", "cat_bills", PayMode.NET_BANKING, "acc_icici", 640, 2100, 2),
        Template("ACT Fibernet", "cat_bills", PayMode.NET_BANKING, "acc_icici", 799, 1199, 1),
        Template("Netflix", "cat_fun", PayMode.CREDIT_CARD, "acc_card", 199, 649, 2),
        Template("BookMyShow", "cat_fun", PayMode.UPI, "acc_hdfc", 240, 1200, 2),
        Template("Apollo Pharmacy", "cat_health", PayMode.UPI, "acc_hdfc", 180, 1600, 2),
        Template("Groww SIP", "cat_invest", PayMode.NET_BANKING, "acc_icici", 2500, 10000, 2),
        Template("Cult.fit", "cat_health", PayMode.CREDIT_CARD, "acc_card", 999, 2499, 1),
        Template("IRCTC", "cat_transport", PayMode.UPI, "acc_hdfc", 320, 2400, 1),
        Template("Amazon Prime", "cat_fun", PayMode.CREDIT_CARD, "acc_card", 299, 1499, 1),
        Template("Starbucks", "cat_food", PayMode.CREDIT_CARD, "acc_card", 260, 780, 2),
        Template("Local Kirana", "cat_groceries", PayMode.CASH, "acc_cash", 60, 640, 4),
        Template("Auto Rickshaw", "cat_transport", PayMode.CASH, "acc_cash", 30, 220, 4),
    )

    private val notes = listOf(
        "", "", "", "with friends", "office lunch", "monthly", "weekend", "urgent",
    )

    /**
     * Builds a fresh journal: 3 months of believable Indian transactions that always
     * line up with the device's current date so the month selector has real history.
     */
    fun seed(today: LocalDate = LocalDate.now()): AppData {
        val random = Random(17)
        val transactions = mutableListOf<Txn>()

        fun add(
            date: LocalDate,
            type: TxType,
            rupees: Int,
            merchant: String,
            category: String?,
            account: String,
            mode: PayMode,
            toAccount: String? = null,
            note: String = "",
        ) {
            transactions += Txn(
                id = "seed_${date.toEpochDay()}_${transactions.size}",
                type = type,
                amountPaise = rupees.toLong() * 100,
                merchant = merchant,
                categoryId = category,
                accountId = account,
                toAccountId = toAccount,
                mode = mode,
                epochDay = date.toEpochDay(),
                note = note,
                createdAt = date.toEpochDay() * 86_400_000L,
                source = TxnSource.SEED,
            )
        }

        for (monthOffset in 2 downTo 0) {
            val anchor = today.minusMonths(monthOffset.toLong())
            val lastDay = when {
                monthOffset == 0 -> today.dayOfMonth
                else -> anchor.lengthOfMonth()
            }
            val budgeted = if (monthOffset == 0) 20 else 26

            // Salary on the 1st
            add(anchor.withDayOfMonth(1), TxType.INCOME, 85_000, "Nexora Technologies", "cat_salary",
                "acc_icici", PayMode.NET_BANKING, note = "Monthly salary")
            add(anchor.withDayOfMonth(2), TxType.EXPENSE, 28_000, "Landlord - Sharma", "cat_rent",
                "acc_icici", PayMode.NET_BANKING, note = "House rent")
            if (random.nextBoolean()) {
                add(
                    anchor.withDayOfMonth((3..minOf(12, lastDay)).random(random)),
                    TxType.INCOME, (6..18).random(random) * 1000, "Upwork Client", "cat_freelance",
                    "acc_hdfc", PayMode.NET_BANKING, note = "Design retainer",
                )
            }
            add(
                anchor.withDayOfMonth(minOf(9, lastDay)), TxType.INCOME, 1_240 + monthOffset * 37,
                "HDFC Bank Interest", "cat_interest", "acc_hdfc", PayMode.NET_BANKING,
            )
            add(
                anchor.withDayOfMonth(minOf(6, lastDay)), TxType.TRANSFER, 12_000,
                "Transfer to HDFC Savings", null, "acc_icici", PayMode.NET_BANKING, "acc_hdfc",
                "Monthly sweep",
            )
            add(
                anchor.withDayOfMonth(minOf(4, lastDay)), TxType.TRANSFER, 6_000,
                "Cash top-up", null, "acc_hdfc", PayMode.CASH, "acc_cash", "For daily spends",
            )
            add(
                anchor.withDayOfMonth(minOf(19, lastDay)), TxType.TRANSFER, 5_000,
                "Cash top-up", null, "acc_hdfc", PayMode.CASH, "acc_cash", "Weekend cash",
            )
            add(
                anchor.withDayOfMonth(minOf(15, lastDay)), TxType.TRANSFER, 2_500,
                "ATM withdrawal", null, "acc_hdfc", PayMode.CASH, "acc_cash", "HDFC ATM, Andheri",
            )

            var produced = 0
            var guard = 0
            while (produced < budgeted && guard < 400) {
                guard++
                val date = anchor.withDayOfMonth((1..lastDay).random(random))
                if (monthOffset == 0 && date.isAfter(today)) continue
                val template = weightedPick(templates, random)
                val rupees = (template.minRupees..template.maxRupees).random(random)
                add(
                    date, TxType.EXPENSE, rupees, template.merchant, template.category,
                    template.account, template.mode, note = notes.random(random),
                )
                produced++
            }
            if (monthOffset == 1) {
                add(anchor.withDayOfMonth(minOf(21, lastDay)), TxType.INCOME, 1_899, "Amazon Refund",
                    "cat_refund", "acc_card", PayMode.CREDIT_CARD, note = "Returned shoes")
            }
        }

        return AppData(
            accounts = defaultAccounts,
            categories = defaultCategories,
            transactions = transactions.sortedByDescending { it.epochDay },
            settings = Settings(
                name = "",
                defaultAccountId = "acc_hdfc",
                defaultMode = PayMode.UPI,
                seeded = true,
                installStamp = System.currentTimeMillis(),
            ),
        )
    }

    private fun weightedPick(items: List<Template>, random: Random): Template {
        val total = items.sumOf { it.weight }
        var roll = random.nextInt(total)
        for (item in items) {
            roll -= item.weight
            if (roll < 0) return item
        }
        return items.last()
    }

    fun freshStart(): AppData = AppData(
        accounts = defaultAccounts,
        categories = defaultCategories,
        transactions = emptyList(),
        settings = Settings(
            defaultAccountId = defaultAccounts.first().id,
            defaultMode = PayMode.UPI,
            seeded = false,
            installStamp = System.currentTimeMillis(),
        ),
    )
}
