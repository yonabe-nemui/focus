package app.focus.personal.ui.theme

import androidx.compose.ui.unit.dp

// 4dp グリッドに基づく spacing scale
// 画面コンポーネントから `FocusSpacing.lg` のように参照する
object FocusSpacing {
    val xxs = 2.dp   // アイコンとテキストの最小隙間
    val xs  = 4.dp   // 密なリスト内要素間
    val sm  = 8.dp   // 要素間の基本余白
    val md  = 12.dp  // セクション内の余白
    val lg  = 16.dp  // 画面端マージン
    val xl  = 24.dp  // セクション間
    val xxl = 32.dp  // 大きなブロック間
}
