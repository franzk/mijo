package com.franzk.mijo.model

data class Ingredient(
    val name: String,
    /** Quantité pour le nombre de portions de référence ; null = « à volonté », « 1 pincée »… */
    val quantity: Double? = null,
    val unit: String? = null,
    val note: String? = null,
)

data class Recipe(
    val id: String,
    val title: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val servings: Int = 4,
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    val restMinutes: Int = 0,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tips: String? = null,
    val favorite: Boolean = false,
    /** 1 à 5, null = pas de note */
    val rating: Int? = null,
    /** Visuel de remplacement quand il n'y a pas de photo */
    val artwork: Artwork = Artwork.Default,
) {
    val totalMinutes: Int get() = prepMinutes + cookMinutes + restMinutes
}

/** Illustration de secours : un emoji sur un dégradé chaleureux. */
data class Artwork(val emoji: String, val from: Long, val to: Long) {
    companion object {
        val Default = Artwork("🍽️", 0xFFF3D98BL, 0xFFE0A458L)
    }
}

fun formatDuration(minutes: Int): String = when {
    minutes <= 0 -> "—"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')}"
}
