# Focus リファクタリング計画書

作成日: 2026-07-15
対象: プロジェクト全体(shared / composeApp / androidApp / server、約4,500行)

コードベース全体のレビューで検出した「冗長な点」「わかりづらい点」「イケてない点」、
およびデザイン・UX 面のモダナイズ候補を、実害の大きさと修正リスクに基づいてフェーズ分けした修正計画。

---

## 優先度サマリー

| # | 修正項目 | 分類 | 実害 | リスク | フェーズ |
|---|---------|------|------|--------|---------|
| 1 | ミュートワード設定が Desktop/iOS/Web で no-op | 機能バグ | 高 | 中 | P1 ✅ |
| 2 | アクセストークンをデバッグログに全文出力 | セキュリティ | 高 | 低 | P1 ✅ |
| 3 | 例外メッセージ文字列によるエラー種別判定 | 設計 | 中 | 中 | P2 ✅ |
| 4 | デッドコード削除 | 冗長 | 低 | 低 | P1 ✅ |
| 5 | Repository のセッション永続化コード重複 | 冗長 | 低 | 低 | P2 ✅ |
| 6 | ViewModel のフェッチロジック重複 | 冗長 | 低 | 中 | P2 ✅ |
| 7 | エントリーポイント4箇所の DI 組み立て重複 | 冗長 | 低 | 低 | P2 ✅ |
| 8 | 投稿日時が生文字列のまま表示される | UX | 中 | 低 | P3 |
| 9 | 検索クエリの二重管理による不整合 | UX | 中 | 低 | P3 |
| 10 | shared 層の日本語ハードコード文字列 | 規約違反 | 低 | 低 | P3 |
| 11 | Android サーバー URL のハードコード | 設定 | 中 | 低 | P3 |
| 12 | 「Rss」命名の実態乖離 | 可読性 | 低 | 中 | P4 |
| 13 | サーバー muteWordStore の永続化なし | 設計 | 中 | 中 | P4 |
| 14 | DateUtils の残骸コメント・println・GMT 非対応 | 品質 | 低 | 低 | P1 ✅ |
| 15 | タブ UI のコピペとソース表示情報の重複 | 冗長 | 低 | 低 | P3 |
| 16 | Android edge-to-edge 未対応(Android 15 で実害) | UX | 高 | 中 | P5 |
| 17 | スケルトンローディング化 + Expressive LoadingIndicator | UX | 中 | 低 | P5 |
| 18 | エラー状態(リトライ)・空状態のデザイン | UX | 中 | 低 | P5 |
| 19 | 長押しコンテキストメニュー(ミュート追加・共有) | UX | 中 | 中 | P5 |
| 20 | 画像まわり(crossfade / 2×2グリッド / alt / ビューア) | UX | 中 | 中 | P5 |
| 21 | デスクトップカラムに無限スクロールがない | 機能バグ | 中 | 低 | P5 |
| 22 | タブレット・折りたたみのアダプティブ対応 | UX | 中 | 高 | P5 |
| 23 | テーマ設定 UI(ダーク/OLED/ダイナミックカラー) | UX | 低 | 中 | P5 |
| 24 | フォントバンドル(Inter + Noto Sans JP) | デザイン | 低 | 低 | P5 |
| 25 | リストアニメーション・TopAppBar スクロール連動 | UX | 低 | 低 | P5 |
| 26 | WebViewScreen の情報不足(プログレス・共有等) | UX | 低 | 低 | P5 |
| 27 | デスクトップ操作性(ウィンドウ記憶・ショートカット) | UX | 低 | 低 | P5 |
| 28 | Android predictive back 未対応 | UX | 低 | 中 | P5 |

---

## フェーズ1: 即時対応(実害が大きい or ノーリスク)【対応済み 2026-07-15】

> 1-2 は案A(ローカル DB 実装)で対応。ミュートワードは `MuteWordEntity` に永続化し、
> 全ソースのフィード取得時にフィルタ適用する実装とした(Web はメモリ保持のみ)。

### 1-1. アクセストークンのログ出力を止める 【セキュリティ】

- **場所**: `shared/src/commonMain/kotlin/app/focus/personal/network/BlueskyClient.kt:46`
- **問題**: `createSession` のレスポンスボディ(accessJwt / refreshJwt を含む)を丸ごと `Napier.d` に出力している。
- **修正**: ステータスコードとエラー時のボディのみログに出す。成功時のトークン入りボディはログに含めない。

