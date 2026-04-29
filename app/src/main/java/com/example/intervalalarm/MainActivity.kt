package com.example.intervalalarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()

        val intervalInput = findViewById<EditText>(R.id.intervalSecondsInput)
        val titleInput = findViewById<EditText>(R.id.titleInput)
        val batteryBtn = findViewById<Button>(R.id.batteryBtn)
        val startBtn = findViewById<Button>(R.id.startBtn)
        val stopBtn = findViewById<Button>(R.id.stopBtn)

        batteryBtn.setOnClickListener {
            openBatteryOptimizationSettings()
        }

        startBtn.setOnClickListener {
            val sec = intervalInput.text.toString().toLongOrNull()
            val title = titleInput.text.toString().trim().ifEmpty { "알림" }
            if (sec == null || sec < 1) {
                Toast.makeText(this, "1초 이상 입력해줘", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ReminderService::class.java).apply {
                action = ReminderService.ACTION_START
                putExtra(ReminderService.EXTRA_INTERVAL_SEC, sec)
                putExtra(ReminderService.EXTRA_TITLE, title)
            }
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "반복 알림 시작: ${sec}초", Toast.LENGTH_SHORT).show()
        }

        stopBtn.setOnClickListener {
            val intent = Intent(this, ReminderService::class.java).apply {
                action = ReminderService.ACTION_STOP
            }
            startService(intent)
            Toast.makeText(this, "반복 알림 중지", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            val pkg = packageName
            if (!pm.isIgnoringBatteryOptimizations(pkg)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$pkg")
                    startActivity(intent)
                    return
                } catch (_: Exception) {}
            }
        }
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}
