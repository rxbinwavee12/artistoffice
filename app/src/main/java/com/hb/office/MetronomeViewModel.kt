package com.hb.office

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MetronomeViewModel : ViewModel() {
    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var metronomeJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    private val tapTimes = mutableListOf<Long>()

    fun setBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(30, 260)
        _bpm.value = clamped
    }

    fun tapTempo() {
        val now = System.currentTimeMillis()
        tapTimes.add(now)

        // Nur die letzten 4 Taps behalten
        if (tapTimes.size > 4) {
            tapTimes.removeAt(0)
        }

        if (tapTimes.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until tapTimes.size) {
                intervals.add(tapTimes[i] - tapTimes[i - 1])
            }
            val avgInterval = intervals.average()
            if (avgInterval > 0) {
                val calculatedBpm = (60000.0 / avgInterval).toInt().coerceIn(30, 260)
                _bpm.value = calculatedBpm
            }
        }
    }

    fun togglePlay() {
        if (_isPlaying.value) {
            stopMetronome()
        } else {
            startMetronome()
        }
    }

    private fun startMetronome() {
        _isPlaying.value = true
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        metronomeJob?.cancel()
        metronomeJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isPlaying.value) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
                val intervalMs = (60000L / _bpm.value).coerceAtLeast(100L)
                delay(intervalMs)
            }
        }
    }

    private fun stopMetronome() {
        _isPlaying.value = false
        metronomeJob?.cancel()
        metronomeJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMetronome()
        toneGenerator?.release()
        toneGenerator = null
    }
}
