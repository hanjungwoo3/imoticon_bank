package com.kakaolinkwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class KakaoLinkWidgetProvider : AppWidgetProvider() {
    
    companion object {
        private const val TAG = "KakaoLinkWidget"
        private const val ACTION_REFRESH = "com.kakaolinkwidget.REFRESH"
        const val ACTION_WEBSITE = "com.kakaolinkwidget.WEBSITE"
        const val ACTION_ITEM_CLICK = "com.kakaolinkwidget.ITEM_CLICK"
        private const val WORK_NAME = "widget_update_work"
        private const val PREF_NAME = "widget_links"
        private const val PREF_EMOTICONS = "emoticon_list_json"
        private const val PREF_LINKS = "link_list_json"
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 모든 위젯 인스턴스 업데이트
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        // 자동 업데이트 작업 시작
        scheduleWidgetUpdate(context)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH -> {
                refreshAllWidgets(context)
            }
            ACTION_WEBSITE -> {
                openLink(context, "https://2244.tistory.com")
            }
            ACTION_ITEM_CLICK -> {
                val url = intent.getStringExtra("url") ?: "https://2244.tistory.com"
                openLink(context, url)
            }
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWidgetUpdate(context)
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        // 새로고침 버튼 설정
        val refreshIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
        refreshIntent.action = ACTION_REFRESH
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
        
        // 제목 클릭 시 사이트 이동
        val websiteIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
        websiteIntent.action = ACTION_WEBSITE
        val websitePendingIntent = PendingIntent.getBroadcast(
            context, 1, websiteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, websitePendingIntent)
        
        // 로딩 상태 표시
        views.setTextViewText(R.id.status_text, context.getString(R.string.loading))
        
        // 위젯 업데이트
        appWidgetManager.updateAppWidget(appWidgetId, views)
        
        // 백그라운드에서 실제 데이터 로드
        loadWidgetData(context, appWidgetManager, appWidgetId)
    }
    
    private fun loadWidgetData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationHelper = NotificationHelper(context)
            val gson = Gson()
            // 1. 이모티콘 fetch
            var emoticons: List<KakaoLink>? = null
            try {
                val parser = RssParser()
                val kakaoLinks = parser.parseKakaoLinks()
                emoticons = kakaoLinks.filter { it.description != "가을타타타" }
            } catch (_: Exception) {}
            val oldEmoticons = loadListFromPrefs(context, PREF_EMOTICONS)
            val emoticonsToUse = if (emoticons.isNullOrEmpty()) oldEmoticons else emoticons
            if (!emoticons.isNullOrEmpty()) {
                val newItems = emoticons.filter { new -> oldEmoticons.none { it.title == new.title } }
                if (newItems.isNotEmpty()) {
                    notificationHelper.showNewEmoticonNotification(newItems.size)
                }
                saveListToPrefs(context, PREF_EMOTICONS, emoticons)
            }
            // 2. 링크 fetch
            var links: List<KakaoLink>? = null
            try {
                val parser = RssParser()
                val kakaoLinks = parser.parseKakaoLinks()
                links = kakaoLinks.filter { it.description == "가을타타타" }
            } catch (_: Exception) {}
            val oldLinks = loadListFromPrefs(context, PREF_LINKS)
            val linksToUse = if (links.isNullOrEmpty()) oldLinks else links
            if (!links.isNullOrEmpty()) {
                val newItems = links.filter { new -> oldLinks.none { it.title == new.title } }
                if (newItems.isNotEmpty()) {
                    notificationHelper.showNewLinkNotification(newItems.size)
                }
                saveListToPrefs(context, PREF_LINKS, links)
            }
            // 3. UI 스레드에서 위젯 업데이트 (가장 최신 데이터로)
            val kakaoLinks = emoticonsToUse + linksToUse
            CoroutineScope(Dispatchers.Main).launch {
                updateWidgetWithData(context, appWidgetManager, appWidgetId, kakaoLinks)
            }
        }
    }

    private fun saveListToPrefs(context: Context, key: String, list: List<KakaoLink>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString(key, json).apply()
    }

    private fun loadListFromPrefs(context: Context, key: String): List<KakaoLink> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<KakaoLink>>() {}.type
        return try {
            Gson().fromJson(json, type)
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    private fun updateWidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        kakaoLinks: List<KakaoLink>
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        // 새로고침 버튼 설정
        val refreshIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
        refreshIntent.action = ACTION_REFRESH
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
        
        // 제목 클릭 시 사이트 이동
        val websiteIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
        websiteIntent.action = ACTION_WEBSITE
        val websitePendingIntent = PendingIntent.getBroadcast(
            context, 1, websiteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, websitePendingIntent)
        
        // 링크 컨테이너 초기화
        views.removeAllViews(R.id.links_container)

        // 출처별 그룹화: 이모티콘(RSS) 3개, todaysppc(가을타타타) 3개
        val emoticons = kakaoLinks.filter { it.description != "가을타타타" }.take(3)
        val todaysppc = kakaoLinks.filter { it.description == "가을타타타" }.take(3)
        var itemIndex = 0
        // 이모티콘(RSS) 그룹
        for (link in emoticons) {
            val itemView = RemoteViews(context.packageName, R.layout.widget_link_item)
            val cleanTitle = simplifyTitle(link.title)
            itemView.setTextViewText(R.id.link_title, cleanTitle)
            itemView.setTextViewText(R.id.link_url, "")
            itemView.setViewVisibility(R.id.link_url, android.view.View.GONE)
            val itemClickIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
            itemClickIntent.action = ACTION_ITEM_CLICK
            itemClickIntent.putExtra("url", link.url)
            val itemPendingIntent = PendingIntent.getBroadcast(
                context, 200 + itemIndex, itemClickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            itemView.setOnClickPendingIntent(R.id.item_container, itemPendingIntent)
            views.addView(R.id.links_container, itemView)
            itemIndex++
        }
        // separator (todaysppc가 있을 때만)
        if (emoticons.isNotEmpty() && todaysppc.isNotEmpty()) {
            val separator = RemoteViews(context.packageName, R.layout.widget_separator)
            views.addView(R.id.links_container, separator)
        }
        // todaysppc 그룹
        for (link in todaysppc) {
            val itemView = RemoteViews(context.packageName, R.layout.widget_link_item)
            val cleanTitle = simplifyTitle(link.title)
            itemView.setTextViewText(R.id.link_title, cleanTitle)
            itemView.setTextViewText(R.id.link_url, "")
            itemView.setViewVisibility(R.id.link_url, android.view.View.GONE)
            val itemClickIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
            itemClickIntent.action = ACTION_ITEM_CLICK
            itemClickIntent.putExtra("url", link.url)
            val itemPendingIntent = PendingIntent.getBroadcast(
                context, 200 + itemIndex, itemClickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            itemView.setOnClickPendingIntent(R.id.item_container, itemPendingIntent)
            views.addView(R.id.links_container, itemView)
            itemIndex++
        }
        
        if (kakaoLinks.isEmpty()) {
            views.setTextViewText(R.id.status_text, "데이터를 불러오는 중...")
            views.setViewVisibility(R.id.status_text, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.status_text, android.view.View.GONE)
        }
        
        // 위젯 업데이트
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    private fun updateWidgetWithError(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        // 새로고침 버튼 설정
        val refreshIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
        refreshIntent.action = ACTION_REFRESH
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
        
        // 제목 클릭 시 사이트 이동
        val websiteIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
        websiteIntent.action = ACTION_WEBSITE
        val websitePendingIntent = PendingIntent.getBroadcast(
            context, 1, websiteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, websitePendingIntent)
        
        views.setTextViewText(R.id.status_text, context.getString(R.string.error_loading))
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    private fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, KakaoLinkWidgetProvider::class.java)
        )
        
        appWidgetIds.forEach { appWidgetId ->
            // 먼저 로딩 상태로 표시
            showLoadingState(context, appWidgetManager, appWidgetId)
            // 백그라운드에서 데이터 로드
            loadWidgetData(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun showLoadingState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        // 새로고침 버튼 설정
        val refreshIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
        refreshIntent.action = ACTION_REFRESH
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)
        
        // 제목 클릭 시 사이트 이동
        val websiteIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
        websiteIntent.action = ACTION_WEBSITE
        val websitePendingIntent = PendingIntent.getBroadcast(
            context, 1, websiteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, websitePendingIntent)
        
        // 모든 기존 아이템 제거
        views.removeAllViews(R.id.links_container)
        
        // 로딩 상태 표시
        views.setTextViewText(R.id.status_text, "🔄 새로고침 중...")
        views.setViewVisibility(R.id.status_text, android.view.View.VISIBLE)
        
        // 위젯 즉시 업데이트
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    private fun openLink(context: Context, url: String) {
        try {
            // 카카오톡 친구 링크인 경우 브라우저를 통한 리다이렉트 방식 사용
            if (url.contains("pf.kakao.com") && url.contains("friend")) {
            }
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 일반 링크이거나 카카오톡 앱 실패 시 Chrome 브라우저를 시도
            intent.setPackage("com.android.chrome")
            
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Chrome이 없으면 기본 브라우저로 시도
                intent.setPackage(null)
                
                try {
                    context.startActivity(intent)
                } catch (e2: Exception) {
                }
            }
        } catch (e: Exception) {
        }
    }
    
    private fun simplifyTitle(originalTitle: String): String {
        // 1. 날짜 파싱 (MM/dd 형식)
        val datePattern = """(\d{2}/\d{2})""".toRegex()
        val dateMatch = datePattern.find(originalTitle)
        val datePrefix = dateMatch?.groupValues?.get(1) ?: ""
        
        // 2. ":" 기준으로 분리해서 뒤쪽 값만 가져오기
        val colonIndex = originalTitle.indexOf(":")
        val contentAfterColon = if (colonIndex != -1 && colonIndex < originalTitle.length - 1) {
            originalTitle.substring(colonIndex + 1).trim()
        } else {
            // ":" 가 없으면 원본에서 날짜 이후 부분 사용
            val afterDate = originalTitle.replace(datePattern, "").trim()
            // 불필요한 단어들 제거
            var content = afterDate
            val wordsToRemove = listOf("카카오톡", "무료", "이모티콘")
            wordsToRemove.forEach { word ->
                content = content.replace(word, "", ignoreCase = true)
            }
            content.trim()
        }
        
        // 3. "날짜 + 분리한 값" 조합
        return if (datePrefix.isNotEmpty() && contentAfterColon.isNotEmpty()) {
            "$datePrefix $contentAfterColon"
        } else if (datePrefix.isNotEmpty()) {
            datePrefix
        } else {
            contentAfterColon.ifEmpty { originalTitle }
        }
    }
    
    private fun scheduleWidgetUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getLong("refresh_interval", 60)
        
        val finalInterval = if (intervalMinutes < 30) 30 else intervalMinutes
        
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            finalInterval, java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(5, java.util.concurrent.TimeUnit.MINUTES)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    private fun cancelWidgetUpdate(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
} 