# ============================================================
# hannote-kv Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-kv -f hannote-kv/hannote-kv-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-kv/pom.xml ./hannote-kv/
COPY hannote-kv/hannote-kv-api/pom.xml ./hannote-kv/hannote-kv-api/
COPY hannote-kv/hannote-kv-biz/pom.xml ./hannote-kv/hannote-kv-biz/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-kv/hannote-kv-biz/pom.xml -am dependency:go-offline -B

COPY hannote-framework/ ./hannote-framework/
COPY hannote-kv/hannote-kv-api/src ./hannote-kv/hannote-kv-api/src
COPY hannote-kv/hannote-kv-biz/src ./hannote-kv/hannote-kv-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-kv/hannote-kv-biz/pom.xml -am clean package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-kv/hannote-kv-biz/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
