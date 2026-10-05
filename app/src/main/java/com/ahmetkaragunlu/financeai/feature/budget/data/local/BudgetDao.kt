package com.ahmetkaragunlu.financeai.feature.budget.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long
    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
    @Query("SELECT * FROM budget_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0)")
    fun observeBudgets(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budget_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND budgetType = 'GENERAL_MONTHLY' LIMIT 1")
    fun observeGeneralBudget(): Flow<BudgetEntity?>
    @Query("SELECT * FROM budget_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND category = :category LIMIT 1")
    suspend fun getBudgetByCategory(category: CategoryType): BudgetEntity?

    @Query("SELECT * FROM budget_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0) AND firestoreId = :firestoreId LIMIT 1")
    suspend fun getBudgetByFirestoreId(firestoreId: String): BudgetEntity?

    @Query("SELECT * FROM budget_table WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0)")
    suspend fun getAllBudgetsOneShot(): List<BudgetEntity>
}
