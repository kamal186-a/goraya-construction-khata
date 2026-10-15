package com.goraya.constructionkhata.logic

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Money {
    /** کل = مقدار × فی یونٹ قیمت، پورے روپے تک گول (BigDecimal، floating-point نہیں) */
    fun total(qty: Double, rate: Long): Long =
        BigDecimal.valueOf(qty).multiply(BigDecimal.valueOf(rate)).setScale(0, RoundingMode.HALF_UP).toLong()

    private fun normalize(s: String): String = buildString {
        for (c in s) when {
            Character.isDigit(c) -> append(Character.digit(c, 10))
            c == '.' || c == '٫' -> append('.')
        }
    }

    fun parse(s: String): Long? = normalize(s).substringBefore('.').toLongOrNull()
    fun parseQty(s: String): Double? = normalize(s).toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }

    fun group(v: Long): String = String.format(Locale.US, "%,d", v)
    fun group(raw: String): String = raw.toLongOrNull()?.let { group(it) } ?: raw
    fun format(v: Long): String = if (v < 0) "-Rs. ${group(-v)}" else "Rs. ${group(v)}"
}

object Dates {
    private val f = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    fun today(): Long = LocalDate.now().toEpochDay()
    fun fmt(day: Long): String = LocalDate.ofEpochDay(day).format(f)
    fun monthStart(day: Long): Long = LocalDate.ofEpochDay(day).withDayOfMonth(1).toEpochDay()
}
