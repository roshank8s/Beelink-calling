package com.beelinking.bridge.call

import android.annotation.SuppressLint
import android.media.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Captures and plays back audio for call forwarding over Bluetooth.
 *
 * On SIM device: captures call audio from mic → sends PCM over BT → plays remote audio to speaker
 * On WiFi device: captures mic audio → sends PCM over BT → plays call audio to speaker
 */
@SuppressLint("MissingPermission")
class AudioBridge(private val scope: CoroutineScope) {

    companion object {
        const val SAMPLE_RATE = 8000  // Narrowband voice
        const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_FACTOR = 2
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var captureJob: Job? = null
    private var playbackActive = false

    private val _capturedAudio = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
    val capturedAudio: SharedFlow<ByteArray> = _capturedAudio

    private val minRecordBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
    private val minPlayBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)

    /** Start capturing audio from microphone */
    fun startCapture() {
        if (captureJob?.isActive == true) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                minRecordBuffer * BUFFER_SIZE_FACTOR
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            audioRecord = null
            return
        }

        captureJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(minRecordBuffer)
            while (isActive) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (bytesRead > 0) {
                    _capturedAudio.emit(buffer.copyOf(bytesRead))
                }
            }
        }
    }

    /** Start audio playback (speaker/earpiece) */
    fun startPlayback() {
        if (playbackActive) return

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .setEncoding(AUDIO_FORMAT)
                        .build()
                )
                .setBufferSizeInBytes(minPlayBuffer * BUFFER_SIZE_FACTOR)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            playbackActive = true
        } catch (e: Exception) {
            audioTrack = null
        }
    }

    /** Feed received PCM data to the speaker */
    fun playAudio(pcmData: ByteArray) {
        if (!playbackActive) startPlayback()
        audioTrack?.write(pcmData, 0, pcmData.size)
    }

    /** Stop all audio */
    fun stop() {
        captureJob?.cancel()
        captureJob = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        playbackActive = false
    }
}
