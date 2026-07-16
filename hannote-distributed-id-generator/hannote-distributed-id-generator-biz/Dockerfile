# ============================================================
# hannote-distributed-id-generator Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-id -f hannote-distributed-id-generator/hannote-distributed-id-generator-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-distributed-id-generator/pom.xml ./hannote-distributed-id-generator/
COPY hannote-distributed-id-generator/hannote-distributed-id-generator-api/pom.xml ./hannote-distributed-id-generator/hannote-distributed-id-generator-api/
COPY hannote-distributed-id-generator/hannote-distributed-id-generator-biz/pom.xml ./hannote-distributed-id-generator/hannote-distributed-id-generator-biz/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-distributed-id-generator/hannote-distributed-id-generator-biz/pom.xml -am dependency:go-offline -B

COPY hannote-framework/ ./hannote-framework/
COPY hannote-distributed-id-generator/hannote-distributed-id-generator-api/src ./hannote-distributed-id-generator/hannote-distributed-id-generator-api/src
COPY hannote-distributed-id-generator/hannote-distributed-id-generator-biz/src ./hannote-distributed-id-generator/hannote-distributed-id-generator-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-distributed-id-generator/hannote-distributed-id-generator-biz/pom.xml -am clean package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-distributed-id-generator/hannote-distributed-id-generator-biz/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
