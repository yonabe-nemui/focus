package app.focus.personal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 の Shapes を上書きするスキーム（Theme.kt で MaterialTheme に渡す）
internal val FocusShapeScheme = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// 画面コンポーネントから直接参照できる定数（MaterialTheme.shapes を使えない場面向け）
object FocusShape {
    val imageCorner        = 8.dp   // フィード内サムネイル
    val imageCornerDetail  = 12.dp  // 詳細画面フル画像
    val avatar             = 999.dp // 円形アバター（CircleShape 相当）
    val chip               = 4.dp   // タグ / バッジ
}
