-- AI 智能助手建表 SQL
-- 对齐《AI智能助手-优化计划.md》§5.1 数据库设计
-- 说明：MyBatis-Plus 逻辑删除字段统一为 deleted（0 否 1 是），与项目全局配置一致。

-- AI 会话表
CREATE TABLE IF NOT EXISTS ai_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id  VARCHAR(64)  NOT NULL COMMENT '会话业务ID(UUID)',
    user_id     BIGINT       NOT NULL COMMENT '所属用户',
    agent_type  VARCHAR(16)  NOT NULL DEFAULT 'data' COMMENT 'data/code/mixed',
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
    content      TEXT         NOT NULL COMMENT '文本内容(纯文本,供记忆回灌)',
    content_type VARCHAR(32)  NOT NULL DEFAULT 'text' COMMENT 'text/photos/html/pending_action',
    extra        JSON         NULL COMMENT '附加数据(照片ids/html/actionId等)',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(0否1是)',
    PRIMARY KEY (id),
    KEY idx_session_time (session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息';

-- AI 待确认写操作表（human-in-the-loop 二次确认，Redis 为主、DB 兜底审计）
CREATE TABLE IF NOT EXISTS ai_pending_action (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    action_id    VARCHAR(64)  NOT NULL COMMENT '待确认操作ID(UUID)',
    session_id   VARCHAR(64)  NOT NULL COMMENT '会话业务ID',
    user_id      BIGINT       NOT NULL COMMENT '所属用户',
    action_type  VARCHAR(32)  NOT NULL COMMENT 'CREATE_ALBUM/ADD_PICTURES/REMOVE_PICTURES/UPDATE_ALBUM/DELETE_ALBUM',
    exec_params  JSON         NULL COMMENT '执行参数JSON',
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/CANCELLED/EXPIRED',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    confirm_time DATETIME     NULL COMMENT '确认时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_action_id (action_id),
    KEY idx_session_action (session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI待确认操作';
