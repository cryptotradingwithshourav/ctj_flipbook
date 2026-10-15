package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.ReadingLanguage
import com.example.data.model.VoicePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSentence = MutableStateFlow("")
    val currentSentence: StateFlow<String> = _currentSentence.asStateFlow()

    private val _equalizerBars = MutableStateFlow(listOf(0.2f, 0.4f, 0.6f, 0.3f, 0.5f))
    val equalizerBars: StateFlow<List<Float>> = _equalizerBars.asStateFlow()

    // Active speed rate
    private var currentRate: Float = 1.0f
    private var activeReadingText: String = ""

    // STT State
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastVoiceCommand = MutableStateFlow<String?>(null)
    val lastVoiceCommand: StateFlow<String?> = _lastVoiceCommand.asStateFlow()

    var onVoiceCommandReceived: ((command: String) -> Unit)? = null

    private var visualizerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e("TtsManager", "Error initializing TTS: ${e.message}")
        }
        setupSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyLanguageAndVoice(ReadingLanguage.HINDI, VoicePreset.FEMALE_NARRATOR)
            setupProgressListener()
        } else {
            Log.e("TtsManager", "TTS initialization failed with code $status")
        }
    }

    fun applyLanguageAndVoice(language: ReadingLanguage, voicePreset: VoicePreset) {
        if (!isInitialized) return

        try {
            val locale = Locale.forLanguageTag(language.code)

            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English if chosen locale is missing on device
                tts?.setLanguage(Locale.US)
            }

            // Adjust pitch/characteristics based on VoicePreset
            when (voicePreset) {
                VoicePreset.FEMALE_NARRATOR -> tts?.setPitch(1.15f)
                VoicePreset.MALE_NARRATOR -> tts?.setPitch(0.85f)
                VoicePreset.STORYTELLER -> tts?.setPitch(0.78f)
                VoicePreset.YOUTHFUL -> tts?.setPitch(1.22f)
            }
        } catch (e: Exception) {
            Log.w("TtsManager", "Could not set voice properties: ${e.message}")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                startVisualizer()
            }

            override fun onDone(utteranceId: String?) {
                _isPlaying.value = false
                stopVisualizer()
            }

            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
                stopVisualizer()
            }
        })
    }

    /**
     * Live Speech Rate Change:
     * Immediately applies the speed rate to the engine and seamlessly updates active playback!
     */
    fun setLiveSpeechRate(newRate: Float, currentText: String? = null) {
        currentRate = newRate.coerceIn(0.5f, 2.0f)
        try {
            tts?.setSpeechRate(currentRate)
        } catch (e: Exception) {
            Log.w("TtsManager", "Error setting speech rate: ${e.message}")
        }

        // If actively playing, seamlessly restart from the current sentence at the new speed!
        if (_isPlaying.value) {
            val textToRead = currentText ?: activeReadingText
            if (textToRead.isNotBlank()) {
                val remainingText = if (_currentSentence.value.isNotBlank() && textToRead.contains(_currentSentence.value)) {
                    val idx = textToRead.indexOf(_currentSentence.value)
                    textToRead.substring(idx)
                } else {
                    textToRead
                }
                speak(remainingText, currentRate)
            }
        }
    }

    fun speak(text: String, rate: Float = currentRate) {
        activeReadingText = text
        currentRate = rate.coerceIn(0.5f, 2.0f)

        if (!isInitialized || text.isBlank()) {
            simulatePlayback(text)
            return
        }

        try {
            tts?.setSpeechRate(currentRate)
            _currentSentence.value = text.lines().firstOrNull { it.isNotBlank() } ?: text.take(60)
            val params = Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "STORY_FLIP_PAGE_${System.currentTimeMillis()}")
            _isPlaying.value = true
            startVisualizer()
        } catch (e: Exception) {
            Log.e("TtsManager", "Failed to speak: ${e.message}")
            simulatePlayback(text)
        }
    }

    fun previewVoice(language: ReadingLanguage, voicePreset: VoicePreset) {
        applyLanguageAndVoice(language, voicePreset)
        val previewText = when (language) {
            ReadingLanguage.HINDI -> "नमस्ते, यह स्टोरीफ्लिप वाचक का पूर्वावलोकन है।"
            ReadingLanguage.BENGALI -> "নমস্কার, এটি আপনার কণ্ঠস্বর পূর্বরূপ।"
            ReadingLanguage.TAMIL -> "வணக்கம், இது உங்கள் குரல் மாதிரிக்காட்சி."
            else -> "Welcome to StoryFlip, listening in ${language.displayName}."
        }
        speak(previewText, currentRate)
    }

    private fun simulatePlayback(text: String) {
        _currentSentence.value = text.lines().firstOrNull { it.isNotBlank() } ?: text.take(60)
        _isPlaying.value = true
        startVisualizer()
    }

    fun pauseOrResume(textToResume: String, rate: Float = currentRate) {
        if (_isPlaying.value) {
            stop()
        } else {
            speak(textToResume, rate)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
        _isPlaying.value = false
        stopVisualizer()
    }

    private fun startVisualizer() {
        visualizerJob?.cancel()
        visualizerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val newBars = listOf(
                    (0.2f + Math.random().toFloat() * 0.7f).coerceIn(0.1f, 1.0f),
                    (0.3f + Math.random().toFloat() * 0.7f).coerceIn(0.1f, 1.0f),
                    (0.5f + Math.random().toFloat() * 0.5f).coerceIn(0.1f, 1.0f),
                    (0.3f + Math.random().toFloat() * 0.6f).coerceIn(0.1f, 1.0f),
                    (0.2f + Math.random().toFloat() * 0.7f).coerceIn(0.1f, 1.0f)
                )
                _equalizerBars.value = newBars
                delay(120)
            }
        }
    }

    private fun stopVisualizer() {
        visualizerJob?.cancel()
        _equalizerBars.value = listOf(0.15f, 0.2f, 0.15f, 0.25f, 0.15f)
    }

    // ==========================================
    // SPEECH RECOGNITION (STT) VOICE COMMANDS
    // ==========================================

    private fun setupSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() { _isListening.value = false }
                        override fun onError(error: Int) { _isListening.value = false }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull()?.lowercase() ?: return
                            _lastVoiceCommand.value = recognized
                            handleRecognizedCommand(recognized)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
        } catch (e: Exception) {
            Log.w("TtsManager", "Speech recognition setup: ${e.message}")
        }
    }

    fun startListeningForVoiceCommands() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // ignore
        }
        _isListening.value = false
    }

    private fun handleRecognizedCommand(text: String) {
        when {
            text.contains("next") || text.contains("अगला") || text.contains("आगे") -> {
                onVoiceCommandReceived?.invoke("NEXT")
            }
            text.contains("previous") || text.contains("back") || text.contains("पिछला") || text.contains("पीछे") -> {
                onVoiceCommandReceived?.invoke("PREV")
            }
            text.contains("pause") || text.contains("stop") || text.contains("रुको") -> {
                stop()
                onVoiceCommandReceived?.invoke("PAUSE")
            }
            text.contains("play") || text.contains("resume") || text.contains("read") || text.contains("पढ़ो") -> {
                if (activeReadingText.isNotBlank()) speak(activeReadingText)
                onVoiceCommandReceived?.invoke("PLAY")
            }
        }
    }

    fun shutdown() {
        stop()
        stopListening()
        try {
            speechRecognizer?.destroy()
            tts?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
    }
}
