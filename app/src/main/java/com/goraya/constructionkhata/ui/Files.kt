package com.goraya.constructionkhata.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import com.goraya.constructionkhata.logic.Csv
import com.goraya.constructionkhata.logic.ReportTable
import java.io.File
import java.io.OutputStream
import java.util.UUID

fun toast(c: Context, m: String) = Toast.makeText(c, m, Toast.LENGTH_SHORT).show()

object Files {
    fun saveCsv(ctx: Context, uri: Uri, t: ReportTable): Boolean = try {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(("\uFEFF" + Csv.build(t)).toByteArray(Charsets.UTF_8)) }
        true
    } catch (e: Exception) { false }

    fun savePdf(ctx: Context, uri: Uri, t: ReportTable): Boolean = try {
        ctx.contentResolver.openOutputStream(uri)?.use { writePdf(t, it) }
        true
    } catch (e: Exception) { false }

    /** منتخب تصویر کو ایپ کے اپنے فولڈر میں کاپی کرتا ہے تاکہ اجازت ختم ہونے پر بھی رہے */
    fun copyImage(ctx: Context, uri: Uri): String? = try {
        val dir = File(ctx.filesDir, "images").apply { mkdirs() }
        val f = File(dir, UUID.randomUUID().toString() + ".jpg")
        ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        f.absolutePath
    } catch (e: Exception) { null }

    fun bitmap(path: String): Bitmap? = try {
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 })
    } catch (e: Exception) { null }

    private fun writePdf(t: ReportTable, out: OutputStream) {
        val doc = PdfDocument()
        val pw = 595; val ph = 842; val m = 30f
        val base = TextPaint().apply { isAntiAlias = true; textSize = 10f; color = Color.BLACK; textAlign = Paint.Align.RIGHT }
        val bold = TextPaint(base).apply { isFakeBoldText = true }
        val big = TextPaint(base).apply { textSize = 16f; isFakeBoldText = true }
        val colW = (pw - 2 * m) / t.headers.size
        fun cell(c: android.graphics.Canvas, text: String, col: Int, y: Float, p: TextPaint) {
            val x = pw - m - col * colW
            val s = TextUtils.ellipsize(text, p, colW - 8, TextUtils.TruncateAt.END).toString()
            c.drawText(s, x - 4, y, p)
        }
        val all = t.rows.map { it to false } + (t.footer?.let { listOf(it to true) } ?: emptyList())
        var idx = 0; var pageNo = 1
        do {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, pageNo).create())
            val c = page.canvas
            var y = m + 10
            if (pageNo == 1) {
                c.drawText(t.title, pw - m, y, big); y += 20
                if (t.subtitle.isNotBlank()) { c.drawText(t.subtitle, pw - m, y, base); y += 18 }
                y += 6
            }
            t.headers.forEachIndexed { i, h -> cell(c, h, i, y, bold) }
            y += 6; c.drawLine(m, y, pw - m, y, base); y += 14
            while (idx < all.size && y < ph - m) {
                val (r, isFooter) = all[idx]
                r.forEachIndexed { i, v -> cell(c, t.cell(v, i), i, y, if (isFooter) bold else base) }
                y += 18; idx++
            }
            doc.finishPage(page); pageNo++
        } while (idx < all.size)
        doc.writeTo(out)
        doc.close()
    }
}
