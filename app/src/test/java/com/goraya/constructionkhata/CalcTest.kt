package com.goraya.constructionkhata

import com.goraya.constructionkhata.data.*
import com.goraya.constructionkhata.logic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcTest {
    private fun exp(id: Long, total: Long, party: Long? = 1, day: Long = 100, labour: Boolean = false) =
        Expense(id = id, date = day, itemName = "ماربل", partyId = party, rate = total, total = total, isLabour = labour)

    private fun pay(id: Long, amt: Long, exp: Long? = null, method: String = "نقد", kind: String = Kind.PAYMENT, day: Long = 100, party: Long? = 1) =
        Payment(id = id, date = day, partyId = party, expenseId = exp, amount = amt, method = method, kind = kind)

    private fun cashIn(amt: Long, day: Long = 100) = CashIn(date = day, amount = amt, source = Src.OWNER)
    private val vendor = Party(id = 1, name = "احمد ماربل", type = "VENDOR")
    private val worker = Party(id = 2, name = "مستری اکرم", type = "WORKER")

    @Test fun quantityTimesRate() {
        assertEquals(2500L, Money.total(2.5, 1000))
        assertEquals(18000L, Money.total(12.0, 1500))
        assertEquals(2L, Money.total(0.5, 3))
    }

    @Test fun parseUrduDigitsAndLimits() {
        assertEquals(1500L, Money.parse("۱٬۵۰۰"))
        assertEquals(null, Money.parse("abc"))
    }

    @Test fun specExampleCashBalance() {
        val d = AllData(
            cashIns = listOf(cashIn(100000)),
            payments = listOf(pay(1, 20000), pay(2, 5000), pay(3, 3000))
        )
        assertEquals(72000L, Calc.balanceUpTo(Calc.CASH, Long.MAX_VALUE, d))
    }

    @Test fun creditPurchaseDoesNotReduceCash() {
        val d = AllData(parties = listOf(vendor), cashIns = listOf(cashIn(100000)), expenses = listOf(exp(1, 50000)))
        val s = Calc.dashboard(d, 100)
        assertEquals(100000L, s.cash)
        assertEquals(50000L, s.totalSpend)
        assertEquals(50000L, s.vendorDue)
        assertEquals("مکمل ادھار", Calc.status(50000, 0))
    }

    @Test fun partialPaymentThenOldDebtPaidNoDoubleCount() {
        val e = exp(1, 50000)
        var d = AllData(parties = listOf(vendor), cashIns = listOf(cashIn(100000)), expenses = listOf(e), payments = listOf(pay(1, 20000, 1)))
        assertEquals(80000L, Calc.dashboard(d, 100).cash)
        assertEquals(30000L, Calc.partyDue(1, d))
        assertEquals("جزوی ادائیگی", Calc.status(50000, Calc.paidFor(1, d.payments)))
        d = d.copy(payments = d.payments + pay(2, 30000, 1, day = 105))
        val s = Calc.dashboard(d, 105)
        assertEquals(50000L, s.cash)
        assertEquals(0L, s.vendorDue)
        assertEquals(50000L, s.totalSpend)
        assertEquals("مکمل ادا شدہ", Calc.status(50000, Calc.paidFor(1, d.payments)))
    }

    @Test fun labourWithAdvance() {
        val total = Money.total(5.0, 2000)
        assertEquals(10000L, total)
        val d = AllData(
            parties = listOf(worker),
            expenses = listOf(exp(1, total, party = 2, labour = true)),
            payments = listOf(pay(1, 3000, 1, kind = Kind.ADVANCE, party = 2))
        )
        assertEquals(7000L, Calc.partyDue(2, d))
        assertEquals(7000L, Calc.dashboard(d, 100).workerDue)
        assertEquals(10000L, Calc.dashboard(d, 100).labourSpend)
    }

    @Test fun refundBringsCashBack() {
        val d = AllData(
            cashIns = listOf(cashIn(10000)),
            expenses = listOf(exp(1, 5000)),
            payments = listOf(pay(1, 5000, 1), pay(2, 2000, kind = Kind.REFUND, day = 101))
        )
        assertEquals(7000L, Calc.balanceUpTo(Calc.CASH, Long.MAX_VALUE, d))
        assertEquals(2000L, Calc.partyDue(1, d))
    }

    @Test fun deletingAndEditingRecalculates() {
        val d = AllData(cashIns = listOf(cashIn(100000)), expenses = listOf(exp(1, 50000)), payments = listOf(pay(1, 20000, 1)))
        val edited = d.copy(expenses = listOf(exp(1, 60000)))
        assertEquals(40000L, Calc.partyDue(1, edited))
        val deleted = d.copy(expenses = emptyList(), payments = emptyList())
        assertEquals(100000L, Calc.balanceUpTo(Calc.CASH, Long.MAX_VALUE, deleted))
        assertEquals(0L, Calc.dashboard(deleted, 100).totalSpend)
    }

    @Test fun transferIsNeitherExpenseNorIncome() {
        val d = AllData(
            cashIns = listOf(cashIn(50000)),
            transfers = listOf(Transfer(date = 100, fromAccount = "نقد", toAccount = "بینک", amount = 10000))
        )
        assertEquals(40000L, Calc.balanceUpTo("نقد", Long.MAX_VALUE, d))
        assertEquals(10000L, Calc.balanceUpTo("بینک", Long.MAX_VALUE, d))
        assertEquals(0L, Calc.dashboard(d, 100).totalSpend)
    }

    @Test fun dailyOpeningAndClosing() {
        val d = AllData(cashIns = listOf(cashIn(100000, 100)), payments = listOf(pay(1, 20000, day = 101)))
        assertEquals(100000L, Calc.balanceUpTo(Calc.CASH, 100, d))
        assertEquals(80000L, Calc.balanceUpTo(Calc.CASH, 101, d))
        val s = Calc.dashboard(d, 101)
        assertEquals(20000L, s.todayCashPaid)
        assertEquals(0L, s.todayReceipts)
    }

    @Test fun reportRangeAndUrduCsv() {
        val d = AllData(
            parties = listOf(vendor),
            expenses = listOf(exp(1, 1000, day = 100), exp(2, 2000, day = 200)),
            payments = listOf(pay(1, 500, 1))
        )
        val t = Reports.build(Reports.Kind.STATEMENT, 90, 150, d)
        assertEquals(1, t.rows.size)
        assertEquals("1000", t.footer!![4])
        val csv = Csv.build(t)
        assertTrue(csv.contains("ماربل"))
        assertTrue(csv.contains("احمد ماربل"))
        assertEquals("1,000", t.cell("1000", 4))
    }
}
