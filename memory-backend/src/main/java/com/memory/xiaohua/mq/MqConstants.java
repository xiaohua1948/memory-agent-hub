package com.memory.xiaohua.mq;

/**
 * Apache RocketMQ 通用常量。
 * 后续任意异步任务（彻底删除、缩略图生成、AI 识别……）都可复用本 topic/consumerGroup 规范，
 * 只需新增自己的 topic 与 consumerGroup 即可，无需再引入新中间件。
 *
 * <p>RocketMQ 与 RabbitMQ 不同：队列无需手动声明，topic 在首次发送时自动创建（autoCreateTopicEnable=true），
 * 消费失败默认重试 max-reconsume-times 次后自动进入 %DLQ%+consumerGroup 死信队列，便于排查。</p>
 */
public final class MqConstants {

    /** 文件相关通用 topic（所有文件异步任务共用，按 tag 区分具体动作） */
    public static final String TOPIC_FILE = "memory-file-topic";

    /** 文件彻底删除 tag（用于消费者过滤） */
    public static final String TAG_FILE_DELETE = "file-delete";

    /** 文件彻底删除消费者组 */
    public static final String CONSUMER_GROUP_FILE_DELETE = "memory-file-delete-group";

    // ==================== 上传后异步分析（事件解耦） ====================
    // 上传主流程只完成"存对象 + 落库"，随后投递分析事件；
    // 下列三类耗时分析由各自独立的消费者组按 tag 消费，互不阻塞、互不影响，
    // 任一分析失败只影响该消费者组自身（重试/死信），不会拖慢或回滚上传。

    /** 逆地理编码 tag：经纬度 → 结构化地址 */
    public static final String TAG_FILE_GEOCODE = "file-geocode";

    /** 逆地理编码消费者组 */
    public static final String CONSUMER_GROUP_FILE_GEOCODE = "memory-file-geocode-group";

    /** 图像分类识别 tag：缩略图 → AI 标签 */
    public static final String TAG_FILE_IMAGE_TAG = "file-image-tag";

    /** 图像分类识别消费者组 */
    public static final String CONSUMER_GROUP_FILE_IMAGE_TAG = "memory-file-image-tag-group";

    /** 人脸检测 tag：缩略图 → 人脸特征 / 人物聚类 */
    public static final String TAG_FILE_FACE = "file-face";

    /** 人脸检测消费者组 */
    public static final String CONSUMER_GROUP_FILE_FACE = "memory-file-face-group";

    private MqConstants() {
    }
}
