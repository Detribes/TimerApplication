package com.detribes.timerapplication.app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.detribes.timerapplication.R
import org.koin.android.ext.android.inject

class TimerService : Service() {

    private val serviceDelegate : ServiceDelegate by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForegroundService()

        serviceDelegate.init(
            onUpdateNotification = { timeLeft -> updateNotification(timeLeft) },
            onTimerFinished = { stopSelf() }
        )

        intent?.getLongExtra("duration", 0L)?.takeIf { it > 0 }?.let {
            serviceDelegate.startTimer(it)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceDelegate.stopTimer()
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
}

