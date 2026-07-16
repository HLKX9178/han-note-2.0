# ============================================================
# hannote-comment Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-comment -f hannote-comment/hannote-comment-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-comment/pom.xml ./hannote-comment/
COPY hannote-comment/hannote-comment-api/pom.xml ./hannote-comment/hannote-comment-api/
COPY hannote-comment/hannote-comment-biz/pom.xml ./hannote-comment/hannote-comment-biz/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-comment/hannote-comment-biz/pom.xml -am dependency:go-offline -B

COPY hannote-framework/ ./hannote-framework/
COPY hannote-comment/hannote-comment-api/src ./hannote-comment/hannote-comment-api/src
COPY hannote-comment/hannote-comment-biz/src ./hannote-comment/hannote-comment-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-comment/hannote-comment-biz/pom.xml -am clean package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-comment/hannote-comment-biz/target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
