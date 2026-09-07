package com.factory.driftlybabysleepsounds.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.driftlybabysleepsounds.DriftlyApplication
import com.factory.driftlybabysleepsounds.audio.AudioEngine
import com.factory.driftlybabysleepsounds.data.local.MixEntity
import com.factory.driftlybabysleepsounds.data.local.MixSoundConfig
import com.factory.driftlybabysleepsounds.data.local.parseSounds
import com.factory.driftlybabysleepsounds.data.model.Sound
import com.factory.driftlybabysleepsounds.data.model.SleepTimerOption
import com.factory.driftlybabysleepsounds.data.repository.SoundRepository
import com.factory.driftlybabysleepsounds.premium.PaywallController
import com.factory.driftlybabysleepsounds.premium.PaywallTrigger
import com.factory.driftlybabysleepsounds.premium.PremiumManager
import com.factory.driftlybabysleepsounds.service.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveSoundUi(val sound: Sound, val volume: Float)

data class PlayerUiState(
    val activeSounds: List<ActiveSoundUi> = emptyList(),
    val remainingTimerMillis: Long? = null,
    val activeTimerOption: SleepTimerOption? = null,
    val saveConfirmation: String? = null,
    val savedMixes: List<MixEntity> = emptyList(),
    val isPremium: Boolean = false
)

val FREE_SLEEP_TIMER_OPTIONS = setOf(SleepTimerOption.OFF, SleepTimerOption.FIFTEEN, SleepTimerOption.THIRTY)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DriftlyApplication
    private val repository: SoundRepository = app.repository
    private val premiumManager: PremiumManager = app.premiumManager

    private val _saveConfirmation = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlayerUiState> = combine(
        AudioEngine.activeVolumes,
        AudioEngine.remainingTimerMillis,
        AudioEngine.activeTimerOption,
        _saveConfirmation,
        repository.observeMixes(),
        premiumManager.isPremium
    ) { flows ->
        val volumes = flows[0] as Map<String, Float>
        val remaining = flows[1] as Long?
        val activeTimerOption = flows[2] as SleepTimerOption?
        val confirmation = flows[3] as String?
        val mixes = flows[4] as List<MixEntity>
        val isPremium = flows[5] as Boolean
        val activeSounds = volumes.mapNotNull { (id, volume) ->
            repository.soundById(id)?.let { ActiveSoundUi(it, volume) }
        }.sortedBy { it.sound.name }
        PlayerUiState(activeSounds, remaining, activeTimerOption, confirmation, mixes, isPremium)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    fun setVolume(soundId: String, volume: Float) {
        AudioEngine.setVolume(soundId, volume)
    }

    fun removeSound(soundId: String) {
        AudioEngine.stopSound(soundId)
    }

    fun setSleepTimer(option: SleepTimerOption) {
        if (option == SleepTimerOption.OFF) {
            AudioEngine.cancelTimer()
            return
        }
        if (!uiState.value.isPremium && option !in FREE_SLEEP_TIMER_OPTIONS) {
            PaywallController.show(PaywallTrigger.SLEEP_TIMER)
            return
        }
        AudioEngine.startTimer(option)
    }

    fun stopAll() {
        AudioEngine.stopAll()
    }

    fun saveCurrentMix(name: String) {
        if (!uiState.value.isPremium) {
            PaywallController.show(PaywallTrigger.SAVE_MIX)
            return
        }
        val configs = uiState.value.activeSounds.map { MixSoundConfig(it.sound.id, it.volume) }
        if (configs.isEmpty() || name.isBlank()) return
        viewModelScope.launch {
            repository.saveMix(name.trim(), configs)
            _saveConfirmation.value = name.trim()
        }
    }

    fun dismissSaveConfirmation() {
        _saveConfirmation.value = null
    }

    fun applyMix(mix: MixEntity) {
        val soundsWithVolumes = mix.parseSounds().mapNotNull { config ->
            repository.soundById(config.soundId)?.let { it to config.volume }
        }
        AudioEngine.applyMix(soundsWithVolumes)
        if (soundsWithVolumes.isNotEmpty()) {
            PlaybackService.start(getApplication())
        }
    }

    fun deleteMix(mix: MixEntity) {
        viewModelScope.launch { repository.deleteMix(mix) }
    }
}
