# ============================================================
# hannote-gateway Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-gateway -f hannote-gateway/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# 复制所有模块的 pom.xml（Maven reactor 需要所有 <modules> 条目对应的 pom 存在）
COPY pom.xml ./
COPY */pom.xml */*/pom.xml ./
COPY hannote-framework/*/pom.xml ./hannote-framework/

# 仅复制本服务所需的源码
COPY hannote-framework/ ./hannote-framework/
COPY hannote-gateway/src ./hannote-gateway/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-gateway -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-gateway/target/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
