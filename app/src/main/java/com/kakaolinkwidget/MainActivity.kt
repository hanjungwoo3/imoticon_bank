package com.kakaolinkwidget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var permissionStatus: TextView
    private lateinit var rssStatus: TextView
    private lateinit var refreshButton: Button
    private lateinit var testNotificationButton: Button
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        updatePermissionStatus()
        if (isGranted) {
            Toast.makeText(this, "✅ 알림 권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ 알림 권한이 거부되었습니다", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupClickListeners()
        
        // 초기 상태 확인
        updatePermissionStatus()
        checkRssStatus()
        scheduleWidgetUpdate()
        
        // 위젯에서 권한 요청으로 실행된 경우 자동으로 권한 요청
        if (intent.getBooleanExtra("request_permission", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                Toast.makeText(this, "🔔 알림 권한이 필요합니다", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "🎉 이모티콘뱅크 앱에 오신 것을 환영합니다!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initializeViews() {
        permissionStatus = findViewById(R.id.permissionStatus)
        rssStatus = findViewById(R.id.rssStatus)
        refreshButton = findViewById(R.id.refreshButton)
        testNotificationButton = findViewById(R.id.testNotificationButton)
    }
    
    private fun setupClickListeners() {
        refreshButton.setOnClickListener {
            refreshStatus()
        }
        
        testNotificationButton.setOnClickListener {
            testNotification()
        }
    }
    
    private fun updatePermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    permissionStatus.text = "✅ 알림 권한: 허용됨"
                    permissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                    testNotificationButton.isEnabled = true
                }
                else -> {
                    permissionStatus.text = "❌ 알림 권한: 거부됨 (클릭하여 설정)"
                    permissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                    testNotificationButton.isEnabled = false
                    
                    // 권한 상태 텍스트 클릭 시 권한 요청
                    permissionStatus.setOnClickListener {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        } else {
            permissionStatus.text = "✅ 알림 권한: 자동 허용 (Android 12 이하)"
            permissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            testNotificationButton.isEnabled = true
        }
    }
    
    private fun checkRssStatus() {
        rssStatus.text = "🔄 RSS 연결 확인 중..."
        rssStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val parser = RssParser()
                val kakaoLinks = parser.parseKakaoLinks()
                
                CoroutineScope(Dispatchers.Main).launch {
                    if (kakaoLinks.isNotEmpty()) {
                        rssStatus.text = "✅ RSS 연결: 정상 (${kakaoLinks.size}개 항목)"
                        rssStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                    } else {
                        rssStatus.text = "⚠️ RSS 연결: 데이터 없음"
                        rssStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_orange_dark))
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    rssStatus.text = "❌ RSS 연결: 오류 발생"
                    rssStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                }
            }
        }
    }
    
    private fun refreshStatus() {
        Toast.makeText(this, "🔄 상태를 새로고침합니다...", Toast.LENGTH_SHORT).show()
        updatePermissionStatus()
        checkRssStatus()
    }
    
    private fun testNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "❌ 알림 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val notificationHelper = NotificationHelper(this)
            notificationHelper.showNewEmoticonNotification(1)
            Toast.makeText(this, "🔔 테스트 알림을 전송했습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ 알림 전송 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun scheduleWidgetUpdate() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val updateWorkRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, java.util.concurrent.TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget_update_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateWorkRequest
        )
    }
    
    override fun onResume() {
        super.onResume()
        // 앱이 다시 foreground로 올 때 상태 새로고침
        updatePermissionStatus()
    }
} 