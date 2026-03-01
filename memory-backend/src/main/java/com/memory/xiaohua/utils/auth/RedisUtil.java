package com.memory.xiaohua.utils.auth;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/7,20:10
 */
@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置键值对。
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value);
    }

    /**
     * 设置键值对，并设置过期时间。
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位（秒、分钟、小时等）
     */
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, timeout, timeUnit);
    }

    public Long hashIncrement(String key, String field, long increment) {
        return redisTemplate.opsForHash().increment(key, field, increment);
    }


        /**
         * 设置整个Hash表并设置过期时间
         * @param key Redis键
         * @param map Hash数据
         * @param timeout 过期时间
         * @param unit 时间单位
         */
        public void hmset(String key, Map<String, ?> map, long timeout, TimeUnit unit) {
            redisTemplate.opsForHash().putAll(key, map);
            redisTemplate.expire(key, timeout, unit);
        }

        /**
         * 获取整个Hash表（String值）
         */
        public Map<String, String> hgetAllAsString(String key) {
            return redisTemplate.<String, String>opsForHash().entries(key);
        }


    /**
     * 如果键不存在，则设置键值对。
     *
     * @param key   键
     * @param value 值
     * @return 是否设置成功
     */
    public boolean setIfAbsent(String key, Object value) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.setIfAbsent(key, value);
    }

    /**
     * 如果键不存在，则设置键值对，并设置过期时间。
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位（秒、分钟、小时等）
     * @return 是否设置成功
     */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit timeUnit) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.setIfAbsent(key, value, timeout, timeUnit);
    }

    /**
     * 获取键对应的值。
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    /**
     * 删除指定的键。
     *
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 判断键是否存在。
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取键的剩余生存时间。
     *
     * @param key 键
     * @return 剩余生存时间（毫秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
    }

    /**
     * 设置哈希表中的字段值。
     *
     * @param key   键
     * @param field 字段
     * @param value 值
     */
    public void hset(String key, String field, Object value) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        ops.put(key, field, value);
    }

    /**
     * 获取哈希表中的字段值。
     *
     * @param key   键
     * @param field 字段
     * @return 字段值
     */
    public Object hget(String key, String field) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        return ops.get(key, field);
    }

    /**
     * 获取哈希表中的所有字段和值。
     *
     * @param key 键
     * @return 字段和值的映射
     */
    public Map<String, Object> hgetAll(String key) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }

    /**
     * 删除哈希表中的字段。
     *
     * @param key   键
     * @param field 字段
     */
    public void hdel(String key, String field) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        ops.delete(key, field);
    }

    /**
     * 获取集合中的所有元素。
     *
     * @param key 键
     * @return 元素集合
     */
    public Set<Object> sGet(String key) {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        return ops.members(key);
    }

    /**
     * 向集合中添加元素。
     *
     * @param key   键
     * @param value 值
     */
    public void sAdd(String key, Object value) {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        ops.add(key, value);
    }

    /**
     * 从集合中删除元素。
     *
     * @param key   键
     * @param value 值
     */
    public void sRemove(String key, Object value) {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        ops.remove(key, value);
    }

    /**
     * 获取列表中的所有元素。
     *
     * @param key 键
     * @return 元素列表
     */
    public List<Object> lGet(String key) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.range(key, 0, -1);
    }

    /**
     * 向列表中添加元素。
     *
     * @param key   键
     * @param value 值
     */
    public void lPush(String key, Object value) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        ops.leftPush(key, value);
    }

    /**
     * 从列表中删除元素。
     *
     * @param key   键
     * @param value 值
     */
    public void lRemove(String key, Object value) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        ops.remove(key, 0, value);
    }

    /**
     * 获取列表中的元素数量。
     *
     * @param key 键
     * @return 元素数量
     */
    public long lSize(String key) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.size(key);
    }

    /**
     * 获取集合中的元素数量。
     *
     * @param key 键
     * @return 元素数量
     */
    public long sSize(String key) {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        return ops.size(key);
    }

    /**
     * 获取哈希表中的字段数量。
     *
     * @param key 键
     * @return 字段数量
     */
    public long hSize(String key) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        return ops.size(key);
    }

    /**
     * 清空哈希表中的所有字段。
     *
     * @param key 键
     */
    public void hClear(String key) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        ops.delete(key, (String[]) ops.keys(key).toArray(new String[0]));
    }

    /**
     * 清空集合中的所有元素。
     *
     * @param key 键
     */
    public void sClear(String key) {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        ops.remove(key, (Object[]) ops.members(key).toArray());
    }

    /**
     * 清空列表中的所有元素。
     *
     * @param key 键
     */
    public void lClear(String key) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        while (ops.size(key) > 0) {
            ops.remove(key, 0, ops.range(key, 0, 0).get(0));
        }
    }
}
