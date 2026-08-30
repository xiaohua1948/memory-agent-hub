# =============================================================
# Memory Agent Hub - User frontend image (Vue3 + Vite)
# Build context: project root
#
# REGISTRY: base-image registry prefix. Default is a China mirror for
#   fast pulls. Override with --build-arg REGISTRY=docker.io if abroad.
# =============================================================
ARG REGISTRY=docker.1ms.run

# ---------- Build stage ----------
FROM ${REGISTRY}/node:22-alpine AS builder
WORKDIR /build

COPY memory-frontend/package.json memory-frontend/package-lock.json ./
RUN npm install --registry=https://registry.npmmirror.com --no-audit --no-fund

COPY memory-frontend/ ./
RUN npm run build-only

# ---------- Runtime stage ----------
FROM ${REGISTRY}/nginx:1.27-alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
