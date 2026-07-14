# Focus

**Focus** は Kotlin Multiplatform 製のニュース集約アプリです。Google News・はてなブックマーク・BlueSky・Misskey のコンテンツを統一フィードとして閲覧できます。

コアコンセプトは「見たくないものは見ない」。ミュートワードによるクライアント側フィルタリングで、目にしたくない話題を除外して読書に集中できます。

対応プラットフォーム: **Android / iOS / Desktop (JVM) / Web (JS)**

## 機能

- Google News(トップ + 8トピック)・はてなブックマーク(人気/新着/IT)の横断フィード
- BlueSky ログイン(2FA 対応)とタイムライン・投稿検索
- Misskey インスタンス接続とホームタイムライン・ノート検索
- ミュートワードによるフィルタリング
  - BlueSky 公式ミュートワード(アカウント設定を取得して適用)
  - ローカルミュートワード(アプリ内で管理し、全ソースに適用)
- Desktop はソース別のマルチカラム表示、モバイルはタブ切り替え + 無限スクロール

## モジュール構成

| モジュール | 内容 |
|---|---|
| `shared/` | ViewModel・Repository・ネットワーククライアント・SQLDelight DB・データモデル |
| `composeApp/` | Compose Multiplatform UI(Android・iOS・Desktop・Web 共通) |
| `androidApp/` | Android エントリーポイント |
| `iosApp/` | iOS エントリーポイント(Xcode プロジェクト) |
| `server/` | Ktor Netty サーバー(ポート 8080。Android クライアントが使用) |

## ビルド・実行

```shell
# Android(APK ビルド)
.\gradlew.bat :androidApp:assembleDebug

# Desktop (JVM)
.\gradlew.bat :composeApp:run

# Web (JS)
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun

# サーバー
.\gradlew.bat :server:run
```

macOS / Linux では `.\gradlew.bat` を `./gradlew` に読み替えてください。
iOS は `iosApp/` を Xcode で開いて実行します。

※ wasmJs ターゲットは SQLDelight が未対応のため現在無効化しています(Web は JS のみ)。

## テスト

```shell
# shared のテスト(commonTest + jvmTest)
.\gradlew.bat :shared:jvmTest

# サーバーのテスト
.\gradlew.bat :server:test
```

## 技術スタック

Compose Multiplatform (Material 3) / Ktor / SQLDelight / kotlinx-serialization / xmlutil / kotlinx-datetime / Napier

## 開発ドキュメント

- [CLAUDE.md](./CLAUDE.md) — アーキテクチャ・実装上の注意・デザインガイドライン
- [REFACTORING_PLAN.md](./REFACTORING_PLAN.md) — リファクタリング・改善計画
