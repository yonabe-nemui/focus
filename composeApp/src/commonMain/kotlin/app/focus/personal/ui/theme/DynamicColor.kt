package app.focus.personal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** ダイナミックカラー(Material You)に対応しているか。Android 12+ のみ true。 */
expect val supportsDynamicColor: Boolean

/** プラットフォームのダイナミックカラースキームを返す。非対応プラットフォームは null。 */
@Composable
expect fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?
