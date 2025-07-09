package com.kakaolinkwidget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID_EMOTICON = "emoticon_updates"
        private const val CHANNEL_ID_LINK = "link_updates"
        private const val NOTIFICATION_ID_EMOTICON = 1001
        private const val NOTIFICATION_ID_LINK = 1002
    }
    
    init {
        createNotificationChannelEmoticon()
        createNotificationChannelLink()
    }
    
    private fun createNotificationChannelEmoticon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "이모티콘 업데이트"
            val descriptionText = "새로운 이모티콘 알림"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_EMOTICON, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotificationChannelLink() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "링크 업데이트"
            val descriptionText = "새로운 링크 알림"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_LINK, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNewEmoticonNotification(newCount: Int) {
        if (newCount <= 0) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://2244.tistory.com"))
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = "🎉 새로운 이모티콘 등록!"
        val content = if (newCount == 1) {
            "새로운 카카오톡 이모티콘 1개가 등록되었습니다"
        } else {
            "새로운 카카오톡 이모티콘 ${newCount}개가 등록되었습니다"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMOTICON)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID_EMOTICON, notification)
        } catch (e: SecurityException) {
            // 알림 권한 오류 처리
        } catch (e: Exception) {
            // 알림 전송 오류 처리
        }
    }

    fun showNewLinkNotification(newCount: Int) {
        if (newCount <= 0) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cafe.naver.com/todaysppc"))
        val pendingIntent = PendingIntent.getActivity(
            context, 
            1, // requestCode 분리
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = "🔗 새로운 링크 등록!"
        val content = if (newCount == 1) {
            "새로운 링크 1개가 등록되었습니다"
        } else {
            "새로운 링크 ${newCount}개가 등록되었습니다"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LINK)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID_LINK, notification)
        } catch (e: SecurityException) {
            // 알림 권한 오류 처리
        } catch (e: Exception) {
            // 알림 전송 오류 처리
        }
    }
} 