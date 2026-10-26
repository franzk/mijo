package com.franzk.mijo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.franzk.mijo.data.RecipeBook
import com.franzk.mijo.platform.Host
import com.franzk.mijo.platform.VoiceEngine

sealed interface Screen {
    data object Catalog : Screen
    data class Detail(val id: String) : Screen
    data class Cooking(val id: String, val startIndex: Int) : Screen
}

/** Navigation minimale, sans bibliothèque : une pile d'écrans. */
class AppState {
    var stack by mutableStateOf(listOf<Screen>(Screen.Catalog))
        private set
    val catalog = CatalogState()

    val current: Screen get() = stack.last()

    fun open(screen: Screen) {
        stack = stack + screen
    }

    /** Remplace l'écran courant (ex. : fiche → mode cuisine, sans empiler deux fois). */
    fun replace(screen: Screen) {
        stack = stack.dropLast(1) + screen
    }

    /** Retour arrière ; renvoie false quand il n'y a plus rien à fermer. */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack = stack.dropLast(1)
        return true
    }
}

@Composable
fun App(state: AppState, book: RecipeBook, voice: VoiceEngine, host: Host) {
    MijoTheme {
        AnimatedContent(
            targetState = state.current,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Catalog -> Box(Modifier.fillMaxSize().background(MijoColors.Cream).statusBarsPadding()) {
                    CatalogScreen(book, state.catalog) { state.open(Screen.Detail(it)) }
                }
                is Screen.Detail -> {
                    val recipe = book.byId(screen.id)
                    if (recipe == null) {
                        LaunchedEffect(screen) { state.back() }
                    } else {
                        RecipeScreen(
                            recipe = recipe,
                            book = book,
                            onClose = { state.back() },
                            onCook = { start -> state.open(Screen.Cooking(recipe.id, start)) },
                        )
                    }
                }
                is Screen.Cooking -> {
                    val recipe = book.byId(screen.id)
                    if (recipe == null || recipe.steps.isEmpty()) {
                        LaunchedEffect(screen) { state.back() }
                    } else {
                        CookingScreen(
                            recipe = recipe,
                            startIndex = screen.startIndex,
                            book = book,
                            voice = voice,
                            host = host,
                            onExit = { state.back() },
                        )
                    }
                }
            }
        }
    }
}
