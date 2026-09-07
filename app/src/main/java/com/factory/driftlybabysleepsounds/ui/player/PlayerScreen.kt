package com.factory.driftlybabysleepsounds.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.driftlybabysleepsounds.data.local.MixEntity
import com.factory.driftlybabysleepsounds.data.local.parseSounds
import com.factory.driftlybabysleepsounds.data.model.SleepTimerOption
import com.factory.driftlybabysleepsounds.premium.PaywallController
import com.factory.driftlybabysleepsounds.premium.PaywallTrigger
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveConfirmation) {
        uiState.saveConfirmation?.let { name ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Saved mix \"$name\"")
                viewModel.dismissSaveConfirmation()
            }
        }
    }

    if (showSaveDialog) {
        SaveMixDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCurrentMix(name)
                showSaveDialog = false
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Mixer",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }

            item {
                if (uiState.activeSounds.isEmpty()) {
                    Text(
                        "No sounds playing yet. Head to Sounds and tap something to start layering your mix.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            items(uiState.activeSounds, key = { it.sound.id }) { activeSound ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(activeSound.sound.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                activeSound.sound.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 10.dp).weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.removeSound(activeSound.sound.id)
                                }
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove ${activeSound.sound.name}")
                            }
                        }
                        Slider(
                            value = activeSound.volume,
                            onValueChange = { viewModel.setVolume(activeSound.sound.id, it) },
                            onValueChangeFinished = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                            valueRange = 0f..1f,
                            modifier = Modifier.semantics {
                                contentDescription = "Volume for ${activeSound.sound.name}"
                            }
                        )
                    }
                }
            }

            if (uiState.activeSounds.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (uiState.isPremium) {
                                    showSaveDialog = true
                                } else {
                                    PaywallController.show(PaywallTrigger.SAVE_MIX)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (uiState.isPremium) Icons.Filled.Save else Icons.Filled.Lock,
                                contentDescription = null
                            )
                            Text(" Save mix", modifier = Modifier.padding(start = 4.dp))
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.stopAll()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Stop all")
                        }
                    }
                }
            }

            item {
                Text(
                    "Sleep timer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            item {
                val remaining = uiState.remainingTimerMillis
                if (remaining != null) {
                    val totalSeconds = remaining / 1000
                    Text(
                        "%02d:%02d remaining".format(totalSeconds / 60, totalSeconds % 60),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                val selectedOption = uiState.activeTimerOption ?: SleepTimerOption.OFF
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SleepTimerOption.entries) { option ->
                        val locked = !uiState.isPremium && option !in FREE_SLEEP_TIMER_OPTIONS
                        FilterChip(
                            selected = option == selectedOption,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setSleepTimer(option)
                            },
                            label = { Text(option.label) },
                            trailingIcon = if (locked) {
                                { Icon(Icons.Filled.Lock, contentDescription = "Premium", modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }

            item {
                Text(
                    "Saved mixes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (uiState.savedMixes.isEmpty()) {
                item {
                    Text(
                        "Mixes you save will show up here for one-tap replay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.savedMixes, key = { it.id }) { mix ->
                SavedMixRow(
                    mix = mix,
                    onApply = { viewModel.applyMix(mix) },
                    onDelete = { viewModel.deleteMix(mix) }
                )
            }
        }
    }
}

@Composable
private fun SavedMixRow(mix: MixEntity, onApply: () -> Unit, onDelete: () -> Unit) {
    val soundCount = remember(mix.soundsSpec) { mix.parseSounds().size }
    val haptics = LocalHapticFeedback.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(mix.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "$soundCount sound${if (soundCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApply()
                }
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Apply ${mix.name}")
            }
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDelete()
                }
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${mix.name}")
            }
        }
    }
}

@Composable
private fun SaveMixDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun confirmSave() {
        if (name.isNotBlank()) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onSave(name)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save mix") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Mix name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirmSave() }),
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(onClick = { confirmSave() }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
