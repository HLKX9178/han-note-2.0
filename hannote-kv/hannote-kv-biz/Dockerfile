# ============================================================
# hannote-kv Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-kv -f hannote-kv/hannote-kv-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY --parents **/pom.xml ./

COPY hannote-framework/ ./hannote-framework/
COPY hannote-kv/hannote-kv-api/src ./hannote-kv/hannote-kv-api/src
COPY hannote-kv/hannote-kv-biz/src ./hannote-kv/hannote-kv-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-kv/hannote-kv-biz -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-kv/hannote-kv-biz/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
