# syntax=docker/dockerfile:1-labs
# ============================================================
# hannote-user-relation Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-user-relation -f hannote-user-relation/hannote-user-relation-biz/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# 递归复制所有层级的 pom.xml，保留原始目录结构（--parents 防止 BuildKit glob 扁平化覆盖）
COPY --parents **/pom.xml ./

# 阶段 1：安装 framework + 所有 *-api 模块到本地仓库，使阶段 2 解析上游依赖时走 .m2 而非 reactor 源码
COPY hannote-framework/ ./hannote-framework/
COPY hannote-user/hannote-user-api/src ./hannote-user/hannote-user-api/src
COPY hannote-oss/hannote-oss-api/src ./hannote-oss/hannote-oss-api/src
COPY hannote-kv/hannote-kv-api/src ./hannote-kv/hannote-kv-api/src
COPY hannote-distributed-id-generator/hannote-distributed-id-generator-api/src ./hannote-distributed-id-generator/hannote-distributed-id-generator-api/src
COPY hannote-note/hannote-note-api/src ./hannote-note/hannote-note-api/src
COPY hannote-user-relation/hannote-user-relation-api/src ./hannote-user-relation/hannote-user-relation-api/src
COPY hannote-count/hannote-count-api/src ./hannote-count/hannote-count-api/src
COPY hannote-comment/hannote-comment-api/src ./hannote-comment/hannote-comment-api/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn install -DskipTests -B -am -pl hannote-framework/hannote-common,hannote-framework/hannote-spring-boot-starter-biz-operationlog,hannote-framework/hannote-spring-boot-starter-biz-context,hannote-framework/hannote-spring-boot-starter-rpc,hannote-user/hannote-user-api,hannote-oss/hannote-oss-api,hannote-kv/hannote-kv-api,hannote-distributed-id-generator/hannote-distributed-id-generator-api,hannote-note/hannote-note-api,hannote-user-relation/hannote-user-relation-api,hannote-count/hannote-count-api,hannote-comment/hannote-comment-api

# 阶段 2：仅复制目标服务源码并构建。上游依赖已在阶段 1 安装到 .m2。
COPY hannote-user-relation/hannote-user-relation-api/src ./hannote-user-relation/hannote-user-relation-api/src
COPY hannote-user-relation/hannote-user-relation-biz/src ./hannote-user-relation/hannote-user-relation-biz/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-user-relation/hannote-user-relation-biz package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-user-relation/hannote-user-relation-biz/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
