# Cloud Run 用イメージ。
# shared モジュールが KMP (Android ターゲット含む) のためコンテナ内でのフルビルドは行わず、
# ローカルで `.\gradlew.bat :server:buildFatJar` した成果物を載せるだけの構成にしている。
# 手順の詳細は DEPLOY.md を参照。
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY build/libs/server-all.jar app.jar

# Cloud Run はコンテナに PORT 環境変数を渡す (Application.kt で参照)
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
