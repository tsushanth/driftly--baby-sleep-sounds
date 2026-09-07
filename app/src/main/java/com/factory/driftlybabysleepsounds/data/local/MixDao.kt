package com.factory.driftlybabysleepsounds.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MixDao {
    @Query("SELECT * FROM saved_mixes ORDER BY createdAt DESC")
    fun observeMixes(): Flow<List<MixEntity>>

    @Insert
    suspend fun insert(mix: MixEntity): Long

    @Delete
    suspend fun delete(mix: MixEntity)
}
