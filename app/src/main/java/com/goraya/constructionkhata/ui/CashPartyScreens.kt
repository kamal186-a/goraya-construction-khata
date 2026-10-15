package com.goraya.constructionkhata.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.goraya.constructionkhata.MainViewModel
import com.goraya.constructionkhata.data.*
import com.goraya.constructionkhata.logic.*

private val Red = Color(0xFFC62828)
private val Green = Color(0xFF2E7D32)

private class Line(val date: Long, val text: String, val amount: Long, val incoming: Boolean, val onDelete: () -> Unit)

@Composable
fun CashBookScreen(vm: MainViewModel) {
    val d by vm.data.collectAsState()
    var day by remember { mutableStateOf(Dates.today()) }
    var show by remember { mutableStateOf("") }
    var dlg by remember { mutableStateOf<Dlg?>(null) }

    val opening = Calc.balanceUpTo(Calc.CASH, day - 1, d)
    val lines = mutableListOf<Line>()
    d.cashIns.filter { it.account == Calc.CASH && it.date == day }.forEach { c ->
        lines += Line(c.date, Src.label(c.source) + if (c.note.isBlank()) "" else " - ${c.note}", c.amount, true) { dlg = Dlg.Confirm("یہ وصولی حذف کریں؟") { vm.deleteCashIn(c) } }
    }
    d.payments.filter { it.method == Calc.CASH && it.date == day }.forEach { p ->
        val who = Reports.partyName(d, p.partyId).let { if (it == "-") "" else " - $it" }
        lines += Line(p.date, Kind.label(p.kind) + who + if (p.note.isBlank()) "" else " (${p.note})", p.amount, p.kind == Kind.REFUND) { dlg = Dlg.Confirm("یہ ادائیگی حذف کریں؟ (کیش بیلنس دوبارہ حساب ہوگا)") { vm.deletePayment(p) } }
    }
    d.transfers.filter { it.date == day }.forEach { t ->
        if (t.toAccount == Calc.CASH) lines += Line(t.date, "منتقلی ${t.fromAccount} سے", t.amount, true) { dlg = Dlg.Confirm("یہ منتقلی حذف کریں؟") { vm.deleteTransfer(t) } }
        if (t.fromAccount == Calc.CASH) lines += Line(t.date, "منتقلی ${t.toAccount} کو", t.amount, false) { dlg = Dlg.Confirm("یہ منتقلی حذف کریں؟") { vm.deleteTransfer(t) } }
    }
    val tin = lines.filter { it.incoming }.sumOf { it.amount }
    val tout = lines.filter { !it.incoming }.sumOf { it.amount }
    val closing = opening + tin - tout
    val dayExpenses = d.expenses.filter { it.date == day }.sumOf { it.total }

    Screen("کیش بُک / روزانہ کھاتہ", null) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { day -= 1 }) { Text("پچھلا") }
                    DateField("دن", day, { day = it }, Modifier.weight(1f))
                    OutlinedButton(onClick = { day += 1 }) { Text("اگلا") }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        StatRow("ابتدائی کیش", rs(opening))
                        StatRow("آج کی وصولی", rs(tin), Green)
                        StatRow("آج کی حقیقی کیش ادائیگی", rs(tout), Red)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        StatRow("اختتامی کیش", rs(closing), bold = true)
                        StatRow("اس دن کے تسلیم شدہ اخراجات", rs(dayExpenses))
                        Calc.ACCOUNTS.filter { it != Calc.CASH }.forEach { a -> StatRow("$a بیلنس (آج تک)", rs(Calc.balanceUpTo(a, Long.MAX_VALUE, d))) }
                    }
                }
            }
            item {
                ButtonGrid(listOf("رقم وصول / جمع" to { show = "in" }, "ادائیگی درج کریں" to { show = "pay" }, "اکاؤنٹس میں منتقلی" to { show = "tr" }))
            }
            item { Text("اس دن کا ریکارڈ", style = MaterialTheme.typography.titleMedium) }
            if (lines.isEmpty()) item { Hint("اس دن کوئی کیش لین دین نہیں۔") }
            items(lines) { l ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(l.text, Modifier.weight(1f))
                        Text((if (l.incoming) "+ " else "- ") + rs(l.amount), color = if (l.incoming) Green else Red)
                        IconButton(onClick = l.onDelete) { Icon(Icons.Default.Delete, "حذف") }
                    }
                }
            }
            item { Text("ادھار خریداری پر کیش کم نہیں ہوتا؛ جب ادائیگی درج ہو تبھی کم ہوگا۔", style = MaterialTheme.typography.bodySmall) }
        }
        when (show) {
            "in" -> CashInDialog(day, { vm.saveCashIn(it) }) { show = "" }
            "pay" -> PaymentDialog(d, null, day, { vm.savePayment(it) }) { show = "" }
            "tr" -> TransferDialog(day, { vm.saveTransfer(it) }) { show = "" }
        }
        DlgHost(dlg, emptyList()) { dlg = null }
    }
}

