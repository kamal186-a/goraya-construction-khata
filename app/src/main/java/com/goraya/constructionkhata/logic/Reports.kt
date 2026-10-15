package com.goraya.constructionkhata.logic

import com.goraya.constructionkhata.data.Expense

/** moneyCols میں شامل کالم کی قیمتیں خام ہندسوں میں ہوتی ہیں؛ اسکرین/PDF پر کاما لگتا ہے، CSV میں خام رہتی ہیں۔ */
data class ReportTable(
    val title: String,
    val subtitle: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val footer: List<String>? = null,
    val moneyCols: Set<Int> = emptySet()
) {
    fun cell(raw: String, col: Int): String = if (col in moneyCols) Money.group(raw) else raw
}

object Reports {
    enum class Kind(val title: String, val ranged: Boolean) {
        STATEMENT("کھاتہ / اخراجات کی فہرست", true),
        CASH("کیش وصولی اور ادائیگی", true),
        TOTAL("کل تعمیراتی خرچہ (کالم کے حساب سے)", false),
        MARBLE("ماربل اور فرش کا خرچہ", true),
        WASHROOM("واش روم کا خرچہ", true),
        PLUMBING("پلمبنگ (پانی اور سیوریج) کا خرچہ", true),
        ELECTRIC("بجلی کا خرچہ", true),
        LABOUR("مزدوری کا خرچہ", true),
        VENDOR("دکاندار کے حساب کی رپورٹ", false),
        OUTSTANDING("ادھار اور بقایا رپورٹ", false),
        LOCATION("جگہ کے حساب سے خرچہ", true),
        BUDGET("بجٹ بمقابلہ اصل خرچہ", false)
    }

    fun locName(d: AllData, id: Long?) = d.locations.firstOrNull { it.id == id }?.name ?: "-"
    fun partyName(d: AllData, id: Long?) = d.parties.firstOrNull { it.id == id }?.name ?: "-"
    fun catName(d: AllData, id: Long?) = d.categories.firstOrNull { it.id == id }?.name ?: "دیگر"

    fun build(kind: Kind, from: Long, to: Long, d: AllData): ReportTable {
        val range = "${Dates.fmt(from)} سے ${Dates.fmt(to)} تک"
        val inRange = d.expenses.filter { it.date in from..to }
        fun byCode(code: String) = inRange.filter { e -> d.categories.firstOrNull { it.id == e.categoryId }?.code == code }
        return when (kind) {
            Kind.STATEMENT -> statement(kind.title, range, inRange, d)
            Kind.MARBLE -> statement(kind.title, range, byCode("E"), d)
            Kind.WASHROOM -> statement(kind.title, range, byCode("F"), d)
            Kind.PLUMBING -> statement(kind.title, range, byCode("G"), d)
            Kind.ELECTRIC -> statement(kind.title, range, byCode("H"), d)
            Kind.LABOUR -> statement(kind.title, range, inRange.filter { it.isLabour }, d)
            Kind.CASH -> cash(range, from, to, d)
            Kind.TOTAL -> grouped(kind.title, "تمام تاریخیں", "کالم", d.expenses) { catName(d, it.categoryId) }
            Kind.LOCATION -> grouped(kind.title, range, "جگہ", inRange) { locName(d, it.locationId).let { n -> if (n == "-") "جگہ منتخب نہیں" else n } }
            Kind.VENDOR -> partyReport(kind.title, d, "VENDOR", onlyDue = false)
            Kind.OUTSTANDING -> partyReport(kind.title, d, null, onlyDue = true)
            Kind.BUDGET -> budget(d)
        }
    }

    private fun statement(title: String, sub: String, list: List<Expense>, d: AllData): ReportTable {
        val rows = list.sortedBy { it.date }.map { e ->
            val paid = Calc.paidFor(e.id, d.payments)
            listOf(Dates.fmt(e.date), e.itemName, locName(d, e.locationId), partyName(d, e.partyId),
                e.total.toString(), paid.toString(), (e.total - paid).toString())
        }
        val tot = list.sumOf { it.total }
        val paid = list.sumOf { Calc.paidFor(it.id, d.payments) }
        return ReportTable(title, sub, listOf("تاریخ", "آئٹم", "جگہ", "دکاندار / کاریگر", "کل رقم", "ادا شدہ", "بقایا"),
            rows, listOf("", "کل", "", "", tot.toString(), paid.toString(), (tot - paid).toString()), setOf(4, 5, 6))
    }

