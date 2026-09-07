package com.factory.driftlybabysleepsounds.data.repository

import com.factory.driftlybabysleepsounds.data.local.FavoriteDao
import com.factory.driftlybabysleepsounds.data.local.FavoriteEntity
import com.factory.driftlybabysleepsounds.data.local.MixDao
import com.factory.driftlybabysleepsounds.data.local.MixEntity
import com.factory.driftlybabysleepsounds.data.local.MixSoundConfig
import com.factory.driftlybabysleepsounds.data.local.toSoundsSpec
import com.factory.driftlybabysleepsounds.data.model.Sound
import com.factory.driftlybabysleepsounds.data.model.SoundCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SoundRepository(
    private val favoriteDao: FavoriteDao,
    private val mixDao: MixDao
) {
    val allSounds: List<Sound> = SoundCatalog.sounds

    fun soundById(id: String): Sound? = SoundCatalog.byId(id)

    fun observeFavoriteIds(): Flow<Set<String>> =
        favoriteDao.observeFavorites().map { list -> list.map { it.soundId }.toSet() }

    suspend fun toggleFavorite(soundId: String) {
        if (favoriteDao.isFavorite(soundId)) {
            favoriteDao.deleteById(soundId)
        } else {
            favoriteDao.insert(FavoriteEntity(soundId = soundId, addedAt = System.currentTimeMillis()))
        }
    }

    fun observeMixes(): Flow<List<MixEntity>> = mixDao.observeMixes()

    suspend fun saveMix(name: String, sounds: List<MixSoundConfig>): Long {
        return mixDao.insert(
            MixEntity(
                name = name,
                soundsSpec = sounds.toSoundsSpec(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMix(mix: MixEntity) {
        mixDao.delete(mix)
    }
}
