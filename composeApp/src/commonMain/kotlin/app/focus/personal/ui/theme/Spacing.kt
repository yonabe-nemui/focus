package app.focus.personal.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4dp グリッドに基づく spacing トークン。
 * 通常は `FocusSpacing.lg` のように直接参照するが、
 * CompositionLocal オーバーライドが必要な場合は `LocalFocusSpacing.current` を使う。
 */
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

/** デフォルト spacing インスタンス。コンポーザブル外からも参照可能。 */
val FocusSpacing = FocusSpacingTokens()

/**
 * FocusTheme 内で提供される spacing CompositionLocal。
 * 画面やコンポーネントから `LocalFocusSpacing.current.lg` のように参照する。
 * staticCompositionLocalOf を使用するため値の変更は全ツリーの再コンポーズをトリガーする。
 */
val LocalFocusSpacing = staticCompositionLocalOf { FocusSpacingTokens() }
