# Memory Agent Hub (Intelligent Gallery System)

## 📝 Project Introduction

Memory Agent Hub is an intelligent gallery management system based on **AI Agents**, aimed at providing a smarter and more convenient experience for photo and media file management. The project adopts a decoupled frontend-backend architecture. The backend is built on **Spring Boot 3**, deeply integrated with **LangChain4j** AI capabilities, supporting conversation with the system via natural language to perform operations such as album creation, photo search, and webpage generation.

## ✨ Core Features

1.  **🤖 AI Intelligent Assistant (AI Agent)**
    *   **Natural Language Interaction**: Users can describe requirements in natural language, such as "Find photos taken in Shanghai" or "Create a new album named Travel".
    *   **Intelligent Decision Making**: The AI automatically analyzes user intent and calls corresponding tools (e.g., database queries, file operations).
    *   **Human-in-the-loop**: For write operations involving deletion or creation, the system generates a confirmation card. Execution only proceeds after user interface confirmation, ensuring data security.

2.  **🖼️ Smart Media Management**
    *   **Face Recognition**: Integrates OpenCV to automatically recognize faces in photos and categorize them.
    *   **Geolocation Parsing**: Automatically parses latitude and longitude from photo EXIF information and performs reverse geocoding to obtain detailed addresses.
    *   **Metadata Parsing**: Supports extraction and display of detailed image (EXIF) and video (Metadata) information.

3.  **⚡ Stable & Efficient File Processing**
    *   **Chunked Upload**: Supports large file chunked uploads, breakpoint resume, and instant upload via MD5 verification.
    *   **Object Storage**: Uses MinIO as the object storage backend, supporting high-concurrency access.

4.  **📊 Data Visualization**
    *   Provides intuitive charts to display file type distribution, storage space usage, tag statistics, and more.

## 🏗️ Tech Stack

*   **Backend**: Java 17, Spring Boot 3, MyBatis-Plus, LangChain4j
*   **Storage**: MySQL 8.0, Redis, MinIO
*   **AI**: LLM (supports streaming output), OpenCV (face/image processing)
*   **Frontend**: Vue 3 + TypeScript
*   **Admin Panel**: Vue 2 + Element UI
*   **Deployment**: Docker, Docker Compose

## 🚀 Quick Start

The project provides a complete Docker deployment solution to start all services within a short time.

### Prerequisites

*   Docker (v20+)
*   Docker Compose (v2+)

### Startup Steps

1.  **Enter the deployment directory**:
    ```bash
    cd docker
    ```

2.  **Configure Environment Variables**:
    Copy the environment variable example file and modify it:
    ```bash
    cp .env.example .env
    # Edit the .env file to configure database passwords, MinIO keys, AI API Keys, etc.
    ```

3.  **Start Services**:
    ```bash
    docker-compose up -d
    ```

4.  **Access the Application**:
    *   **User Frontend**: `http://localhost`
    *   **Admin Panel**: `http://localhost:8080` (Default credentials: admin/admin123)
    *   **API Documentation**: After startup, visit `http://localhost:8088/swagger-ui.html`

> **Note**: Building images for the first time may take longer; please wait patiently. Ensure all configurations in `docker/.env` are filled in correctly.

## 📂 Project Structure

```text
memory-agent-hub/
├── docker/                  # Docker deployment configuration (Dockerfile, docker-compose.yml)
├── memory-backend/          # Backend core code (Java)
│   ├── src/main/java/.../ai/        # AI Agent core logic
│   ├── src/main/java/.../controller/ # REST API controllers
│   └── src/main/java/.../service/    # Business logic implementation
├── memory-frontend/         # User frontend (Vue 3 + TS)
├── memory-frontend-admin/   # Admin panel (Vue 2)
└── LICENSE
```

## 📝 Main Module Descriptions

*   **AgentOrchestrator**: AI task orchestration center, responsible for parsing user intent and scheduling tools.
*   **PendingActionService**: Pending action service, handles operations initiated by the AI that require user confirmation (e.g., deleting an album).
*   **Text2SqlService**: Text-to-SQL service, converts natural language into secure database query statements for searching photos.
*   **UploadService**: File upload service, handles chunked uploads and interaction with MinIO.

## 🤝 Contribution Guidelines

Welcome to submit Issues or Pull Requests. Before submitting code, please ensure the code style complies with project specifications.

## 📄 License

This project is based on an open-source license. For details, please refer to the [LICENSE](LICENSE) file.