### 1-2. ミュートワード設定画面の no-op 問題を解消 【機能バグ】

- **場所**:
  - `shared/src/commonMain/kotlin/app/focus/personal/repository/RssRepository.kt:243-245`(スタブ実装)
  - `composeApp/.../ui/MuteWordSettingsScreen.kt`、`SettingsScreen.kt`(無条件で画面表示)
- **問題**: `RssRepository.fetchMuteWords/addMuteWord/deleteMuteWord` は空実装のスタブ。
  Desktop / iOS / Web では設定画面でワードを追加しても保存されず、リロードで消える。
  実装があるのはサーバー経由の Android(`ServerRssRepository`)のみ。
- **修正方針**(いずれかを選択):
  - **案A(推奨)**: `RssRepository` にローカル DB(SQLDelight)ベースのミュートワード CRUD を実装し、
    全ソースのフィード取得時にクライアント側でフィルタ適用する。全プラットフォームで機能が揃う。
  - **案B**: 未対応プラットフォームでは設定メニュー項目自体を非表示にする(機能を絞る暫定対応)。
- **案Aの作業内容**:
  1. `FocusDatabase.sq` に `MuteWordEntity` テーブルと CRUD クエリを追加
  2. `RssRepository` のスタブ3メソッドを DB 実装に差し替え
  3. `fetchAllGoogleTopics` / `fetchAllHatenaEntries` / `fetchBlueskyPage` / `fetchMisskeyPage` の
     返却前にミュートワードフィルタを適用(既存の `matchesMutedWord` を再利用)
  4. DB スキーマ変更のため開発端末ではアプリ再インストールが必要(CLAUDE.md 記載の運用)

### 1-3. デッドコードの削除 【冗長】

呼び出し元ゼロを確認済みのものを削除する:

- `shared/src/commonMain/kotlin/app/focus/personal/Greeting.kt`
- `shared/src/commonMain/kotlin/app/focus/personal/Platform.kt` と全プラットフォームの `Platform.*.kt`(5ファイル)
- `RssRepository.saveFeed` / `getPagedItemsByCategory` / `getItemsByCategory`(`RssRepository.kt:247-316`)
- `FocusDatabase.sq` の `RssChannelEntity` / `RssItemEntity` テーブルと関連クエリ7個
  (insertChannel / lastInsertedId / insertItem / selectAllChannelsByCategory /
  selectItemsByChannelId / selectPagedItemsByCategory / deleteChannelByCategory)
  ※ 1-2 で案Aを採る場合はスキーマ変更をまとめて実施する
- `FeedRepository.fetchBlueskyEntries` とその実装2箇所
- `FocusApiClient.fetchBlueskyFeed` / `fetchMisskeyFeed`
- `BlueskyModels.kt:14` の `BlueskyPreferences`

削除後に全ターゲットのビルドが通ることを確認する。

### 1-4. DateUtils のクリーンアップ 【品質】

- **場所**: `shared/src/commonMain/kotlin/app/focus/personal/util/DateUtils.kt`
- **問題**:
  - `:31` に `// ... rest of code` という生成残骸コメント
  - エラー時に Napier でなく `println` を使用
  - 手書き RFC822 パーサが `"GMT"` 等のタイムゾーン名に非対応
    (Google News の pubDate はたまたま UTC 扱いになり動作している)
- **修正**: 残骸コメント削除、`println` → `Napier.w`、タイムゾーン名(GMT/UT/UTC 最低限)の明示対応。
  既存の `DateUtilsTest` にタイムゾーン名ケースを追加。

---

## フェーズ2: 構造の重複解消【対応済み 2026-07-15】

> 2-1 の UI 文言化は FeedErrorKind(GENERIC / AUTH_CODE_INVALID / RATE_LIMITED)を
> RssUiState.Error に持たせ、BlueskyLoginScreen がリソースから解決する形で実装。
> 2-3 では ViewModel が Misskey エラーを握りつぶす挙動を廃止し、他ソースと同様に
> Error 状態として表示するよう統一した。

### 2-1. エラー種別を型で表現する 【設計】

- **場所**:
  - `BlueskyClient.kt:55,57,59,62`(`throw Exception("AuthFactorRequired")` 等)
  - `RssViewModel.kt:125-137`(`when (e.message)` による分岐)
  - `BlueskyLoginScreen.kt:97`(`uiState.message.contains("429")`)
