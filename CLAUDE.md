# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

このファイルは、リポジトリ内のコードを扱う際に Claude Code (claude.ai/code) へ提供するガイダンスです。

## プロジェクト概要

**Focus** は Kotlin Multiplatform 製のニュース集約アプリ。Google News・はてなブックマーク・BlueSky からコンテンツを取得する。コアコンセプトは「見たくないものは見ない」—— BlueSky のミュートワードを使ってクライアント側でコンテンツをフィルタリングする（BlueSky API はサーバー側でミュートワードを適用しないため）。

## ビルド・実行コマンド

```bash
# Android
.\gradlew.bat :composeApp:assembleDebug

# Desktop (JVM)
.\gradlew.bat :composeApp:run

# サーバー
.\gradlew.bat :server:run

# Web (Wasm - 高速)
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun

# Web (JS - 旧ブラウザ対応)
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun
```

iOS: `iosApp/` を Xcode で開く。

## テスト実行

```bash
# shared モジュールのマルチプラットフォームテスト
.\gradlew.bat :shared:commonTest

# サーバーの JVM テスト
.\gradlew.bat :server:test

# Compose UI テスト
.\gradlew.bat :composeApp:commonTest
```

テストは `shared/src/commonTest/`（DateUtils の日付パーステスト等）と `server/src/test/`（Ktor エンドポイントテスト）に存在する。

## アーキテクチャ

**モジュール構成:**
- `shared/` — MVVM ビジネスロジック: ViewModel・Repository・ネットワーククライアント・SQLDelight DB・データモデル
- `composeApp/` — Android・iOS・Desktop・Web で共有する Compose Multiplatform UI
- `androidApp/` — Android エントリーポイント（DI セットアップ・OkHttp HttpClient 初期化）
- `server/` — Ktor Netty サーバー（ポート 8080）

**データフロー:** `UI (composeApp)` → `RssViewModel (shared)` → `RssRepository (shared)` → ネットワーククライアント + SQLDelight DB

**ネットワーククライアント** (`shared/src/commonMain/kotlin/.../network/`):
- `GoogleRssClient` — Google News RSS (XML)
- `HatenaRssClient` — はてなブックマーク RDF/RSS（ブックマーク数含む）
- `BlueskyClient` — BlueSky XRPC API（セッション・投稿検索・ミュートワード設定）

**プラットフォーム別 HTTP エンジン**（各エントリーポイントで注入）:
- Android/Desktop/JVM: OkHttp
- iOS: Darwin
- Web: デフォルト JS エンジン

**DI はフレームワーク不使用** — `MainActivity.kt`（Android）・`MainViewController.kt`（iOS）で手動インスタンス化。`DriverFactory` は `expect/actual` でプラットフォーム別 DB ドライバーを返す。

**全ソースの正規化:** 各クライアントは独自モデル（`BlueskyPost`, `HatenaRssItem` 等）を `RssItem` に変換して統一フィードとして扱う。

## 実装上の注意事項

- **スターインポート禁止** — `import package.*` は使わない
- **文字列のハードコーディング禁止** — 表示文字列は `strings.xml` 等のリソースファイルに定義する
- **SQLDelight の Boolean** — ネイティブ Boolean ではなく `INTEGER`（Long）で保存（`0L`/`1L`）
- **開発中の DB マイグレーション** — スキーマ変更時はアプリをアンインストールして DB をリセット
- **Ktor ContentNegotiation** — 全プラットフォームで JSON 設定を適用する（シリアライズに必須）
- **マルチモジュール依存** — 依存をモジュール間で公開したい場合は `api` を使う、または各モジュールで `implementation` を重複宣言する
- **BlueSky 2FA フロー** — 2FA 有効アカウントの初回ログインは HTTP 401 (`AuthFactorTokenRequired`) が返る。トークン入力を促してリトライする

## 技術スタック

| レイヤー | ライブラリ |
|---|---|
| UI | Compose Multiplatform (Material 3) |
| ネットワーク | Ktor HTTP Client |
| データベース | SQLDelight |
| JSON | kotlinx-serialization |
| XML/RDF | xmlutil |
| 日時 | kotlinx-datetime |
| ログ | Napier |
| 状態管理 | Kotlin Coroutines / StateFlow |