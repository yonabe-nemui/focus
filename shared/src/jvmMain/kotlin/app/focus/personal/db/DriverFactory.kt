package app.focus.personal.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Desktop 用。DB は ~/.focus/focus.db に保存する。
 * 初回はスキーマを作成し、以降は PRAGMA user_version でスキーマのバージョンを管理する。
 * 開発中にスキーマを変えた場合はこのファイルを削除してリセットする。
 */
actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = File(System.getProperty("user.home"), ".focus/focus.db")
        dbFile.parentFile.mkdirs()
        return JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", schema = FocusDatabase.Schema)
    }
}
