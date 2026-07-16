# ============================================================
# hannote-data-align Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-data-align -f hannote-data-align/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY */pom.xml */*/pom.xml ./
COPY hannote-framework/*/pom.xml ./hannote-framework/

COPY hannote-framework/ ./hannote-framework/
COPY hannote-data-align/src ./hannote-data-align/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-data-align -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-data-align/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
