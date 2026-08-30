# =============================================================
# Memory Agent Hub - Backend image
# Build context: project root (docker-compose.yml sets context: ..)
#
# REGISTRY: base-image registry prefix. Default is a China mirror for
#   fast pulls. Override with --build-arg REGISTRY=docker.io if abroad.
# =============================================================
ARG REGISTRY=docker.1ms.run

# ---------- Build stage ----------
FROM ${REGISTRY}/maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Huawei Cloud Maven mirror for fast dependency downloads in China
COPY docker/backend/settings.xml /root/.m2/settings.xml

# Copy pom + system-scoped opencv jar first to leverage layer cache
COPY memory-backend/pom.xml .
COPY memory-backend/src/main/resources/opencv/lib/opencv-455.jar src/main/resources/opencv/lib/opencv-455.jar

# Copy source and package (downloads deps on demand; `dependency:go-offline`
# is intentionally skipped - it can hang on mirror misses).
# BuildKit cache mount keeps the local repo across build retries.
COPY memory-backend/src ./src
RUN --mount=type=cache,target=/root/.m2/repository mvn -B clean package -Dmaven.test.skip=true

# ---------- Runtime stage ----------
# Ubuntu 22.04 (jammy): libopencv-java is 4.5.4, closest to project's 4.5.5
FROM ${REGISTRY}/eclipse-temurin:17-jre-jammy
LABEL maintainer="memory-agent-hub"

# Aliyun APT mirror + install OpenCV Java native lib
# (provides /usr/lib/jni/libopencv_java*.so)
RUN sed -i 's|http://archive.ubuntu.com/ubuntu|https://mirrors.ustc.edu.cn/ubuntu|g; s|http://security.ubuntu.com/ubuntu|https://mirrors.ustc.edu.cn/ubuntu|g' /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends libopencv4.5-java libopencv4.5d-jni \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m"
EXPOSE 8088
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
