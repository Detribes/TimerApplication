package com.detribes.timerapplication.features.timerscreen.presentation

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detribes.timerapplication.R
import com.detribes.timerapplication.app.services.ServiceDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TimerState(
    @StringRes val startPauseText: Int,
    val visibility: Int
) {
    data object Idle :
        TimerState(R.string.start, View.INVISIBLE)

    data class Running(
        val timeLeft: Long,
    ) : TimerState(R.string.pause, View.VISIBLE)

    data class Paused(
        val timeLeft: Long,
    ) : TimerState(R.string.resume, View.VISIBLE)

    data object Finished : TimerState(R.string.start, View.INVISIBLE)
}

class TimerScreenViewModel(
    private val serviceDelegate: ServiceDelegate
) : ViewModel() {

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state

    init {
        viewModelScope.launch {
            serviceDelegate.timeFlow.collect { time ->
                val current = _state.value
                if (time <= 0L) {
                    _state.value = TimerState.Finished
                } else {
                    _state.value = when (current) {
                        is TimerState.Paused -> TimerState.Paused(time)
                        else -> TimerState.Running(time)
                    }
                }
            }
        }
    }

    fun startTimer(seconds: Long) {
        serviceDelegate.startTimer(seconds)
        _state.value = TimerState.Running(seconds)
    }

    fun pauseTimer() {
        serviceDelegate.pauseTimer()
        val current = (_state.value as? TimerState.Running) ?: return
        _state.value = TimerState.Paused(current.timeLeft)
    }

    fun resumeTimer() {
        serviceDelegate.resumeTimer()
        val current = (_state.value as? TimerState.Paused) ?: return
        _state.value = TimerState.Running(current.timeLeft)
    }

    fun stopTimer() {
        serviceDelegate.stopTimer()
        _state.value = TimerState.Idle
    }
}