@Composable
private fun CashInDialog(day: Long, onSave: (CashIn) -> Unit, dismiss: () -> Unit) {
    val ctx = LocalContext.current
    var date by remember { mutableStateOf(day) }
    var src by remember { mutableStateOf(Src.OWNER) }
    var acc by remember { mutableStateOf(Calc.CASH) }
    var amt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("رقم وصول / جمع") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("تاریخ", date, { date = it })
                DropdownField("قسم", Src.all.map { it to Src.label(it) }, src, { src = it })
                DropdownField("اکاؤنٹ", Calc.ACCOUNTS.map { it to it }, acc, { acc = it })
                MoneyField("رقم (Rs.)", amt) { amt = it }
                Field("نوٹ", note, { note = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = Money.parse(amt) ?: 0L
                if (a <= 0) toast(ctx, "رقم صفر سے زیادہ ہونی چاہیے") else { onSave(CashIn(date = date, amount = a, account = acc, source = src, note = note.trim())); dismiss() }
            }) { Text("محفوظ کریں") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("منسوخ") } }
    )
}

@Composable
private fun TransferDialog(day: Long, onSave: (Transfer) -> Unit, dismiss: () -> Unit) {
    val ctx = LocalContext.current
    var date by remember { mutableStateOf(day) }
    var from by remember { mutableStateOf(Calc.ACCOUNTS[0]) }
    var to by remember { mutableStateOf(Calc.ACCOUNTS[1]) }
    var amt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("اکاؤنٹس کے درمیان منتقلی") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("تاریخ", date, { date = it })
                DropdownField("کہاں سے", Calc.ACCOUNTS.map { it to it }, from, { from = it })
                DropdownField("کہاں کو", Calc.ACCOUNTS.map { it to it }, to, { to = it })
                MoneyField("رقم (Rs.)", amt) { amt = it }
                Field("نوٹ", note, { note = it })
                Text("منتقلی نیا خرچہ یا آمدنی شمار نہیں ہوتی۔", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = Money.parse(amt) ?: 0L
                if (a <= 0) toast(ctx, "رقم صفر سے زیادہ ہونی چاہیے")
                else if (from == to) toast(ctx, "دونوں اکاؤنٹ ایک جیسے نہیں ہو سکتے")
                else { onSave(Transfer(date = date, fromAccount = from, toAccount = to, amount = a, note = note.trim())); dismiss() }
            }) { Text("محفوظ کریں") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("منسوخ") } }
    )
}

@Composable
fun PaymentDialog(d: AllData, fixedParty: Long?, day: Long, onSave: (Payment) -> Unit, dismiss: () -> Unit) {
    val ctx = LocalContext.current
    var date by remember { mutableStateOf(day) }
    var kind by remember { mutableStateOf(Kind.PAYMENT) }
    var party by remember { mutableStateOf(fixedParty) }
    var expId by remember { mutableStateOf<Long?>(null) }
    var amt by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(Calc.CASH) }
    var note by remember { mutableStateOf("") }
    val open = d.expenses.filter { it.partyId == party && party != null && it.total - Calc.paidFor(it.id, d.payments) > 0 }
    val sel = open.firstOrNull { it.id == expId }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("ادائیگی درج کریں") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("تاریخ", date, { date = it })
                DropdownField("قسم", Kind.all.map { it to Kind.label(it) }, kind, { kind = it; if (it == Kind.REFUND) expId = null })
                if (fixedParty == null) DropdownField("دکاندار / کاریگر", listOf(0L to "— بغیر نام —") + d.parties.filter { it.active }.map { it.id to it.name }, party ?: 0L,
                    { party = it.takeIf { v -> v != 0L }; expId = null })
                if (kind != Kind.REFUND && open.isNotEmpty()) DropdownField("کس بل پر؟ (اختیاری)",
                    listOf(0L to "— عمومی ادائیگی —") + open.map { it.id to "${Dates.fmt(it.date)} ${it.itemName} (باقی ${Money.group(it.total - Calc.paidFor(it.id, d.payments))})" }, expId ?: 0L,
                    { expId = it.takeIf { v -> v != 0L } })
                MoneyField("رقم (Rs.)", amt) { amt = it }
                DropdownField("اکاؤنٹ / طریقہ", Calc.ACCOUNTS.map { it to it }, method, { method = it })
                Field("نوٹ", note, { note = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = Money.parse(amt) ?: 0L
                val left = sel?.let { it.total - Calc.paidFor(it.id, d.payments) }
                if (a <= 0) toast(ctx, "رقم صفر سے زیادہ ہونی چاہیے")
                else if (left != null && a > left) toast(ctx, "رقم اس بل کے باقی (${Money.group(left)}) سے زیادہ ہے")
                else { onSave(Payment(date = date, partyId = party, expenseId = expId, amount = a, method = method, kind = kind, note = note.trim())); dismiss() }
            }) { Text("محفوظ کریں") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("منسوخ") } }
    )
}

