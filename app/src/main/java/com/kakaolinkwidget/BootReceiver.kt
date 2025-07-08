package com.kakaolinkwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // 위젯이 활성화되어 있는지 확인 후 업데이트 작업 시작
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, KakaoLinkWidgetProvider::class.java)
            )
            
            if (appWidgetIds.isNotEmpty()) {
                // 위젯 프로바이더의 스케줄 업데이트 메서드 호출
                val widgetIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
                widgetIntent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                widgetIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                context.sendBroadcast(widgetIntent)
            }
        }
    }
} 