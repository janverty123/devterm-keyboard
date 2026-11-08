package com.oasan.devtermkeyboard

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.view.Gravity
import android.view.ViewGroup
import java.util.Locale

/**
 * Voice is launched in a focused Activity so Google Speech/other installed
 * recognizers can show their standard voice UI. On-device SpeechRecognizer
 * is used as a fallback where no speech Activity is available.
 * Results travel through a private ResultReceiver held by the initiating IME.
 */
class VoiceRecognitionActivity : Activity() {
    private var receiver: ResultReceiver? = null
    private var recognizer: SpeechRecognizer? = null
    private var completed = false
    private var status: TextView? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiver = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("result_receiver", ResultReceiver::class.java)
        } else intent.getParcelableExtra("result_receiver")
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF152238.toInt())
            setPadding(36, 48, 36, 48)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Opening voice typing…"
            textSize = 20f
            setTextColor(-1)
            gravity = Gravity.CENTER
        }
        panel.addView(status, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        val cancel = Button(this).apply {
            text = "Cancel"
            setAllCaps(false)
            setOnClickListener { finish() }
        }
        panel.addView(cancel)
        setContentView(panel)

        if (savedInstanceState == null) openProvider()
    }

    private fun openProvider() {
        val voice = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice typing")
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(voice, 41)
        } catch (_: ActivityNotFoundException) {
            startFallback()
        } catch (_: SecurityException) {
            startFallback()
        }
    }

    private fun startFallback() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            status?.text = "Microphone permission needed for the built-in recognizer."
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 42)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status?.text = "Install or enable a speech recognition service in Android settings."
            return
        }
        try {
            val useLocal = Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
            recognizer = if (useLocal && Build.VERSION.SDK_INT >= 31) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    status?.text = "Listening… speak now"
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    status?.text = "Processing speech…"
                }
                override fun onError(error: Int) {
                    status?.text = "Speech recognition error: $error. Check microphone and speech services."
                    Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500L)
                }
                override fun onResults(results: Bundle?) {
                    deliver(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull())
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            }
            status?.text = "Starting speech recognition…"
            recognizer?.startListening(request)
        } catch (_: Exception) {
            status?.text = "Speech recognition cannot start. Try installing Google speech services."
        }
    }

    @Deprecated("Android Activity result API is used without external dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41) {
            if (resultCode == RESULT_OK) deliver(
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            ) else finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 42) {
            if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) startFallback()
            else {
                status?.text = "Microphone access denied. Voice input is optional."
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 900L)
            }
        }
    }

    private fun deliver(text: String?) {
        if (completed) return
        completed = true
        val speech = text?.trim().orEmpty()
        if (speech.isNotEmpty()) {
            receiver?.send(RESULT_OK, Bundle().apply { putString("spoken_text", speech) })
        }
        finish()
    }

    override fun onDestroy() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }
}
