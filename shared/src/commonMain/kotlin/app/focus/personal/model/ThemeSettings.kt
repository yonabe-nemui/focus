package app.focus.personal.model

/** テーマモード。SYSTEM は OS のダークモード設定に追従する。 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * ユーザーのテーマ設定。PreferenceStore にキーバリューで永続化される。
 * 文言化・配色の解決は UI 層(composeApp)が行う。
 */
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val oledBlack: Boolean = false,
    val dynamicColor: Boolean = false,
) {
    companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_OLED_BLACK = "oled_black"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"

        private const val VALUE_TRUE = "true"

        fun fromPreferences(get: (String) -> String?): ThemeSettings = ThemeSettings(
            mode = get(KEY_THEME_MODE)?.let { value ->
                ThemeMode.entries.firstOrNull { it.name == value }
            } ?: ThemeMode.SYSTEM,
            oledBlack = get(KEY_OLED_BLACK) == VALUE_TRUE,
            dynamicColor = get(KEY_DYNAMIC_COLOR) == VALUE_TRUE,
        )
    }

    /** 永続化用のキーバリュー表現。 */
    fun toPreferences(): Map<String, String> = mapOf(
        KEY_THEME_MODE to mode.name,
        KEY_OLED_BLACK to oledBlack.toString(),
        KEY_DYNAMIC_COLOR to dynamicColor.toString(),
    )
}
