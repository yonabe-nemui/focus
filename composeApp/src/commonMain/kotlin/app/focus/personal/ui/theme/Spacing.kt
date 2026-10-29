package app.focus.personal.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp グリッドに基づく spacing トークン。`FocusSpacing.lg` のように参照する。 */
data class FocusSpacingTokens(
    val xxs: Dp  = 2.dp,   // アイコン/テキスト最小隙間
    val xs: Dp   = 4.dp,   // 密なリスト内要素間
    val sm: Dp   = 8.dp,   // 要素間の基本余白
    val md: Dp   = 12.dp,  // セクション内余白（フィードアイテム垂直余白）
    val lg: Dp   = 16.dp,  // 画面端マージン（標準水平余白）
    val xl: Dp   = 20.dp,  // セクション内の広い余白
    val xxl: Dp  = 24.dp,  // セクション間
    val xxxl: Dp = 32.dp,  // 大きなブロック間（画面ヘッダー等）
)

val FocusSpacing = FocusSpacingTokens()
