# ============================================================
# hannote-oss Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-oss -f hannote-oss/hannote-oss-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-oss/pom.xml ./hannote-oss/
COPY hannote-oss/hannote-oss-api/pom.xml ./hannote-oss/hannote-oss-api/
COPY hannote-oss/hannote-oss-biz/pom.xml ./hannote-oss/hannote-oss-biz/
COPY hannote-framework/ ./hannote-framework/
COPY hannote-oss/hannote-oss-api/src ./hannote-oss/hannote-oss-api/src
COPY hannote-oss/hannote-oss-biz/src ./hannote-oss/hannote-oss-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-oss/hannote-oss-biz/pom.xml -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-oss/hannote-oss-biz/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
