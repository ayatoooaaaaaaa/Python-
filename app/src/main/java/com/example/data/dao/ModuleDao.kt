package com.example.data.dao

import androidx.room.*
import com.example.data.model.ModulePackage
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules ORDER BY name ASC")
    fun getAllModules(): Flow<List<ModulePackage>>

    @Query("SELECT * FROM modules WHERE name = :name")
    suspend fun getModuleByName(name: String): ModulePackage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: ModulePackage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModulePackage>)

    @Update
    suspend fun updateModule(module: ModulePackage)

    @Query("UPDATE modules SET isInstalled = :isInstalled, downloadProgress = :progress WHERE name = :name")
    suspend fun updateDownloadState(name: String, isInstalled: Boolean, progress: Float)
}