- **問題**: マジックストリングが ネットワーク層 → ViewModel → UI の3層を貫通している。
- **修正**:
  1. `sealed class BlueskyAuthException : Exception()` を新設
     (`AuthFactorRequired` / `AuthFactorInvalid` / `RateLimited` / `Unauthorized` / `Http(status, body)`)
  2. `BlueskyClient` で HTTP ステータス・レスポンス内容から適切な型を throw
  3. `RssViewModel` は `when (e)` の型マッチに変更
  4. UI 状態にエラー種別(enum)を持たせ、文言化は UI 層でリソースから行う(3-3 と連動)

### 2-2. セッション永続化の重複を SessionStore に抽出 【冗長】

- **場所**: `RssRepository.kt:119-163` と `ServerRssRepository.kt:29-97`
- **問題**: BlueSky セッションと Misskey 設定の save/get/clear/refresh/login が2クラスでほぼ同一実装。
- **修正**:
  1. `SessionStore(database: FocusDatabase?)` クラスを新設し、
     BlueSky セッションと Misskey 設定の永続化 CRUD を移動
  2. 両 Repository は `SessionStore` を保持して委譲
  3. `loginBluesky` / `refreshBlueskySession` の「API 呼び出し + 保存」も共通化候補

### 2-3. ViewModel のフェッチロジック統合 【冗長】

- **場所**: `RssViewModel.kt` の `fetchInitial()`(188-201行)と `loadColumn()`(356-387行)
- **問題**: 「ソースから初回ページを取得しカーソルを保存する」`when (source)` 分岐がほぼ同一。
- **修正**: 共通の `suspend fun fetchFirstPage(source: RssSource): List<RssItem>` に統合し、
  結果の反映先(`_uiState` / `_columnStates`)だけを呼び出し側で分ける。
  未ログイン時の空リスト返却もこの関数に寄せる。

### 2-4. エントリーポイントの DI 組み立てを共通化 【冗長】

- **場所**: `MainActivity.kt` / `MainViewController.kt` / jvm `main.kt` / web `main.kt`
- **問題**: HttpClient + JSON 設定 + クライアント4種 + Repository + ViewModel の組み立てがコピペ。
- **修正**: shared に共通ファクトリを新設:
  ```kotlin
  fun createRssViewModel(
      engine: HttpClientEngineFactory<*>?,  // null なら platform default
      database: FocusDatabase?,
      scope: CoroutineScope,
  ): RssViewModel
  ```
  Android の `ServerRssRepository` 分岐(サーバー URL 指定)もパラメータで吸収する。

---

## フェーズ3: UX・規約まわり

### 3-1. 投稿日時の相対時刻表示 【UX】

- **場所**: `FeedItem.kt:90,155`、`PostDetailScreen.kt`、`ArticleHeader.kt`
- **問題**: `item.pubDate` の生文字列を表示するため、BlueSky/Misskey では
  `2026-07-15T09:00:00.123Z` のような ISO 文字列がそのまま見える。
- **修正**:
  1. `pubDateMillis` から相対時刻を生成する `formatRelativeTime(millis, now): String` を shared に新設
     (`3分前` / `2時間前` / `7月10日` のような段階表示。文言はリソース化)
  2. UI 各所の `item.pubDate` 表示を置き換え
  3. commonTest に境界値テストを追加

### 3-2. 検索クエリの一元管理 【UX】

- **場所**: `RssListScreen.kt:74-75`(`localBlueskyQuery` 等)と `RssViewModel` の `searchQueries`
- **問題**: UI ローカル state と ViewModel 内部状態が独立しており、
  検索後に画面遷移して戻ると入力欄は空なのに結果は絞り込まれたままになる。
- **修正**: `searchQueries` を `StateFlow<Map<RssSource, String>>` として公開し、
  入力欄の初期値・表示値を ViewModel 側に寄せる(single source of truth)。
  `DesktopRssScreen.kt` の `FeedColumn` 内ローカル state も同様に対応。

### 3-3. shared 層のハードコード文字列排除 【規約違反】

- **場所**: `RssViewModel.kt:132`(「認証コードが正しくないか、期限が切れています。」)、
  同 `:135`(`"Login failed: ..."`)など
