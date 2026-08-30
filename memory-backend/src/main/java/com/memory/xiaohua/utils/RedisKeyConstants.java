package com.memory.xiaohua.utils;

/**
 * Redis key 前缀常量（修复 #20：集中管理魔法值，避免散落各处拼写不一致）
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /** 邮箱验证码 */
    public static final String EMAIL_CODE = "email:code:";

    /** 邮箱验证码发送频率锁 */
    public static final String EMAIL_LOCK = "email:lock:";

    /** 分享链接 */
    public static final String SHARE_LINK = "share:link:";

    /** 临时下载令牌 */
    public static final String TEMP_DOWNLOAD = "tempDownload:";
}
