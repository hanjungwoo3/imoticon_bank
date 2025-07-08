package com.kakaolinkwidget

data class KakaoLink(
    val title: String,
    val url: String,
    val description: String
) {
    fun isValid(): Boolean {
        return url.contains("pf.kakao.com") && url.contains("friend")
    }
} 