- **問題**: CLAUDE.md の「文字列ハードコーディング禁止」に違反。shared 層で UI 文言を組んでいる。
- **修正**: 2-1 のエラー型導入とセットで、ViewModel はエラー種別のみ保持し、
  文言は composeApp 層で `strings.xml`(composeResources)から解決する。

### 3-4. Android サーバー URL の設定化 【設定】

- **場所**: `MainActivity.kt:39`(`"http://10.0.2.2:$SERVER_PORT"`)
- **問題**: エミュレータ専用アドレスのハードコード。実機では動作しない。
- **修正**: `BuildConfig` フィールド(debug: `10.0.2.2`、release: 本番 URL)または
  gradle.properties 経由で注入する。

### 3-5. タブ UI とソース表示情報の共通化 【冗長】

- **場所**: `RssListScreen.kt:89-108`(Tab 4つの手書きコピペ)、
  `DesktopRssScreen.kt:73-86`(`sourceDisplayName` / `icon` が jvmMain 内 private)
- **修正**:
  1. `sourceDisplayName` / `RssSource.icon` を commonMain の `ui/components/` へ移動
  2. `RssListScreen` のタブを `RssSource.entries.forEach` ループに置き換え

---

## フェーズ4: 大きめの設計改善(別途判断)

影響範囲が広いため、フェーズ1〜3 完了後に着手可否を判断する。

### 4-1. 「Rss」命名の見直し 【可読性】

- `RssViewModel` → `FeedViewModel`、`RssSource` → `FeedSource`、
  `loadAllTopics()` → `loadCurrentSource()` 等のリネーム。
- `RssItem` は XML パース用モデル(RSS 由来)と UI 表示用モデル(`FeedItem` 的なもの)への分離を検討。
  `authorName != null` による SNS/ニュース判定(`App.kt:62`、`FeedItem.kt:52`)も
  `enum ItemKind { NEWS, SNS_POST }` 等の明示的な型に置き換える。
- 機械的リネームだが全ファイルに波及するため、他の修正がすべて落ち着いてから単独 PR で行う。

### 4-2. サーバー muteWordStore の設計見直し 【設計】

- **場所**: `server/src/main/kotlin/app/focus/personal/Application.kt:40`
- **問題**: `CopyOnWriteArrayList` はプロセスグローバル(全ユーザー共有)かつ再起動で消える。
  クライアント側(1-2 案A)にミュートワードを持たせた場合、サーバー側の存在意義も再検討対象。
- **修正候補**: ファイル or SQLite 永続化、もしくはクライアント側フィルタへの一本化によるサーバー機能の削除。
- あわせて `Application.kt:88` の「accessJwt 空文字列 = 未ログイン」センチネルを nullable に変更する
  (`FocusApiClient.BlueSkyFeedRequest.accessJwt` を `String?` に)。

### 4-3. ミュートワード2系統の整理 【わかりづらさ】

- BlueSky 公式ミュートワード(`BlueskyClient.getMutedWords`、単語境界考慮)と
  自前ミュートワード(サーバー `muteWordStore`、単純 `contains`)が併存し、判定ロジックも異なる。
- 1-2 / 4-2 の結果を踏まえ、フィルタ判定を `matchesMutedWord`(`RssRepository.kt:71`)に一本化し、
  設定画面にどちらのワードを操作しているかを明示する。

---

## フェーズ5: デザイン・UX モダナイズ

UI 全体レビュー(2026-07-15)で検出したモダナイズ候補。CLAUDE.md の方針
(Material 3 Expressive・フラット・読書体験に集中)は維持したまま体感品質を上げる。
機能リファクタ(P1〜P3)とは独立して進められるが、5-1 と 5-6 は実害があるため優先する。

### 5-1. Android edge-to-edge 対応 【実害あり・優先】

- **場所**: `androidApp/src/main/kotlin/app/focus/personal/MainActivity.kt`
- **問題**: `enableEdgeToEdge()` を呼んでおらず、Android 15 では OS 側で edge-to-edge が
  強制されるため、ナビゲーションバー/ステータスバーとコンテンツの重なりが崩れるリスクがある。
- **修正**:
  1. `MainActivity.onCreate` で `enableEdgeToEdge()` を呼ぶ
  2. フィードの `LazyColumn` に navigation bar insets ぶんの `contentPadding` を追加し、
     最後のアイテムがバーに隠れないようにする
  3. IME insets(検索フィールド・ログイン画面)の確認
