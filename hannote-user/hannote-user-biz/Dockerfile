# ============================================================
# hannote-user Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-user -f hannote-user/hannote-user-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-user/pom.xml ./hannote-user/
COPY hannote-user/hannote-user-api/pom.xml ./hannote-user/hannote-user-api/
COPY hannote-user/hannote-user-biz/pom.xml ./hannote-user/hannote-user-biz/
COPY hannote-framework/ ./hannote-framework/
COPY hannote-user/hannote-user-api/src ./hannote-user/hannote-user-api/src
COPY hannote-user/hannote-user-biz/src ./hannote-user/hannote-user-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-user/hannote-user-biz -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-user/hannote-user-biz/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
