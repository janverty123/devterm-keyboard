package com.oasan.devtermkeyboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import java.util.Locale

/**
 * In-keyboard dictation, without launching an external recognition Activity.
 * Android's installed speech service performs recognition; Keylix receives only
 * partial and final text. Destroy the mic session when dictation ends.
 */
internal class InlineVoiceController(
    private val context: Context,
    private val onState: (State, String?) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onFailure: (String) -> Unit
) {
    enum class State { IDLE, STARTING, LISTENING, PROCESSING }

    private var recognizer: SpeechRecognizer? = null
    private var generation = 0
    var state: State = State.IDLE
        private set

    val active: Boolean get() = state != State.IDLE

    fun start() {
        if (active) return
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onFailure("Enable microphone permission in Keylix settings.")
            return
        }
        val localAvailable = Build.VERSION.SDK_INT >= 31 &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        val systemAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        if (!localAvailable && !systemAvailable) {
            onFailure("No speech recognizer is installed. Enable Google Speech Services or another recognizer.")
            return
        }
        generation++
        val session = generation
        try {
            // Prefer the default Android speech provider (including Google when
            // installed). An on-device provider is a fallback.
            val engine = if (systemAvailable) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            }
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (session != generation) return
                    transition(State.LISTENING, null)
                }
                override fun onBeginningOfSpeech() {
                    if (session != generation) return
                    transition(State.LISTENING, null)
                }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    if (session != generation) return
                    transition(State.PROCESSING, null)
                }
                override fun onError(error: Int) {
                    if (session != generation) return
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized. Try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap the microphone to retry."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access in Keylix settings."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Speech service needs a connection or an offline language model."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Try again."
                        SpeechRecognizer.ERROR_CLIENT -> "The speech provider could not start."
                        else -> "Speech recognition failed (code $error)."
                    }
                    stop()
                    onFailure(msg)
                }
                override fun onResults(results: Bundle?) {
                    if (session != generation) return
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    stop()
                    if (spoken.isNotEmpty()) onFinalText(spoken)
                    else onFailure("Nothing was recognized. Try again.")
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (session != generation) return
                    val spoken = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim()
                    if (!spoken.isNullOrEmpty()) transition(State.LISTENING, spoken)
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            transition(State.STARTING, null)
            engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            })
        } catch (error: Exception) {
            stop()
            onFailure("Unable to start Android speech recognition: " + error.javaClass.simpleName)
        }
    }

    fun stop() {
        generation++
        val old = recognizer
        recognizer = null
        try { old?.cancel() } catch (_: Exception) { }
        try { old?.destroy() } catch (_: Exception) { }
        transition(State.IDLE, null)
    }

    private fun transition(newState: State, preview: String?) {
        state = newState
        onState(newState, preview)
    }
}
