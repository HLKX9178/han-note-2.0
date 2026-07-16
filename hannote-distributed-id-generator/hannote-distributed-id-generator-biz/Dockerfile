# ============================================================
# hannote-distributed-id-generator Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-id -f hannote-distributed-id-generator/hannote-distributed-id-generator-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY **/pom.xml ./

COPY hannote-framework/ ./hannote-framework/
COPY hannote-distributed-id-generator/hannote-distributed-id-generator-api/src ./hannote-distributed-id-generator/hannote-distributed-id-generator-api/src
COPY hannote-distributed-id-generator/hannote-distributed-id-generator-biz/src ./hannote-distributed-id-generator/hannote-distributed-id-generator-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-distributed-id-generator/hannote-distributed-id-generator-biz -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-distributed-id-generator/hannote-distributed-id-generator-biz/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
