package com.ahmetkaragunlu.financeai.feature.budget.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long
    @Update
    suspend fun updateBudget(budget: BudgetEntity)
    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
    @Query("SELECT * FROM budget_table")
    fun observeBudgets(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budget_table WHERE budgetType = 'GENERAL_MONTHLY' LIMIT 1")
    fun observeGeneralBudget(): Flow<BudgetEntity?>
    @Query("SELECT * FROM budget_table WHERE category = :category LIMIT 1")
    suspend fun getBudgetByCategory(category: CategoryType): BudgetEntity?
    @Query("SELECT * FROM budget_table WHERE syncedToFirebase = 0")
    fun observeUnsyncedBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget_table WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getBudgetByFirestoreId(firestoreId: String): BudgetEntity?

    @Query("SELECT * FROM budget_table")
    suspend fun getAllBudgetsOneShot(): List<BudgetEntity>
}
