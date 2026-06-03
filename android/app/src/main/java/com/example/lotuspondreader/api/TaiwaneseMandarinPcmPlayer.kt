package com.example.lotuspondreader.api

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaiwaneseMandarinPcmPlayer {

    private val sampleRate = 24000 // Gemini default spec
    
    /**
     * Decodes Base64 PCM data and writes it to an authentic AudioTrack channel
     */
    suspend fun playBase64Pcm(base64Data: String) = withContext(Dispatchers.IO) {
        try {
            // Convert to raw byte frames
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            
            // Calculate optimal buffer sizing
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            // Build native Android linear PCM hardware track
            val audioTrack = AudioTrack.Builder()
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
            audioTrack.play()
            
            // Block thread waiting for completion to safely release stream resources
            val durationMs = ((audioBytes.size / 2.0) / sampleRate) * 1000
            Thread.sleep(durationMs.toLong() + 200)
            
            audioTrack.stop()
            audioTrack.release()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
