package com.goraya.constructionkhata.logic

import com.goraya.constructionkhata.data.*

data class AllData(
    val categories: List<Category> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val items: List<Item> = emptyList(),
    val locations: List<Location> = emptyList(),
    val parties: List<Party> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val cashIns: List<CashIn> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val diary: List<DiaryEntry> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
    val audit: List<AuditLog> = emptyList()
) {
    val budget: Long get() = settings["budget"]?.toLongOrNull() ?: 0L
}

object Kind {
    const val PAYMENT = "PAYMENT"
    const val ADVANCE = "ADVANCE"
    const val REFUND = "REFUND"
    val all = listOf(PAYMENT, ADVANCE, REFUND)
    fun label(k: String) = when (k) {
        PAYMENT -> "ادائیگی"
        ADVANCE -> "ایڈوانس"
        else -> "رقم واپس ملی (وصولی)"
    }
}

object Src {
    const val OPENING = "OPENING"
    const val OWNER = "OWNER"
    const val OTHER = "OTHER"
    val all = listOf(OPENING, OWNER, OTHER)
    fun label(s: String) = when (s) {
        OPENING -> "ابتدائی بیلنس"
        OWNER -> "مالک کی طرف سے جمع"
        else -> "دیگر وصولی"
    }
}

object Calc {
    const val CASH = "نقد"
    val ACCOUNTS = listOf("نقد", "بینک", "موبائل اکاؤنٹ")
    val DEFAULT_UNITS = listOf(
        "مربع فٹ", "مربع میٹر", "مکعب فٹ", "مکعب میٹر", "فٹ", "میٹر", "کلوگرام", "من", "ٹن", "بیگ", "اینٹ",
        "عدد", "لیٹر", "دن", "گھنٹہ", "چکر", "ٹھیکہ", "دیگر"
    )
    const val FIXED_UNIT = "مقررہ رقم"

    fun units(d: AllData): List<String> =
        DEFAULT_UNITS + (d.settings["units"]?.split("|")?.filter { it.isNotBlank() } ?: emptyList())

    /** کیش پر اثر: REFUND = رقم آئی، باقی = رقم گئی */
    fun effect(p: Payment): Long = if (p.kind == Kind.REFUND) p.amount else -p.amount

    /** اکاؤنٹ کا بیلنس دن (شامل) تک */
    fun balanceUpTo(account: String, day: Long, d: AllData): Long {
        var b = 0L
        for (c in d.cashIns) if (c.account == account && c.date <= day) b += c.amount
        for (p in d.payments) if (p.method == account && p.date <= day) b += effect(p)
        for (t in d.transfers) if (t.date <= day) {
            if (t.toAccount == account) b += t.amount
            if (t.fromAccount == account) b -= t.amount
        }
        return b
    }

    fun paidFor(expenseId: Long, payments: List<Payment>): Long =
        payments.filter { it.expenseId == expenseId && it.kind != Kind.REFUND }.sumOf { it.amount }

    fun status(total: Long, paid: Long): String = when {
        paid <= 0 -> "مکمل ادھار"
        paid >= total -> "مکمل ادا شدہ"
        else -> "جزوی ادائیگی"
    }

    /** (کل خریداری/مزدوری, خالص ادائیگی, بقایا) */
    fun partyTotals(partyId: Long, d: AllData): Triple<Long, Long, Long> {
        val total = d.expenses.filter { it.partyId == partyId }.sumOf { it.total }
        val ps = d.payments.filter { it.partyId == partyId }
        val paid = ps.filter { it.kind != Kind.REFUND }.sumOf { it.amount } - ps.filter { it.kind == Kind.REFUND }.sumOf { it.amount }
        return Triple(total, paid, total - paid)
    }

    fun partyDue(partyId: Long, d: AllData): Long = partyTotals(partyId, d).third

    data class Dashboard(
        val cash: Long, val todayReceipts: Long, val todayCashPaid: Long, val todayExpenses: Long,
        val totalSpend: Long, val materialSpend: Long, val labourSpend: Long,
        val vendorDue: Long, val workerDue: Long, val ownerDeposits: Long,
        val budget: Long, val budgetLeft: Long
    )

    fun dashboard(d: AllData, today: Long): Dashboard {
        val todayPays = d.payments.filter { it.method == CASH && it.date == today }
        val receipts = d.cashIns.filter { it.account == CASH && it.date == today }.sumOf { it.amount } +
            todayPays.filter { it.kind == Kind.REFUND }.sumOf { it.amount }
        val paid = todayPays.filter { it.kind != Kind.REFUND }.sumOf { it.amount }
        val total = d.expenses.sumOf { it.total }
        val labour = d.expenses.filter { it.isLabour }.sumOf { it.total }
        fun due(type: String) = d.parties.filter { it.type == type }.map { partyDue(it.id, d) }.filter { it > 0 }.sum()
        return Dashboard(
            cash = balanceUpTo(CASH, Long.MAX_VALUE, d),
            todayReceipts = receipts, todayCashPaid = paid,
            todayExpenses = d.expenses.filter { it.date == today }.sumOf { it.total },
            totalSpend = total, materialSpend = total - labour, labourSpend = labour,
            vendorDue = due("VENDOR"), workerDue = due("WORKER"),
            ownerDeposits = d.cashIns.filter { it.source == Src.OWNER }.sumOf { it.amount },
            budget = d.budget, budgetLeft = d.budget - total
        )
    }

    data class Recent(val text: String, val amount: Long, val date: Long, val stamp: Long, val incoming: Boolean)

    fun recent(d: AllData, n: Int): List<Recent> {
        val list = mutableListOf<Recent>()
        d.expenses.forEach { list += Recent("خرچہ: ${it.itemName}", it.total, it.date, it.createdAt, false) }
        d.payments.forEach {
            list += Recent("${Kind.label(it.kind)} (${it.method})", it.amount, it.date, it.createdAt, it.kind == Kind.REFUND)
        }
        d.cashIns.forEach { list += Recent(Src.label(it.source), it.amount, it.date, it.createdAt, true) }
        return list.sortedWith(compareByDescending<Recent> { it.date }.thenByDescending { it.stamp }).take(n)
    }
}
