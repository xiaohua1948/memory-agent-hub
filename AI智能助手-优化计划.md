# AI 智能助手（Data Agent + Code Agent）实现计划 · 优化版 v2

> 本计划在原计划（`glittery-puzzling-puppy.md`）基础上，参考 `yu-ai-code-mother-master` 代码库（程序员鱼皮「AI 零代码应用生成平台」）的工程实现，重点补齐原计划缺失的三大能力：
> 1. **多轮对话 / 会话管理**（原计划"前端维护消息数组、刷新即清空"，无法支撑"多轮对话描述逐步达成目的"）；
> 2. **多模型可配置**（原计划仅一个 `qwen-plus`，无法按职责拆分"标题生成模型 / 任务模型 / 路由模型"）；
> 3. **AI 侧数据库设计**（原计划无任何建表，会话与消息无法持久化）。
>
> 已拍板的既有决策（text2filter、human-in-the-loop 二次确认、userId 隔离、`@Tool` 工具调用）全部保留，仅做架构升级。

---

## 0. 参考项目关键结论（yu-ai-code-mother 学到什么）

| 维度 | 参考项目做法 | 对本计划的启发 |
|---|---|---|
| 多模型配置 | 4 个独立 `@ConfigurationProperties` 配置类 + `@Scope("prototype")` bean：`chat-model` / `streaming-chat-model` / `reasoning-streaming-chat-model` / `routing-chat-model`，供应商可混搭（DeepSeek + 通义 qwen-turbo） | 按"角色"拆分模型，每个模型独立 base-url / api-key / model-name / temperature / max-tokens |
| 模型路由 | `AiCodeGenTypeRoutingService` 用**轻量模型（qwen-turbo，max-tokens=100）做结构化输出**，返回枚举决定走哪条分支 | 用轻量模型做意图路由 / 标题生成，主模型专注任务 |
| 多轮记忆 | `RedisChatMemoryStore`（langchain4j-community-redis）+ `MessageWindowChatMemory(maxMessages=20)`，按 `memoryId`（appId）隔离；`loadChatHistoryToMemory` 从 DB 回灌 | 后端持久化记忆 + DB 历史兜底，跨刷新/跨会话不丢上下文 |
| AI 服务实例管理 | `AiCodeGeneratorServiceFactory` + Caffeine 缓存（max 1000、30min 写过期、10min 读过期），按 appId+类型 缓存 AiService | 工厂 + Caffeine 缓存 AiService 实例，`@Scope("prototype")` 解决并发 |
| 工具管理 | `ToolManager` 统一注册 `BaseTool[]`（`@Resource` 注入全部工具），`hallucinatedToolNameStrategy` 处理幻觉，`maxSequentialToolsInvocations(20)` | 工具统一注册 + 幻觉兜底 + 调用上限 |
| Prompt 管理 | system prompt 外置 `resources/prompt/*.txt`，`@SystemMessage(fromResource=...)` | prompt 外置，便于迭代与灰度 |
| 护栏 | `PromptSafetyInputGuardrail`（输入）、`RetryOutputGuardrail`（输出） | 输入/输出护栏 |
| 流式输出 | `Flux<String>` / `TokenStream` + 多策略 `StreamHandler`（SimpleText / JsonMessage）区分工具调用与文本 | 流式输出按消息类型分流 |
| **版本注意** | 参考项目实际用 `langchain4j-open-ai-spring-boot-starter` / `langchain4j-reactor` / `langchain4j-community-redis-spring-boot-starter`，版本 `1.1.0-beta7` | **原计划写的 `1.11.0` 版本号有误**，需以参考项目同款 `1.1.0-beta7` 为准（或核实中央仓库最新 stable beta 后统一锁定） |

---

## 1. 总体架构（单体分层，对齐参考项目模块化思想）

参考项目是微服务（`yu-ai-code-ai` 能力层 + `yu-ai-code-app` 编排层 + `yu-ai-code-model` 实体层）。本项目是单体 Spring Boot，**不做微服务拆分，但沿用"AI 能力层 / 业务编排层 / 实体层"的分包隔离**：

