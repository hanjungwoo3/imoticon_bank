package com.kakaolinkwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "WidgetUpdateWorker"
    }
    
    override suspend fun doWork(): Result {
        return try {
            // 위젯 업데이트 브로드캐스트 전송
            val intent = Intent(applicationContext, KakaoLinkWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(applicationContext, KakaoLinkWidgetProvider::class.java)
            )
            
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            applicationContext.sendBroadcast(intent)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
} 