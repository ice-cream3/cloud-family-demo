package com.kfpd.cloud.common.utils;

import java.time.Duration;
import java.util.Set;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.util.StringUtils;

public class RedisUtils {

    private final RedissonClient redissonClient;

    public RedisUtils(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> T get(String key) {
        return this.<T>bucket(key).get();
    }

    public <T> void set(String key, T value) {
        bucket(key).set(value);
    }

    public <T> void set(String key, T value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            set(key, value);
            return;
        }
        bucket(key).set(value, ttl);
    }

    public boolean exists(String key) {
        return bucket(key).isExists();
    }

    public boolean expire(String key, Duration ttl) {
        requirePositiveDuration(ttl, "ttl must be positive");
        return bucket(key).expire(ttl);
    }

    public boolean delete(String key) {
        return bucket(key).delete();
    }

    public <T> boolean addToSet(String key, T value) {
        return set(key).add(value);
    }

    public <T> boolean removeFromSet(String key, T value) {
        return set(key).remove(value);
    }

    public <T> Set<T> readSet(String key) {
        return this.<T>set(key).readAll();
    }

    public boolean deleteSet(String key) {
        return set(key).delete();
    }

    public long incrementAndGet(String key) {
        return atomicLong(key).incrementAndGet();
    }

    public long addAndGet(String key, long delta) {
        return atomicLong(key).addAndGet(delta);
    }

    public long getAtomicLong(String key) {
        return atomicLong(key).get();
    }

    public boolean expireAtomicLong(String key, Duration ttl) {
        requirePositiveDuration(ttl, "ttl must be positive");
        return atomicLong(key).expire(ttl);
    }

    public boolean deleteAtomicLong(String key) {
        return atomicLong(key).delete();
    }

    private <T> RBucket<T> bucket(String key) {
        return redissonClient.getBucket(requireKey(key));
    }

    private <T> RSet<T> set(String key) {
        return redissonClient.getSet(requireKey(key));
    }

    private RAtomicLong atomicLong(String key) {
        return redissonClient.getAtomicLong(requireKey(key));
    }

    private String requireKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("redis key must not be blank");
        }
        return key.trim();
    }

    private void requirePositiveDuration(Duration duration, String message) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(message);
        }
    }
}
