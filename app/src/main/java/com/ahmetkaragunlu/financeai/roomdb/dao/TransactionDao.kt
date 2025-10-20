import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transaction_table ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transaction_table WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getAllTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_table WHERE category = :category AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByCategoryAndDate(category: CategoryType, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transaction_table WHERE `transaction` = 'INCOME' AND date BETWEEN :startDate AND :endDate")
    fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transaction_table WHERE `transaction` = 'EXPENSE' AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?>
    @Query("""
    SELECT category, SUM(amount) as totalAmount
    FROM transaction_table
    WHERE `transaction` = :transactionType AND date BETWEEN :startDate AND :endDate
    GROUP BY category
""")
    fun getCategoryByTypeAndDateRange(
        transactionType: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryExpense>>

}
