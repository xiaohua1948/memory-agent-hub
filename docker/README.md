# Memory Agent Hub - Docker 部署

## 架构

```
┌───────────────────────── docker-compose ─────────────────────────┐
│  mysql:8.0    redis:7.2    minio:latest    backend:8088          │
│  :3307        :6380        :9002/:9003     frontend:8081         │
│                                            admin:8083            │
└──────────────────────────────────────────────────────────────────┘
```

## 快速开始

```bash
cd docker

# 1. 启动基础设施（MySQL / Redis / MinIO）
docker compose up -d mysql redis minio

# 2. 迁移本地数据（可选，首次部署时执行）
powershell -ExecutionPolicy Bypass -File migrate/migrate-mysql.ps1
powershell -ExecutionPolicy Bypass -File migrate/migrate-minio.ps1

# 3. 构建并启动全部服务（首次构建较慢）
docker compose up -d --build

# 4. 查看状态
docker compose ps
```

## 访问地址

| 服务       | 地址                          | 说明               |
|------------|-------------------------------|--------------------|
| 用户前端   | http://127.0.0.1:8081        | memory-frontend    |
| 管理后台   | http://127.0.0.1:8083        | memory-frontend-admin |
| 后端 API   | http://127.0.0.1:8089        | memory-backend     |
| MinIO API  | http://127.0.0.1:9002        | S3 兼容接口        |
| MinIO 控制台 | http://127.0.0.1:9003      | Web 管理界面       |
| MySQL      | 127.0.0.1:3307 (root/123456) | Docker MySQL       |
| Redis      | 127.0.0.1:6380 (123456)      | Docker Redis       |

## 端口说明

为避免与宿主机本地服务（MySQL 3306 / Redis 6379 / MinIO 9000 / 后端 8088）冲突，
Docker 服务默认使用备用端口。确认本地服务已停止后，可在 `.env` 中改回标准端口：

```
MYSQL_PORT=3306
REDIS_PORT=6379
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
BACKEND_PORT=8088
```

修改后重启：`docker compose up -d`

管理后台的 API 地址在构建时注入，切换端口后需重新构建：
```
docker compose build admin
docker compose up -d admin
```

## 数据迁移说明

### MySQL
- 脚本自动从本地 MySQL（127.0.0.1:3306）导出 `memory_space` 库并导入 Docker MySQL。
- 本地 mysqldump 路径如需调整，修改 `migrate/migrate-mysql.ps1` 中的 `$MYSQLDUMP`。

### MinIO
- 使用 `minio/mc` 镜像将本地 MinIO 的 `pictures` bucket 镜像到 Docker MinIO。
- 通过 `host.docker.internal` 访问宿主机端口。

### 数据卷
所有数据保存在 `docker/data/` 目录（`.env` 中 `DATA_DIR` 可调整）：
- `data/mysql/` - MySQL 数据
- `data/redis/` - Redis 数据
- `data/minio/` - MinIO 数据

## 常见命令

```bash
# 查看日志
docker compose logs -f backend

# 重启单个服务
docker compose restart backend

# 停止
docker compose down

# 停止并删除数据卷（数据会丢失！）
docker compose down -v
```

## 环境变量

详见 `.env` 文件，主要配置项：
- `MYSQL_ROOT_PASSWORD` / `MYSQL_DATABASE`：MySQL 密码与数据库名
- `REDIS_PASSWORD`：Redis 密码
- `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`：MinIO 凭据（默认与原本地一致）
- `MINIO_PUBLIC_URL`：浏览器访问 MinIO 的地址（预签名 URL 使用）
- `ADMIN_API_URL`：管理后台访问后端的地址（构建时注入）
