# ============================================================
# hannote-oss Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-oss -f hannote-oss/hannote-oss-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY --parents **/pom.xml ./

COPY hannote-framework/ ./hannote-framework/
COPY hannote-oss/hannote-oss-api/src ./hannote-oss/hannote-oss-api/src
COPY hannote-oss/hannote-oss-biz/src ./hannote-oss/hannote-oss-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-oss/hannote-oss-biz -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-oss/hannote-oss-biz/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
