package com.akito.matomeweb.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "read_articles")
data class ReadArticle(
    @PrimaryKey val link: String,
    val readAt: Long = System.currentTimeMillis()
)
