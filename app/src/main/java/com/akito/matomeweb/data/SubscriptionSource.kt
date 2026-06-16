package com.akito.matomeweb.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_sources")
data class SubscriptionSource(
    @PrimaryKey val url: String,
    val name: String,
    val isEnabled: Boolean
)
