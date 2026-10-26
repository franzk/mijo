package com.franzk.mijo.cooking

import com.franzk.mijo.model.normalize

enum class VoiceCommand { Next, Previous, Repeat }

/** Ce que l'application doit faire après une commande. */
data class Outcome(
    /** Texte à lire à voix haute */
    val speech: String,
    /** La position a changé (à mémoriser) */
    val moved: Boolean,
)

/**
 * Navigation pure dans les étapes : aucune notion d'étape « validée »,
 * seulement une position. Une commande ne déplace jamais de plus d'une étape.
 */
class CookingSession(private val steps: List<String>, startIndex: Int = 0) {

    var index: Int = startIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
        private set

    /** Vrai quand on a dit « Suivant » sur la dernière étape. */
    var finished: Boolean = false
        private set

    val total: Int get() = steps.size

    fun currentSpeech(): String =
        if (finished) END_SPEECH else "Étape ${index + 1}. ${steps[index]}"

    fun handle(command: VoiceCommand): Outcome = when (command) {
        VoiceCommand.Repeat -> Outcome(currentSpeech(), moved = false)
        VoiceCommand.Next -> when {
            finished -> Outcome(END_SPEECH, moved = false)
            index < steps.lastIndex -> {
                index++
                Outcome(currentSpeech(), moved = true)
            }
            else -> {
                finished = true
                Outcome(END_SPEECH, moved = true)
            }
        }
        VoiceCommand.Previous -> when {
            finished -> {
                finished = false
                Outcome(currentSpeech(), moved = true)
            }
            index > 0 -> {
                index--
                Outcome(currentSpeech(), moved = true)
            }
            else -> Outcome("Vous êtes déjà à la première étape.", moved = false)
        }
    }

    companion object {
        const val END_SPEECH = "La recette est terminée. Bon appétit !"
    }
}

/**
 * Mots reconnus → commande. Les variantes couvrent les accords et les
 * erreurs de reconnaissance fréquentes ; tout le reste est ignoré.
 */
fun parseVoiceCommand(recognized: String): VoiceCommand? {
    val words = normalize(recognized)
        .replace(Regex("[^a-z ]"), " ")
        .split(' ')
        .filter { it.isNotBlank() && it != "unk" }
    // Une commande est un énoncé court : on ignore les phrases de conversation.
    if (words.isEmpty() || words.size > 4) return null
    for (w in words) {
        when (w) {
            in NEXT -> return VoiceCommand.Next
            in PREVIOUS -> return VoiceCommand.Previous
            in REPEAT -> return VoiceCommand.Repeat
        }
    }
    return null
}

private val NEXT = setOf("suivant", "suivante", "suivants", "suivantes")
private val PREVIOUS = setOf("precedent", "precedente", "precedents", "precedentes")
private val REPEAT = setOf("repete", "repeter", "repetez", "repetes")

/** Vocabulaire fermé fourni au moteur de reconnaissance (avec accents). */
val VOICE_GRAMMAR_WORDS = listOf(
    "suivant", "suivante",
    "précédent", "précédente",
    "répète", "répéter", "répétez",
)