    private fun cash(sub: String, from: Long, to: Long, d: AllData): ReportTable {
        class L(val date: Long, val text: String, val inn: Long, val out: Long)
        val ls = mutableListOf<L>()
        d.cashIns.filter { it.account == Calc.CASH && it.date in from..to }
            .forEach { ls += L(it.date, Src.label(it.source) + if (it.note.isBlank()) "" else " - ${it.note}", it.amount, 0) }
        d.payments.filter { it.method == Calc.CASH && it.date in from..to }.forEach {
            val who = partyName(d, it.partyId).let { n -> if (n == "-") "" else " - $n" }
            if (it.kind == Kind.REFUND) ls += L(it.date, Kind.label(it.kind) + who, it.amount, 0)
            else ls += L(it.date, Kind.label(it.kind) + who, 0, it.amount)
        }
        d.transfers.filter { it.date in from..to }.forEach {
            if (it.toAccount == Calc.CASH) ls += L(it.date, "منتقلی ${it.fromAccount} سے", it.amount, 0)
            if (it.fromAccount == Calc.CASH) ls += L(it.date, "منتقلی ${it.toAccount} کو", 0, it.amount)
        }
        val opening = Calc.balanceUpTo(Calc.CASH, from - 1, d)
        val tin = ls.sumOf { it.inn }
        val tout = ls.sumOf { it.out }
        val rows = mutableListOf(listOf("", "ابتدائی کیش", opening.toString(), ""))
        rows += ls.sortedBy { it.date }.map { listOf(Dates.fmt(it.date), it.text, it.inn.toString(), it.out.toString()) }
        rows += listOf("", "اختتامی کیش", (opening + tin - tout).toString(), "")
        return ReportTable("کیش وصولی اور ادائیگی", sub, listOf("تاریخ", "تفصیل", "وصولی", "ادائیگی"), rows,
            listOf("", "کل", tin.toString(), tout.toString()), setOf(2, 3))
    }

    private fun grouped(title: String, sub: String, h: String, list: List<Expense>, key: (Expense) -> String): ReportTable {
        val g = list.groupBy(key).map { (k, v) -> Triple(k, v.size, v.sumOf { it.total }) }.sortedByDescending { it.third }
        return ReportTable(title, sub, listOf(h, "اندراجات", "کل رقم"),
            g.map { listOf(it.first, it.second.toString(), it.third.toString()) },
            listOf("کل", g.sumOf { it.second }.toString(), g.sumOf { it.third }.toString()), setOf(2))
    }

    private fun partyReport(title: String, d: AllData, type: String?, onlyDue: Boolean): ReportTable {
        val ps = d.parties.filter { type == null || it.type == type }
            .map { it to Calc.partyTotals(it.id, d) }
            .filter { !onlyDue || it.second.third != 0L }
        return ReportTable(title, "تمام تاریخیں", listOf("نام", "قسم", "کل", "ادا شدہ", "بقایا"),
            ps.map { (p, t) -> listOf(p.name, if (p.type == "VENDOR") "دکاندار" else "کاریگر", t.first.toString(), t.second.toString(), t.third.toString()) },
            listOf("کل", "", ps.sumOf { it.second.first }.toString(), ps.sumOf { it.second.second }.toString(), ps.sumOf { it.second.third }.toString()),
            setOf(2, 3, 4))
    }

    private fun budget(d: AllData): ReportTable {
        val dash = Calc.dashboard(d, Dates.today())
        val rows = mutableListOf(
            listOf("تخمینی بجٹ", dash.budget.toString()),
            listOf("اب تک کا حقیقی خرچہ", dash.totalSpend.toString()),
            listOf(if (dash.budgetLeft >= 0) "باقی بجٹ" else "بجٹ سے زیادہ خرچہ", Math.abs(dash.budgetLeft).toString()),
            listOf("سامان پر خرچہ", dash.materialSpend.toString()),
            listOf("مزدوری پر خرچہ", dash.labourSpend.toString()),
            listOf("دکانداروں کا بقایا", dash.vendorDue.toString()),
            listOf("کاریگروں کا بقایا", dash.workerDue.toString())
        )
        return ReportTable("بجٹ بمقابلہ اصل خرچہ", "تمام تاریخیں", listOf("تفصیل", "رقم"), rows, null, setOf(1))
    }
}

object Csv {
    private fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
    fun build(t: ReportTable): String {
        val sb = StringBuilder()
        sb.append(q(t.title)).append("\r\n")
        if (t.subtitle.isNotBlank()) sb.append(q(t.subtitle)).append("\r\n")
        sb.append(t.headers.joinToString(",") { q(it) }).append("\r\n")
        t.rows.forEach { r -> sb.append(r.joinToString(",") { q(it) }).append("\r\n") }
        t.footer?.let { f -> sb.append(f.joinToString(",") { q(it) }).append("\r\n") }
        return sb.toString()
    }
}
