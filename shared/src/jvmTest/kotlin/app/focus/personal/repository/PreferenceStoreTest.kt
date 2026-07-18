package app.focus.personal.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.ThemeMode
import app.focus.personal.model.ThemeSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferenceStoreTest {

    private fun createDatabase(): FocusDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FocusDatabase.Schema.create(driver)
        return FocusDatabase(driver)
    }

    @Test
    fun `put した値を get で取得できる`() {
        val store = PreferenceStore(createDatabase())
        store.put("theme_mode", "DARK")
        assertEquals("DARK", store.get("theme_mode"))
    }

    @Test
    fun `同じキーへの put は上書きされる`() {
        val store = PreferenceStore(createDatabase())
        store.put("theme_mode", "DARK")
        store.put("theme_mode", "LIGHT")
        assertEquals("LIGHT", store.get("theme_mode"))
    }

    @Test
    fun `未設定のキーは null を返す`() {
        val store = PreferenceStore(createDatabase())
        assertNull(store.get("unknown_key"))
    }

    @Test
    fun `database が null でもメモリ保持で動作する`() {
        val store = PreferenceStore(null)
        assertNull(store.get("theme_mode"))
        store.put("theme_mode", "DARK")
        assertEquals("DARK", store.get("theme_mode"))
    }

    @Test
    fun `ThemeSettings の永続化ラウンドトリップ`() {
        val store = PreferenceStore(createDatabase())
        val settings = ThemeSettings(mode = ThemeMode.DARK, oledBlack = true, dynamicColor = false)
        settings.toPreferences().forEach { (key, value) -> store.put(key, value) }
        assertEquals(settings, ThemeSettings.fromPreferences(store::get))
    }

    @Test
    fun `未保存状態からの復元はデフォルト設定になる`() {
        val store = PreferenceStore(createDatabase())
        assertEquals(ThemeSettings(), ThemeSettings.fromPreferences(store::get))
    }
}
