# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

このファイルは、リポジトリ内のコードを扱う際に Claude Code (claude.ai/code) へ提供するガイダンスです。

## プロジェクト概要

**Focus** は Kotlin Multiplatform 製のニュース集約アプリ。Google News・はてなブックマーク・BlueSky・Misskey からコンテンツを取得する。コアコンセプトは「見たくないものは見ない」——2系統のミュートワードでクライアント側フィルタリングを行う:

- **BlueSky 公式ミュートワード**: アカウントに設定済みのミュートワードを取得し、BlueSky フィードに適用（BlueSky API はサーバー側でミュートワードを適用しないため）
- **ローカルミュートワード**: アプリ内設定画面で管理し、全ソースのフィードに適用。ローカル DB（`MuteWordEntity`）に永続化（DB のない Web はメモリ保持でセッション内のみ有効）

リファクタリング・改善の計画は `REFACTORING_PLAN.md` を参照。

## ビルド・実行コマンド

```bash
# Android（Android アプリモジュールは androidApp。composeApp ではない）
.\gradlew.bat :androidApp:assembleDebug

# Desktop (JVM)
.\gradlew.bat :composeApp:run

# サーバー
.\gradlew.bat :server:run

# Web (JS)
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun
```

iOS: `iosApp/` を Xcode で開く。

**注意**: wasmJs ターゲットは SQLDelight が未対応のため無効化されている（`shared/build.gradle.kts`・`composeApp/build.gradle.kts` でコメントアウト中）。Web は JS ターゲットのみ。

## テスト実行

```bash
# shared のテスト（commonTest + jvmTest を JVM で実行。最速でカバレッジも広い）
.\gradlew.bat :shared:jvmTest

# shared の全ターゲットテスト
.\gradlew.bat :shared:allTests

# サーバーの JVM テスト
.\gradlew.bat :server:test
```

テストの場所:
- `shared/src/commonTest/` — DateUtils の日付パーステスト等
- `shared/src/jvmTest/` — RssRepository のテスト（インメモリ SQLite + MockEngine）
- `server/src/test/` — Ktor エンドポイントテスト

## アーキテクチャ

**モジュール構成:**
- `shared/` — MVVM ビジネスロジック: ViewModel・Repository・ネットワーククライアント・SQLDelight DB・データモデル
- `composeApp/` — Android・iOS・Desktop・Web で共有する Compose Multiplatform UI
- `androidApp/` — Android エントリーポイント（DI セットアップ・OkHttp HttpClient 初期化）
- `server/` — Ktor Netty サーバー（ポート 8080）

**データフロー:** `UI (composeApp)` → `RssViewModel (shared)` → `FeedRepository (shared, interface)` → ネットワーククライアント + SQLDelight DB

`FeedRepository` の実装は2つあり、プラットフォームで使い分ける:
- `RssRepository` — 各ソースへ直接アクセス（iOS・Desktop・Web で使用）
- `ServerRssRepository` — 自前 Ktor サーバー経由でアクセス（Android で使用。`FocusApiClient` でサーバーの `/api/feed/*` を叩く）

**ネットワーククライアント** (`shared/src/commonMain/kotlin/.../network/`):
- `GoogleRssClient` — Google News RSS (XML)
- `HatenaRssClient` — はてなブックマーク RDF/RSS（ブックマーク数含む）
- `BlueskyClient` — BlueSky XRPC API（セッション・投稿検索・ミュートワード設定）
- `MisskeyClient` — Misskey API（ノート検索・ホームタイムライン）
- `FocusApiClient` — 自前 Ktor サーバーの REST API クライアント（ServerRssRepository が使用）

**プラットフォーム別 HTTP エンジン**（各エントリーポイントで注入）:
- Android/Desktop/JVM: OkHttp
- iOS: Darwin
- Web: デフォルト JS エンジン

**DI はフレームワーク不使用** — 各エントリーポイントで手動インスタンス化する: `MainActivity.kt`（Android）・`MainViewController.kt`（iOS）・`composeApp/src/jvmMain/.../main.kt`（Desktop）・`composeApp/src/webMain/.../main.kt`（Web。DB なしで `RssRepository(null, ...)` を構築）。`DriverFactory` は `expect/actual` でプラットフォーム別 DB ドライバーを返す。

**全ソースの正規化:** 各クライアントは独自モデル（`BlueskyPost`, `HatenaItem`, `MisskeyNote` 等）を `toRssItem()` で `RssItem` に変換して統一フィードとして扱う。

## 実装上の注意事項

- **スターインポート禁止** — `import package.*` は使わない
- **文字列のハードコーディング禁止** — 表示文字列は `strings.xml` 等のリソースファイルに定義する
- **SQLDelight の Boolean** — ネイティブ Boolean ではなく `INTEGER`（Long）で保存（`0L`/`1L`）
- **開発中の DB マイグレーション** — スキーマ変更時はアプリをアンインストールして DB をリセット。本番運用時は `.sqm` によるマイグレーションが必要
- **Ktor ContentNegotiation** — 全プラットフォームで JSON 設定を適用する（シリアライズに必須）
- **はてな User-Agent** — はてなブックマーク RSS 取得時は、将来的に適切な User-Agent の設定を推奨
- **マルチモジュール依存** — 依存をモジュール間で公開したい場合は `api` を使う、または各モジュールで `implementation` を重複宣言する
- **BlueSky 2FA フロー** — 2FA 有効アカウントの初回ログインは HTTP 401 (`AuthFactorTokenRequired`) が返る。トークン入力を促してリトライする

## デザインガイドライン

### デザインシステム
- Material 3（Material 3 Expressive ガイドラインに準拠）
- ターゲット: 読書体験に集中できる SNS / ニュースリーダー

### 情報密度
- Twitter / X 系の高密度レイアウト
- カードではなく、薄い HorizontalDivider（alpha 低め）でアイテムを区切る
- エレベーションは使わず、完全フラットなデザイン
- 不要な余白は最小限に

### タイポグラフィ
- 本文: 15〜16sp、行間 1.5〜1.6
- タイトルと本文で明確な階層を作る（サイズ + ウェイトの差）
- 役割: bodyLarge（本文）、titleMedium（タイトル）、labelSmall（メタ情報）

### カラー & ダークモード
- ダークモードのベース: #121212〜#1A1A1A（純黒は使わない）
- OLED モード（#000）はユーザー設定でオプション提供
- コントラスト比は最低でも WCAG AA を満たす

### コンポーネント設計
- デザイントークンは `ui/theme/` に集約（Color, Type, Spacing, Shape）
- 共通コンポーネント: FeedItem, ArticleHeader, SectionDivider など
- 画面はトークンと共通コンポーネントを組み合わせて構成

### 参考実装
- Jetpack Compose Samples: Jetnews
- Tivi（Chris Banes）

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