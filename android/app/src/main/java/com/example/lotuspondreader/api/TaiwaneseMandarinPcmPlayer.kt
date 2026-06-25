package com.example.lotuspondreader.api

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaiwaneseMandarinPcmPlayer {

    private val sampleRate = 24000 // Gemini default spec
    
    suspend fun playBase64Pcm(base64Data: String, speed: Float = 1.0f) {
        val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
        playRawPcm(audioBytes, speed)
    }

    /**
     * Writes raw ByteArray PCM frames to an authentic AudioTrack channel
     */
    suspend fun playRawPcm(audioBytes: ByteArray, speed: Float = 1.0f) = withContext(Dispatchers.IO) {
        var audioTrack: AudioTrack? = null
        try {
            // Calculate optimal buffer sizing
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            // Build native Android linear PCM hardware track
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize.coerceAtLeast(audioBytes.size))
                .setTransferMode(AudioTrack.MODE_STATIC) // Static supports complete buffer load
                .build()
            
            // Static playback: load entire buffer array, play, and clean resources on completion
            audioTrack.write(audioBytes, 0, audioBytes.size)

            if (speed != 1.0f) {
                audioTrack.playbackParams = android.media.PlaybackParams().apply {
                    this.speed = speed
                }
            }

            audioTrack.play()
            
            // Suspend coroutine waiting for completion to safely release stream resources
            val durationMs = (((audioBytes.size / 2.0) / sampleRate) * 1000) / speed
            kotlinx.coroutines.delay(durationMs.toLong() + 200)
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Handle cancellation cleanly
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                // ignore cleanup errors
            }
        }
    }
}
