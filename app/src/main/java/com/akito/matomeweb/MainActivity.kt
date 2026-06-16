package com.akito.matomeweb

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akito.matomeweb.data.FavoriteArticle
import com.akito.matomeweb.data.SubscriptionSource
import com.akito.matomeweb.model.MainListItem
import com.akito.matomeweb.ui.components.ArticleCard
import com.akito.matomeweb.ui.components.BannerAdView
import com.akito.matomeweb.ui.components.NativeAdCard
import com.akito.matomeweb.ui.theme.MatomewebTheme
import com.akito.matomeweb.ui.viewmodel.MainUiState
import com.akito.matomeweb.ui.viewmodel.MainViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var mInterstitialAd: InterstitialAd? = null
    private var articleClickCount = 0
    private var isReturningFromArticle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // AdMobの初期化
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MainActivity) {
                loadInterstitialAd()
            }
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            
            MatomewebTheme(darkTheme = isDarkMode) {
                MainScreen(
                    viewModel = viewModel,
                    onArticleClicked = {
                        articleClickCount++
                        isReturningFromArticle = true
                    }
                )
            }
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-8950375321788767/6455846296", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            })
    }

    private fun showInterstitialAd() {
        if (mInterstitialAd != null && articleClickCount >= 5) {
            mInterstitialAd?.show(this)
            articleClickCount = 0
            loadInterstitialAd()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isReturningFromArticle) {
            showInterstitialAd()
            isReturningFromArticle = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onArticleClicked: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val allSources by viewModel.allSources.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val articleListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            0 -> "新着記事"
                            1 -> "お気に入り"
                            else -> "サイト管理"
                        }
                    )
                },
                actions = {
                    if (selectedTab == 0) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "reloadScale")

                        IconButton(
                            onClick = { 
                                viewModel.fetchRss()
                                coroutineScope.launch {
                                    articleListState.animateScrollToItem(0)
                                }
                            },
                            interactionSource = interactionSource,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("新着") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("保存") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("設定") }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ArticleListScreen(uiState, viewModel, articleListState, onArticleClicked)
                    1 -> FavoriteListScreen(favorites, context, viewModel, onArticleClicked)
                    2 -> SettingsScreen(allSources, viewModel)
                }
            }
            BannerAdView()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(uiState: MainUiState, viewModel: MainViewModel, listState: LazyListState, onArticleClicked: () -> Unit) {
    val context = LocalContext.current
    PullToRefreshBox(
        isRefreshing = uiState is MainUiState.Loading,
        onRefresh = { viewModel.fetchRss() },
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is MainUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MainUiState.Success -> {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(state.items) { item ->
                        when (item) {
                            is MainListItem.ArticleItem -> {
                                val article = item.article
                                ArticleCard(
                                    article = article,
                                    isFavorite = article.isFavorite,
                                    onFavoriteClick = { viewModel.toggleFavorite(article) },
                                    onClick = {
                                        onArticleClicked()
                                        viewModel.markAsRead(article)
                                        val intent = CustomTabsIntent.Builder().build()
                                        intent.launchUrl(context, Uri.parse(article.link))
                                    }
                                )
                            }
                            is MainListItem.AdItem -> {
                                NativeAdCard(nativeAd = item.nativeAd)
                            }
                        }
                    }
                    // 追加ボタンと重ならないように末尾に十分な余白を追加
                    item { 
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }
            is MainUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "エラーが発生しました: ${state.message}")
                }
            }
        }
    }
}

@Composable
fun FavoriteListScreen(favorites: List<FavoriteArticle>, context: android.content.Context, viewModel: MainViewModel, onArticleClicked: () -> Unit) {
    if (favorites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("お気に入りはまだありません")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(favorites) { fav ->
                val article = com.akito.matomeweb.model.Article(
                    title = fav.title,
                    link = fav.link,
                    pubDate = fav.pubDate,
                    sourceName = fav.sourceName,
                    imageUrl = fav.imageUrl,
                    isFavorite = true
                )
                ArticleCard(
                    article = article,
                    isFavorite = true,
                    onFavoriteClick = { viewModel.toggleFavorite(article) },
                    onClick = {
                        onArticleClicked()
                        viewModel.markAsRead(article)
                        val intent = CustomTabsIntent.Builder().build()
                        intent.launchUrl(context, Uri.parse(fav.link))
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(sources: List<SubscriptionSource>, viewModel: MainViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    val catalog = viewModel.sourceCatalog
    val subscribedUrls = sources.map { it.url }.toSet()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.List, contentDescription = "Add Source")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(
                    "アプリ設定",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("ダークモード") },
                    supportingContent = { Text("画面の配色を暗くします") },
                    trailingContent = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) }
                        )
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Text(
                    "購読中のサイト",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            if (sources.isEmpty()) {
                item {
                    Text("購読中のサイトはありません", modifier = Modifier.padding(16.dp))
                }
            }
            items(sources) { source ->
                ListItem(
                    headlineContent = { Text(source.name) },
                    supportingContent = { Text(source.url, maxLines = 1) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = source.isEnabled,
                                onCheckedChange = { viewModel.updateSourceStatus(source.url, it) }
                            )
                            IconButton(onClick = { viewModel.deleteSource(source) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Text(
                    "アプリについて / お問い合わせ",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("お問い合わせ先 (メール)") },
                    supportingContent = { Text("appks1999@gmail.com") }, // あなたのメールアドレスに書き換えてください
                    overlineContent = { Text("不具合報告・削除依頼はこちら") }
                )
            }
            item {
                Text(
                    "当アプリは各ニュースサイトのRSSフィードを利用して情報を集約しています。記事の著作権は各配信元に帰属します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(16.dp)
                )
            }
            // 追加ボタンと重ならないように末尾に十分な余白を追加
            item { 
                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }

    if (showAddDialog) {
        var showManualInput by remember { mutableStateOf(false) }
        var manualName by remember { mutableStateOf("") }
        var manualUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (showManualInput) "サイトを手動で追加" else "サイトカタログ") },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (showManualInput) {
                        Column {
                            TextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("サイト名") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = manualUrl,
                                onValueChange = { manualUrl = it },
                                label = { Text("RSS URL (http://...)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        LazyColumn {
                            val availableInCatalog = catalog.filter { !subscribedUrls.contains(it.url) }
                            if (availableInCatalog.isEmpty()) {
                                item { Text("カタログの全サイトを購読済みです", modifier = Modifier.padding(8.dp)) }
                            }
                            items(availableInCatalog) { site ->
                                ListItem(
                                    headlineContent = { Text(site.name) },
                                    modifier = Modifier.clickable {
                                        viewModel.addSource(site.name, site.url)
                                        showAddDialog = false
                                    }
                                )
                            }
                            item {
                                TextButton(
                                    onClick = { showManualInput = true },
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                ) {
                                    Text("リストにないサイトを手動で追加")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (showManualInput) {
                    Button(onClick = {
                        if (manualName.isNotBlank() && manualUrl.isNotBlank()) {
                            viewModel.addSource(manualName, manualUrl)
                            showAddDialog = false
                        }
                    }) {
                        Text("追加")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (showManualInput) showManualInput = false else showAddDialog = false
                }) {
                    Text(if (showManualInput) "キャンセル" else "閉じる")
                }
            }
        )
    }
}
