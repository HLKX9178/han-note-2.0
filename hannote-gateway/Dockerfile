# ============================================================
# hannote-gateway Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-gateway -f hannote-gateway/Dockerfile .
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# 1. 复制所有 pom.xml（利用 Docker 层缓存，依赖不变时跳过重新下载）
COPY pom.xml ./
COPY hannote-framework/pom.xml ./hannote-framework/
COPY hannote-framework/hannote-common/pom.xml ./hannote-framework/hannote-common/
COPY hannote-framework/hannote-spring-boot-starter-biz-context/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-context/
COPY hannote-framework/hannote-spring-boot-starter-biz-operationlog/pom.xml ./hannote-framework/hannote-spring-boot-starter-biz-operationlog/
COPY hannote-framework/hannote-spring-boot-starter-rpc/pom.xml ./hannote-framework/hannote-spring-boot-starter-rpc/
COPY hannote-gateway/pom.xml ./hannote-gateway/

# 2. 下载依赖（此层在 pom 不变时可缓存）
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-gateway/pom.xml -am dependency:go-offline -B

# 3. 复制源码
COPY hannote-framework/ ./hannote-framework/
COPY hannote-gateway/src ./hannote-gateway/src

# 4. 编译打包
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f hannote-gateway/pom.xml -am clean package -DskipTests -B

# ---------- 运行阶段 ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-gateway/target/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
