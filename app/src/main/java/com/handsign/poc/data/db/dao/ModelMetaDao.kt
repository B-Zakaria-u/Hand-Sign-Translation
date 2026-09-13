package com.handsign.poc.data.db.dao

import androidx.room.*
import com.handsign.poc.data.db.entity.ModelMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meta: ModelMetaEntity): Long

    @Query("SELECT * FROM model_meta ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ModelMetaEntity>>

    @Query("SELECT * FROM model_meta WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ModelMetaEntity?

    @Query("SELECT * FROM model_meta WHERE id = :id")
    suspend fun getById(id: Long): ModelMetaEntity?

    @Transaction
    suspend fun activateModel(id: Long) {
        deactivateAll()
        setActive(id)
    }

    @Query("UPDATE model_meta SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE model_meta SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Query("DELETE FROM model_meta WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM model_meta")
    suspend fun count(): Int
}
