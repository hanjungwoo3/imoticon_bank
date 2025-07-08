package com.kakaolinkwidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class KakaoLinkWidgetFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var kakaoLinks: List<KakaoLink> = emptyList()

    override fun onCreate() {
        // 캐시된 데이터 로드
        kakaoLinks = KakaoLinkWidgetService.getCachedData()
    }

    override fun onDataSetChanged() {
        // 캐시된 데이터 새로 로드
        kakaoLinks = KakaoLinkWidgetService.getCachedData()
    }

    override fun onDestroy() {
        // 정리 작업
    }

    override fun getCount(): Int {
        return kakaoLinks.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_link_item)

        if (position < kakaoLinks.size) {
            val link = kakaoLinks[position]

            // 제목 간단하게 파싱: "월/일 카카오톡 무료 이모티콘 : 육아티콘" -> "월/일 육아티콘"
            val cleanTitle = simplifyTitle(link.title)
            views.setTextViewText(R.id.link_title, cleanTitle)

            // URL을 숨김 (보이지 않게 설정)
            views.setTextViewText(R.id.link_url, "")
            views.setViewVisibility(R.id.link_url, android.view.View.GONE)

            // 각 아이템마다 고유한 action으로 클릭 이벤트 설정
            val clickIntent = Intent(context, KakaoLinkWidgetProvider::class.java)
            clickIntent.action = "${KakaoLinkWidgetProvider.ACTION_WEBSITE}_$position"
            clickIntent.putExtra("url", link.url)
            clickIntent.putExtra("position", position)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                position,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.item_container, pendingIntent)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun simplifyTitle(title: String): String {
        // "7/8 카카오톡 무료 이모티콘 : 육아티콘" -> "7/8 육아티콘"
        var simplified = title
            .replace("카카오톡", "")
            .replace("무료", "")
            .replace("이모티콘", "")
            .replace(":", "")
            .replace("：", "") // 전각 콜론도 제거
            .replace("\\s+".toRegex(), " ") // 연속된 공백을 하나로
            .trim()

        return simplified
    }

    // 이모티콘(카카오톡 친구 링크)와 일반 게시물 분리
    private fun getEmoticons(): List<KakaoLink> = kakaoLinks.filter { it.isValid() }
    private fun getPosts(): List<KakaoLink> = kakaoLinks.filterNot { it.isValid() }
    fun getEmoticonCount(): Int = getEmoticons().size
} 