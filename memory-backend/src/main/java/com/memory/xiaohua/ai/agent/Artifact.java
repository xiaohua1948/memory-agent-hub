package com.memory.xiaohua.ai.agent;

import java.util.List;

/**
 * 工件（Artifact）——本轮对话中产生的可引用数据产物
 *
 * <p>解决的核心问题：模型说「用这些照片」时，「这些」到底指什么？
 * 此前靠 {@code queriedPhotoIds} 一个并集 Set 承载，导致「先查上海照片、
 * 再查家人相册」之后，用户说「用这些照片」会拿到 25 张的混合并集——语义错误
 * 且没有任何一方能察觉。
 *
 * <p>改为显式工件后：
 * <ul>
 *   <li>每次查询产出<b>独立</b>工件 {@code photos#1}、{@code photos#2}，不再合并；</li>
 *   <li>模型引用时写句柄，指代明确无歧义；</li>
 *   <li>工具不再回传裸 fileId 列表，省 token 也根除抄错 ID 的风险。</li>
 * </ul>
 *
 * <p>生命周期：当前编排轮次（存于 {@link AgentToolContext}，请求结束即释放）。
 * 跨轮复用沿用已有的「最近一次查询照片」机制（{@code recentPhotoIds}）。
 *
 * @author xiaohua
 */
public class Artifact {

    private final String ref;
    private final String type;
    private final String summary;
    private final List<String> fileIds;

    public Artifact(String ref, String type, String summary, List<String> fileIds) {
        this.ref = ref;
        this.type = type;
        this.summary = summary;
        this.fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
    }

    /** 句柄，如 photos#1 */
    public String getRef() {
        return ref;
    }

    public String getType() {
        return type;
    }

    /** 给模型看的一句话摘要，如「16张·上海·>2MB」 */
    public String getSummary() {
        return summary;
    }

    public List<String> getFileIds() {
        return fileIds;
    }

    public int size() {
        return fileIds.size();
    }

    @Override
    public String toString() {
        return ref + "（" + summary + "）";
    }
}
