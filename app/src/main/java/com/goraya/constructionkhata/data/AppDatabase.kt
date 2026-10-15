package com.goraya.constructionkhata.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Category::class, SubCategory::class, Item::class, Location::class, Party::class,
        Expense::class, Payment::class, CashIn::class, Transfer::class, DiaryEntry::class,
        Setting::class, AuditLog::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "goraya_khata.db")
                .build().also { instance = it }
        }
    }
}
