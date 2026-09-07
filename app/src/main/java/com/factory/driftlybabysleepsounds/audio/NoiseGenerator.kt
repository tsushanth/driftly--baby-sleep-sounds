package com.factory.driftlybabysleepsounds.audio

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val SAMPLE_RATE = 44100

/**
 * Produces a continuous, click-free stream of audio. Implementations keep their own
 * running state so successive calls to [fill] pick up exactly where the last chunk
 * left off (no loop-boundary seams like a sampled/looped clip would have).
 */
interface NoiseGenerator {
    /** Fills [buffer] with the next [length] samples, each in the range [-1f, 1f]. */
    fun fill(buffer: FloatArray, length: Int)
}

internal fun clamp(value: Float): Float = max(-1f, min(1f, value))

internal fun nextWhite(random: Random): Float = random.nextFloat() * 2f - 1f
