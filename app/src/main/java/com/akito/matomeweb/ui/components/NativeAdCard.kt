package com.akito.matomeweb.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.akito.matomeweb.R
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdCard(nativeAd: NativeAd, modifier: Modifier = Modifier) {
    // 従来のXMLレイアウトをインフレートする方法に切り替えます。
    // これにより、AdMob SDKが期待する正確なView階層が保証され、バリデータを確実にパスできます。
    AndroidView(
        factory = { context ->
            val inflater = LayoutInflater.from(context)
            val adView = inflater.inflate(R.layout.native_ad_layout, null) as NativeAdView
            
            // XML内の各ViewをNativeAdViewのプロパティに登録
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.mediaView = adView.findViewById(R.id.ad_media)
            adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            
            adView
        },
        update = { adView ->
            // データの流し込み
            (adView.headlineView as? TextView)?.text = nativeAd.headline
            (adView.bodyView as? TextView)?.text = nativeAd.body
            (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
            (adView.callToActionView as? Button)?.text = nativeAd.callToAction
            (adView.callToActionView as? Button)?.visibility = 
                if (nativeAd.callToAction == null) android.view.View.GONE else android.view.View.VISIBLE

            // MediaViewの設定（画像も自動的にハンドルされます）
            adView.mediaView?.setMediaContent(nativeAd.mediaContent)
            
            // 重要：最後にこれを呼び出す
            adView.setNativeAd(nativeAd)
        },
        modifier = modifier.fillMaxWidth()
    )
}
