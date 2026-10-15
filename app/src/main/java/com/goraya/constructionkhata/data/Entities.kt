package com.goraya.constructionkhata.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "sub_categories", indices = [Index("categoryId")])
data class SubCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val active: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "items", indices = [Index("subCategoryId")])
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subCategoryId: Long,
    val name: String,
    val defaultUnit: String = "",
    val description: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "locations")
data class Location(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true,
    val sortOrder: Int = 0
)

/** type: VENDOR (دکاندار) یا WORKER (کاریگر / ٹھیکیدار) */
@Entity(tableName = "parties")
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "VENDOR",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val active: Boolean = true
)

/** تمام رقمیں پورے روپوں (Long) میں۔ date = epochDay */
@Entity(tableName = "expenses", indices = [Index("date"), Index("partyId"), Index("categoryId"), Index("locationId"), Index("itemId")])
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val itemId: Long? = null,
    val itemName: String,
    val description: String = "",
    val locationId: Long? = null,
    val partyId: Long? = null,
    val quantity: Double = 1.0,
    val unit: String = "عدد",
    val rate: Long = 0,
    val total: Long = 0,
    val isLabour: Boolean = false,
    val receiptPath: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** kind: PAYMENT / ADVANCE (رقم نکلی) یا REFUND (رقم واپس ملی) */
@Entity(tableName = "payments", indices = [Index("date"), Index("partyId"), Index("expenseId")])
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val partyId: Long? = null,
    val expenseId: Long? = null,
    val amount: Long,
    val method: String = "نقد",
    val kind: String = "PAYMENT",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** source: OPENING / OWNER / OTHER */
@Entity(tableName = "cash_ins", indices = [Index("date")])
data class CashIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val amount: Long,
    val account: String = "نقد",
    val source: String = "OWNER",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transfers", indices = [Index("date")])
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val fromAccount: String,
    val toAccount: String,
    val amount: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "diary", indices = [Index("date")])
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val work: String = "",
    val workers: String = "",
    val labourCount: Int = 0,
    val materialArrived: String = "",
    val materialUsed: String = "",
    val pendingWork: String = "",
    val problems: String = "",
    val extraPurchase: String = "",
    val loss: String = "",
    val returned: String = "",
    val photoPath: String = ""
)

@Entity(tableName = "settings")
data class Setting(@PrimaryKey val key: String, val value: String)

@Entity(tableName = "audit_log")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val action: String,
    val detail: String
)
