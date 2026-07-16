package app.focus.personal.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.focus.personal.ui.theme.FocusSpacing

/**
 * ローディング中に FeedItem と同じリスト形状で表示するプレースホルダ。
 * フラットデザイン方針に合わせ、シマーは色のパルスのみで表現する。
 */
@Composable
fun FeedItemSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(FocusSpacing.sm))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color),
                )
                Spacer(Modifier.height(FocusSpacing.sm))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color),
                )
                Spacer(Modifier.height(FocusSpacing.xxs))
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color),
                )
            }
        }
        SectionDivider()
    }
}

/** Loading 状態でフィード全体を占めるスケルトン列。 */
@Composable
fun FeedListSkeleton(modifier: Modifier = Modifier, rows: Int = 8) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(rows) {
            FeedItemSkeleton()
        }
    }
}
