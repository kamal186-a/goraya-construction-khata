package com.goraya.constructionkhata.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goraya.constructionkhata.MainViewModel
import com.goraya.constructionkhata.data.DiaryEntry
import com.goraya.constructionkhata.data.Location
import com.goraya.constructionkhata.logic.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------- مزید ----------------
@Composable
fun MoreScreen(go: (String) -> Unit) {
    Screen("مزید", null) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).verticalScroll(rememberScrollState())) {
            ButtonGrid(listOf(
                "مزدوری درج کریں" to { go("labour") },
                "دکاندار اور ادھار" to { go("parties") },
                "تمام اندراجات" to { go("transactions") },
                "رپورٹس" to { go("reports") },
                "بجٹ" to { go("budget") },
                "روزانہ سائٹ ڈائری" to { go("diary") },
                "تعمیر کی جگہیں" to { go("locations") },
                "بیک اَپ اور بحالی" to { go("backup") },
                "سیٹنگز" to { go("settings") }
            ))
        }
    }
}

@Composable
fun BackupScreen(onBack: () -> Unit) {
    Screen("بیک اَپ اور بحالی", onBack) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("یہ حصہ ابھی شامل نہیں کیا گیا۔", style = MaterialTheme.typography.titleMedium)
            Text("مقامی بیک اَپ فائل اور آن لائن بیک اَپ اگلے مرحلے میں بنیں گے۔ تب تک آپ کا ڈیٹا صرف اسی موبائل میں محفوظ ہے؛ ایپ ان انسٹال کرنے یا موبائل خراب ہونے پر ضائع ہو سکتا ہے۔ ایپ ان انسٹال نہ کریں۔")
        }
    }
}

// ---------------- تمام اندراجات ----------------
@Composable
fun TransactionsScreen(vm: MainViewModel, go: (String) -> Unit, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    var from by remember { mutableStateOf(Dates.monthStart(Dates.today())) }
    var to by remember { mutableStateOf(Dates.today()) }
    var q by remember { mutableStateOf("") }
    val list = d.expenses.filter { it.date in from..to && it.itemName.contains(q.trim()) }
    Screen("تمام اندراجات", onBack) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("تاریخ سے", from, { from = it }, Modifier.weight(1f))
                    DateField("تاریخ تک", to, { to = it }, Modifier.weight(1f))
                }
            }
            item { Field("آئٹم تلاش کریں", q, { q = it }) }
            item { Text("کل: ${rs(list.sumOf { it.total })}  (${list.size} اندراج)", fontWeight = FontWeight.Bold) }
            if (list.isEmpty()) item { Hint("اس مدت میں کوئی اندراج نہیں۔") }
            items(list, key = { it.id }) { e ->
                val paid = Calc.paidFor(e.id, d.payments)
                Card(Modifier.fillMaxWidth().clickable { go("expense/${e.id}/0") }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${Dates.fmt(e.date)}  ${e.itemName}")
                        Text("${Reports.partyName(d, e.partyId)} | ${Reports.locName(d, e.locationId)}", style = MaterialTheme.typography.bodySmall)
                        StatRow("کل ${Money.group(e.total)} | ادا ${Money.group(paid)}", "باقی ${Money.group(e.total - paid)}")
                    }
                }
            }
        }
    }
}

