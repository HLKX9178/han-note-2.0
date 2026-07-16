# ============================================================
# hannote-user-relation Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-user-relation -f hannote-user-relation/hannote-user-relation-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-user-relation/pom.xml ./hannote-user-relation/
COPY hannote-user-relation/hannote-user-relation-api/pom.xml ./hannote-user-relation/hannote-user-relation-api/
COPY hannote-user-relation/hannote-user-relation-biz/pom.xml ./hannote-user-relation/hannote-user-relation-biz/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-user-relation/hannote-user-relation-biz/pom.xml -am dependency:go-offline -B

COPY hannote-framework/ ./hannote-framework/
COPY hannote-user-relation/hannote-user-relation-api/src ./hannote-user-relation/hannote-user-relation-api/src
COPY hannote-user-relation/hannote-user-relation-biz/src ./hannote-user-relation/hannote-user-relation-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-user-relation/hannote-user-relation-biz/pom.xml -am clean package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-user-relation/hannote-user-relation-biz/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
