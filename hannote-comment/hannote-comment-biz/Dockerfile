# ============================================================
# hannote-comment Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-comment -f hannote-comment/hannote-comment-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY --parents **/pom.xml ./

COPY hannote-framework/ ./hannote-framework/
COPY hannote-comment/hannote-comment-api/src ./hannote-comment/hannote-comment-api/src
COPY hannote-comment/hannote-comment-biz/src ./hannote-comment/hannote-comment-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-comment/hannote-comment-biz -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-comment/hannote-comment-biz/target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
