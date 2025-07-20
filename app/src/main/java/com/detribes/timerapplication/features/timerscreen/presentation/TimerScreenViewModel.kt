package com.detribes.timerapplication.features.timerscreen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detribes.timerapplication.app.services.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TimerState {
    data object Idle : TimerState()
    data class Running(val timeLeft: Long) : TimerState()
    data class Paused(val timeLeft: Long) : TimerState()
    data object Finished : TimerState()
}

class TimerScreenViewModel : ViewModel() {
    private var service: TimerService? = null
    private var serviceJob: Job? = null
    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state

    fun bindService(service: TimerService) {
        this.service = service
        serviceJob = viewModelScope.launch {
            service.timeFlow.collect { time ->
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
        service?.startTimer(seconds)
        _state.value = TimerState.Running(seconds)
    }

    fun pauseTimer() {
        service?.pauseTimer()
        val current = (_state.value as? TimerState.Running) ?: return
        _state.value = TimerState.Paused(current.timeLeft)
    }

    fun resumeTimer() {
        service?.resumeTimer()
        val current = (_state.value as? TimerState.Paused) ?: return
        _state.value = TimerState.Running(current.timeLeft)
    }

    fun stopTimer() {
        service?.stopTimer()
        _state.value = TimerState.Idle
    }

}
