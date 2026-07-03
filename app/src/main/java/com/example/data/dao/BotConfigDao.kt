package com.example.data.dao

import androidx.room.*
import com.example.data.model.BotConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface BotConfigDao {
    @Query("SELECT * FROM bot_configs WHERE id = 1")
    fun getBotConfigFlow(): Flow<BotConfig?>

    @Query("SELECT * FROM bot_configs WHERE id = 1")
    suspend fun getBotConfig(): BotConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: BotConfig)
}
