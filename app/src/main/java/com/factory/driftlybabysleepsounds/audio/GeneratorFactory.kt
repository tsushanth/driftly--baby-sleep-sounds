package com.factory.driftlybabysleepsounds.audio

import com.factory.driftlybabysleepsounds.data.model.GeneratorType

object GeneratorFactory {
    fun create(type: GeneratorType): NoiseGenerator = when (type) {
        GeneratorType.WHITE_NOISE -> WhiteNoiseGenerator()
        GeneratorType.PINK_NOISE -> PinkNoiseGenerator()
        GeneratorType.BROWN_NOISE -> BrownNoiseGenerator()
        GeneratorType.FAN -> FanGenerator()
        GeneratorType.RAIN -> RainGenerator()
        GeneratorType.OCEAN_WAVES -> OceanWaveGenerator()
        GeneratorType.WIND -> WindGenerator()
        GeneratorType.HEARTBEAT -> HeartbeatGenerator()
        GeneratorType.MUSIC_BOX_LULLABY -> MusicBoxLullabyGenerator()
        GeneratorType.PIANO_LULLABY -> PianoLullabyGenerator()
    }
}