// ---------------- ڈائری ----------------
@Composable
fun DiaryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    var edit by remember { mutableStateOf<DiaryEntry?>(null) }
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    Screen("روزانہ سائٹ ڈائری", onBack, floating = {
        FloatingActionButton(onClick = { edit = DiaryEntry(date = Dates.today()) }) { Icon(Icons.Default.Add, "نئی ڈائری") }
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (d.diary.isEmpty()) item { Hint("ابھی کوئی ڈائری نہیں۔ + دبائیں۔") }
            items(d.diary, key = { it.id }) { e ->
                Card(Modifier.fillMaxWidth().clickable { edit = e }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(Dates.fmt(e.date), fontWeight = FontWeight.Bold)
                        if (e.work.isNotBlank()) Text("کام: ${e.work}")
                        if (e.labourCount > 0) Text("مزدور: ${e.labourCount}")
                        if (e.pendingWork.isNotBlank()) Text("باقی کام: ${e.pendingWork}")
                        if (e.problems.isNotBlank()) Text("مسئلہ: ${e.problems}")
                        Thumb(e.photoPath)
                    }
                }
            }
        }
        edit?.let { x ->
            DiaryDialog(x, { vm.saveDiary(it) }, { dlg = Dlg.Confirm("یہ ڈائری حذف کریں؟") { vm.deleteDiary(x) } }) { edit = null }
        }
        DlgHost(dlg, emptyList()) { dlg = null }
    }
}

@Composable
private fun DiaryDialog(x: DiaryEntry, onSave: (DiaryEntry) -> Unit, onDelete: () -> Unit, dismiss: () -> Unit) {
    val ctx = LocalContext.current
    var date by remember { mutableStateOf(x.date) }
    var work by remember { mutableStateOf(x.work) }
    var workers by remember { mutableStateOf(x.workers) }
    var cnt by remember { mutableStateOf(if (x.labourCount > 0) x.labourCount.toString() else "") }
    var arrived by remember { mutableStateOf(x.materialArrived) }
    var used by remember { mutableStateOf(x.materialUsed) }
    var pending by remember { mutableStateOf(x.pendingWork) }
    var problems by remember { mutableStateOf(x.problems) }
    var extra by remember { mutableStateOf(x.extraPurchase) }
    var loss by remember { mutableStateOf(x.loss) }
    var ret by remember { mutableStateOf(x.returned) }
    var photo by remember { mutableStateOf(x.photoPath) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) Files.copyImage(ctx, uri)?.let { photo = it }
    }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("سائٹ ڈائری") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("تاریخ", date, { date = it })
                Field("آج کیا کام ہوا؟", work, { work = it }, singleLine = false)
                Field("کون سے کاریگر آئے؟", workers, { workers = it })
                MoneyField("کتنے مزدور تھے؟", cnt) { cnt = it }
                Field("کون سا سامان پہنچا؟", arrived, { arrived = it }, singleLine = false)
                Field("کون سا سامان استعمال ہوا؟", used, { used = it }, singleLine = false)
                Field("کون سا کام باقی ہے؟", pending, { pending = it }, singleLine = false)
                Field("کام میں کیا مسئلہ آیا؟", problems, { problems = it }, singleLine = false)
                Field("آج کی اضافی خریداری", extra, { extra = it })
                Field("ٹوٹ پھوٹ یا ضیاع", loss, { loss = it })
                Field("سامان کی واپسی", ret, { ret = it })
                OutlinedButton(onClick = { pick.launch("image/*") }) { Text(if (photo.isBlank()) "آج کی تصویر" else "تصویر تبدیل کریں") }
                Thumb(photo)
                if (x.id != 0L) TextButton(onClick = { dismiss(); onDelete() }) { Text("ڈائری حذف کریں", color = Color.Red) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(x.copy(date = date, work = work.trim(), workers = workers.trim(), labourCount = cnt.toIntOrNull() ?: 0,
                    materialArrived = arrived.trim(), materialUsed = used.trim(), pendingWork = pending.trim(), problems = problems.trim(),
                    extraPurchase = extra.trim(), loss = loss.trim(), returned = ret.trim(), photoPath = photo))
                dismiss()
            }) { Text("محفوظ کریں") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("منسوخ") } }
    )
}

