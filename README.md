
<p align="center">
  <img src="https://img.shields.io/badge/SpringBoot-3.x-6DB33F?logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Vue-3.x-42B883?logo=vue.js&logoColor=white" />
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5-409EFF" />
  <img src="https://img.shields.io/badge/LangChain-4j-00C8C8" />
  <img src="https://img.shields.io/badge/RocketMQ-5.x-C40025" />
  <img src="https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/OSS-阿里云对象存储-FF6A00" />
  <img src="https://img.shields.io/badge/Docker-20+-2496ED?logo=docker&logoColor=white" />
</p>

<h1 align="center">Memory Agent Hub · 智能体云相册管理平台</h1>

<p align="center">
  <b>一个融合 AI Agent 与云存储的智能相册管理系统，让照片管理更聪明、更高效。</b>
</p>

<p align="center">
  <a href="https://gitee.com/xiaohua1875/memory-agent-hub">📦 Gitee 仓库</a> ·
  <a href="#-核心特性">✨ 核心特性</a> ·
  <a href="#-技术栈">🏗️ 技术栈</a> ·
  <a href="#-快速开始">🚀 快速开始</a> ·
  <a href="#-项目结构">📂 项目结构</a>
</p>

---

## 🔗 仓库地址

*   **Gitee**：[https://gitee.com/xiaohua1875/memory-agent-hub](https://gitee.com/xiaohua1875/memory-agent-hub)

> 欢迎 Star ⭐ 与 Fork，您的支持是我们持续迭代的动力！

## 📝 项目简介

**Memory Agent Hub** 是一个基于 **AI Agent** 的智能云相册管理平台，旨在提供更智能、更便捷的照片与媒体文件管理体验。项目采用前后端分离架构，后端基于 **Spring Boot 3** 深度集成 **LangChain4j** AI 能力，通过消息队列 **RocketMQ** 解耦耗时任务，结合 **Redis** 缓存与 **OSS** 对象存储，构建高可用、高并发的媒体管理服务体系。

系统支持通过自然语言与 AI 对话，完成相册创建、照片搜索、智能识别等操作；内置图像识别（ImageReg）能力，实现人脸归类与场景理解，让海量照片“会说话”。

## ✨ 核心特性

1.  **🤖 AI 智能助手 (AI Agent)**
    *   **自然语言交互**：用户可通过自然语言描述需求，如“找出在上海拍摄的照片”或“创建一个名为旅行的新相册”。
    *   **智能决策**：AI 自动分析用户意图并调用相应工具（数据库查询、文件操作等）。
    *   **Human-in-the-loop**：涉及删除或新建等写操作时，系统生成待确认卡片，仅在用户确认后执行，保障数据安全。

2.  **🖼️ 智能媒体管理**
    *   **图像识别 (ImageReg)**：集成图像识别能力，自动识别照片中的人脸、物体与场景并归类。
    *   **地理位置解析**：自动解析照片 EXIF 经纬度，逆向地理编码获取详细地址。
    *   **元数据解析**：支持图片（EXIF）与视频（Metadata）详细信息提取与展示。

3.  **⚡ 稳定高效的文件处理**
    *   **分片上传**：支持大文件分片上传、断点续传与 MD5 校验秒传。
    *   **对象存储 (OSS)**：基于云对象存储后端，支持高并发访问与弹性扩容。
    *   **异步解耦**：耗时任务（如识别、转码）通过 RocketMQ 异步处理，提升响应速度。

4.  **📊 数据可视化**
    *   提供直观图表展示文件类型分布、存储空间使用、标签统计等信息。

5.  **🚀 云原生部署**
    *   全程容器化（Docker），支持一键编排部署，运维简单可靠。

## 🏗️ 技术栈

| 分类 | 技术 |
| --- | --- |
| **后端框架** | Spring Boot 3、MyBatis-Plus、Java 17 |
| **前端框架** | Vue 3、TypeScript、Vue 2（管理后台 + Element UI） |
| **AI / 智能** | LangChain4j、LLM（流式输出）、ImageReg 图像识别 |
| **消息队列** | RocketMQ |
| **缓存** | Redis |
| **存储** | MySQL 8.0、OSS 对象存储 |
| **部署** | Docker、Docker Compose |

**核心技术关键词**：`SpringBoot3` · `Vue3` · `MyBatis-Plus` · `LangChain` · `RocketMQ` · `Redis` · `OSS` · `ImageReg` · `Docker`

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
    # 编辑 .env 文件，配置数据库密码、OSS 密钥、AI API Key 等
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
*   **UploadService**: 文件上传服务，处理分片上传与 OSS 交互。

## 🤝 贡献指南

欢迎提交 Issue 或 Pull Request。在提交代码前，请确保代码风格符合项目规范。

## 📄 许可证

本项目基于开源许可证，详情请查看 [LICENSE](LICENSE) 文件。