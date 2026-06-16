package com.akito.matomeweb.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.akito.matomeweb.model.Article
import com.akito.matomeweb.ui.theme.MatomewebTheme

@Composable
fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 押した時に少し小さくする（0.98倍）アニメーション
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = if (isPressed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
            // 画像がある場合のみ表示領域を確保する
            if (article.imageUrl != null) {
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = rememberVectorPainter(Icons.Default.BrokenImage)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 右側にテキストコンテンツ（画像がない場合はここが横いっぱいに広がる）
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 上段：日付とサイト名
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    article.pubDate?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = article.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE91E63), // まとめサイトっぽいピンク系（お好みで調整可能）
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 中段：タイトル
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    fontSize = 15.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (article.isRead) 0.5f else 1f
                    }
                )
            }

            // 右端にお気に入りボタン
            onFavoriteClick?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(20.dp),
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }
        }
        // 下線の区切り
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
}

@Preview(showBackground = true)
@Composable
fun ArticleCardPreview() {
    MatomewebTheme {
        ArticleCard(
            article = Article(
                title = "Composeでまとめアンテナアプリを作る方法を解説！",
                link = "https://example.com",
                pubDate = "2024/05/19 12:00",
                sourceName = "あきとブログ",
                imageUrl = "https://example.com/image.jpg"
            ),
            onClick = {}
        )
    }
}
