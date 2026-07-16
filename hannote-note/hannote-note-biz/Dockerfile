# ============================================================
# hannote-note Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-note -f hannote-note/hannote-note-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-note/pom.xml ./hannote-note/
COPY hannote-note/hannote-note-api/pom.xml ./hannote-note/hannote-note-api/
COPY hannote-note/hannote-note-biz/pom.xml ./hannote-note/hannote-note-biz/
COPY hannote-framework/ ./hannote-framework/
COPY hannote-note/hannote-note-api/src ./hannote-note/hannote-note-api/src
COPY hannote-note/hannote-note-biz/src ./hannote-note/hannote-note-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-note/hannote-note-biz/pom.xml -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-note/hannote-note-biz/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
