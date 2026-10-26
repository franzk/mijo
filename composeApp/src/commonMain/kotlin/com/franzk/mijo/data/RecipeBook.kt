package com.franzk.mijo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.franzk.mijo.model.Recipe
import com.franzk.mijo.platform.KeyValueStore

/**
 * Carnet de recettes. Pour ce premier prototype, les recettes sont celles
 * d'exemple ; favoris, notes et positions de reprise sont conservés localement.
 */
class RecipeBook(private val store: KeyValueStore) {

    var recipes: List<Recipe> by mutableStateOf(
        sampleRecipes.map { r ->
            r.copy(
                favorite = store.getInt(favKey(r.id))?.let { it == 1 } ?: r.favorite,
                rating = store.getInt(rateKey(r.id))?.let { if (it == 0) null else it } ?: r.rating,
            )
        }
    )
        private set

    /** Positions de reprise, une par recette (plusieurs préparations en parallèle). */
    private val progress = mutableStateMapOf<String, Int>().apply {
        sampleRecipes.forEach { r -> store.getInt(stepKey(r.id))?.let { put(r.id, it) } }
    }

    val allTags: List<String> get() = recipes.flatMap { it.tags }.distinct().sorted()

    fun byId(id: String): Recipe? = recipes.firstOrNull { it.id == id }

    fun toggleFavorite(id: String) = update(id) { r ->
        val fav = !r.favorite
        store.putInt(favKey(id), if (fav) 1 else 0)
        r.copy(favorite = fav)
    }

    fun setRating(id: String, rating: Int?) = update(id) { r ->
        store.putInt(rateKey(id), rating ?: 0)
        r.copy(rating = rating)
    }

    fun progressOf(id: String): Int? = progress[id]

    fun saveProgress(id: String, index: Int) {
        progress[id] = index
        store.putInt(stepKey(id), index)
    }

    fun clearProgress(id: String) {
        progress.remove(id)
        store.remove(stepKey(id))
    }

    private fun update(id: String, change: (Recipe) -> Recipe) {
        recipes = recipes.map { if (it.id == id) change(it) else it }
    }

    private fun favKey(id: String) = "fav:$id"
    private fun rateKey(id: String) = "rate:$id"
    private fun stepKey(id: String) = "step:$id"
}
