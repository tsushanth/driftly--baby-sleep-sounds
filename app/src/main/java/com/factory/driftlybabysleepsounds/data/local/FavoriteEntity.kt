package com.factory.driftlybabysleepsounds.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val soundId: String,
    val addedAt: Long
)