```
com.memory.xiaohua
├── ai
│   ├── config                # 多模型配置（AiModelConfig 等，§3）
│   ├── model                 # AI 模型配置属性类（TaskModelProperties 等）
│   ├── tool                  # @Tool 工具（AgentTools + ToolManager，§6.3）
│   ├── prompt                # system prompt 资源（resources/prompt/*.txt）
│   ├── agent                 # Agent 编排（AiAgentService / AiAgentServiceImpl，§6.4）
│   ├── service               # 标题生成 / 意图路由等轻量 AI 服务（§6.5）
│   └── memory                # 对话记忆装配（AiChatMemoryFactory，§4）
├── model
│   ├── entity                # AiSession / AiMessage 实体（§5）
│   └── dto/ai                # 请求/响应 DTO（复用原计划 DTO 清单 + 会话相关）
├── mapper                    # AiSessionMapper / AiMessageMapper（§5）
├── service                   # AiQueryService / AiExecuteService / AiSessionService
└── controller                # AiController（chat/execute/session/...）
```

关键分层原则（来自参考项目）：
- **AI 能力层（`ai.*`）**：只负责"调模型、管工具、管记忆"，不直接碰业务 Service 的写库逻辑。
- **业务编排层（`service.*`）**：`AiQueryService`（读）、`AiExecuteService`（写执行）、`AiSessionService`（会话/消息持久化），复用现有 `FileService` / `AlbumService`。
- **实体层（`model.entity`）**：AI 相关的会话、消息实体与现有实体并列。

---

## 2. 核心流程（多轮 + 确认闭环）

```
用户发消息
   │  POST /ai/chat { sessionId?, agentType, content, fileIds[] }
   ▼
[1] 解析/创建会话（无 sessionId 则新建 AiSession）
[2] 保存 user 消息 → AiMessage(role=user)
[3] 装配对话记忆：RedisChatMemoryStore(memoryId=sessionId) ← 空则从 DB 回灌最近 20 条
[4] 按 agentType 取对应工具集 + system prompt
[5] 有界 tool-use 循环（≤ max-iterations）
       读工具 → 执行并回填结果
       写工具 → 生成 PendingAction 存 Redis → 短路（返回待确认卡片）
[6] 总结调用 → AiChatResponse
[7] 保存 assistant 消息 → AiMessage(role=assistant, contentType=...)
[8] 若为首条消息 → 异步用"标题模型"生成会话标题 → 更新 AiSession.title
   ▼
前端渲染（文本 / 照片 / 确认卡片 / HTML）
   └─ 用户点「确认」→ POST /ai/execute { actionId } → 后端执行写操作
```

---

## 3. 多模型配置（核心优化点 ①）

### 3.1 模型角色划分

| 模型角色 | 用途 | 建议模型 | 温度 | max-tokens | 说明 |
|---|---|---|---|---|---|
| `task-chat-model` | **任务模型**：data/code agent 主对话 + 工具调用 | `qwen-plus`（或 `deepseek-chat`） | 0.2 | 4096 | 原计划的 `qwen-plus` 保留为任务模型 |
| `routing-chat-model` | **路由模型**：意图识别（判断 data/code、是否写操作），结构化输出 | `qwen-turbo` | 0 | 100 | 轻量、快、省 token，参考项目同款思路 |
| `title-chat-model` | **标题模型**：会话标题生成 | `qwen-turbo` | 0 | 50 | 首条消息后异步调用 |
| `reasoning-chat-model` | （可选）**推理模型**：复杂 Code Agent 任务 | `deepseek-reasoner` | 0.1 | 8192 | 预留，支持供应商混搭 |

> 设计目标：**大任务用强模型、小任务用轻模型**。标题、路由这类"低难度、高频、对 token 敏感"的活儿交给 `qwen-turbo`，把主任务模型的上下文窗口留给真正的工具调用。

### 3.2 配置结构（application.yml）

