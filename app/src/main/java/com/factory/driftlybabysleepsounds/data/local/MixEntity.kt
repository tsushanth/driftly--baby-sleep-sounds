package com.factory.driftlybabysleepsounds.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [soundsSpec] encodes the sounds in the mix and their volumes as
 * "soundId:volume,soundId:volume" (volume is a float from 0.0 to 1.0).
 */
@Entity(tableName = "saved_mixes")
data class MixEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val soundsSpec: String,
    val createdAt: Long
)

data class MixSoundConfig(val soundId: String, val volume: Float)

fun MixEntity.parseSounds(): List<MixSoundConfig> {
    if (soundsSpec.isBlank()) return emptyList()
    return soundsSpec.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 2) return@mapNotNull null
        val volume = parts[1].toFloatOrNull() ?: return@mapNotNull null
        MixSoundConfig(soundId = parts[0], volume = volume)
    }
}

fun List<MixSoundConfig>.toSoundsSpec(): String =
    joinToString(",") { "${it.soundId}:${it.volume}" }
