package com.kakaolinkwidget

import android.util.Log
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.nio.charset.Charset

class RssParser {
    
    companion object {
        private const val RSS_URL = "https://2244.tistory.com/rss"
        private const val TAG = "RssParser"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun parseKakaoLinks(): List<KakaoLink> {
        val result = mutableListOf<KakaoLink>()
        // 1. 기존 이모티콘(RSS) 3개만
        try {
            val request = Request.Builder()
                .url(RSS_URL)
                .addHeader("User-Agent", "KakaoLink Widget Android App")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val input = SyndFeedInput()
                val feed = input.build(XmlReader(responseBody.byteInputStream()))
                val entries = feed.entries.take(3)
                for (entry in entries) {
                    val title = entry.title ?: ""
                    val desc = entry.description?.value ?: ""
                    val friendUrl = extractKakaoFriendUrl(desc)
                    val url = friendUrl ?: (entry.link ?: "")
                    result.add(KakaoLink(title, url, desc))
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("KakaoLinkWidget", "RSS 파싱 오류: ${e.message}")
        }

        // 2. todaysppc에서 '가을타타타' 글 3개
        try {
            val url = "http://m.todaysppc.com/renewal/list.php?id=free"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                val html = bytes?.toString(Charset.forName("euc-kr")) ?: ""
                android.util.Log.d("KakaoLinkWidget", "todaysppc HTML 길이: ${html.length}")
                val doc = org.jsoup.Jsoup.parse(html)
                val liList = doc.select("ul[data-role=listview] > li")
                android.util.Log.d("KakaoLinkWidget", "todaysppc li 개수: ${liList.size}")
                var added = 0
                for (li in liList) {
                    val a = li.selectFirst("a") ?: continue
                    val ps = a.select("p")
                    if (ps.size < 2) continue
                    val titleRaw = ps[0].text().trim()
                    val infoRaw = ps[1].text().trim()
                    // 글쓴이 추출: infoRaw에서 strong 태그의 첫 번째 값
                    val strongs = ps[1].select("strong")
                    val author = if (strongs.size > 0) strongs[0].text().trim() else ""
                    val date = infoRaw.split(" ").firstOrNull() ?: ""
                    android.util.Log.d("KakaoLinkWidget", "todaysppc 글쓴이: $author, 날짜: $date, 제목: $titleRaw")
                    if (author == "가을타타타") {
                        val link = a.attr("href")
                        val fullUrl = if (link.startsWith("http")) link else "http://m.todaysppc.com/renewal/" + link
                        val title = "$date $titleRaw"
                        result.add(KakaoLink(title, fullUrl, author))
                        added++
                        if (added >= 3) break
                    }
                }
                android.util.Log.d("KakaoLinkWidget", "todaysppc '가을타타타' 글 추가 개수: $added")
            } else {
                android.util.Log.d("KakaoLinkWidget", "todaysppc 응답 실패: ${response.code}")
            }
        } catch (e: Exception) {
            android.util.Log.d("KakaoLinkWidget", "todaysppc 파싱 오류: ${e.message}")
        }
        return result
    }
    
    private fun extractKakaoFriendUrl(description: String): String? {
        // http://pf.kakao.com/*/friend 형식의 링크를 찾되, _xaxdxdxmj는 제외
        val pattern = "http://pf\\.kakao\\.com/([^/]+)/friend".toRegex()
        val matches = pattern.findAll(description)
        
        for (match in matches) {
            val fullUrl = match.value
            val channelId = match.groupValues[1]
            
            // _xaxdxdxmj 제외
            if (channelId != "_xaxdxdxmj") {
                return fullUrl
            }
        }
        
        return null
    }
} 