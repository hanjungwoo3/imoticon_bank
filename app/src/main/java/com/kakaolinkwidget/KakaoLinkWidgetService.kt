package com.kakaolinkwidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class KakaoLinkWidgetService : RemoteViewsService() {
    
    companion object {
        private var cachedLinks: List<KakaoLink> = emptyList()
        
        // 캐시된 데이터 업데이트
        fun updateCachedData(links: List<KakaoLink>) {
            cachedLinks = links
        }
        
        // 캐시된 데이터 가져오기
        fun getCachedData(): List<KakaoLink> {
            return cachedLinks
        }
    }
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return KakaoLinkWidgetFactory(this.applicationContext, intent)
    }
} 