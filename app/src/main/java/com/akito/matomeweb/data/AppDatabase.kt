package com.akito.matomeweb.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SubscriptionSource::class, FavoriteArticle::class, ReadArticle::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "matomeweb_database"
                )
                .fallbackToDestructiveMigration() // 構造が変わった時に自動でリセットする
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
