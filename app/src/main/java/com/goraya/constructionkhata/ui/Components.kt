@file:OptIn(ExperimentalMaterial3Api::class)

package com.goraya.constructionkhata.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.goraya.constructionkhata.data.Item
import com.goraya.constructionkhata.logic.Dates
import com.goraya.constructionkhata.logic.Money
import java.time.LocalDate

@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    floating: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "واپس") } },
                actions = actions
            )
        },
        floatingActionButton = floating,
        content = content
    )
}

@Composable
fun Field(
    label: String, value: String, onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, enabled: Boolean = true
) = OutlinedTextField(
    value = value, onValueChange = onChange, modifier = modifier, label = { Text(label) },
    singleLine = singleLine, enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = keyboard)
)

@Composable
fun MoneyField(label: String, value: String, onChange: (String) -> Unit) =
    Field(label, value, { onChange(it.filter { c -> Character.isDigit(c) }) }, keyboard = KeyboardType.Number)

@Composable
fun QtyField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) =
    Field(label, value, { onChange(it.filter { c -> Character.isDigit(c) || c == '.' }) }, modifier, KeyboardType.Decimal)

@Composable
fun <T> DropdownField(label: String, options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var open by remember { mutableStateOf(false) }
    val text = options.firstOrNull { it.first == selected }?.second ?: ""
    Box(modifier) {
        OutlinedTextField(
            value = text, onValueChange = {}, readOnly = true, label = { Text(label) },
            modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
        )
        Box(Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); open = false }) }
        }
    }
}

@Composable
fun DateField(label: String, day: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val ctx = LocalContext.current
    Box(modifier) {
        OutlinedTextField(value = Dates.fmt(day), onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        Box(Modifier.matchParentSize().clickable {
            val d = LocalDate.ofEpochDay(day)
            DatePickerDialog(ctx, { _, y, m, dd -> onChange(LocalDate.of(y, m + 1, dd).toEpochDay()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        })
    }
}

@Composable
fun StatRow(label: String, value: String, color: Color = Color.Unspecified, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else null)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ButtonGrid(buttons: List<Pair<String, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (t, a) -> Button(onClick = a, modifier = Modifier.weight(1f).height(52.dp)) { Text(t) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RowMenu(actions: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, "مزید") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { (t, a) -> DropdownMenuItem(text = { Text(t) }, onClick = { open = false; a() }) }
        }
    }
}

@Composable
fun Thumb(path: String) {
    val bmp = remember(path) { if (path.isBlank()) null else Files.bitmap(path) }
    if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(90.dp))
}

@Composable
fun Hint(text: String) = Text(text, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)

/** ایک ہی جگہ سے تمام عام ڈائیلاگ */
sealed class Dlg {
    class Prompt(val title: String, val label: String, val initial: String = "", val onOk: (String) -> Unit) : Dlg()
    class ItemD(val title: String, val item: Item?, val onOk: (String, String, String) -> Unit) : Dlg()
    class Confirm(val msg: String, val onYes: () -> Unit) : Dlg()
}

@Composable
fun DlgHost(dlg: Dlg?, units: List<String>, dismiss: () -> Unit) {
    when (dlg) {
        is Dlg.Prompt -> {
            var v by remember(dlg) { mutableStateOf(dlg.initial) }
            AlertDialog(
                onDismissRequest = dismiss, title = { Text(dlg.title) },
                text = { Field(dlg.label, v, { v = it }) },
                confirmButton = { TextButton(onClick = { if (v.isNotBlank()) { dlg.onOk(v.trim()); dismiss() } }) { Text("محفوظ کریں") } },
                dismissButton = { TextButton(onClick = dismiss) { Text("منسوخ") } }
            )
        }
        is Dlg.ItemD -> {
            var n by remember(dlg) { mutableStateOf(dlg.item?.name ?: "") }
            var u by remember(dlg) { mutableStateOf(dlg.item?.defaultUnit ?: "") }
            var ds by remember(dlg) { mutableStateOf(dlg.item?.description ?: "") }
            AlertDialog(
                onDismissRequest = dismiss, title = { Text(dlg.title) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field("آئٹم کا نام (اردو میں)", n, { n = it })
                        DropdownField("ڈیفالٹ یونٹ (اختیاری)", listOf("" to "— کوئی نہیں —") + units.map { it to it }, u, { u = it })
                        Field("تفصیل (اختیاری)", ds, { ds = it })
                    }
                },
                confirmButton = { TextButton(onClick = { if (n.isNotBlank()) { dlg.onOk(n.trim(), u, ds.trim()); dismiss() } }) { Text("محفوظ کریں") } },
                dismissButton = { TextButton(onClick = dismiss) { Text("منسوخ") } }
            )
        }
        is Dlg.Confirm -> AlertDialog(
            onDismissRequest = dismiss, title = { Text("تصدیق") }, text = { Text(dlg.msg) },
            confirmButton = { TextButton(onClick = { dlg.onYes(); dismiss() }) { Text("ہاں") } },
            dismissButton = { TextButton(onClick = dismiss) { Text("نہیں") } }
        )
        null -> {}
    }
}

@Composable
fun PartyDialog(title: String, type: String, initial: com.goraya.constructionkhata.data.Party?, onOk: (com.goraya.constructionkhata.data.Party) -> Unit, dismiss: () -> Unit) {
    var n by remember { mutableStateOf(initial?.name ?: "") }
    var ph by remember { mutableStateOf(initial?.phone ?: "") }
    var ad by remember { mutableStateOf(initial?.address ?: "") }
    var nt by remember { mutableStateOf(initial?.notes ?: "") }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("نام", n, { n = it })
                Field("فون نمبر", ph, { ph = it }, keyboard = KeyboardType.Phone)
                Field("پتہ", ad, { ad = it })
                Field("نوٹس", nt, { nt = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (n.isNotBlank()) {
                    onOk((initial ?: com.goraya.constructionkhata.data.Party(name = n, type = type)).copy(name = n.trim(), phone = ph.trim(), address = ad.trim(), notes = nt.trim()))
                    dismiss()
                }
            }) { Text("محفوظ کریں") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("منسوخ") } }
    )
}

fun fmtQty(q: Double): String = if (q == Math.floor(q) && q < 1e12) q.toLong().toString() else q.toString()
fun rs(v: Long) = Money.format(v)
