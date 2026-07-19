package app.focus.personal

/**
 * 接続先サーバーの解決結果。baseUrl が null なら各ソースへ直接アクセスする。
 * 実体は resolveServerConfig()(src/debug と src/release で実装が異なる)が返す。
 */
data class ServerConfig(val baseUrl: String?, val apiKey: String?)
