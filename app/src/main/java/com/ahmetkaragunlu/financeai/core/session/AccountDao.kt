package com.ahmetkaragunlu.financeai.core.session

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "account_preferences")
data class AccountPreferences(@PrimaryKey val ownerId: String, val currencyCode: String)

@Entity(tableName = "active_account")
data class ActiveAccountRow(@PrimaryKey val id: Int = 0, val ownerId: String)

@Dao
interface AccountDao {
    @Query("SELECT * FROM account_preferences WHERE ownerId = :ownerId")
    suspend fun get(ownerId: String): AccountPreferences?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(preferences: AccountPreferences)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActive(row: ActiveAccountRow)
    @Query("DELETE FROM active_account")
    suspend fun clearActive()
    @Query("SELECT currencyCode FROM account_preferences WHERE ownerId = (SELECT ownerId FROM active_account WHERE id = 0)")
    fun observeCurrency(): Flow<String?>
}
