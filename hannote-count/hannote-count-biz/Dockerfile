# ============================================================
# hannote-count Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-count -f hannote-count/hannote-count-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-count/pom.xml ./hannote-count/
COPY hannote-count/hannote-count-api/pom.xml ./hannote-count/hannote-count-api/
COPY hannote-count/hannote-count-biz/pom.xml ./hannote-count/hannote-count-biz/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-count/hannote-count-biz/pom.xml -am dependency:go-offline -B

COPY hannote-framework/ ./hannote-framework/
COPY hannote-count/hannote-count-api/src ./hannote-count/hannote-count-api/src
COPY hannote-count/hannote-count-biz/src ./hannote-count/hannote-count-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-count/hannote-count-biz/pom.xml -am clean package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-count/hannote-count-biz/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
