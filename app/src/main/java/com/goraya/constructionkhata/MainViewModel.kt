package com.goraya.constructionkhata

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.goraya.constructionkhata.data.*
import com.goraya.constructionkhata.logic.AllData
import com.goraya.constructionkhata.logic.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val dao = db.dao()

    init { viewModelScope.launch(Dispatchers.IO) { Seed.ensure(db) } }

    val data: StateFlow<AllData> = combine(
        listOf<Flow<Any>>(
            dao.categories(), dao.subCategories(), dao.items(), dao.locations(), dao.parties(), dao.expenses(),
            dao.payments(), dao.cashIns(), dao.transfers(), dao.diary(), dao.settings(), dao.audit()
        )
    ) { a ->
        AllData(
            categories = a[0] as List<Category>, subCategories = a[1] as List<SubCategory>, items = a[2] as List<Item>,
            locations = a[3] as List<Location>, parties = a[4] as List<Party>, expenses = a[5] as List<Expense>,
            payments = a[6] as List<Payment>, cashIns = a[7] as List<CashIn>, transfers = a[8] as List<Transfer>,
            diary = a[9] as List<DiaryEntry>,
            settings = (a[10] as List<Setting>).associate { it.key to it.value },
            audit = a[11] as List<AuditLog>
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AllData())

    private fun io(block: suspend () -> Unit) { viewModelScope.launch(Dispatchers.IO) { block() } }
    private suspend fun log(action: String, detail: String) =
        dao.addAudit(AuditLog(time = System.currentTimeMillis(), action = action, detail = detail))
    private suspend fun <T> main(v: T, f: (T) -> Unit) = withContext(Dispatchers.Main) { f(v) }

    // ---- کالم / ذیلی کالم / آئٹم ----
    fun addCategory(name: String) = io { dao.saveCategory(Category(name = name, sortOrder = data.value.categories.size)); log("کالم شامل", name) }
    fun saveCategory(c: Category, why: String) = io { dao.saveCategory(c); log(why, c.name) }
    fun deleteCategory(c: Category, done: (Boolean) -> Unit) = io {
        val ok = dao.subCountForCat(c.id) == 0
        if (ok) { dao.deleteCategory(c.id); log("کالم حذف", c.name) }
        main(ok, done)
    }
    fun addSub(catId: Long, name: String) = io { dao.saveSub(SubCategory(categoryId = catId, name = name, sortOrder = data.value.subCategories.size)); log("ذیلی کالم شامل", name) }
    fun saveSub(s: SubCategory, why: String) = io { dao.saveSub(s); log(why, s.name) }
    fun deleteSub(s: SubCategory, done: (Boolean) -> Unit) = io {
        val ok = dao.itemCountForSub(s.id) == 0
        if (ok) { dao.deleteSub(s.id); log("ذیلی کالم حذف", s.name) }
        main(ok, done)
    }
    fun saveItem(i: Item, why: String) = io { dao.saveItem(i); log(why, i.name) }
    fun deleteItem(i: Item, done: (Boolean) -> Unit) = io {
        val ok = dao.expenseCountForItem(i.id) == 0
        if (ok) { dao.deleteItem(i.id); log("آئٹم حذف", i.name) }
        main(ok, done)
    }

    // ---- جگہیں ----
    fun addLocation(name: String) = io { dao.saveLocation(Location(name = name, sortOrder = data.value.locations.size)); log("جگہ شامل", name) }
    fun saveLocation(l: Location, why: String) = io { dao.saveLocation(l); log(why, l.name) }

    // ---- دکاندار / کاریگر ----
    fun saveParty(p: Party, onId: (Long) -> Unit = {}) = io {
        val id = dao.saveParty(p)
        log(if (p.id == 0L) "پارٹی شامل" else "پارٹی میں ترمیم", p.name)
        main(if (p.id == 0L) id else p.id, onId)
    }

    // ---- خرچہ ----
    fun saveExpense(e: Expense, payments: List<Payment>) = io {
        db.withTransaction {
            val id = dao.saveExpense(e)
            val eid = if (e.id == 0L) id else e.id
            if (e.id != 0L) dao.updatePaymentsParty(eid, e.partyId)
            payments.forEach { dao.savePayment(it.copy(expenseId = eid, partyId = e.partyId)) }
            log(if (e.id == 0L) "نیا اندراج" else "اندراج میں ترمیم", "${e.itemName} ${Money.format(e.total)}")
        }
    }
    fun deleteExpense(e: Expense) = io {
        db.withTransaction {
            dao.deletePaymentsOfExpense(e.id)
            dao.deleteExpense(e.id)
            log("اندراج حذف", "${e.itemName} ${Money.format(e.total)}")
        }
    }

    // ---- ادائیگی / کیش ----
    fun savePayment(p: Payment) = io { dao.savePayment(p); log("ادائیگی درج", "${p.kind} ${Money.format(p.amount)}") }
    fun deletePayment(p: Payment) = io { dao.deletePayment(p.id); log("ادائیگی حذف", "${p.kind} ${Money.format(p.amount)}") }
    fun saveCashIn(c: CashIn) = io { dao.saveCashIn(c); log("وصولی درج", "${c.source} ${Money.format(c.amount)}") }
    fun deleteCashIn(c: CashIn) = io { dao.deleteCashIn(c.id); log("وصولی حذف", Money.format(c.amount)) }
    fun saveTransfer(t: Transfer) = io { dao.saveTransfer(t); log("منتقلی", "${t.fromAccount} ← ${t.toAccount} ${Money.format(t.amount)}") }
    fun deleteTransfer(t: Transfer) = io { dao.deleteTransfer(t.id); log("منتقلی حذف", Money.format(t.amount)) }

    // ---- ڈائری / سیٹنگز ----
    fun saveDiary(x: DiaryEntry) = io { dao.saveDiary(x) }
    fun deleteDiary(x: DiaryEntry) = io { dao.deleteDiary(x.id); log("ڈائری حذف", fmtDate(x.date)) }
    fun setSetting(k: String, v: String) = io { dao.saveSetting(Setting(k, v)); log("سیٹنگ", "$k") }

    private fun fmtDate(d: Long) = com.goraya.constructionkhata.logic.Dates.fmt(d)
}
