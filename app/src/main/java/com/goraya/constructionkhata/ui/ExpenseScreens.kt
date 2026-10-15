package com.goraya.constructionkhata.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.goraya.constructionkhata.MainViewModel
import com.goraya.constructionkhata.data.Expense
import com.goraya.constructionkhata.data.Payment
import com.goraya.constructionkhata.logic.Calc
import com.goraya.constructionkhata.logic.Dates
import com.goraya.constructionkhata.logic.Kind
import com.goraya.constructionkhata.logic.Money

@Composable
fun ExpenseFormScreen(vm: MainViewModel, expenseId: Long, itemId: Long, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    val ctx = LocalContext.current
    val existing = d.expenses.firstOrNull { it.id == expenseId }
    val preItem = d.items.firstOrNull { it.id == itemId }
    val preCat = preItem?.let { i -> d.subCategories.firstOrNull { it.id == i.subCategoryId }?.categoryId }
    val k = listOf(existing?.id, preItem?.id)

    var date by remember(k) { mutableStateOf(existing?.date ?: Dates.today()) }
    var catId by remember(k) { mutableStateOf(existing?.categoryId ?: preCat) }
    var subId by remember(k) { mutableStateOf(existing?.subCategoryId ?: preItem?.subCategoryId) }
    var itemSel by remember(k) { mutableStateOf(existing?.itemId ?: preItem?.id) }
    var itemName by remember(k) { mutableStateOf(existing?.itemName ?: preItem?.name ?: "") }
    var desc by remember(k) { mutableStateOf(existing?.description ?: "") }
    var locId by remember(k) { mutableStateOf(existing?.locationId) }
    var partyId by remember(k) { mutableStateOf(existing?.partyId) }
    var fixed by remember(k) { mutableStateOf(existing?.unit == Calc.FIXED_UNIT) }
    var qty by remember(k) { mutableStateOf(existing?.let { if (it.unit == Calc.FIXED_UNIT) "" else fmtQty(it.quantity) } ?: "") }
    var unit by remember(k) { mutableStateOf(existing?.unit?.takeIf { it != Calc.FIXED_UNIT } ?: preItem?.defaultUnit ?: "") }
    var rate by remember(k) { mutableStateOf(existing?.let { if (it.unit == Calc.FIXED_UNIT) "" else it.rate.toString() } ?: "") }
    var fixedAmt by remember(k) { mutableStateOf(existing?.let { if (it.unit == Calc.FIXED_UNIT) it.total.toString() else "" } ?: "") }
    var paid by remember(k) { mutableStateOf("") }
    var method by remember(k) { mutableStateOf(Calc.CASH) }
    var notes by remember(k) { mutableStateOf(existing?.notes ?: "") }
    var receipt by remember(k) { mutableStateOf(existing?.receiptPath ?: "") }
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    var newParty by remember { mutableStateOf(false) }
    var newUnit by remember { mutableStateOf(false) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) Files.copyImage(ctx, uri)?.let { receipt = it } ?: toast(ctx, "تصویر محفوظ نہیں ہو سکی")
    }

    val qtyV = Money.parseQty(qty)
    val rateV = Money.parse(rate)
    val total = if (fixed) Money.parse(fixedAmt) ?: 0L else if (qtyV != null && rateV != null) Money.total(qtyV, rateV) else 0L
    val alreadyPaid = existing?.let { Calc.paidFor(it.id, d.payments) } ?: 0L
    val paidNow = if (existing == null) Money.parse(paid) ?: 0L else 0L
    val remaining = total - alreadyPaid - paidNow
    val isLabour = existing?.isLabour == true
    val partyType = if (isLabour) "WORKER" else "VENDOR"
    val units = Calc.units(d)

    Screen(if (existing == null) "نیا خرچہ / خریداری" else "اندراج میں ترمیم", onBack) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DateField("تاریخ", date, { date = it })
            DropdownField("بڑا کالم", d.categories.filter { it.active || it.id == catId }.map { it.id to it.name }, catId,
                { catId = it; subId = null; itemSel = null })
            DropdownField("ذیلی کالم", d.subCategories.filter { it.categoryId == catId && (it.active || it.id == subId) }.map { it.id to it.name }, subId,
                { subId = it; itemSel = null })
            DropdownField("آئٹم", d.items.filter { it.subCategoryId == subId && (it.active || it.id == itemSel) }.map { it.id to it.name }, itemSel, { id ->
                itemSel = id
                d.items.firstOrNull { it.id == id }?.let { i -> itemName = i.name; if (i.defaultUnit.isNotBlank()) unit = i.defaultUnit }
            })
            Field("آئٹم کا نام (خود لکھ بھی سکتے ہیں)", itemName, { itemName = it })
            Field("تفصیل", desc, { desc = it })
            DropdownField("تعمیر کی جگہ", listOf(0L to "— منتخب نہیں —") + d.locations.filter { it.active || it.id == locId }.map { it.id to it.name }, locId ?: 0L,
                { locId = it.takeIf { v -> v != 0L } })
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(if (isLabour) "کاریگر" else "دکاندار", listOf(0L to "— منتخب نہیں —") + d.parties.filter { it.type == partyType && (it.active || it.id == partyId) }.map { it.id to it.name },
                    partyId ?: 0L, { partyId = it.takeIf { v -> v != 0L } }, Modifier.weight(1f))
                OutlinedButton(onClick = { newParty = true }) { Text("نیا") }
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("مقدار لاگو نہیں، مقررہ رقم درج کروں", Modifier.weight(1f))
                Switch(fixed, { fixed = it })
            }
            if (fixed) {
                MoneyField("مقررہ رقم (Rs.)", fixedAmt) { fixedAmt = it }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QtyField("مقدار", qty, { qty = it }, Modifier.weight(1f))
                    DropdownField("یونٹ", units.map { it to it }.let { if (unit.isNotBlank() && unit !in units) it + (unit to unit) else it }, unit, { unit = it }, Modifier.weight(1f))
                }
                TextButton(onClick = { newUnit = true }) { Text("+ اپنا نیا یونٹ بنائیں") }
                MoneyField("فی یونٹ قیمت (Rs.)", rate) { rate = it }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    StatRow("کل قیمت", rs(total), bold = true)
                    if (existing != null) StatRow("اب تک ادا شدہ", rs(alreadyPaid))
                }
            }
            if (existing == null) {
                MoneyField("ابھی ادا کی گئی رقم (Rs.)", paid) { paid = it }
                DropdownField("ادائیگی کا طریقہ", Calc.ACCOUNTS.map { it to it }, method, { method = it })
            } else Text("مزید ادائیگی کے لیے دکاندار/کاریگر کے کھاتے یا کیش بُک سے ادائیگی درج کریں۔", style = MaterialTheme.typography.bodySmall)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    StatRow("باقی رقم", rs(remaining), if (remaining < 0) Color.Red else Color.Unspecified)
                    StatRow("ادائیگی کی حالت", if (total > 0) Calc.status(total, alreadyPaid + paidNow) else "-")
                }
            }
            if (remaining < 0) Text("ادا شدہ رقم کل قیمت سے زیادہ نہیں ہو سکتی۔", color = Color.Red)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedButton(onClick = { pick.launch("image/*") }) { Text(if (receipt.isBlank()) "رسید / بل کی تصویر" else "تصویر تبدیل کریں") }
                Thumb(receipt)
            }
            Field("اضافی نوٹس", notes, { notes = it }, singleLine = false)
            Button(
                onClick = {
                    if (itemName.isBlank()) { toast(ctx, "آئٹم کا نام لکھیں"); return@Button }
                    if (total <= 0) { toast(ctx, "کل قیمت صفر نہیں ہو سکتی (مقدار اور قیمت درج کریں)"); return@Button }
                    if (remaining < 0) return@Button
                    val e = Expense(
                        id = existing?.id ?: 0, date = date, categoryId = catId, subCategoryId = subId, itemId = itemSel,
                        itemName = itemName.trim(), description = desc.trim(), locationId = locId, partyId = partyId,
                        quantity = if (fixed) 1.0 else qtyV ?: 1.0, unit = if (fixed) Calc.FIXED_UNIT else unit.ifBlank { "عدد" },
                        rate = if (fixed) total else rateV ?: 0L, total = total, isLabour = isLabour,
                        receiptPath = receipt, notes = notes.trim(), createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    val pays = if (existing == null && paidNow > 0) listOf(Payment(date = date, partyId = partyId, amount = paidNow, method = method, kind = Kind.PAYMENT)) else emptyList()
                    vm.saveExpense(e, pays)
                    toast(ctx, "محفوظ ہو گیا")
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("محفوظ کریں") }
            if (existing != null) OutlinedButton(
                onClick = { dlg = Dlg.Confirm("یہ اندراج اور اس کی ادائیگیاں حذف کریں؟ (کیش بیلنس دوبارہ حساب ہوگا)") { vm.deleteExpense(existing); onBack() } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("اندراج حذف کریں", color = Color.Red) }
        }
        DlgHost(dlg, units) { dlg = null }
        if (newParty) PartyDialog(if (isLabour) "نیا کاریگر" else "نیا دکاندار", partyType, null, { p -> vm.saveParty(p) { partyId = it } }) { newParty = false }
        if (newUnit) {
            var u by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { newUnit = false }, title = { Text("نیا یونٹ") }, text = { Field("یونٹ کا نام", u, { u = it }) },
                confirmButton = {
                    TextButton(onClick = {
                        if (u.isNotBlank()) {
                            vm.setSetting("units", ((d.settings["units"] ?: "").split("|").filter { it.isNotBlank() } + u.trim()).distinct().joinToString("|"))
                            unit = u.trim(); newUnit = false
                        }
                    }) { Text("محفوظ کریں") }
                },
                dismissButton = { TextButton(onClick = { newUnit = false }) { Text("منسوخ") } }
            )
        }
    }
}

