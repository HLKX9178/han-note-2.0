# ============================================================
# hannote-gateway Dockerfile
# Build context: 仓库根目录
# 构建命令: docker build -t hannote-gateway -f hannote-gateway/Dockerfile .
# ============================================================

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# **/pom.xml 递归匹配所有层级的 pom.xml，保留原始目录结构
COPY **/pom.xml ./

COPY hannote-framework/ ./hannote-framework/
COPY hannote-gateway/src ./hannote-gateway/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl hannote-gateway -am install -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/hannote-gateway/target/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
