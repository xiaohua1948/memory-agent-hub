# =============================================================
# Memory Agent Hub - Admin frontend image (Vue2 + vue-cli 4)
# Build context: project root
# Build arg: VUE_APP_BASE_API (browser-facing backend URL)
#
# REGISTRY: base-image registry prefix. Default is a China mirror for
#   fast pulls. Override with --build-arg REGISTRY=docker.io if abroad.
# =============================================================
ARG REGISTRY=docker.1ms.run

# ---------- Build stage ----------
# vue-cli 4 (webpack 4) needs the legacy md4 hash. Node 16 ships OpenSSL 1.1.1
# which still supports md4, so --openssl-legacy-provider is NOT needed here
# (that flag is Node 17+ only and Node 16 rejects it in NODE_OPTIONS).
FROM ${REGISTRY}/node:16-alpine AS builder
WORKDIR /build

ARG VUE_APP_BASE_API=http://127.0.0.1:8088
ENV VUE_APP_BASE_API=$VUE_APP_BASE_API

COPY memory-frontend-admin/package.json memory-frontend-admin/package-lock.json ./
# --legacy-peer-deps: @wangeditor/editor-for-vue peer deps conflict with Vue 2.6
RUN npm install --legacy-peer-deps --registry=https://registry.npmmirror.com --no-audit --no-fund

COPY memory-frontend-admin/ ./
# package.json's build:prod uses Windows-only "SET ..." syntax; call CLI directly
RUN npx vue-cli-service build

# ---------- Runtime stage ----------
FROM ${REGISTRY}/nginx:1.27-alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY docker/admin/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
