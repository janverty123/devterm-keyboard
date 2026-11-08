package com.oasan.devtermkeyboard

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.Locale

/**
 * Voice recognition is delegated to the user's installed Android speech provider.
 * Depending on the provider, audio may leave the device. No audio is saved here.
 */
internal class VoiceInputController(
    private val service: DeveloperKeyboardService,
    private val update: (Boolean) -> Unit,
    private val onText: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    val isActive: Boolean get() = active

    fun toggle() {
        if (active) {
            close()
            return
        }
        if (service.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(service, "Enable Microphone in Termora settings first", Toast.LENGTH_LONG).show()
            val settings = Intent(service, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("request_voice_permission", true)
            }
            service.startActivity(settings)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(service)) {
            Toast.makeText(service, "No Android speech recognition service is installed", Toast.LENGTH_LONG).show()
            return
        }
        close()
        try {
            val engine = SpeechRecognizer.createSpeechRecognizer(service)
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Toast.makeText(service, "Listening…", Toast.LENGTH_SHORT).show()
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    Toast.makeText(service, "Voice recognition stopped (code $error)", Toast.LENGTH_SHORT).show()
                    close()
                }
                override fun onResults(results: Bundle?) {
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim()
                    if (!spoken.isNullOrEmpty()) onText(spoken)
                    close()
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            active = true
            update(true)
            engine.startListening(request)
        } catch (e: Exception) {
            close()
            Toast.makeText(service, "Could not start voice input", Toast.LENGTH_SHORT).show()
        }
    }

    fun close() {
        val old = recognizer
        recognizer = null
        val previous = active
        active = false
        if (previous) update(false)
        try { old?.cancel() } catch (_: Exception) { }
        try { old?.destroy() } catch (_: Exception) { }
    }
}