// ---------------- بجٹ ----------------
@Composable
fun BudgetScreen(vm: MainViewModel, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    val ctx = LocalContext.current
    var text by remember(d.budget) { mutableStateOf(if (d.budget > 0) d.budget.toString() else "") }
    val s = Calc.dashboard(d, Dates.today())
    val byCat = d.expenses.groupBy { Reports.catName(d, it.categoryId) }.mapValues { e -> e.value.sumOf { it.total } }.toList().sortedByDescending { it.second }
    val byLoc = d.expenses.groupBy { Reports.locName(d, it.locationId) }.mapValues { e -> e.value.sumOf { it.total } }.toList().sortedByDescending { it.second }
    Screen("بجٹ", onBack) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MoneyField("پورے گھر کا تخمینی بجٹ (Rs.)", text) { text = it }
            Button(onClick = { vm.setSetting("budget", (Money.parse(text) ?: 0L).toString()); toast(ctx, "بجٹ محفوظ ہو گیا") }, Modifier.fillMaxWidth()) { Text("بجٹ محفوظ کریں") }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    StatRow("تخمینی بجٹ", rs(s.budget))
                    StatRow("اب تک کا حقیقی خرچہ", rs(s.totalSpend), bold = true)
                    if (s.budget > 0) StatRow(if (s.budgetLeft >= 0) "باقی بجٹ" else "بجٹ سے زیادہ خرچہ", rs(Math.abs(s.budgetLeft)), if (s.budgetLeft >= 0) Color(0xFF2E7D32) else Color.Red)
                    StatRow("سامان پر خرچہ", rs(s.materialSpend))
                    StatRow("مزدوری پر خرچہ", rs(s.labourSpend))
                    StatRow("دکانداروں کا ادھار", rs(s.vendorDue))
                    StatRow("کاریگروں کے باقی واجبات", rs(s.workerDue))
                }
            }
            if (s.budget > 0 && s.totalSpend * 100 >= s.budget * 90) Text(if (s.totalSpend > s.budget) "وارننگ: بجٹ سے زیادہ خرچہ ہو گیا ہے!" else "وارننگ: بجٹ کی 90٪ حد پہنچ گئی ہے۔", color = Color.Red)
            Text("کالم کے حساب سے خرچہ", style = MaterialTheme.typography.titleMedium)
            byCat.forEach { StatRow(it.first, rs(it.second)) }
            Text("جگہ (کمرے / واش روم) کے حساب سے خرچہ", style = MaterialTheme.typography.titleMedium)
            byLoc.forEach { StatRow(if (it.first == "-") "جگہ منتخب نہیں" else it.first, rs(it.second)) }
        }
    }
}

// ---------------- جگہیں ----------------
@Composable
fun LocationsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    Screen("تعمیر کی جگہیں", onBack, actions = {
        TextButton(onClick = { dlg = Dlg.Prompt("نئی جگہ", "نام") { vm.addLocation(it) } }) { Text("نئی جگہ") }
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(d.locations, key = { it.id }) { l: Location ->
                val spent = d.expenses.filter { it.locationId == l.id }.sumOf { it.total }
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text(l.name + if (l.active) "" else " (غیر فعال)")
                            Text("خرچہ: ${rs(spent)}", style = MaterialTheme.typography.bodySmall)
                        }
                        RowMenu(listOf(
                            "نام تبدیل کریں" to { dlg = Dlg.Prompt("جگہ کا نام", "نام", l.name) { vm.saveLocation(l.copy(name = it), "جگہ کا نام تبدیل") } },
                            (if (l.active) "غیر فعال کریں" else "بحال کریں") to { vm.saveLocation(l.copy(active = !l.active), if (l.active) "جگہ غیر فعال" else "جگہ بحال") }
                        ))
                    }
                }
            }
        }
        DlgHost(dlg, emptyList()) { dlg = null }
    }
}