@Composable
fun LabourFormScreen(vm: MainViewModel, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    val ctx = LocalContext.current
    var date by remember { mutableStateOf(Dates.today()) }
    var workerId by remember { mutableStateOf<Long?>(null) }
    var workType by remember { mutableStateOf("") }
    var locId by remember { mutableStateOf<Long?>(null) }
    var contract by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf("") }
    var wage by remember { mutableStateOf("") }
    var contractAmt by remember { mutableStateOf("") }
    var advance by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(Calc.CASH) }
    var notes by remember { mutableStateOf("") }
    var newWorker by remember { mutableStateOf(false) }

    val daysV = Money.parseQty(days)
    val wageV = Money.parse(wage)
    val total = if (contract) Money.parse(contractAmt) ?: 0L else if (daysV != null && wageV != null) Money.total(daysV, wageV) else 0L
    val adv = Money.parse(advance) ?: 0L
    val pd = Money.parse(paid) ?: 0L
    val remaining = total - adv - pd
    val labourCat = d.categories.firstOrNull { it.code == "L" }
    val workTypes = d.items.filter { i -> d.subCategories.firstOrNull { it.id == i.subCategoryId }?.categoryId == labourCat?.id && i.active }

    Screen("مزدوری درج کریں", onBack) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DateField("تاریخ", date, { date = it })
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField("کاریگر کا نام", d.parties.filter { it.type == "WORKER" && (it.active || it.id == workerId) }.map { it.id to it.name }, workerId, { workerId = it }, Modifier.weight(1f))
                OutlinedButton(onClick = { newWorker = true }) { Text("نیا") }
            }
            DropdownField("کام کی قسم", workTypes.map { it.name to it.name }, workType.takeIf { it.isNotBlank() }, { workType = it })
            Field("کام کی قسم (خود لکھیں)", workType, { workType = it })
            DropdownField("کام کی جگہ", listOf(0L to "— منتخب نہیں —") + d.locations.filter { it.active }.map { it.id to it.name }, locId ?: 0L, { locId = it.takeIf { v -> v != 0L } })
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("ٹھیکے کی رقم (روزانہ اجرت نہیں)", Modifier.weight(1f)); Switch(contract, { contract = it })
            }
            if (contract) MoneyField("ٹھیکے کی کل رقم (Rs.)", contractAmt) { contractAmt = it }
            else {
                QtyField("کام کے دن", days, { days = it })
                MoneyField("روزانہ کی اجرت (Rs.)", wage) { wage = it }
            }
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { StatRow("کل مزدوری", rs(total), bold = true) } }
            MoneyField("ایڈوانس (Rs.)", advance) { advance = it }
            MoneyField("ابھی ادا کی گئی رقم (Rs.)", paid) { paid = it }
            DropdownField("ادائیگی کا طریقہ", Calc.ACCOUNTS.map { it to it }, method, { method = it })
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    StatRow("باقی مزدوری", rs(remaining), if (remaining < 0) Color.Red else Color.Unspecified)
                    StatRow("حالت", if (total > 0) Calc.status(total, adv + pd) else "-")
                }
            }
            if (remaining < 0) Text("ایڈوانس + ادا شدہ رقم کل مزدوری سے زیادہ نہیں ہو سکتی۔", color = Color.Red)
            Field("نوٹس", notes, { notes = it }, singleLine = false)
            Button(
                onClick = {
                    if (workerId == null) { toast(ctx, "کاریگر منتخب کریں یا نیا بنائیں"); return@Button }
                    if (workType.isBlank()) { toast(ctx, "کام کی قسم لکھیں"); return@Button }
                    if (total <= 0) { toast(ctx, "کل مزدوری صفر ہے"); return@Button }
                    if (remaining < 0) return@Button
                    val e = Expense(
                        date = date, categoryId = labourCat?.id, itemName = workType.trim(), locationId = locId, partyId = workerId,
                        quantity = if (contract) 1.0 else daysV ?: 1.0, unit = if (contract) "ٹھیکہ" else "دن",
                        rate = if (contract) total else wageV ?: 0L, total = total, isLabour = true, notes = notes.trim()
                    )
                    val pays = buildList {
                        if (adv > 0) add(Payment(date = date, amount = adv, method = method, kind = Kind.ADVANCE, note = "ایڈوانس"))
                        if (pd > 0) add(Payment(date = date, amount = pd, method = method, kind = Kind.PAYMENT))
                    }
                    vm.saveExpense(e, pays)
                    toast(ctx, "محفوظ ہو گیا")
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("محفوظ کریں") }
        }
        if (newWorker) PartyDialog("نیا کاریگر / ٹھیکیدار", "WORKER", null, { p -> vm.saveParty(p) { workerId = it } }) { newWorker = false }
    }
}
