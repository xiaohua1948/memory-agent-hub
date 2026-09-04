

# Memory Agent Hub (智能体云相册管理平台)

## 📝 项目简介

Memory Agent Hub 是一个基于 **AI Agent** 的智能云相册管理平台，旨在提供更智能、更便捷的照片与媒体文件管理体验。项目采用前后端分离架构，后端基于 **Spring Boot 3** 深度集成了 **LangChain4j** AI 能力，支持通过自然语言与系统对话，实现相册创建、照片搜索、网页生成等操作。

## ✨ 核心特性

1.  **🤖 AI 智能助手 (AI Agent)**
    *   **自然语言交互**：用户可以通过自然语言描述需求，如“找出在上海拍摄的照片”或“创建一个名为旅行的新相册”。
    *   **智能决策**：AI 会自动分析用户意图并调用相应的工具（如数据库查询、文件操作）。
    *   **Human-in-the-loop**：涉及删除或新建等写操作时，系统会生成待确认卡片，仅在用户界面确认后才会真正执行，保障数据安全。

2.  **🖼️ 智能媒体管理**
    *   **人脸识别**：集成 OpenCV 自动识别照片中的人脸，并进行归类。
    *   **地理位置解析**：自动解析照片 EXIF 信息中的经纬度，逆向地理编码获取详细地址。
    *   **元数据解析**：支持图片（EXIF）和视频（Metadata）详细信息的提取与展示。

3.  **⚡ 稳定高效的文件处理**
    *   **分片上传**：支持大文件分片上传、断点续传和 MD5 校验秒传。
    *   **对象存储**：使用 MinIO 作为对象存储后端，支持高并发访问。

4.  **📊 数据可视化**
    *   提供直观的图表展示文件类型分布、存储空间使用、标签统计等信息。

## 🏗️ 技术栈

*   **后端**: Java 17, Spring Boot 3, MyBatis-Plus, LangChain4j
*   **存储**: MySQL 8.0, Redis, MinIO
*   **AI**: LLM (支持流式输出), OpenCV (人脸/图像处理)
*   **前端**: Vue 3 + TypeScript
*   **管理后台**: Vue 2 + Element UI
*   **部署**: Docker, Docker Compose

## 🚀 快速开始

项目提供了完整的 Docker 部署方案，可在短时间内启动全部服务。

### 前置条件

*   Docker (v20+)
*   Docker Compose (v2+)

### 启动步骤

1.  **进入部署目录**：
    ```bash
    cd docker
    ```

2.  **配置环境变量**：
    复制环境变量示例文件并修改：
    ```bash
    cp .env.example .env
    # 编辑 .env 文件，配置数据库密码、MinIO 密钥、AI API Key 等
    ```

3.  **启动服务**：
    ```bash
    docker-compose up -d
    ```

4.  **访问应用**：
    *   **用户前端**: `http://localhost`
    *   **管理后台**: `http://localhost:8080` (默认账号 admin/admin123)
    *   **API 文档**: 启动后访问 `http://localhost:8088/swagger-ui.html`

> **注意**: 首次构建镜像耗时较长，请耐心等待。确保 `docker/.env` 中的配置均已正确填写。

## 📂 项目结构

```text
memory-agent-hub/
├── docker/                  # Docker 部署配置 (Dockerfile, docker-compose.yml)
├── memory-backend/          # 后端核心代码 (Java)
│   ├── src/main/java/.../ai/        # AI Agent 核心逻辑
│   ├── src/main/java/.../controller/ # REST API 控制器
│   └── src/main/java/.../service/    # 业务逻辑实现
├── memory-frontend/         # 用户前端 (Vue 3 + TS)
├── memory-frontend-admin/   # 管理后台 (Vue 2)
└── LICENSE
```

## 📝 主要模块说明

*   **AgentOrchestrator**: AI 任务编排中心，负责解析用户意图并调度工具。
*   **PendingActionService**: 待办操作服务，处理 AI 发起的需要用户确认的操作（如删除相册）。
*   **Text2SqlService**: 文本转 SQL 服务，将自然语言转换为安全的数据库查询语句，用于搜索照片。
*   **UploadService**: 文件上传服务，处理分片上传与 MinIO 交互。

## 🤝 贡献指南

欢迎提交 Issue 或 Pull Request。在提交代码前，请确保代码风格符合项目规范。

## 📄 许可证

本项目基于开源许可证，详情请查看 [LICENSE](LICENSE) 文件。