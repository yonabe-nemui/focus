package app.focus.personal.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(FocusDatabase.Schema, "focus.db")
}
