package com.akito.matomeweb.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akito.matomeweb.data.AppDatabase
import com.akito.matomeweb.data.FavoriteArticle
import com.akito.matomeweb.data.SubscriptionSource
import com.akito.matomeweb.model.Article
import com.akito.matomeweb.model.MainListItem
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val items: List<MainListItem>) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = RssParser()
    private val dao = AppDatabase.getDatabase(application).appDao()
    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    
    private val _rawArticles = MutableStateFlow<List<Article>>(emptyList())
    private val _nativeAds = MutableStateFlow<List<NativeAd>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    
    private val _isAdFree = MutableStateFlow(false)
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    private fun checkAdFreeStatus() {
        val until = prefs.getLong("ad_free_until", 0L)
        _isAdFree.value = System.currentTimeMillis() < until
    }

    fun setAdFreeFor24Hours() {
        val duration = 2 * 60 * 60 * 1000L // 2時間
        val until = System.currentTimeMillis() + duration
        prefs.edit().putLong("ad_free_until", until).apply()
        _isAdFree.value = true
        
        // 指定時間後に自動でフラグを戻す
        viewModelScope.launch {
            delay(duration)
            _isAdFree.value = false
        }
    }

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    val favorites: Flow<List<FavoriteArticle>> = dao.getAllFavorites()
    val readLinks: Flow<List<String>> = dao.getAllReadLinks()

    val uiState: StateFlow<MainUiState> = combine(
        _rawArticles, 
        favorites, 
        readLinks, 
        _nativeAds, 
        _isLoading, 
        _errorMessage,
        isAdFree // 広告非表示フラグを追加
    ) { flowArray ->
        val articles = flowArray[0] as List<Article>
        val favs = flowArray[1] as List<FavoriteArticle>
        val reads = flowArray[2] as List<String>
        val ads = flowArray[3] as List<NativeAd>
        val loading = flowArray[4] as Boolean
        val error = flowArray[5] as? String
        val adFree = flowArray[6] as Boolean

        if (error != null) {
            MainUiState.Error(error)
        } else if (loading && articles.isEmpty()) {
            MainUiState.Loading
        } else {
            val favLinks = favs.map { it.link }.toSet()
            val readLinksSet = reads.toSet()
            val updatedArticles = articles.map { 
                it.copy(
                    isFavorite = favLinks.contains(it.link),
                    isRead = readLinksSet.contains(it.link)
                ) 
            }
            
            val mixedItems = mutableListOf<MainListItem>()
            updatedArticles.forEachIndexed { index, article ->
                mixedItems.add(MainListItem.ArticleItem(article))
                // 広告非表示モードでない場合のみ、10記事ごとに広告を挿入
                if (!adFree && (index + 1) % 10 == 0 && ads.isNotEmpty()) {
                    val adIndex = ((index + 1) / 10 - 1) % ads.size
                    mixedItems.add(MainListItem.AdItem(ads[adIndex]))
                }
            }

            MainUiState.Success(mixedItems)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState.Loading)

    private val defaultSources = listOf(
        SubscriptionSource("http://hamusoku.com/index.rdf", "ハムスター速報", true),
        SubscriptionSource("http://blog.livedoor.jp/kinisoku/index.rdf", "キニ速", true),
        SubscriptionSource("http://blog.livedoor.jp/news23vip/index.rdf", "VIPPERな俺", true),
        SubscriptionSource("http://blog.livedoor.jp/goldennews/index.rdf", "ゴールデンタイムズ", true),
        SubscriptionSource("http://blog.livedoor.jp/dqnplus/index.rdf", "痛いニュース", true),
        SubscriptionSource("http://blog.livedoor.jp/nwknews/index.rdf", "哲学ニュース", true),
        SubscriptionSource("http://himasoku.com/index.rdf", "暇人速報", true),
        SubscriptionSource("http://alfalfalfa.com/index.rdf", "アルファルファモザイク", true),
        SubscriptionSource("http://majikichi.com/index.rdf", "マジキチ速報", true),
        SubscriptionSource("http://blog.esuteru.com/index.rdf", "はちま起稿", true),
        SubscriptionSource("http://gahalog.2chblog.jp/index.rdf", "ガハろぐNews", true),
        SubscriptionSource("http://blog.livedoor.jp/yakiusoku/index.rdf", "日刊やきう速報", true),
        SubscriptionSource("http://jin115.com/index.rdf", "オレ的ゲーム速報", true),
        SubscriptionSource("http://burusoku-vip.com/index.rdf", "ぶる速-VIP", true),
        SubscriptionSource("http://chaos2ch.com/index.rdf", "カオスちゃんねる", true),
        SubscriptionSource("http://blog.livedoor.jp/jyoushiki43/index.rdf", "常識的に考えた", true),
        SubscriptionSource("http://news4wide.livedoor.biz/index.rdf", "VIPワイドガイド", true),
        SubscriptionSource("http://blog.livedoor.jp/rock1963roll/index.rdf", "なんJ PRIDE", true)
    )

    init {
        checkAdFreeStatus()
        viewModelScope.launch {
            val currentSources = dao.getAllSources().first()
            if (currentSources.isEmpty()) {
                defaultSources.forEach { dao.insertSource(it) }
            } else {
                defaultSources.forEach { default ->
                    currentSources.find { it.url == default.url }?.let { existing ->
                        if (existing.name != default.name) {
                            dao.insertSource(default)
                        }
                    }
                }
            }
            loadNativeAds()
            fetchRss()
        }
    }

    private fun loadNativeAds() {
        val adLoader = AdLoader.Builder(getApplication(), "ca-app-pub-8950375321788767/1578924138")
            .forNativeAd { nativeAd ->
                _nativeAds.value = _nativeAds.value + nativeAd
            }
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), 3)
    }

    private fun parseDateToLong(dateString: String?): Long {
        if (dateString == null) return 0L
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                if (!dateString.contains("+") && !dateString.contains("GMT") && !dateString.contains("JST")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(dateString)
                if (date != null) return date.time
            } catch (e: Exception) {
                continue
            }
        }
        return 0L
    }

    private fun formatDisplayDate(dateString: String?): String? {
        if (dateString == null) return null
        val time = parseDateToLong(dateString)
        if (time == 0L) return dateString
        
        return try {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            sdf.format(Date(time))
        } catch (e: Exception) {
            dateString
        }
    }

    private fun extractImageUrl(item: com.prof18.rssparser.model.RssItem): String? {
        var rawHtml = (item.description ?: "") + (item.content ?: "")
        if (rawHtml.isBlank()) return item.image

        // エスケープ解除（重要）
        if (rawHtml.contains("&lt;")) {
            rawHtml = rawHtml.replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&amp;", "&")
        }

        val doc = org.jsoup.Jsoup.parse(rawHtml)
        val imgs = doc.select("img")
        val preferred = listOf("blogimg.jp", "archives", "imgs", "image", "upload", "fc2.com")
        val ignore = listOf("pixel", "counter", "tracking", "ads.", "favicon", "clap", "emoji")

        return imgs.mapIndexed { index, img ->
            val url = img.attr("src").ifBlank { img.attr("data-src") }
            var score = 0
            if (preferred.any { url.contains(it) }) score += 300
            if (ignore.any { url.contains(it) }) score -= 1000
            score += (10 - index) * 20
            url to score
        }.filter { it.second > 0 }.maxByOrNull { it.second }?.first ?: item.image
    }

    private suspend fun fetchOgpImage(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = org.jsoup.Jsoup.connect(url).timeout(3000).get()
            doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
    }

    fun fetchRss() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val sources = dao.getAllSources().first().filter { it.isEnabled }
                
                val allArticles = coroutineScope {
                    sources.map { source ->
                        async {
                            try {
                                val channel = parser.getRssChannel(source.url)
                                channel.items.map { item ->
                                    val link = item.link ?: ""
                                    var imageUrl = extractImageUrl(item)
                                    if (imageUrl == null && link.isNotBlank()) {
                                        imageUrl = fetchOgpImage(link)
                                    }
                                    Article(
                                        title = item.title ?: "No Title",
                                        link = link,
                                        pubDate = formatDisplayDate(item.pubDate),
                                        sourceName = source.name,
                                        imageUrl = imageUrl
                                    )
                                }
                            } catch (e: Exception) {
                                emptyList<Article>()
                            }
                        }
                    }.awaitAll().flatten()
                }

                _rawArticles.value = allArticles.sortedByDescending { 
                    parseDateToLong(it.pubDate) 
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown Error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(article: Article) {
        viewModelScope.launch {
            val isFav = dao.isFavorite(article.link)
            if (isFav) {
                dao.deleteFavorite(FavoriteArticle(article.link, "", null, "", null, 0L))
            } else {
                dao.insertFavorite(FavoriteArticle(
                    link = article.link,
                    title = article.title,
                    pubDate = article.pubDate,
                    sourceName = article.sourceName,
                    imageUrl = article.imageUrl,
                    savedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    fun markAsRead(article: Article) {
        viewModelScope.launch {
            dao.insertReadArticle(com.akito.matomeweb.data.ReadArticle(article.link))
        }
    }

    val allSources = dao.getAllSources()

    val sourceCatalog = listOf(
        SubscriptionSource("http://hamusoku.com/index.rdf", "ハムスター速報", true),
        SubscriptionSource("http://blog.livedoor.jp/kinisoku/index.rdf", "キニ速", true),
        SubscriptionSource("http://blog.livedoor.jp/news23vip/index.rdf", "VIPPERな俺", true),
        SubscriptionSource("http://blog.livedoor.jp/goldennews/index.rdf", "ゴールデンタイムズ", true),
        SubscriptionSource("http://blog.livedoor.jp/dqnplus/index.rdf", "痛いニュース", true),
        SubscriptionSource("http://blog.livedoor.jp/nwknews/index.rdf", "哲学ニュース", true),
        SubscriptionSource("http://himasoku.com/index.rdf", "暇人速報", true),
        SubscriptionSource("http://alfalfalfa.com/index.rdf", "アルファルファモザイク", true),
        SubscriptionSource("http://majikichi.com/index.rdf", "マジキチ速報", true),
        SubscriptionSource("http://blog.esuteru.com/index.rdf", "はちま起稿", true),
        SubscriptionSource("http://gahalog.2chblog.jp/index.rdf", "ガハろぐNews", true),
        SubscriptionSource("http://blog.livedoor.jp/yakiusoku/index.rdf", "日刊やきう速報", true),
        SubscriptionSource("http://jin115.com/index.rdf", "オレ的ゲーム速報", true),
        SubscriptionSource("http://burusoku-vip.com/index.rdf", "ぶる速-VIP", true),
        SubscriptionSource("http://chaos2ch.com/index.rdf", "カオスちゃんねる", true),
        SubscriptionSource("http://blog.livedoor.jp/jyoushiki43/index.rdf", "常識的に考えた", true),
        SubscriptionSource("http://news4wide.livedoor.biz/index.rdf", "VIPワイドガイド", true),
        SubscriptionSource("http://blog.livedoor.jp/rock1963roll/index.rdf", "なんJ PRIDE", true),
        // 以下はカタログ専用（初回は購読されない）
        SubscriptionSource("http://world-fusigi.net/index.rdf", "不思議.net", true),
        SubscriptionSource("http://blog.livedoor.jp/domesoccer/index.rdf", "ドメサカ板", true),
        SubscriptionSource("http://nekomemo.com/index.rdf", "ねこメモ", true),
        SubscriptionSource("http://fnf.ldblog.jp/index.rdf", "いてつくブログ", true),
        SubscriptionSource("http://kijyosoku.com/index.rdf", "鬼女まとめ速報", true),
        SubscriptionSource("http://www.negisoku.com/index.rdf", "ネギ速", true),
        SubscriptionSource("http://brow2ing.doorblog.jp/index.rdf", "ブラブラブラウジング", true),
        SubscriptionSource("http://yaraon.blog109.fc2.com/?xml", "やらおん！", true),
        SubscriptionSource("http://seikatuch.com/index.rdf", "生活ちゃんねる", true)

    )

    fun updateSourceStatus(url: String, enabled: Boolean) {
        viewModelScope.launch {
            dao.updateSourceStatus(url, enabled)
            fetchRss()
        }
    }

    fun addSource(name: String, url: String) {
        viewModelScope.launch {
            dao.insertSource(SubscriptionSource(url, name, true))
            fetchRss()
        }
    }

    fun deleteSource(source: SubscriptionSource) {
        viewModelScope.launch {
            dao.deleteSource(source)
            fetchRss()
        }
    }
}
