package com.factory.driftlybabysleepsounds.audio

import kotlin.random.Random

class WhiteNoiseGenerator(private val random: Random = Random(System.nanoTime())) : NoiseGenerator {
    override fun fill(buffer: FloatArray, length: Int) {
        for (i in 0 until length) {
            buffer[i] = nextWhite(random) * 0.5f
        }
    }
}

/**
 * Paul Kellet's "economy" pink-noise filter: a bank of leaky integrators at different
 * time constants whose sum approximates a -3dB/octave spectrum.
 */
class PinkNoiseGenerator(private val random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var b3 = 0f
    private var b4 = 0f
    private var b5 = 0f
    private var b6 = 0f

    override fun fill(buffer: FloatArray, length: Int) {
        for (i in 0 until length) {
            val white = nextWhite(random)
            b0 = 0.99886f * b0 + white * 0.0555179f
            b1 = 0.99332f * b1 + white * 0.0750759f
            b2 = 0.96900f * b2 + white * 0.1538520f
            b3 = 0.86650f * b3 + white * 0.3104856f
            b4 = 0.55000f * b4 + white * 0.5329522f
            b5 = -0.7616f * b5 - white * 0.0168980f
            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f
            b6 = white * 0.115926f
            buffer[i] = clamp(pink * 0.11f)
        }
    }
}

/** A random walk (leaky integration of white noise) approximates -6dB/octave brown noise. */
class BrownNoiseGenerator(private val random: Random = Random(System.nanoTime())) : NoiseGenerator {
    private var lastOut = 0f

    override fun fill(buffer: FloatArray, length: Int) {
        for (i in 0 until length) {
            val white = nextWhite(random)
            val out = (lastOut + 0.02f * white) / 1.02f
            lastOut = out
            buffer[i] = clamp(out * 3.5f)
        }
    }
}
