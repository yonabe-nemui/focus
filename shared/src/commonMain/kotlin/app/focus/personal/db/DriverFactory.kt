package app.focus.personal.db

import app.cash.sqldelight.db.SqlDriver

/** プラットフォーム別の SQLite ドライバーを生成する。 */
expect class DriverFactory {
    fun createDriver(): SqlDriver
}
