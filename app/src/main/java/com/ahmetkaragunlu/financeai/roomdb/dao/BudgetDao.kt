package com.ahmetkaragunlu.financeai.roomdb.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    // Tüm bütçe kurallarını getir (Analiz sayfasındaki liste için)
    @Query("SELECT * FROM budget_table")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    // Sadece Genel Aylık Bütçeyi getir (En üstteki kart için)
    @Query("SELECT * FROM budget_table WHERE budgetType = 'GENERAL_MONTHLY' LIMIT 1")
    fun getGeneralBudget(): Flow<BudgetEntity?>

    // Belirli bir kategorinin bütçe kuralını kontrol etmek için
    @Query("SELECT * FROM budget_table WHERE category = :category LIMIT 1")
    suspend fun getBudgetByCategory(category: CategoryType): BudgetEntity?

    // Firestore ID'ye göre silme (İleride lazım olacak)
    @Query("DELETE FROM budget_table WHERE firestoreId = :firestoreId")
    suspend fun deleteBudgetByFirestoreId(firestoreId: String)

    // Henüz senkronize olmamış kayıtlar (Sync servisi için)
    @Query("SELECT * FROM budget_table WHERE syncedToFirebase = 0")
    fun getUnsyncedBudgets(): Flow<List<BudgetEntity>>
}