```yaml
langchain4j:
  open-ai:
    task-chat-model:
      base-url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
      api-key: ${QWEN_API_KEY:}
      model-name: qwen-plus
      temperature: 0.2
      max-tokens: 4096
      log-requests: false
      log-responses: false
    routing-chat-model:
      base-url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
      api-key: ${QWEN_API_KEY:}
      model-name: qwen-turbo
      temperature: 0
      max-tokens: 100
    title-chat-model:
      base-url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
      api-key: ${QWEN_API_KEY:}
      model-name: qwen-turbo
      temperature: 0
      max-tokens: 50
    # 可选：复杂推理模型（供应商可换）
    reasoning-chat-model:
      base-url: ${REASONING_BASE_URL:https://api.deepseek.com}
      api-key: ${REASONING_API_KEY:}
      model-name: deepseek-reasoner
      temperature: 0.1
      max-tokens: 8192
```

### 3.3 代码结构（对齐参考项目 `RoutingAiModelConfig` / `StreamingChatModelConfig`）

每个模型一个 `@ConfigurationProperties` 属性类 + 一个 `@Scope("prototype")` bean，通过 `SpringContextUtil.getBean("xxxPrototype", ChatModel.class)` 按需取用（解决并发，避免单例 `ChatModel` 被多线程共享）：

```java
// ai/model/TaskModelProperties.java
@ConfigurationProperties(prefix = "langchain4j.open-ai.task-chat-model")
public record TaskModelProperties(String baseUrl, String apiKey, String modelName,
                                  Double temperature, Integer maxTokens) {}

// ai/config/AiModelConfig.java
@Configuration
public class AiModelConfig {
    @Bean @Scope("prototype")
    public ChatModel taskChatModelPrototype(TaskModelProperties p) {
        return OpenAiChatModel.builder().baseUrl(p.baseUrl()).apiKey(p.apiKey())
                .modelName(p.modelName()).temperature(p.temperature()).maxTokens(p.maxTokens()).build();
    }
    // routingChatModelPrototype / titleChatModelPrototype / reasoningChatModelPrototype 同理
}
```

> **供应商混搭**：所有模型走 OpenAI 兼容端点（DashScope / DeepSeek / 硅基流动等均可），各自独立 `base-url` + `api-key`，天然支持多供应商。

### 3.4 依赖（替换原计划 D1 的依赖，版本修正）

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>1.1.0-beta7</version>
</dependency>
<!-- 对话记忆 Redis 存储（多轮核心依赖） -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-community-redis</artifactId>
    <version>1.1.0-beta7</version>
</dependency>
```

> 说明：
> - 原计划写 `langchain4j:1.11.0` 有误，LangChain4j 无此版本，参考项目实际用 `1.1.0-beta7`。落地前以中央仓库最新 stable beta 为准，**全模块版本必须一致**。
> - 本项目已引入 `spring-boot-starter-data-redis`，`RedisChatMemoryStore` 可直接复用（或独立 Redis 实例/db，见 §4.3）。
> - 保留原计划"不用 starter、手动构建模型"的策略（`langchain4j-open-ai` 非 starter 版），降低对 Spring Boot 3.4.1 的自动配置兼容风险；仅 `langchain4j-community-redis` 手动 `RedisChatMemoryStore.builder()` 构建。

---

## 4. 多轮对话 / 会话管理（核心优化点 ②）

### 4.1 记忆分层：Redis（运行时记忆）+ DB（持久真相）

对齐参考项目：`MessageWindowChatMemory`（窗口记忆，maxMessages=20）落在 `RedisChatMemoryStore`（按 `sessionId` 隔离），DB `ai_message` 表作为持久历史。

```
RedisChatMemoryStore (memoryId = sessionId, ttl 可配)   ← 运行时记忆（LangChain4j 自动读写）
        ↕ 空则回灌
