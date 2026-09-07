package com.factory.driftlybabysleepsounds.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Delete
    suspend fun delete(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE soundId = :soundId")
    suspend fun deleteById(soundId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE soundId = :soundId)")
    suspend fun isFavorite(soundId: String): Boolean
}
