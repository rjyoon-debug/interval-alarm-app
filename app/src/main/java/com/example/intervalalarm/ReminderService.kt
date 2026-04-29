package com.example.intervalalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask

class ReminderService : Service() {
    private var timer: Timer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sec = intent.getLongExtra(EXTRA_INTERVAL_SEC, 60L).coerceAtLeast(1L)
                val title = intent.getStringExtra(EXTRA_TITLE)?.ifBlank { "알림" } ?: "알림"
                startForeground(NOTI_ID_SERVICE, buildServiceNotification(sec, title))
                startRepeating(sec, title)
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRepeating(sec: Long, title: String) {
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                showReminderNotification(sec, title)
            }
        }, sec * 1000, sec * 1000)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel1 = NotificationChannel(CHANNEL_SERVICE, "서비스 채널", NotificationManager.IMPORTANCE_LOW)
            val channel2 = NotificationChannel(CHANNEL_REMINDER, "리마인더 채널", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel1)
            nm.createNotificationChannel(channel2)
        }
    }

    private fun buildServiceNotification(sec: Long, title: String): Notification {
        createChannel()
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Interval Alarm 실행 중")
            .setContentText("${sec}초 간격 · 제목: ${title}")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun showReminderNotification(sec: Long, title: String) {
        createChannel()
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val n = NotificationCompat.Builder(this, CHANNEL_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("${sec}초 반복 알림")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .build()
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n)
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_INTERVAL_SEC = "interval_sec"
        const val EXTRA_TITLE = "title"
        private const val CHANNEL_SERVICE = "interval_alarm_service"
        private const val CHANNEL_REMINDER = "interval_alarm_reminder"
        private const val NOTI_ID_SERVICE = 10001
    }
}