@Composable
fun PartiesScreen(vm: MainViewModel, go: (String) -> Unit, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var add by remember { mutableStateOf(false) }
    val type = if (tab == 0) "VENDOR" else "WORKER"
    val list = d.parties.filter { it.type == type && it.name.contains(query.trim()) }
    Screen("دکاندار، کاریگر اور ادھار", onBack, actions = { TextButton(onClick = { add = true }) { Text("نیا شامل کریں") } }) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("دکاندار", Modifier.padding(12.dp)) }
                Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("کاریگر / ٹھیکیدار", Modifier.padding(12.dp)) }
            }
            Field("تلاش کریں", query, { query = it }, Modifier.fillMaxWidth().padding(12.dp))
            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (list.isEmpty()) item { Hint("کوئی نام نہیں۔ اوپر سے نیا شامل کریں۔") }
                items(list, key = { it.id }) { p ->
                    val due = Calc.partyDue(p.id, d)
                    Card(Modifier.fillMaxWidth().clickable { go("ledger/${p.id}") }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name + if (p.active) "" else " (غیر فعال)")
                                if (p.phone.isNotBlank()) Text(p.phone, style = MaterialTheme.typography.bodySmall)
                            }
                            Column { Text("بقایا"); Text(rs(due), color = if (due > 0) Red else Green) }
                        }
                    }
                }
            }
        }
        if (add) PartyDialog(if (tab == 0) "نیا دکاندار" else "نیا کاریگر / ٹھیکیدار", type, null, { vm.saveParty(it) }) { add = false }
    }
}

@Composable
fun LedgerScreen(vm: MainViewModel, partyId: Long, go: (String) -> Unit, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    val party = d.parties.firstOrNull { it.id == partyId }
    var pay by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    if (party == null) { Screen("کھاتہ", onBack) { Hint("پارٹی نہیں ملی۔") }; return }
    val (total, paid, due) = Calc.partyTotals(partyId, d)
    val exps = d.expenses.filter { it.partyId == partyId }
    val pays = d.payments.filter { it.partyId == partyId }

    Screen(party.name, onBack, actions = {
        TextButton(onClick = { edit = true }) { Text("ترمیم") }
        TextButton(onClick = { vm.saveParty(party.copy(active = !party.active)) }) { Text(if (party.active) "غیر فعال" else "بحال") }
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        if (party.phone.isNotBlank()) Text("فون: ${party.phone}")
                        if (party.address.isNotBlank()) Text("پتہ: ${party.address}")
                        StatRow(if (party.type == "VENDOR") "کل خریداری" else "کل مزدوری", rs(total))
                        StatRow("ادا شدہ (خالص)", rs(paid))
                        StatRow("بقایا", rs(due), if (due > 0) Red else Green, bold = true)
                    }
                }
            }
            item { Button(onClick = { pay = true }, Modifier.fillMaxWidth().height(50.dp)) { Text("ادائیگی / ایڈوانس درج کریں") } }
            item { Text("خریداریاں / اندراجات", style = MaterialTheme.typography.titleMedium) }
            if (exps.isEmpty()) item { Hint("کوئی اندراج نہیں۔") }
            items(exps, key = { "e${it.id}" }) { e ->
                val ep = Calc.paidFor(e.id, d.payments)
                Card(Modifier.fillMaxWidth().clickable { go("expense/${e.id}/0") }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${Dates.fmt(e.date)}  ${e.itemName}")
                        Text("${fmtQty(e.quantity)} ${e.unit} × ${Money.group(e.rate)}", style = MaterialTheme.typography.bodySmall)
                        StatRow("کل ${Money.group(e.total)} | ادا ${Money.group(ep)}", "باقی ${Money.group(e.total - ep)}")
                        Text(Calc.status(e.total, ep), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Text("ادائیگیاں", style = MaterialTheme.typography.titleMedium) }
            if (pays.isEmpty()) item { Hint("کوئی ادائیگی نہیں۔") }
            items(pays, key = { "p${it.id}" }) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text("${Dates.fmt(p.date)}  ${Kind.label(p.kind)} (${p.method})")
                            if (p.note.isNotBlank()) Text(p.note, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rs(p.amount), color = if (p.kind == Kind.REFUND) Green else Red)
                        IconButton(onClick = { dlg = Dlg.Confirm("یہ ادائیگی حذف کریں؟") { vm.deletePayment(p) } }) { Icon(Icons.Default.Delete, "حذف") }
                    }
                }
            }
        }
        if (pay) PaymentDialog(d, partyId, Dates.today(), { vm.savePayment(it) }) { pay = false }
        if (edit) PartyDialog("ترمیم", party.type, party, { vm.saveParty(it) }) { edit = false }
        DlgHost(dlg, emptyList()) { dlg = null }
    }
}