- **備考**: 着手時は edge-to-edge 対応用スキル(`edge-to-edge`)を使用する

### 5-2. スケルトンローディング + Expressive LoadingIndicator

- **場所**: `RssListScreen.kt:164`、`DesktopRssScreen.kt:266`(全画面 `CircularProgressIndicator`)
- **修正**:
  1. `FeedItem` と同レイアウトのシマー付きプレースホルダ `FeedItemSkeleton` を
     `ui/components/` に新設し、Loading 状態でリスト形状のまま数行表示する
  2. ボタン内などの小型スピナーは Material 3 1.4+ の `LoadingIndicator`(Expressive)に置き換え
  3. `PullToRefreshBox` のインジケータも Expressive 版に更新

### 5-3. エラー状態・空状態のデザイン

- **場所**: `RssListScreen.kt:203-210`、`DesktopRssScreen.kt:279-291`(テキスト1行のみ)
- **問題**: エラー時にリトライ手段がない。検索0件・フィード空のときは空白画面になる。
- **修正**:
  1. `ErrorState(message, onRetry)` コンポーネントを新設(アイコン + 説明 + リトライボタン)
  2. `EmptyState(icon, message)` コンポーネントを新設(検索0件 / フィード空で出し分け)
  3. モバイル・デスクトップ両方の Error/Success(空) 分岐に適用

### 5-4. フィードアイテムのコンテキストメニュー 【コンセプト直結】

- **問題**: コアコンセプトが「見たくないものは見ない」なのに、ミュートワード追加は
  設定画面の奥からテキスト入力するしかなく、フィードから直接ミュートする導線がない。
- **修正**:
  1. `FeedItem` に長押し(デスクトップは右クリック)で `DropdownMenu` を表示
  2. メニュー項目: 「共有」「ブラウザで開く」「この投稿からミュートワードを追加…」
  3. ミュート追加はダイアログで単語を編集して確定 → 即時にフィードへフィルタ反映
- **依存**: ミュートワードの保存先が必要なため P1-2(案A)の完了後に実施

### 5-5. 画像まわりの改善

- **場所**: `FeedItem.kt:174-213`、`PostDetailScreen.kt:90-106`、`BlueskyModels.kt`
- **修正**:
  1. `AsyncImage` に crossfade + プレースホルダ色を設定(画像の唐突な出現を解消)
  2. 複数画像を横スクロールから Twitter/Bluesky 風の 2×2 グリッドに変更
     (縦リスト内の横スクロール競合も解消される)
  3. **BlueSky の alt テキストを活用する**: `BlueskyEmbedImage.alt` は取得済みなのに
     `RssItem` に乗せず `contentDescription = null` で捨てている。
     `RssItem` に `imageAlts: List<String>?` を追加して contentDescription に設定
  4. 詳細画面の `height(280.dp)` 固定 + `FillWidth` をアスペクト比ベースに変更
  5. 詳細画面の画像タップでフルスクリーンビューア(ピンチズーム対応)を表示

### 5-6. デスクトップカラムの無限スクロール 【実害あり・優先】

- **場所**: `DesktopRssScreen.kt:261-292`(`ColumnFeedList`)
- **問題**: `loadMore` を呼んでおらず、BlueSky/Misskey カラムは初回50件で打ち止め。
  モバイル(`RssListScreen.kt:171-181`)には実装済みの末尾検知がデスクトップにない。
- **修正**: モバイルと同じ `derivedStateOf` による末尾検知を `ColumnFeedList` に追加。
  ViewModel 側はカラム毎の `loadMore(source)` が必要(現状はカレントソース前提)なので
  シグネチャを `loadMore(source: RssSource)` に拡張する。

### 5-7. タブレット・折りたたみのアダプティブ対応

- **問題**: マルチカラム表示が jvmMain 専用(`DesktopRssScreen.kt`)のため、
  Android タブレット・折りたたみでは狭いスマホレイアウトのまま。
- **修正**:
  1. `DesktopRssScreen` を commonMain へ移動(`sourceDisplayName`/`icon` の共通化 = P3-5 と連動)
  2. `WindowSizeClass` で Compact = タブ型 / Expanded = マルチカラムを出し分け
  3. 投稿詳細は `material3-adaptive` の `ListDetailPaneScaffold` による2ペイン化を検討
