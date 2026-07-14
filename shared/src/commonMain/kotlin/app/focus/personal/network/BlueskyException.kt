package app.focus.personal.network

/**
 * BlueSky API のエラーを表す型。
 * メッセージ文字列の比較ではなく型マッチ（`when (e) { is ... }`）で分岐する。
 */
sealed class BlueskyException(message: String) : Exception(message) {

    /** 2FA 有効アカウントの初回ログイン。認証コードの入力を促してリトライする。 */
    class AuthFactorRequired : BlueskyException("Auth factor token required")

    /** 入力された 2FA 認証コードが不正または期限切れ。 */
    class AuthFactorInvalid : BlueskyException("Auth factor token invalid or expired")

    /** レートリミット (HTTP 429 / RateLimitExceeded)。時間を置いてリトライが必要。 */
    class RateLimited : BlueskyException("Rate limit exceeded")

    /** 認証失敗（2FA 以外の 401）。 */
    class Unauthorized(body: String) : BlueskyException("Unauthorized: $body")

    /** その他の HTTP エラー。 */
    class Http(status: String, body: String) : BlueskyException("HTTP $status: $body")
}
