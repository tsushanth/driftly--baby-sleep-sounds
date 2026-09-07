package com.factory.driftlybabysleepsounds.audio

import com.factory.driftlybabysleepsounds.data.model.SleepTimerOption
import com.factory.driftlybabysleepsounds.data.model.Sound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Application-wide singleton owning every currently playing [SoundPlayer]. Kept independent of
 * any Android [android.content.Context] or [android.app.Service] lifecycle so the UI and the
 * playback service can both drive it directly; the service exists only to keep the process
 * alive and show a notification while sounds are active.
 */
object AudioEngine {
    private val players = mutableMapOf<String, SoundPlayer>()
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _activeVolumes = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeVolumes: StateFlow<Map<String, Float>> = _activeVolumes.asStateFlow()

    private val _remainingTimerMillis = MutableStateFlow<Long?>(null)
    val remainingTimerMillis: StateFlow<Long?> = _remainingTimerMillis.asStateFlow()

    private val _activeTimerOption = MutableStateFlow<SleepTimerOption?>(null)
    val activeTimerOption: StateFlow<SleepTimerOption?> = _activeTimerOption.asStateFlow()

    private var timerJob: Job? = null

    /** Invoked whenever the active-sound set transitions to empty, so the host service can stop. */
    var onAllStopped: (() -> Unit)? = null

    fun isActive(soundId: String): Boolean = _activeVolumes.value.containsKey(soundId)

    fun toggleSound(sound: Sound, defaultVolume: Float = 0.8f) {
        if (isActive(sound.id)) {
            stopSound(sound.id)
        } else {
            startSound(sound, defaultVolume)
        }
    }

    fun startSound(sound: Sound, volume: Float) {
        val player = players.getOrPut(sound.id) { SoundPlayer(GeneratorFactory.create(sound.generatorType)) }
        player.start(volume)
        _activeVolumes.update { it + (sound.id to volume) }
    }

    fun setVolume(soundId: String, volume: Float) {
        players[soundId]?.setVolume(volume)
        if (_activeVolumes.value.containsKey(soundId)) {
            _activeVolumes.update { it + (soundId to volume) }
        }
    }

    fun stopSound(soundId: String) {
        players[soundId]?.stop {
            players.remove(soundId)
        }
        _activeVolumes.update { current ->
            val updated = current - soundId
            if (updated.isEmpty()) {
                cancelTimer()
                onAllStopped?.invoke()
            }
            updated
        }
    }

    fun stopAll() {
        players.keys.toList().forEach { id -> players[id]?.stop { players.remove(id) } }
        _activeVolumes.value = emptyMap()
        cancelTimer()
        onAllStopped?.invoke()
    }

    /** Replaces whatever is currently playing with the given set of sounds and volumes. */
    fun applyMix(soundsWithVolumes: List<Pair<Sound, Float>>) {
        val keepIds = soundsWithVolumes.map { it.first.id }.toSet()
        players.keys.filter { it !in keepIds }.forEach { id -> players[id]?.stop { players.remove(id) } }
        soundsWithVolumes.forEach { (sound, volume) -> startSound(sound, volume) }
        _activeVolumes.value = soundsWithVolumes.associate { it.first.id to it.second }
    }

    fun startTimer(option: SleepTimerOption) {
        cancelTimer()
        if (option == SleepTimerOption.OFF || option.minutes <= 0) return
        val totalMillis = option.minutes * 60_000L
        _activeTimerOption.value = option
        _remainingTimerMillis.value = totalMillis
        timerJob = engineScope.launch {
            var remaining = totalMillis
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _remainingTimerMillis.value = remaining.coerceAtLeast(0)
            }
            _remainingTimerMillis.value = null
            _activeTimerOption.value = null
            stopAll()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingTimerMillis.value = null
        _activeTimerOption.value = null
    }
}
