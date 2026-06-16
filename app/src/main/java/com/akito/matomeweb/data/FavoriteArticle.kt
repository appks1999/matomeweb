package com.akito.matomeweb.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_articles")
data class FavoriteArticle(
    @PrimaryKey val link: String,
    val title: String,
    val pubDate: String?,
    val sourceName: String,
    val imageUrl: String?,
    val savedAt: Long
)
