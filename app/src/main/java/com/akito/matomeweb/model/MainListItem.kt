package com.akito.matomeweb.model

import com.google.android.gms.ads.nativead.NativeAd

sealed class MainListItem {
    data class ArticleItem(val article: Article) : MainListItem()
    data class AdItem(val nativeAd: NativeAd) : MainListItem()
}
