# サーバーのデプロイ (Google Cloud Run)

## 構成

- リージョン: `asia-northeast1` (東京)
- サービス名: `focus-server`
- 認証: `/api` 配下は `X-Api-Key` ヘッダーによる簡易認証 (サーバー側は環境変数 `FOCUS_API_KEY`)。`/health` は無認証。
- ビルド方式: ローカルで fat JAR を作成し、`gcloud run deploy --source` で Dockerfile ごと Cloud Build に渡す。
  (shared モジュールが Android ターゲットを含む KMP のため、コンテナ内フルビルドは Android SDK が必要になり非現実的)

## API キーの取り扱い【重要】

- キーは **Secret Manager に保存**し、Cloud Run へは `--set-secrets` で渡す。
  `--set-env-vars` での直接指定はサービス定義(コンソールや `gcloud run services describe`)に
  平文で残るため使わない。
- Android 側のキーは **ユーザーホームの `~/.gradle/gradle.properties`** に置く。
  リポジトリ側の `gradle.properties` には書かない(コミット厳禁)。
- キーは BuildConfig 経由で APK に埋め込まれるため、**APK を不特定多数に配布しない**こと
  (個人利用前提の防御。公開配布するならこの方式は不可)。
- キーの生成はシェル履歴に残らない方法で行う(下記手順はファイル経由で投入)。

## 初回セットアップ

```powershell
# gcloud CLI 未導入なら https://cloud.google.com/sdk/docs/install から
gcloud auth login
gcloud projects create <PROJECT_ID>   # 既存プロジェクトを使う場合は不要
gcloud config set project <PROJECT_ID>
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com

# API キーを生成して Secret Manager に登録 (履歴・画面にキーを出さない)
$bytes = [byte[]]::new(32); [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes) | Set-Content -NoNewline "$env:TEMP\focus-api-key.txt"
gcloud secrets create focus-api-key --data-file="$env:TEMP\focus-api-key.txt"

# Cloud Run のサービスアカウントにシークレットの読み取り権限を付与
$projectNumber = gcloud projects describe <PROJECT_ID> --format="value(projectNumber)"
gcloud secrets add-iam-policy-binding focus-api-key `
  --member="serviceAccount:$projectNumber-compute@developer.gserviceaccount.com" `
  --role="roles/secretmanager.secretAccessor"

# キーの値は ~/.gradle/gradle.properties へ転記してから一時ファイルを削除
Remove-Item "$env:TEMP\focus-api-key.txt"
```

## デプロイ

```powershell
# 1. fat JAR をビルド (server/build/libs/server-all.jar が生成される)
.\gradlew.bat :server:buildFatJar

# 2. Cloud Run へデプロイ (リポジトリルートから実行)
gcloud run deploy focus-server `
  --source server `
  --region asia-northeast1 `
  --allow-unauthenticated `
  --memory 512Mi `
  --set-secrets "FOCUS_API_KEY=focus-api-key:latest"
```

デプロイ完了時に表示される `https://focus-server-xxxxx.a.run.app` がサーバー URL。

確認:

```powershell
curl https://focus-server-xxxxx.a.run.app/health                             # 200 ok
curl https://focus-server-xxxxx.a.run.app/api/feed/google                    # 401 (キーなし)
curl -H "X-Api-Key: <キー>" https://focus-server-xxxxx.a.run.app/api/feed/google  # 200
```

## Android アプリ側の設定

**ユーザーホーム**の `~/.gradle/gradle.properties` に(リポジトリ側には書かない):

```properties
focus.android.serverUrl.release=https://focus-server-xxxxx.a.run.app
focus.android.serverApiKey=<Secret Manager に登録したキーと同じ値>
```

- debug ビルドはこれまで通り `focus.android.serverUrl` (デフォルト `http://10.0.2.2:8080`)。
  ローカルサーバーは `FOCUS_API_KEY` 未設定で起動すれば認証なしで動く。
- **debug ビルドの接続先切り替え**: 画面左下の「DEBUG」チップをタップするとダイアログで
  「ローカルサーバー」(`focus.android.serverUrl`) と「Cloud Run」(`focus.android.serverUrl.release` +
  API キー) を切り替えられる。選択は SharedPreferences に保持され、切り替え時はアプリが再生成される。
  実装は `androidApp/src/debug/`(release は `src/release/` の no-op で、メニューは一切含まれない)。

## 運用メモ

- リクエストがない間はインスタンス 0 にスケールし課金なし。個人利用なら無料枠内に収まる想定。
- コールドスタート (JVM 起動で数秒) が気になる場合: `--min-instances 1` (常時課金が発生)。
- キーのローテーション: 新しい値を `gcloud secrets versions add focus-api-key --data-file=...` で追加 →
  再デプロイ(`:latest` 参照のため)→ `~/.gradle/gradle.properties` を更新してアプリを再ビルド。
- ログ確認: `gcloud run services logs read focus-server --region asia-northeast1`