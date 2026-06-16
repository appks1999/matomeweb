package com.akito.matomeweb.model

data class Article(
    val title: String,
    val link: String,
    val pubDate: String?,
    val sourceName: String,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val isRead: Boolean = false
)