// ---------------- سیٹنگز ----------------
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    val ctx = LocalContext.current
    var name by remember(d.settings["project_name"]) { mutableStateOf(d.settings["project_name"] ?: "میرا گھر") }
    val fmt = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US) }
    Screen("سیٹنگز", onBack) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("پروجیکٹ کا نام", name, { name = it })
            Button(onClick = { vm.setSetting("project_name", name.trim()); toast(ctx, "محفوظ ہو گیا") }, Modifier.fillMaxWidth()) { Text("محفوظ کریں") }
            Text("اپنے بنائے ہوئے یونٹس: " + ((d.settings["units"] ?: "").split("|").filter { it.isNotBlank() }.joinToString("، ").ifBlank { "کوئی نہیں" }))
            Text("(نیا یونٹ خرچہ کے فارم میں '+ اپنا نیا یونٹ بنائیں' سے بنتا ہے۔)", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("تبدیلیوں کا ریکارڈ (آخری 100)", style = MaterialTheme.typography.titleMedium)
            d.audit.forEach { Text("${fmt.format(Date(it.time))}  ${it.action}: ${it.detail}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

// ---------------- رپورٹس ----------------
@Composable
fun ReportsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val d by vm.data.collectAsState()
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf(Reports.Kind.STATEMENT) }
    var from by remember { mutableStateOf(Dates.today()) }
    var to by remember { mutableStateOf(Dates.today()) }
    var table by remember { mutableStateOf<ReportTable?>(null) }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val t = table
        if (uri != null && t != null) toast(ctx, if (Files.saveCsv(ctx, uri, t)) "CSV محفوظ ہو گئی" else "CSV محفوظ نہیں ہو سکی")
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val t = table
        if (uri != null && t != null) toast(ctx, if (Files.savePdf(ctx, uri, t)) "PDF محفوظ ہو گئی" else "PDF محفوظ نہیں ہو سکی")
    }
    val today = Dates.today()
    Screen("رپورٹس", onBack) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DropdownField("رپورٹ کی قسم", Reports.Kind.values().map { it to it.title }, kind, { kind = it; table = null })
            if (kind.ranged) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { from = today; to = today }) { Text("آج") }
                    OutlinedButton(onClick = { from = today - 1; to = today - 1 }) { Text("کل") }
                    OutlinedButton(onClick = { from = today - 6; to = today }) { Text("ہفتہ") }
                    OutlinedButton(onClick = { from = Dates.monthStart(today); to = today }) { Text("مہینہ") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("تاریخ سے", from, { from = it }, Modifier.weight(1f))
                    DateField("تاریخ تک", to, { to = it }, Modifier.weight(1f))
                }
            }
            Button(onClick = { table = Reports.build(kind, from, to, d) }, Modifier.fillMaxWidth().height(50.dp)) { Text("رپورٹ دکھائیں") }
            table?.let { t ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { csv.launch("GORAYA_Report_${Dates.fmt(today)}.csv") }, Modifier.weight(1f)) { Text("CSV ایکسپورٹ") }
                    OutlinedButton(onClick = { pdf.launch("GORAYA_Report_${Dates.fmt(today)}.pdf") }, Modifier.weight(1f)) { Text("PDF ایکسپورٹ") }
                }
                Text(t.title, style = MaterialTheme.typography.titleMedium)
                if (t.subtitle.isNotBlank()) Text(t.subtitle)
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Row { t.headers.forEach { Text(it, Modifier.width(105.dp).padding(4.dp), fontWeight = FontWeight.Bold) } }
                    HorizontalDivider()
                    if (t.rows.isEmpty()) Hint("اس مدت میں کوئی ریکارڈ نہیں۔")
                    t.rows.forEach { r -> Row { r.forEachIndexed { i, c -> Text(t.cell(c, i), Modifier.width(105.dp).padding(4.dp)) } } }
                    t.footer?.let { f ->
                        HorizontalDivider()
                        Row { f.forEachIndexed { i, c -> Text(t.cell(c, i), Modifier.width(105.dp).padding(4.dp), fontWeight = FontWeight.Bold) } }
                    }
                }
            }
        }
    }
}