ai_message 表 (session_id 维度, 最近 N 条)              ← 持久真相源（跨 Redis 过期/重启仍可恢复）
```

### 4.2 记忆装配（`AiChatMemoryFactory`）

```java
public MessageWindowChatMemory buildMemory(String sessionId) {
    MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .id(sessionId)                       // memoryId = sessionId，多会话天然隔离
            .chatMemoryStore(redisChatMemoryStore)
            .maxMessages(20)
            .build();
    // 若记忆为空（首次 / Redis 过期），从 DB 回灌最近 20 条历史
    if (memory.messages().isEmpty()) {
        aiSessionService.loadHistoryToMemory(sessionId, memory, 20);
    }
    return memory;
}
```

- `loadHistoryToMemory`：查 `ai_message` 最近 20 条，按时间正序，`role=user → UserMessage.from`，`role=assistant → AiMessage.from`，逐条 `memory.add(...)`。
- 写侧：每次 `chat` 前保存 user 消息到 DB，`chat` 结束后保存 assistant 消息到 DB（记忆本身由 LangChain4j 追加，DB 兜底）。

### 4.3 Redis 配置（与业务 Redis 隔离，避免 key 冲突）

`RedisChatMemoryStore.builder().host().port().password().ttl()` 手动构建，`ttl` 建议 7 天（`604800`）。与业务 Redis（sa-token / PendingAction）可共用同一实例，通过 key 前缀区分（LangChain4j 记忆 key 自带 `chat-memory` 前缀），或单独指定 database 隔离。

### 4.4 会话标题生成（用户明确要求）

- 触发：会话首条用户消息后，`Thread.startVirtualThread`（或 `@Async`）异步调用 `AiTitleService.generateTitle(firstMessage)`。
- 实现：`AiServices.builder(AiTitleService.class).chatModel(titleChatModelPrototype).build()`，接口 `@SystemMessage("你是标题生成器，用不超过 12 字概括用户意图，直接返回标题。")`，结构化输出 `String`。
- 落库：`UPDATE ai_session SET title = ? WHERE session_id = ?`；前端会话列表即时刷新。
- 兜底：标题生成失败（无 key / 超时）则取首条消息前 12 字（对齐参考项目 `initPrompt.substring(0,12)` 的兜底）。

### 4.5 意图路由（可选增强，非 MVP 必做）

`AiIntentRoutingService` 用 `routing-chat-model` 结构化输出判断 `agentType ∈ {data, code}`。前端 `el-segmented` 增加「智能」档（默认仍保留手动 Data/Code 切换）。MVP 可先用手动切换 + 后端 `agentType` 入参，路由模型能力预留。

---

## 5. 数据库设计（核心优化点 ③，原计划缺失）

### 5.1 建表 SQL（`sql/ai_tables.sql` 新增）

```sql
-- AI 会话表
CREATE TABLE IF NOT EXISTS ai_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id  VARCHAR(64)  NOT NULL COMMENT '会话业务ID(UUID)',
    user_id     BIGINT       NOT NULL COMMENT '所属用户',
    agent_type  VARCHAR(16)  NOT NULL DEFAULT 'data' COMMENT 'data/code',
    title       VARCHAR(128) NULL COMMENT '会话标题(AI生成)',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0进行中 1已结束',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(0否1是)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_update (user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话';

