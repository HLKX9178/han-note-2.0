# ============================================================
# hannote-search Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-search -f hannote-search/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY */pom.xml */*/pom.xml ./
COPY hannote-framework/*/pom.xml ./hannote-framework/

COPY hannote-framework/ ./hannote-framework/
COPY hannote-search/src ./hannote-search/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-search -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-search/target/*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
