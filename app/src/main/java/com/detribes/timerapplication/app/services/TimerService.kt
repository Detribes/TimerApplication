package com.detribes.timerapplication.app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.detribes.timerapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val binder = LocalBinder()
    private val _timeFlow = MutableStateFlow(0L)
    private var remainingTime = 0L
    private var isPaused = false
    val timeFlow: StateFlow<Long> = _timeFlow

    private var job: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "timer_channel",
                "Таймер",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle("Таймер")
            .setContentText("Таймер запущен")
            .setSmallIcon(R.drawable.ic_timer)
            .build()

        startForeground(1, notification)
    }

    private fun updateNotification(timeLeft: Long) {
        val notification = NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle("Осталось времени")
            .setContentText("$timeLeft сек")
            .setSmallIcon(R.drawable.ic_timer)
            .build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(1, notification)
    }

    private fun runTimer() {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (remainingTime > 0 && !isPaused) {
                delay(1000)
                remainingTime -= 1
                _timeFlow.value = remainingTime
                updateNotification(remainingTime)
            }
            if (remainingTime <= 0) {
                stopSelf()
            }
        }
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
        stopSelf()
    }

}
