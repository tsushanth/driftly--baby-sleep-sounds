package com.factory.driftlybabysleepsounds.data.model

import androidx.compose.ui.graphics.vector.ImageVector

data class Sound(
    val id: String,
    val name: String,
    val description: String,
    val category: SoundCategory,
    val generatorType: GeneratorType,
    val icon: ImageVector,
    val isPremium: Boolean = false
)
