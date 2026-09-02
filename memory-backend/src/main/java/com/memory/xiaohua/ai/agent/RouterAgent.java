package com.memory.xiaohua.ai.agent;

import com.memory.xiaohua.ai.config.AiProperties;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Router Agent —— 基于轻量模型做意图分诊
 *
 * <p>将用户复合请求拆解为带依赖关系的有序子任务 {@link AgentPlan}，输出纯 JSON。
 * 轻量模型（deepseek-chat，max-tokens=100）只负责「结构化分诊」，主任务交给主模型。
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class RouterAgent {

    private final AiProperties properties;

    @Autowired(required = false)
    @Qualifier("routerModel")
    private ChatModel routerModel;

    public RouterAgent(AiProperties properties) {
        this.properties = properties;
    }

    /**
     * 意图分诊：输出执行计划 JSON。
     *
     * @param userInput 本轮用户输入
     * @param history   历史对话文本（多轮指代上下文，可为空）
     */
    public String route(String userInput, String history) {
        if (routerModel == null) {
            // 无路由模型时降级：单步 MIXED
            return null;
        }
        String prompt = """
                你是智能相册助手的意图路由器。将用户输入拆解为若干有序子任务，输出 JSON 数组。
                可用子任务类型（agent 字段）：
                - QUERY：查询照片（纯读）
                - ALBUM：创建/修改/删除相册或增删照片（含写操作）
                - CODE：生成 HTML 网页（纯读）

                输出格式（只输出 JSON，不要解释）：
                [{"step":1,"agent":"QUERY","input":"...","dependsOn":null,"write":false}, ...]

                规则：
                1. step 从 1 递增；dependsOn 指依赖的前置 step（无依赖为 null）。
                2. 写操作（建/改/删相册）write=true，其余 false。
                3. 无法确定的请求，输出空数组 []。
                4. 若用户使用了「刚才/这些/它」等指代词，须结合历史对话解析指代对象。

                历史对话：
                %s

                用户输入：%s
                """.formatted(history == null ? "" : history, userInput);

        return routerModel.chat(prompt);
    }
}
