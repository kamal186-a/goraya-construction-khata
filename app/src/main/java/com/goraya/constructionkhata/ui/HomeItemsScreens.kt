package com.goraya.constructionkhata.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.goraya.constructionkhata.MainViewModel
import com.goraya.constructionkhata.data.Item
import com.goraya.constructionkhata.logic.Calc
import com.goraya.constructionkhata.logic.Dates

private val Red = Color(0xFFC62828)
private val Green = Color(0xFF2E7D32)

@Composable
fun HomeScreen(vm: MainViewModel, go: (String) -> Unit) {
    val d by vm.data.collectAsState()
    val s = Calc.dashboard(d, Dates.today())
    val recent = Calc.recent(d, 8)
    Screen("GORAYA Construction Khata", null) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("پروجیکٹ: ${d.settings["project_name"] ?: "میرا گھر"}")
                        Text("موجودہ کیش بیلنس")
                        Text(rs(s.cash), style = MaterialTheme.typography.headlineMedium, color = if (s.cash < 0) Red else Green)
                    }
                }
            }
            if (s.budget > 0 && s.totalSpend * 100 >= s.budget * 90) item {
                val over = s.totalSpend > s.budget
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (over) Color(0xFFFFCDD2) else Color(0xFFFFF3CD))) {
                    Text(if (over) "وارننگ: خرچہ بجٹ سے ${rs(-s.budgetLeft)} بڑھ گیا ہے!" else "وارننگ: خرچہ بجٹ کی 90٪ حد تک پہنچ گیا ہے۔", Modifier.padding(12.dp))
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        StatRow("آج کی وصولی", rs(s.todayReceipts))
                        StatRow("آج کی حقیقی کیش ادائیگی", rs(s.todayCashPaid))
                        StatRow("آج کے کل اخراجات", rs(s.todayExpenses))
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        StatRow("گھر پر اب تک کل خرچہ", rs(s.totalSpend), bold = true)
                        StatRow("سامان کی خریداری کا خرچہ", rs(s.materialSpend))
                        StatRow("مزدوری کا خرچہ", rs(s.labourSpend))
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        StatRow("دکانداروں کا بقایا ادھار", rs(s.vendorDue), Red)
                        StatRow("کاریگروں کی باقی مزدوری", rs(s.workerDue), Red)
                        StatRow("مالک کی طرف سے کل جمع", rs(s.ownerDeposits))
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        if (s.budget > 0) {
                            StatRow("دستیاب بجٹ", rs(s.budget))
                            StatRow(if (s.budgetLeft >= 0) "باقی بجٹ" else "بجٹ سے زیادہ", rs(Math.abs(s.budgetLeft)), if (s.budgetLeft >= 0) Green else Red)
                        } else Text("بجٹ درج نہیں۔ (مزید ← بجٹ)", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            item {
                ButtonGrid(listOf(
                    "نیا خرچہ درج کریں" to { go("expense/0/0") },
                    "نئی رقم وصول کریں" to { go("cash") },
                    "ادائیگی درج کریں" to { go("cash") },
                    "سامان شامل کریں" to { go("items") },
                    "مزدوری درج کریں" to { go("labour") },
                    "دکاندار شامل کریں" to { go("parties") },
                    "روزانہ کا کھاتہ" to { go("cash") },
                    "رپورٹس" to { go("reports") },
                    "بیک اَپ اور بحالی" to { go("backup") }
                ))
            }
            item { Text("حالیہ لین دین", style = MaterialTheme.typography.titleMedium) }
            if (recent.isEmpty()) item { Hint("ابھی کوئی لین دین نہیں۔") }
            items(recent) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(r.text); Text(Dates.fmt(r.date), style = MaterialTheme.typography.bodySmall) }
                        Text((if (r.incoming) "+ " else "- ") + rs(r.amount), color = if (r.incoming) Green else Red)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("بیک اَپ اور آن لائن سنک")
                        Text("ابھی شامل نہیں کیا گیا (اگلے مرحلے میں)۔ تب تک ڈیٹا صرف اسی موبائل میں محفوظ ہے۔", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemsScreen(vm: MainViewModel, go: (String) -> Unit) {
    val d by vm.data.collectAsState()
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showInactive by remember { mutableStateOf(false) }
    val open = remember { mutableStateListOf<String>() }
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    val units = Calc.units(d)
    fun toggle(k: String) { if (open.contains(k)) open.remove(k) else open.add(k) }
    fun result(ok: Boolean) = toast(ctx, if (ok) "حذف ہو گیا" else "حذف نہیں ہو سکا: اس کے اندر آئٹم/مالی ریکارڈ موجود ہے۔ غیر فعال کریں۔")

    Screen("سامان اور آئٹمز", null, actions = {
        TextButton(onClick = { dlg = Dlg.Prompt("نیا کالم", "کالم کا نام") { vm.addCategory(it) } }) { Text("نیا کالم") }
    }) { pad ->
        Column(Modifier.padding(pad)) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Field("آئٹم تلاش کریں", query, { query = it }, Modifier.weight(1f))
                Text("غیر فعال", Modifier.padding(horizontal = 6.dp))
                Switch(showInactive, { showInactive = it })
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (query.isNotBlank()) {
                    val found = d.items.filter { it.name.contains(query.trim()) && (showInactive || it.active) }
                    if (found.isEmpty()) item { Hint("کوئی آئٹم نہیں ملا۔") }
                    items(found, key = { "f${it.id}" }) { i ->
                        val sub = d.subCategories.firstOrNull { it.id == i.subCategoryId }
                        val cat = d.categories.firstOrNull { it.id == sub?.categoryId }
                        ItemRow(i, "${cat?.name ?: ""} ← ${sub?.name ?: ""}", 0, vm, go, units, d, { dlg = it }, ::result)
                    }
                } else {
                    d.categories.filter { showInactive || it.active }.forEach { c ->
                        item(key = "c${c.id}") {
                            Row(Modifier.fillMaxWidth().clickable { toggle("c${c.id}") }.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text((if (open.contains("c${c.id}")) "▼ " else "◀ ") + c.name + if (c.active) "" else " (غیر فعال)",
                                    Modifier.weight(1f).padding(vertical = 12.dp), style = MaterialTheme.typography.titleSmall)
                                RowMenu(listOf(
                                    "نیا ذیلی کالم" to { dlg = Dlg.Prompt("نیا ذیلی کالم", "نام") { vm.addSub(c.id, it) } },
                                    "نام تبدیل کریں" to { dlg = Dlg.Prompt("کالم کا نام", "نام", c.name) { vm.saveCategory(c.copy(name = it), "کالم کا نام تبدیل") } },
                                    (if (c.active) "غیر فعال کریں" else "بحال کریں") to { vm.saveCategory(c.copy(active = !c.active), if (c.active) "کالم غیر فعال" else "کالم بحال") },
                                    "مستقل حذف (صرف خالی ہو تو)" to { dlg = Dlg.Confirm("کالم '${c.name}' حذف کریں؟") { vm.deleteCategory(c, ::result) } }
                                ))
                            }
                            HorizontalDivider()
                        }
                        if (open.contains("c${c.id}")) d.subCategories.filter { it.categoryId == c.id && (showInactive || it.active) }.forEach { s ->
                            item(key = "s${s.id}") {
                                Row(Modifier.fillMaxWidth().clickable { toggle("s${s.id}") }.padding(start = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text((if (open.contains("s${s.id}")) "▼ " else "◀ ") + s.name + if (s.active) "" else " (غیر فعال)", Modifier.weight(1f).padding(vertical = 10.dp))
                                    RowMenu(listOf(
                                        "نیا آئٹم" to { dlg = Dlg.ItemD("نیا آئٹم", null) { n, u, ds -> vm.saveItem(Item(subCategoryId = s.id, name = n, defaultUnit = u, description = ds, sortOrder = d.items.size), "آئٹم شامل") } },
                                        "نام تبدیل کریں" to { dlg = Dlg.Prompt("ذیلی کالم کا نام", "نام", s.name) { vm.saveSub(s.copy(name = it), "ذیلی کالم کا نام تبدیل") } },
                                        (if (s.active) "غیر فعال کریں" else "بحال کریں") to { vm.saveSub(s.copy(active = !s.active), if (s.active) "ذیلی کالم غیر فعال" else "ذیلی کالم بحال") },
                                        "مستقل حذف (صرف خالی ہو تو)" to { dlg = Dlg.Confirm("ذیلی کالم '${s.name}' حذف کریں؟") { vm.deleteSub(s, ::result) } }
                                    ))
                                }
                            }
                            if (open.contains("s${s.id}")) d.items.filter { it.subCategoryId == s.id && (showInactive || it.active) }.forEach { i ->
                                item(key = "i${i.id}") { ItemRow(i, "", 52, vm, go, units, d, { dlg = it }, ::result) }
                            }
                        }
                    }
                }
            }
        }
        DlgHost(dlg, units) { dlg = null }
    }
}

@Composable
private fun ItemRow(
    i: Item, path: String, indent: Int, vm: MainViewModel, go: (String) -> Unit, units: List<String>,
    d: com.goraya.constructionkhata.logic.AllData, setDlg: (Dlg) -> Unit, result: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable(enabled = i.active) { go("expense/0/${i.id}") }.padding(start = (12 + indent).dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(i.name + if (i.active) "" else " (غیر فعال)", color = if (i.active) Color.Unspecified else MaterialTheme.colorScheme.outline)
            if (path.isNotBlank()) Text(path, style = MaterialTheme.typography.bodySmall)
        }
        RowMenu(listOf(
            "ترمیم (نام / یونٹ / تفصیل)" to { setDlg(Dlg.ItemD("آئٹم میں ترمیم", i) { n, u, ds -> vm.saveItem(i.copy(name = n, defaultUnit = u, description = ds), "آئٹم میں ترمیم") }) },
            (if (i.active) "غیر فعال کریں" else "بحال کریں") to { vm.saveItem(i.copy(active = !i.active), if (i.active) "آئٹم غیر فعال" else "آئٹم بحال") },
            "مستقل حذف (اگر مالی ریکارڈ نہ ہو)" to { setDlg(Dlg.Confirm("آئٹم '${i.name}' حذف کریں؟") { vm.deleteItem(i, result) }) }
        ))
    }
}
