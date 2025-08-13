package com.detribes.timerapplication.app.services

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ServiceDelegate {
    private val _timeFlow = MutableStateFlow(0L)
    val timeFlow: StateFlow<Long> = _timeFlow

    private var remainingTime = 0L
    private var isPaused = false
    private var job: Job? = null

    private var onUpdateNotification: ((Long) -> Unit)? = null
    private var onTimerFinished: (() -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(
        onUpdateNotification: (Long) -> Unit,
        onTimerFinished: () -> Unit
    ) {
        this.onUpdateNotification = onUpdateNotification
        this.onTimerFinished = onTimerFinished
    }

    fun startTimer(duration: Long) {
        job?.cancel()
        remainingTime = duration
        isPaused = false
        runTimer()
    }

    fun pauseTimer() {
        isPaused = true
    }

    fun resumeTimer() {
        if (isPaused) {
            isPaused = false
            runTimer()
        }
    }

    fun stopTimer() {
        job?.cancel()
        remainingTime = 0L
        _timeFlow.value = 0L
        onTimerFinished?.invoke()
    }

    private fun runTimer() {
        job = scope.launch {
            while (remainingTime > 0 && !isPaused) {
                delay(1000)
                remainingTime -= 1
                _timeFlow.value = remainingTime
                onUpdateNotification?.invoke(remainingTime)
            }
            if (remainingTime <= 0) {
                onTimerFinished?.invoke()
            }
        }
    }
}
