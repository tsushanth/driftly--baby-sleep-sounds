package com.factory.driftlybabysleepsounds.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cyclone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves

object SoundCatalog {

    val sounds: List<Sound> = listOf(
        Sound(
            id = "white_noise",
            name = "White Noise",
            description = "Even, full-spectrum hush that masks sudden noises",
            category = SoundCategory.WHITE_NOISE,
            generatorType = GeneratorType.WHITE_NOISE,
            icon = Icons.Filled.GraphicEq
        ),
        Sound(
            id = "pink_noise",
            name = "Pink Noise",
            description = "Softer, deeper hiss favored for baby sleep studies",
            category = SoundCategory.WHITE_NOISE,
            generatorType = GeneratorType.PINK_NOISE,
            icon = Icons.Filled.BlurOn,
            isPremium = true
        ),
        Sound(
            id = "brown_noise",
            name = "Brown Noise",
            description = "Deep rumbling hush, like a distant waterfall",
            category = SoundCategory.WHITE_NOISE,
            generatorType = GeneratorType.BROWN_NOISE,
            icon = Icons.Filled.Terrain,
            isPremium = true
        ),
        Sound(
            id = "fan",
            name = "Box Fan",
            description = "Steady whirring fan with a gentle blade hum",
            category = SoundCategory.WHITE_NOISE,
            generatorType = GeneratorType.FAN,
            icon = Icons.Filled.Air,
            isPremium = true
        ),
        Sound(
            id = "rain",
            name = "Gentle Rain",
            description = "Soft rainfall with scattered droplets",
            category = SoundCategory.NATURE,
            generatorType = GeneratorType.RAIN,
            icon = Icons.Filled.WaterDrop
        ),
        Sound(
            id = "ocean_waves",
            name = "Ocean Waves",
            description = "Slow swelling waves rolling onto shore",
            category = SoundCategory.NATURE,
            generatorType = GeneratorType.OCEAN_WAVES,
            icon = Icons.Filled.Waves,
            isPremium = true
        ),
        Sound(
            id = "wind",
            name = "Night Wind",
            description = "Drifting breeze through open air",
            category = SoundCategory.NATURE,
            generatorType = GeneratorType.WIND,
            icon = Icons.Filled.Cyclone
        ),
        Sound(
            id = "heartbeat",
            name = "Heartbeat Womb",
            description = "Warm womb ambience with a steady heartbeat",
            category = SoundCategory.WOMB,
            generatorType = GeneratorType.HEARTBEAT,
            icon = Icons.Filled.MonitorHeart,
            isPremium = true
        ),
        Sound(
            id = "music_box",
            name = "Music Box",
            description = "Delicate wind-up music box melody",
            category = SoundCategory.LULLABY,
            generatorType = GeneratorType.MUSIC_BOX_LULLABY,
            icon = Icons.Filled.Toys,
            isPremium = true
        ),
        Sound(
            id = "piano_lullaby",
            name = "Piano Lullaby",
            description = "Slow, soothing piano melody",
            category = SoundCategory.LULLABY,
            generatorType = GeneratorType.PIANO_LULLABY,
            icon = Icons.Filled.Piano,
            isPremium = true
        )
    )

    fun byId(id: String): Sound? = sounds.find { it.id == id }
}
