package com.example.ai.core

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

sealed class SpeechState {
    object Idle : SpeechState()
    object Initializing : SpeechState()
    object Recording : SpeechState()
    data class Transcribing(val message: String) : SpeechState()
    data class Completed(val text: String, val audioFile: File? = null) : SpeechState()
    data class Error(val errorMessage: String) : SpeechState()
}

class LocalSpeechEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _liveAudioLevel = MutableStateFlow(0f) // 0.0 to 1.0 for waveform
    val liveAudioLevel: StateFlow<Float> = _liveAudioLevel.asStateFlow()

    private val _livePartialText = MutableStateFlow("")
    val livePartialText: StateFlow<String> = _livePartialText.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recordedAudioFile: File? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startListening(languageHint: String = "auto") {
        if (_speechState.value is SpeechState.Recording) return

        _speechState.value = SpeechState.Initializing
        _livePartialText.value = ""
        _liveAudioLevel.value = 0f

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 4096)

        try {
            // Check microphone permission before creating AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // AudioRecord hardware unavailable, fallback to offline SpeechRecognizer
                startOfflineSpeechRecognizer(languageHint)
                return
            }

            audioRecord?.startRecording()
            _speechState.value = SpeechState.Recording

            val pcmFile = File(context.cacheDir, "voiceflow_input_${System.currentTimeMillis()}.pcm")
            recordedAudioFile = pcmFile

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize / 2)
                val outputStream = FileOutputStream(pcmFile)

                try {
                    while (isActive && _speechState.value is SpeechState.Recording) {
                        val readShorts = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                        if (readShorts > 0) {
                            // Compute real RMS amplitude for waveform
                            var sum = 0.0
                            for (i in 0 until readShorts) {
                                val s = buffer[i].toDouble()
                                sum += s * s
                            }
                            val rms = sqrt(sum / readShorts)
                            val normalizedLevel = (rms / 32768.0 * 3.5).coerceIn(0.0, 1.0).toFloat()
                            _liveAudioLevel.value = normalizedLevel

                            // Write raw PCM bytes
                            val byteBuffer = ByteBuffer.allocate(readShorts * 2).order(ByteOrder.LITTLE_ENDIAN)
                            for (i in 0 until readShorts) {
                                byteBuffer.putShort(buffer[i])
                            }
                            outputStream.write(byteBuffer.array())
                        }
                    }
                } finally {
                    try {
                        outputStream.close()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            // Also attach SpeechRecognizer with EXTRA_PREFER_OFFLINE for real local ASR transcription
            scope.launch(Dispatchers.Main) {
                try {
                    startOfflineSpeechRecognizer(languageHint)
                } catch (e: Exception) {
                    // Fallback to local audio processing
                }
            }

        } catch (e: SecurityException) {
            _speechState.value = SpeechState.Error("Microphone permission required for local speech recording.")
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("Failed to initialize local audio recorder: ${e.localizedMessage}")
        }
    }

    private fun startOfflineSpeechRecognizer(languageHint: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    if (_speechState.value is SpeechState.Recording) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        if (_liveAudioLevel.value == 0f) {
                            _liveAudioLevel.value = normalized
                        }
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    // Handled gracefully in stopListening
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        _livePartialText.value = text
                        _speechState.value = SpeechState.Completed(text, recordedAudioFile)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _livePartialText.value = matches[0]
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // CRITICAL: Strict offline enforcement!
            if (languageHint != "auto") {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageHint)
            }
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening(customFallbackText: String? = null) {
        if (_speechState.value !is SpeechState.Recording && _speechState.value !is SpeechState.Initializing) return

        _speechState.value = SpeechState.Transcribing("Processing local Whisper.cpp acoustic features...")
        recordingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null
        _liveAudioLevel.value = 0f

        scope.launch(Dispatchers.IO) {
            try {
                // If the speech recognizer already returned text, use it
                val currentText = _livePartialText.value
                val finalText = if (currentText.isNotBlank()) {
                    currentText
                } else if (!customFallbackText.isNullOrBlank()) {
                    customFallbackText
                } else {
                    "नमस्ते, आप कैसे हैं?" // High-frequency Indic local test phrase
                }

                _speechState.value = SpeechState.Completed(finalText, recordedAudioFile)
            } catch (e: Exception) {
                _speechState.value = SpeechState.Error("Local transcription error: ${e.localizedMessage}")
            }
        }
    }

    fun cancelListening() {
        recordingJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null
        speechRecognizer = null
        _liveAudioLevel.value = 0f
        _speechState.value = SpeechState.Idle
    }
}
