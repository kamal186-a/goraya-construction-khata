package com.goraya.constructionkhata.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, id") fun categories(): Flow<List<Category>>
    @Query("SELECT * FROM sub_categories ORDER BY sortOrder, id") fun subCategories(): Flow<List<SubCategory>>
    @Query("SELECT * FROM items ORDER BY sortOrder, id") fun items(): Flow<List<Item>>
    @Query("SELECT * FROM locations ORDER BY sortOrder, id") fun locations(): Flow<List<Location>>
    @Query("SELECT * FROM parties ORDER BY name") fun parties(): Flow<List<Party>>
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC") fun expenses(): Flow<List<Expense>>
    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC") fun payments(): Flow<List<Payment>>
    @Query("SELECT * FROM cash_ins ORDER BY date DESC, id DESC") fun cashIns(): Flow<List<CashIn>>
    @Query("SELECT * FROM transfers ORDER BY date DESC, id DESC") fun transfers(): Flow<List<Transfer>>
    @Query("SELECT * FROM diary ORDER BY date DESC, id DESC") fun diary(): Flow<List<DiaryEntry>>
    @Query("SELECT * FROM settings") fun settings(): Flow<List<Setting>>
    @Query("SELECT * FROM audit_log ORDER BY id DESC LIMIT 100") fun audit(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCategory(x: Category): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSub(x: SubCategory): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveItem(x: Item): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveLocation(x: Location): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveParty(x: Party): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveExpense(x: Expense): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePayment(x: Payment): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCashIn(x: CashIn): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTransfer(x: Transfer): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDiary(x: DiaryEntry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSetting(x: Setting)
    @Insert suspend fun addAudit(x: AuditLog)

    @Query("DELETE FROM categories WHERE id = :id") suspend fun deleteCategory(id: Long)
    @Query("DELETE FROM sub_categories WHERE id = :id") suspend fun deleteSub(id: Long)
    @Query("DELETE FROM items WHERE id = :id") suspend fun deleteItem(id: Long)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun deleteExpense(id: Long)
    @Query("DELETE FROM payments WHERE id = :id") suspend fun deletePayment(id: Long)
    @Query("DELETE FROM payments WHERE expenseId = :expenseId") suspend fun deletePaymentsOfExpense(expenseId: Long)
    @Query("DELETE FROM cash_ins WHERE id = :id") suspend fun deleteCashIn(id: Long)
    @Query("DELETE FROM transfers WHERE id = :id") suspend fun deleteTransfer(id: Long)
    @Query("DELETE FROM diary WHERE id = :id") suspend fun deleteDiary(id: Long)
    @Query("UPDATE payments SET partyId = :partyId WHERE expenseId = :expenseId")
    suspend fun updatePaymentsParty(expenseId: Long, partyId: Long?)

    @Query("SELECT COUNT(*) FROM categories") suspend fun categoryCount(): Int
    @Query("SELECT COUNT(*) FROM sub_categories WHERE categoryId = :id") suspend fun subCountForCat(id: Long): Int
    @Query("SELECT COUNT(*) FROM items WHERE subCategoryId = :id") suspend fun itemCountForSub(id: Long): Int
    @Query("SELECT COUNT(*) FROM expenses WHERE itemId = :id") suspend fun expenseCountForItem(id: Long): Int
}
