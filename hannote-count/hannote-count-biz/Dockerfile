# ============================================================
# hannote-count Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-count -f hannote-count/hannote-count-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY --parents **/pom.xml ./

COPY hannote-framework/ ./hannote-framework/
COPY hannote-count/hannote-count-api/src ./hannote-count/hannote-count-api/src
COPY hannote-count/hannote-count-biz/src ./hannote-count/hannote-count-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-count/hannote-count-biz -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-count/hannote-count-biz/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
