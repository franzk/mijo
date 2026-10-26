package com.franzk.mijo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.franzk.mijo.cooking.VOICE_GRAMMAR_WORDS
import com.franzk.mijo.cooking.VoiceCommand
import com.franzk.mijo.cooking.parseVoiceCommand
import com.franzk.mijo.platform.VoiceEngine
import com.franzk.mijo.platform.VoiceStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException
import java.util.Locale

/**
 * Voix hors ligne :
 * - lecture : synthèse vocale d'Android (voix française de l'appareil) ;
 * - écoute : Vosk, modèle français embarqué, vocabulaire limité aux commandes.
 *
 * Fonctionnement en alternance : la reconnaissance est mise en pause pendant
 * la lecture, puis reprend un court instant après, pour que l'application ne
 * prenne jamais sa propre voix pour une commande.
 */
class VoskVoiceEngine(context: Context) : VoiceEngine {

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private val _status = MutableStateFlow<VoiceStatus>(VoiceStatus.Idle)
    override val status: StateFlow<VoiceStatus> = _status

    private val _commands = MutableSharedFlow<VoiceCommand>(extraBufferCapacity = 4)
    override val commands: SharedFlow<VoiceCommand> = _commands

    // --- état (toujours manipulé sur le fil principal)
    private var model: Model? = null
    private var modelError: String? = null
    private var recognizer: Recognizer? = null
    private var service: SpeechService? = null
    private var sessionActive = false
    private var hostPaused = false
    private var speaking = false
    private var utterance = 0
    private var lastCommandAt = 0L
    private var ttsReady = false
    private var ttsError: String? = null
    private var pendingSpeech: String? = null
    private val retry = Runnable { ensureListening() }

    private val tts: TextToSpeech = TextToSpeech(app) { code -> main.post { onTtsInit(code) } }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                main.post { onSpeechDone(utteranceId) }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { onSpeechDone(utteranceId) }
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                main.post { onSpeechDone(utteranceId) }
            }
        })
        // Copie du modèle des assets vers le stockage interne (une seule fois par version).
        StorageService.unpack(
            app, "model-fr", "model",
            { m ->
                model = m
                ensureListening()
            },
            { e ->
                Log.e(TAG, "Modèle vocal", e)
                modelError = "Modèle vocal introuvable · utilisez les boutons"
                refreshStatus()
            },
        )
    }

    // ------------------------------------------------------------ API

    override fun startSession() {
        main.post {
            sessionActive = true
            hostPaused = false
            ensureListening()
        }
    }

    override fun endSession() {
        main.post {
            sessionActive = false
            pendingSpeech = null
            stopRecognition()
            if (speaking) tts.stop()
            speaking = false
            _status.value = VoiceStatus.Idle
        }
    }

    override fun speak(text: String) {
        main.post {
            if (!ttsReady) {
                if (ttsError == null) pendingSpeech = text // la synthèse démarre encore
                refreshStatus()
                return@post
            }
            speaking = true
            service?.setPause(true)
            utterance++
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u$utterance")
            refreshStatus()
        }
    }

    // ------------------------------------------------------------ cycle de vie de l'hôte

    fun onHostPause() {
        main.post {
            hostPaused = true
            stopRecognition()
            if (speaking) {
                tts.stop()
                speaking = false
            }
            if (sessionActive) _status.value = VoiceStatus.Idle
        }
    }

    fun onHostResume() {
        main.post {
            hostPaused = false
            ensureListening()
        }
    }

    fun shutdown() {
        main.removeCallbacksAndMessages(null)
        stopRecognition()
        tts.stop()
        tts.shutdown()
        model?.close()
        model = null
    }

    // ------------------------------------------------------------ interne

    private fun onTtsInit(code: Int) {
        if (code != TextToSpeech.SUCCESS) {
            ttsError = "Synthèse vocale indisponible sur cet appareil"
        } else {
            val result = tts.setLanguage(Locale.FRANCE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsError = "Voix française absente · à installer dans les réglages de synthèse vocale"
            } else {
                ttsReady = true
            }
        }
        val text = pendingSpeech
        pendingSpeech = null
        if (ttsReady && text != null && sessionActive) speak(text) else refreshStatus()
    }

    private fun onSpeechDone(utteranceId: String?) {
        if (utteranceId != "u$utterance") return // lecture interrompue par une plus récente
        speaking = false
        // Petit délai : laisse la fin du son s'éteindre avant de réécouter.
        main.postDelayed({ if (!speaking) ensureListening() }, 350)
        refreshStatus()
    }

    private fun hasMicPermission() =
        app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun ensureListening() {
        main.removeCallbacks(retry)
        if (!sessionActive || hostPaused) return refreshStatus()
        if (!hasMicPermission()) return refreshStatus()
        val m = model ?: return refreshStatus()
        if (service == null) {
            try {
                val grammar = JSONArray(VOICE_GRAMMAR_WORDS + "[unk]").toString()
                val rec = Recognizer(m, SAMPLE_RATE, grammar)
                val svc = SpeechService(rec, SAMPLE_RATE)
                recognizer = rec
                service = svc
                svc.startListening(listener)
            } catch (e: IOException) {
                Log.w(TAG, "Micro indisponible", e)
                stopRecognition()
                _status.value = VoiceStatus.Unavailable("Micro occupé · nouvelle tentative…")
                main.postDelayed(retry, 2000)
                return
            }
        }
        service?.setPause(speaking)
        refreshStatus()
    }

    private fun stopRecognition() {
        main.removeCallbacks(retry)
        service?.let {
            it.stop()
            it.shutdown()
        }
        service = null
        recognizer?.close()
        recognizer = null
    }

    private fun refreshStatus() {
        _status.value = when {
            !sessionActive -> VoiceStatus.Idle
            speaking -> VoiceStatus.Speaking
            hostPaused -> VoiceStatus.Idle
            !hasMicPermission() -> VoiceStatus.NeedsPermission
            modelError != null -> VoiceStatus.Unavailable(modelError!!)
            model == null -> VoiceStatus.Preparing
            service == null -> VoiceStatus.Preparing
            ttsError != null -> VoiceStatus.Unavailable("J'écoute, mais ${ttsError!!.replaceFirstChar { it.lowercaseChar() }}")
            else -> VoiceStatus.Listening
        }
    }

    private fun onRecognized(hypothesis: String?) {
        if (hypothesis == null || speaking || !sessionActive) return
        val text = try {
            JSONObject(hypothesis).optString("text")
        } catch (e: Exception) {
            return
        }
        val command = parseVoiceCommand(text) ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastCommandAt < DEBOUNCE_MS) return // une commande = un seul déplacement
        lastCommandAt = now
        _commands.tryEmit(command)
    }

    private val listener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {}
        override fun onResult(hypothesis: String?) = onRecognized(hypothesis)
        override fun onFinalResult(hypothesis: String?) = onRecognized(hypothesis)
        override fun onError(exception: Exception?) {
            Log.w(TAG, "Reconnaissance", exception)
            stopRecognition()
            _status.value = VoiceStatus.Unavailable("Micro interrompu · nouvelle tentative…")
            main.postDelayed(retry, 1500)
        }
        override fun onTimeout() {
            stopRecognition()
            ensureListening()
        }
    }

    private companion object {
        const val TAG = "Mijo"
        const val SAMPLE_RATE = 16000f
        const val DEBOUNCE_MS = 800L
    }
}