- **備考**: 影響範囲が広いため、P3-5 完了後に単独で実施する

### 5-8. テーマ設定 UI(ダーク / OLED / ダイナミックカラー)

- **現状**: `FocusOledColorScheme` は定義済み(`Color.kt:88`)だが切替 UI がない。
  `Theme.kt` はダイナミックカラー注入に対応済みだが `MainActivity` から渡していない。
- **修正**:
  1. `SettingsScreen` にテーマ選択(システム / ライト / ダーク)と OLED トグルを追加
  2. 設定の永続化(SQLDelight に UserPreference テーブル追加、または multiplatform-settings 導入)
  3. Android 12+ では `dynamicDarkColorScheme(context)` を `FocusTheme` に配線
     (ダイナミックカラー ON/OFF も設定項目にする)

### 5-9. フォントバンドル(Inter + Noto Sans JP)

- **場所**: `Type.kt:8-9`(コメントで準備済みと記載、未実施)
- **修正**: composeResources/font/ に TTF を追加し `FontFamily` 参照へ切り替え。
  システムフォント依存で字面がバラつく Desktop / Web で特に効果が大きい。

### 5-10. リスト・アプリバーのマイクロインタラクション

- **修正**:
  1. `LazyColumn` のアイテムに `Modifier.animateItem()` を追加
     (リフレッシュ時の新着マージ・並べ替えがアニメーションする)
  2. `TopAppBarDefaults.enterAlwaysScrollBehavior` でスクロール時にアプリバーを退避
     (読書領域の最大化 = CLAUDE.md の方針に合致)
  3. リフレッシュで新着があった場合に「新着 N 件」ピル(タップで先頭へ)を表示 ※任意

### 5-11. WebViewScreen の充実

- **場所**: `composeApp/src/commonMain/kotlin/app/focus/personal/ui/WebViewScreen.kt`
- **問題**: タイトルが「Webコンテンツ」固定。読み込みプログレス・共有・「ブラウザで開く」がない。
- **修正**: 読み込みプログレスバー(最低限)、TopAppBar にページタイトル/URL 表示、
  「ブラウザで開く」アクションを追加。

### 5-12. その他の小改善

- **検索フィールド**: `OutlinedTextField` を M3 `SearchBar` 風の丸い検索フィールドへ。
  デバウンス検索(入力停止後に自動検索)の導入も検討
- **TextField の統一**: ログイン画面は filled `TextField`、他は `OutlinedTextField` と
  混在しているためどちらかに統一
- **Misskey インスタンス URL 初期値**: `MisskeySettingsScreen.kt:47` の `"misskey.io"`
  ハードコードを定数化(または placeholder 表示に変更)
- **はてなブックマーク数**: テキスト表示から小型 tonal チップ/バッジへ
- **デスクトップ操作性**: `rememberWindowState` の永続化(ウィンドウサイズ・位置の記憶)、
  キーボードショートカット(j/k: 記事移動、r: リフレッシュ)
- **Android predictive back**: 手書きバックスタックのため予測型戻るジェスチャーに非対応。
  Navigation 3 への移行(`navigation-3` スキルあり)とあわせて検討

---

## 進め方

1. 各フェーズは独立してコミット可能な粒度に分割する(1項目 = 1コミット目安)
2. 各修正後に以下を実行して確認する:
   ```
   .\gradlew.bat :shared:jvmTest
   .\gradlew.bat :server:test
   .\gradlew.bat :androidApp:assembleDebug   (Android ビルド確認)
   .\gradlew.bat :composeApp:run             (Desktop 動作確認)
   ```
3. フェーズ1-2(ミュートワード)の方針(案A/案B)は着手前に決定が必要
4. フェーズ4 はフェーズ1〜3 の完了後に改めて要否を判断する
5. フェーズ5 は機能リファクタ(P1〜P3)と独立して進められるが、以下の順序制約がある:
   - 5-1(edge-to-edge)と 5-6(デスクトップ無限スクロール)は実害があるため P5 内で最優先
   - 5-4(コンテキストメニュー)は P1-2 案A(ミュートワードのローカル保存)の完了が前提
   - 5-7(アダプティブ対応)は P3-5(ソース表示情報の共通化)の完了後に実施
   - UI 変更を含む項目は Desktop(`:composeApp:run`)と Android の両方で見た目を確認する
