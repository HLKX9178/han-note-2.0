# ============================================================
# hannote-note Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-note -f hannote-note/hannote-note-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY --parents **/pom.xml ./

COPY hannote-framework/ ./hannote-framework/
COPY hannote-note/hannote-note-api/src ./hannote-note/hannote-note-api/src
COPY hannote-note/hannote-note-biz/src ./hannote-note/hannote-note-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-note/hannote-note-biz -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-note/hannote-note-biz/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
