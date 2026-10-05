package com.ahmetkaragunlu.financeai.core.session.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.ahmetkaragunlu.financeai.core.session.local.entity.AccountPreferences
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow

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
