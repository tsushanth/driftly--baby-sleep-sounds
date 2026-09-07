package com.factory.driftlybabysleepsounds.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.driftlybabysleepsounds.DriftlyApplication
import com.factory.driftlybabysleepsounds.audio.AudioEngine
import com.factory.driftlybabysleepsounds.data.model.Sound
import com.factory.driftlybabysleepsounds.data.model.SoundCategory
import com.factory.driftlybabysleepsounds.data.model.SleepTimerOption
import com.factory.driftlybabysleepsounds.data.repository.SoundRepository
import com.factory.driftlybabysleepsounds.premium.PaywallController
import com.factory.driftlybabysleepsounds.premium.PaywallTrigger
import com.factory.driftlybabysleepsounds.premium.PremiumManager
import com.factory.driftlybabysleepsounds.service.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val sounds: List<Sound> = emptyList(),
    val selectedCategory: SoundCategory? = null,
    val favoriteIds: Set<String> = emptySet(),
    val activeVolumes: Map<String, Float> = emptyMap(),
    val remainingTimerMillis: Long? = null,
    val isPremium: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DriftlyApplication
    private val repository: SoundRepository = app.repository
    private val premiumManager: PremiumManager = app.premiumManager

    private val _selectedCategory = MutableStateFlow<SoundCategory?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedCategory,
        repository.observeFavoriteIds(),
        AudioEngine.activeVolumes,
        AudioEngine.remainingTimerMillis,
        premiumManager.isPremium
    ) { category, favorites, active, remaining, isPremium ->
        val sounds = repository.allSounds.filter { category == null || it.category == category }
        HomeUiState(
            sounds = sounds,
            selectedCategory = category,
            favoriteIds = favorites,
            activeVolumes = active,
            remainingTimerMillis = remaining,
            isPremium = isPremium
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState(sounds = repository.allSounds))

    fun selectCategory(category: SoundCategory?) {
        _selectedCategory.value = category
    }

    fun toggleSound(sound: Sound) {
        val state = uiState.value
        val isStarting = !state.activeVolumes.containsKey(sound.id)
        if (isStarting && !state.isPremium) {
            if (sound.isPremium) {
                PaywallController.show(PaywallTrigger.PREMIUM_SOUND)
                return
            }
            if (state.activeVolumes.isNotEmpty()) {
                PaywallController.show(PaywallTrigger.MULTI_MIX)
                return
            }
        }
        AudioEngine.toggleSound(sound)
        ensureServiceState()
    }

    fun toggleFavorite(soundId: String) {
        viewModelScope.launch { repository.toggleFavorite(soundId) }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        if (option == SleepTimerOption.OFF) {
            AudioEngine.cancelTimer()
        } else {
            AudioEngine.startTimer(option)
        }
    }

    fun stopAll() {
        AudioEngine.stopAll()
    }

    private fun ensureServiceState() {
        if (AudioEngine.activeVolumes.value.isNotEmpty()) {
            PlaybackService.start(getApplication())
        }
    }
}