-- AI 消息表
CREATE TABLE IF NOT EXISTS ai_message (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id   VARCHAR(64)  NOT NULL COMMENT '会话业务ID',
    user_id      BIGINT       NOT NULL COMMENT '所属用户',
    role         VARCHAR(16)  NOT NULL COMMENT 'user/assistant/tool',
    content      TEXT         NOT NULL COMMENT '文本内容',
    content_type VARCHAR(32)  NOT NULL DEFAULT 'text' COMMENT 'text/photos/html/pending_action',
    extra        JSON         NULL COMMENT '附加数据(照片ids/html/actionId等)',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(0否1是)',
    PRIMARY KEY (id),
    KEY idx_session_time (session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息';
```

> 说明：
> - 本项目 MyBatis-Plus 逻辑删除字段统一为 `deleted`（参考项目 MyBatis-Flex 用 `isDelete`），保持一致。
> - `idx_session_time` 为游标查询核心索引（对齐参考项目 `idx_appId_createTime`），用于历史消息"向前翻页"。
> - `extra` JSON 存照片结果 / HTML / pendingAction 等结构化内容，`content` 存纯文本（供记忆回灌），保证记忆里只有干净的 user/assistant 文本。

### 5.2 实体与 Mapper

- `model/entity/AiSession.java`、`AiMessage.java`（`@TableName`、逻辑删除字段 `@TableLogic deleted`）。
- `mapper/AiSessionMapper.java`、`AiMessageMapper.java`（继承 `BaseMapper`）。
- `resources/mapper/AiSessionMapper.xml`、`AiMessageMapper.xml`（仅当有自定义 SQL，如"按 session_id 查最近 N 条"用 `@Select` 注解或 XML）。

### 5.3 会话/消息服务（`AiSessionService`）

| 方法 | 说明 |
|---|---|
| `createSession(userId, agentType)` | 新建会话，返回 sessionId |
| `listSessions(userId)` | 会话列表（按 update_time 倒序，含 title） |
| `getSession(sessionId, userId)` | 校验归属 + 返回会话 |
| `updateTitle(sessionId, title)` | 标题生成后更新 |
| `addMessage(sessionId, userId, role, content, contentType, extra)` | 追加消息 |
| `loadHistoryToMemory(sessionId, memory, maxCount)` | DB → 记忆回灌（§4.2） |
| `listMessages(sessionId, userId, cursor, size)` | 历史消息游标分页（刷新/翻页用） |

所有方法强制 `userId` 归属校验（越权静默拒绝），对齐原计划 §9 安全约束。

---

## 6. 后端设计（在原计划基础上增量修改）

### 6.1 依赖（pom.xml 变更，替换原计划 §1）

- 新增 `langchain4j-open-ai` + `langchain4j-community-redis`（版本 `1.1.0-beta7`，见 §3.4）。
- 保留 Jackson（`spring-boot-starter-web` 已含）。
- 新增 Caffeine（AI 服务实例缓存，可选）：`com.github.benmanes.caffeine:caffeine`。

### 6.2 配置（application.yml 变更，替换原计划 §2）

- 删除原 `qwen:` 单块，替换为 §3.2 的 `langchain4j.open-ai.*` 多模型块。
- 新增 `ai:` 业务参数：`ai.max-iterations: 4`、`ai.memory-max-messages: 20`、`ai.memory-ttl: 604800`、`ai.size-limit: 50`。

### 6.3 工具（复用原计划 D1/D2，新增 ToolManager）

原计划 `@Tool` 7 工具 + `ToolSpecifications.toolSpecificationsFrom(...)` 手动生成 schema **保留**。新增对齐参考项目的 `ToolManager`：

- `ai/tool/AgentTools.java`：`@Tool` 工具方法（`query_files`/`list_albums`/`generate_html`/`create_album`/`update_album`/`delete_album`/`add_pictures_to_album`）。
- 读/写分类在工具元数据中标注（`isWrite`），供编排层短路判断。
- 幻觉兜底：手动循环里若 `ToolExecutionRequest.name()` 未命中 `ToolManager`，回填 `ToolExecutionResultMessage.from(request, "Error: there is no tool called " + name)`（对齐 `hallucinatedToolNameStrategy`）。
- 工具调用上限：`max-iterations: 4` 保留（对齐 `maxSequentialToolsInvocations` 思想）。

### 6.4 Agent 编排（`AiAgentService`，升级原计划 §4）

```java
// service/AiAgentService.java
AiChatResponse chat(AiChatRequest req, Long userId);
```

`impl/AiAgentServiceImpl` 流程（在原计划有界循环基础上，注入"会话 + 记忆"）：

1. `AiSessionService.getOrCreateSession(sessionId, userId, agentType)`。
2. 保存 user 消息 → `AiMessage`。
3. `AiChatMemoryFactory.buildMemory(sessionId)` 装配记忆（含 DB 回灌）。
4. 按 `agentType` 组装 system prompt（`resources/prompt/data-agent-system-prompt.txt` / `code-agent-system-prompt.txt`）+ 记忆中的历史 messages + 工具 schema。
5. 有界 tool-use 循环（≤ `max-iterations`）：
   - 读工具 → 执行 → 回填 `ToolExecutionResultMessage` → 收集 `photos`/`html` → 继续；
   - 写工具 → 构建 `PendingAction` 存 Redis（`ai:action:{actionId}`，TTL 10min）→ **短路**。
6. 无工具调用 → 总结调用 → `AiChatResponse`。
7. 保存 assistant 消息 → `AiMessage(role=assistant, contentType=...)`。
8. 首条消息 → 异步生成标题（§4.4）。

> 与原计划的关键差异：`messages` 不再由前端全量回传，改为**后端基于 sessionId 从记忆/DB 自动装配**；请求只需携带 `sessionId` + 本轮 `content`（+ 可选 `fileIds`）。

### 6.5 轻量 AI 服务（新增，对齐参考项目 AiServices 用法）

| 服务 | 模型 | 说明 |
|---|---|---|
| `AiTitleService` | `title-chat-model` | `@SystemMessage` 结构化输出标题 |
| `AiIntentRoutingService`（可选） | `routing-chat-model` | 结构化输出 `agentType` 枚举 |

### 6.6 其余服务（原计划保留）

- `AiQueryService` / `AiExecuteService` / DTO（`AiChatRequest` 增加 `sessionId` 字段）、`PendingAction` / `PendingActionType`、`AiController`（新增 `GET/POST /ai/session`、`GET /ai/session/{sessionId}/messages`）—— 全部保留原计划定义，仅补充会话相关接口。

### 6.7 文件清单（增量）

新增：
- `ai/config/AiModelConfig.java`、`ai/config/AiChatMemoryConfig.java`
- `ai/model/*.java`（`TaskModelProperties` 等 4 个属性类）
- `ai/tool/ToolManager.java`
- `ai/memory/AiChatMemoryFactory.java`
- `ai/service/AiTitleService.java`（+ impl）
- `model/entity/AiSession.java`、`AiMessage.java`
- `mapper/AiSessionMapper.java`、`AiMessageMapper.java`
- `service/AiSessionService.java` + `impl/AiSessionServiceImpl.java`
- `resources/prompt/*.txt`（data/code/title system prompt）
- `sql/ai_tables.sql`

修改：
- `pom.xml`（依赖版本修正 + community-redis + caffeine）
- `application.yml` / `application.yml.example`（多模型块替换原 `qwen:` 块）
- 原计划 D4/D5（`FileMapper` 日期过滤、`AlbumController` 返回 albumId）**不变**

---

## 7. 前端设计（在原计划基础上增量修改）

### 7.1 会话管理（新增，替换原计划 §5 "前端维护消息数组"）

- **会话列表**：`AgentChat.vue` 顶部新增会话切换/新建按钮，`GET /ai/session` 拉取会话列表（含 `title`），点击切换会话加载历史消息（`GET /ai/session/{sessionId}/messages` 游标分页）。
- **新建会话**：首次打开或点「新建」→ `POST /ai/session` 返回 `sessionId`。
- **消息发送**：请求体改为 `{ sessionId, agentType, content, fileIds }`，不再回传完整 `messages`。
- **标题展示**：会话列表项显示 AI 生成的 `title`（未生成时显示首条消息前 12 字）。

### 7.2 其余（原计划保留）

- 全局浮动聊天窗、Agent 切换（`el-segmented`，可选「智能」档）、照片上下文条、消息列表、确认卡片（`/ai/execute`）、HTML `<iframe sandbox>` 预览 —— 原计划 §1~§4 逻辑不变。
- API 层 `src/api/ai/ai.ts` 新增 `listSessions` / `createSession` / `listMessages`。
- 类型 `dataType.d.ts` 新增 `AiSession` / `AiSessionListItem`。

---

## 8. 安全与约束（原计划 §9 保留 + 增量）

- userId 一次性获取并透传所有工具与 execute；读工具复用按 userId 过滤的现有方法；`resolveFileIds` / `selectAlbumById` 归属校验，越权静默丢弃。
- **会话归属校验**：`AiSessionService.getSession(sessionId, userId)` 校验 sessionId 归属当前用户，越权抛 404/403，**记忆回灌与消息查询均以 userId 过滤**。
- `PendingAction` Redis + TTL；`/ai/execute` 只接受 `actionId`，校验 actionId 对应 userId。
- 服务端复校验 `albumName≤30`、`description≤80`；`max-iterations` 4、`size≤50`、工具结果截断最小字段、记忆窗口 20 条，控制 token。
- 记忆数据脱敏：仅 user/assistant 纯文本入记忆，照片 URL / HTML / pendingAction 不进记忆（存 `extra`），避免污染上下文与 token 浪费。

---

## 9. 验证方式（在原计划 §验证基础上增量）

1. **多轮回归**：同一 `sessionId` 连续 3 轮对话（"帮我找北京的照片"→"按 2026.8.31 过滤"→"建个相册"），确认第 2/3 轮模型能感知前文（如第二轮的"过滤"作用于第一轮的照片），且刷新页面/重开会话后历史仍在。
2. **跨 Redis 过期**：手动 `FLUSHDB` 或删记忆 key 后重开会话，确认能从 `ai_message` 回灌最近 20 条历史。
3. **多模型验证**：分别用 `routing-chat-model`（意图/标题）、`task-chat-model`（主任务）观察日志确认调用的是不同模型（`log-requests` 开启时打印 modelName）。
4. **标题生成**：首条消息后数秒内会话列表出现 AI 标题；标题模型 key 缺失时回退首条消息前 12 字。
5. **会话隔离**：用户 A 的 `sessionId` 被用户 B 调用 → 拒绝；用户 B 无法看到 A 的会话列表。
6. **既有用例**：原计划 §验证 1~5（data 读/写、code 生成 HTML、安全回归、兼容性）全部保留。

---

## 10. 与原计划差异清单（变更对照）

| # | 原计划 | 优化后 | 依据 |
|---|---|---|---|
| 1 | 前端维护 `messages`，后端无状态，刷新清空 | 后端 `AiSession` + `AiMessage` 持久化，`sessionId` 维度多轮记忆 | 参考项目 `chat_history` + `RedisChatMemoryStore` |
| 2 | 单一 `qwen-plus` | 4 角色多模型（任务/路由/标题/推理），独立配置，供应商混搭 | 参考项目 `routing/streaming/reasoning-chat-model` |
| 3 | 无 AI 建表 | 新增 `ai_session` + `ai_message` | 参考项目 `chat_history` 表 |
| 4 | 无会话标题 | 首条消息后异步用标题模型生成 `title` | 用户明确要求 + 参考项目 appName 兜底 |
| 5 | `langchain4j:1.11.0` | `langchain4j-open-ai` + `langchain4j-community-redis`，版本 `1.1.0-beta7` | 参考项目实际版本（原版本号有误） |
| 6 | 工具直接散在 `AgentTools` | 新增 `ToolManager` 统一注册 + 幻觉兜底 | 参考项目 `ToolManager` |
| 7 | 手动循环无记忆 | 手动循环 + `MessageWindowChatMemory`（窗口 20） | 参考项目 `AiCodeGeneratorServiceFactory` |
| 8 | 单实例 `ChatModel` | `@Scope("prototype")` 多例 + `SpringContextUtil.getBean` | 参考项目并发方案 |
| 9 | 无护栏 | （可选）输入护栏 | 参考项目 `PromptSafetyInputGuardrail` |
| 10 | system prompt 内联 | prompt 外置 `resources/prompt/*.txt` | 参考项目 `@SystemMessage(fromResource)` |

---

## 11. 明确不做（MVP 范围外，与原计划一致）

- `remove_pictures_from_album` 工具、真 text2sql（自由 SQL 执行）。
- 流式输出 SSE（`Flux`/`TokenStream` 可作为下一迭代，参考项目已打通，可平滑引入）。
- 意图「智能路由」档（`routing-chat-model` 能力预留，MVP 用手动切换）。
- 多租户/组织级会话共享、会话导出、消息编辑。
