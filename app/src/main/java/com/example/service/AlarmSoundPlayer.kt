package com.example.service

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.sin

class AlarmSoundPlayer(private val context: Context) {
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var ringtone: Ringtone? = null
    @Volatile private var mediaPlayer: android.media.MediaPlayer? = null
    @Volatile private var isPlaying = false
    @Volatile private var playbackJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun play(soundType: String) {
        synchronized(this) {
            stop()
            isPlaying = true

            if (soundType == "System Default") {
                try {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ringtone = RingtoneManager.getRingtone(context, alarmUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone?.audioAttributes = android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        ringtone?.streamType = AudioManager.STREAM_ALARM
                    }
                    ringtone?.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // fallback to synthesized
                    playSynthesizedSound("Classic Sirens")
                }
            } else if (soundType == "Custom MP3 Siren") {
                playCustomMp3()
            } else if (soundType == "Uploaded Siren") {
                playUploadedSiren()
            } else {
                playSynthesizedSound(soundType)
            }
        }
    }

    fun stop() {
        synchronized(this) {
            isPlaying = false
            playbackJob?.cancel()
            playbackJob = null

            try {
                audioTrack?.apply {
                    if (state == AudioTrack.STATE_INITIALIZED) {
                        stop()
                        release()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            audioTrack = null

            try {
                ringtone?.apply {
                    if (isPlaying) {
                        stop()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            ringtone = null

            try {
                mediaPlayer?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        // Ignore if not playing
                    }
                    release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
    }

    private fun playCustomMp3() {
        playbackJob = coroutineScope.launch(Dispatchers.IO) {
            val cacheFile = java.io.File(context.cacheDir, "siren_cached.mp3")
            val sirenUrl = "https://raw.githubusercontent.com/AnandChowdhary/sounds/master/siren.mp3"

            try {
                val mp = android.media.MediaPlayer().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                }

                if (cacheFile.exists() && cacheFile.length() > 1000) {
                    mp.setDataSource(cacheFile.absolutePath)
                    mp.prepare()
                } else {
                    mp.setDataSource(sirenUrl)
                    mp.prepare()
                    // Download and cache for future offline plays in background
                    launch(Dispatchers.IO) {
                        try {
                            val connection = java.net.URL(sirenUrl).openConnection()
                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000
                            connection.getInputStream().use { input ->
                                java.io.FileOutputStream(cacheFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (cacheFile.exists()) {
                                cacheFile.delete()
                            }
                        }
                    }
                }

                synchronized(this@AlarmSoundPlayer) {
                    if (!isPlaying) {
                        mp.release()
                        return@launch
                    }
                    mediaPlayer = mp
                    mp.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isPlaying) {
                    launch(Dispatchers.Default) {
                        playSynthesizedSound("Classic Sirens")
                    }
                }
            }
        }
    }

    private fun playUploadedSiren() {
        playbackJob = coroutineScope.launch(Dispatchers.IO) {
            val userFile = java.io.File(context.filesDir, "custom_user_siren.mp3")
            if (!userFile.exists() || userFile.length() == 0L) {
                if (isPlaying) {
                    launch(Dispatchers.Default) {
                        playSynthesizedSound("Classic Sirens")
                    }
                }
                return@launch
            }

            try {
                val mp = android.media.MediaPlayer().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                    setDataSource(userFile.absolutePath)
                    prepare()
                }

                synchronized(this@AlarmSoundPlayer) {
                    if (!isPlaying) {
                        mp.release()
                        return@launch
                    }
                    mediaPlayer = mp
                    mp.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isPlaying) {
                    launch(Dispatchers.Default) {
                        playSynthesizedSound("Classic Sirens")
                    }
                }
            }
        }
    }

    private fun playSynthesizedSound(soundType: String) {
        playbackJob = coroutineScope.launch {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBufferSize > 0) minBufferSize * 2 else 4096

            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val attributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val format = AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()
                AudioTrack(attributes, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_ALARM,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            synchronized(this@AlarmSoundPlayer) {
                if (!isPlaying) {
                    track.release()
                    return@launch
                }
                audioTrack = track
            }

            try {
                track.play()
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            }

            var phase = 0.0
            val buffer = ShortArray(1024)
            var sampleCount = 0L

            while (isPlaying) {
                for (i in buffer.indices) {
                    val frequency = getFrequency(soundType, sampleCount, sampleRate)
                    val angle = 2.0 * Math.PI * frequency / sampleRate
                    phase += angle
                    if (phase > 2.0 * Math.PI) {
                        phase -= 2.0 * Math.PI
                    }
                    val amplitude = getAmplitude(soundType, sampleCount, sampleRate)
                    buffer[i] = (sin(phase) * 32767.0 * amplitude).toInt().coerceIn(-32768, 32767).toShort()
                    sampleCount++
                }
                val written = audioTrack?.write(buffer, 0, buffer.size) ?: 0
                if (written <= 0) break
            }
        }
    }

    private fun getFrequency(soundType: String, sampleCount: Long, sampleRate: Int): Double {
        val seconds = sampleCount.toDouble() / sampleRate
        return when (soundType) {
            "Classic Sirens" -> {
                // Sweeping between 500 Hz and 1100 Hz every 0.6 seconds
                val cyclePos = (seconds % 0.6) / 0.6
                if (cyclePos < 0.5) {
                    500.0 + (1100.0 - 500.0) * (cyclePos * 2)
                } else {
                    1100.0 - (1100.0 - 500.0) * ((cyclePos - 0.5) * 2)
                }
            }
            "Retro Beeps" -> {
                // Simple 800 Hz pitch, we will chop the amplitude instead
                880.0
            }
            "Zen Waves" -> {
                // A soothing dual tone or slowly breathing frequency around 220Hz
                220.0 + 5.0 * sin(2.0 * Math.PI * seconds * 0.5)
            }
            "Digital Alert" -> {
                // High frequency alert
                1440.0
            }
            else -> 800.0
        }
    }

    private fun getAmplitude(soundType: String, sampleCount: Long, sampleRate: Int): Double {
        val seconds = sampleCount.toDouble() / sampleRate
        return when (soundType) {
            "Classic Sirens" -> 0.8
            "Retro Beeps" -> {
                // Beep for 0.15s, silent for 0.15s
                val cycle = seconds % 0.3
                if (cycle < 0.15) 0.8 else 0.0
            }
            "Zen Waves" -> {
                // Breathing amplitude mod
                0.5 + 0.3 * sin(2.0 * Math.PI * seconds * 1.5)
            }
            "Digital Alert" -> {
                // Staccato bursts: 3 fast beeps of 80ms, then 600ms pause
                val cycle = seconds % 0.9
                if (cycle < 0.08 || (cycle in 0.12..0.20) || (cycle in 0.24..0.32)) {
                    0.8
                } else {
                    0.0
                }
            }
            else -> 0.7
        }
    }
}
