package app.focus.personal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.inter_bold
import focus.composeapp.generated.resources.inter_medium
import focus.composeapp.generated.resources.inter_regular
import focus.composeapp.generated.resources.noto_sans_jp_bold
import focus.composeapp.generated.resources.noto_sans_jp_medium
import focus.composeapp.generated.resources.noto_sans_jp_regular
import org.jetbrains.compose.resources.Font

// Font families: Inter (Latin) + Noto Sans JP (CJK)。3ウェイト構成(Regular / Medium / Bold)。
// SemiBold 指定は最も近い Bold に解決される。
// skiko ターゲット(Desktop / Web / iOS)は FontFamily 内フォールバックで JP グリフが Noto に解決され、
// Android は OS 標準フォールバック(Noto Sans CJK)を利用する。
@Composable
internal fun focusFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_bold, FontWeight.Bold),
    Font(Res.font.noto_sans_jp_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_jp_medium, FontWeight.Medium),
    Font(Res.font.noto_sans_jp_bold, FontWeight.Bold),
)

/** バンドルフォントを適用した Typography。FocusTheme 内で解決する。 */
@Composable
internal fun focusTypography(): Typography {
    val family = focusFontFamily()
    return remember(family) {
        Typography(
            displayLarge   = FocusTypography.displayLarge.copy(fontFamily = family),
            displayMedium  = FocusTypography.displayMedium.copy(fontFamily = family),
            displaySmall   = FocusTypography.displaySmall.copy(fontFamily = family),
            headlineLarge  = FocusTypography.headlineLarge.copy(fontFamily = family),
            headlineMedium = FocusTypography.headlineMedium.copy(fontFamily = family),
            headlineSmall  = FocusTypography.headlineSmall.copy(fontFamily = family),
            titleLarge     = FocusTypography.titleLarge.copy(fontFamily = family),
            titleMedium    = FocusTypography.titleMedium.copy(fontFamily = family),
            titleSmall     = FocusTypography.titleSmall.copy(fontFamily = family),
            bodyLarge      = FocusTypography.bodyLarge.copy(fontFamily = family),
            bodyMedium     = FocusTypography.bodyMedium.copy(fontFamily = family),
            bodySmall      = FocusTypography.bodySmall.copy(fontFamily = family),
            labelLarge     = FocusTypography.labelLarge.copy(fontFamily = family),
            labelMedium    = FocusTypography.labelMedium.copy(fontFamily = family),
            labelSmall     = FocusTypography.labelSmall.copy(fontFamily = family),
        )
    }
}

internal val FocusTypography = Typography(

    // ── Display ──────────────────────────────────────────────────────────────
    // M3 Expressive で強調されるセクション見出し（現状未使用、拡張余地として保持）
    displayLarge = TextStyle(
        fontSize   = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.25).sp,
    ),

    // ── Headline ─────────────────────────────────────────────────────────────
    // 設定画面・ログイン画面のページタイトル
    headlineLarge = TextStyle(
        fontSize      = 32.sp,
        lineHeight    = 40.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),

    // ── Title ─────────────────────────────────────────────────────────────────
    // フィードのニュースタイトル（titleMedium）・SNS 著者名（titleSmall）に使用
    titleLarge = TextStyle(
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),

    // ── Body ──────────────────────────────────────────────────────────────────
    // SNS/ニュースリーダー向けチューニング:
    //   - bodyMedium: SNS 投稿本文（15sp で読みやすく、CJK 対応で letterSpacing 0）
    //   - 行間比率 1.6 で長文スキャンの疲労を低減
    bodyLarge = TextStyle(
        fontSize      = 16.sp,
        lineHeight    = 26.sp,   // 16 × 1.625 ≈ 1.6
        fontWeight    = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontSize      = 15.sp,   // SNS 投稿本文に最適な読みやすいサイズ
        lineHeight    = 24.sp,   // 15 × 1.6 = 24
        fontWeight    = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontSize      = 13.sp,
        lineHeight    = 21.sp,   // 13 × 1.6 = 20.8 ≈ 21
        fontWeight    = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),

    // ── Label ────────────────────────────────────────────────────────────────
    // 日付・メタ情報・タグ・バッジ
    labelLarge = TextStyle(
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontSize      = 11.sp,
        lineHeight    = 16.sp,   // 小さい字でも行間を確保して読みやすく
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    ),
)
