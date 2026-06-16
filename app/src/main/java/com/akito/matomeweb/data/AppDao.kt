package com.akito.matomeweb.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Subscription Sources
    @Query("SELECT * FROM subscription_sources")
    fun getAllSources(): Flow<List<SubscriptionSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SubscriptionSource)

    @Delete
    suspend fun deleteSource(source: SubscriptionSource)

    @Query("UPDATE subscription_sources SET isEnabled = :enabled WHERE url = :url")
    suspend fun updateSourceStatus(url: String, enabled: Boolean)

    // Favorite Articles
    @Query("SELECT * FROM favorite_articles ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(article: FavoriteArticle)

    @Delete
    suspend fun deleteFavorite(article: FavoriteArticle)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_articles WHERE link = :link)")
    suspend fun isFavorite(link: String): Boolean

    // Read Articles
    @Query("SELECT link FROM read_articles")
    fun getAllReadLinks(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadArticle(readArticle: ReadArticle)
}
