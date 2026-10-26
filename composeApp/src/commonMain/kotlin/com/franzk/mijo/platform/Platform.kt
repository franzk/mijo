package com.franzk.mijo.platform

import com.franzk.mijo.cooking.VoiceCommand
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** État de la voix, affiché en permanence dans le mode cuisine. */
sealed interface VoiceStatus {
    /** Chargement du modèle ou démarrage du micro */
    data object Preparing : VoiceStatus
    data object Listening : VoiceStatus
    data object Speaking : VoiceStatus
    /** Session arrêtée (hors du mode cuisine, appli en arrière-plan) */
    data object Idle : VoiceStatus
    data object NeedsPermission : VoiceStatus
    /** Micro ou voix indisponible : les boutons tactiles restent utilisables */
    data class Unavailable(val message: String) : VoiceStatus
}

/**
 * Lecture + écoute en alternance : jamais d'écoute pendant la lecture,
 * pour ne pas prendre sa propre voix pour une commande.
 */
interface VoiceEngine {
    val status: StateFlow<VoiceStatus>
    val commands: SharedFlow<VoiceCommand>

    /** Entrée dans le mode cuisine. */
    fun startSession()

    /** Sortie du mode cuisine : libère le micro. */
    fun endSession()

    /** Lit le texte, puis reprend l'écoute. */
    fun speak(text: String)
}

/** Petit stockage clé-valeur local (positions de reprise, favoris, notes). */
interface KeyValueStore {
    fun getInt(key: String): Int?
    fun putInt(key: String, value: Int)
    fun remove(key: String)
}

/** Services de l'hôte (écran allumé, permission micro). */
interface Host {
    fun keepScreenOn(on: Boolean)
    fun hasMicPermission(): Boolean
    fun requestMicPermission(onResult: (Boolean) -> Unit)
}
