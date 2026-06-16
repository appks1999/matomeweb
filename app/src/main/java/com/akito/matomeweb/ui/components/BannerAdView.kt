package com.akito.matomeweb.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // テスト用バナー広告ID
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-8950375321788767/3055657335"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
