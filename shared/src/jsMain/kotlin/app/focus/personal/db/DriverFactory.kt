package app.focus.personal.db

import app.cash.sqldelight.db.SqlDriver

// Web は DB を使わない(永続化なしでメモリ保持)。expect を満たすためだけの実装。
actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        throw UnsupportedOperationException("Web では DB を使用しない")
}
