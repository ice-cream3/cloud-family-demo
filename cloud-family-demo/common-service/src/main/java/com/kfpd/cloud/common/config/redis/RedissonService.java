package com.kfpd.cloud.common.config.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.LongCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author mario
 * @ClassName RedissonService
 * @Description Redisson操作工具类
 */
@RequiredArgsConstructor
public class RedissonService {

    private static final List<String> TIMEUNIT = Arrays.asList("DAYS", "HOURS", "MINUTES", "SECONDS", "MILLISECONDS", "MICROSECONDS", "NANOSECONDS");

    private static final Logger log = LoggerFactory.getLogger(RedissonService.class);

    private final RedissonClient redissonClient;

    private final RedisTemplate<Object, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    private final RedissonProperties properties;

    public void set(String key, Object value) {
        redissonClient.getBucket(key).set(value);
    }

    /**
     * 获取锁
     * 【启动锁续期】机制,默认加锁时长30秒
     * @param key key
     */
    public void lock(String key) {
        redissonClient.getLock(key).lock();
    }

    /**
     * 获取锁对象
     * @param key k
     * @return 锁对象
     */
    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    /**
     * 获取锁
     * 不启动锁续期机制,指定leaseTime时长后自动释放锁
     * @param key key
     * @param leaseTime 锁过期时间
     */
    public void lock(String key, long leaseTime) {
        redissonClient.getLock(key).lock(leaseTime, TimeUnit.SECONDS);
    }

    /**
     * 检查此锁是否被任何线程锁定
     * 返回：如果锁定则为 true，否则为 false
     * @param key key
     */
    public boolean isLocked(String key) {
        return redissonClient.getLock(key).isLocked();
    }


    /**
     * 尝试获取锁
     *  不启动锁续期机制,指定leaseTime时长后自动释放锁
     * @param key key
     * @param waitTime 获取锁等待时间,超时如果没获取到锁返回false
     * @param leaseTime 锁过期时间
     * @return 是否获取
     */
    public boolean tryLock(String key, long waitTime, long leaseTime) {
        try {
            return redissonClient.getLock(key).tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        try {
            return redissonClient.getLock(key).tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 尝试获取锁
     *  【启动锁续期】机制,默认加锁时长30秒
     * @param key 锁key
     * @param time 尝试等待时间,超时如果没获取到锁返回false
     * @return 是否获取锁
     */
    public boolean tryLock(String key, long time) {
        try {
            return redissonClient.getLock(key).tryLock(time, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 合并多把业务锁，只有全部获得后才能进入批量操作。
     */
    public RLock getMultiLock(Collection<String> keys) {
        RLock[] locks = keys.stream().map(redissonClient::getLock).toArray(RLock[]::new);
        return redissonClient.getMultiLock(locks);
    }

    /**
     * 尝试获取锁
     *  【启动锁续期】机制,默认加锁时长30秒
     * @param key 锁key
     * @return 是否获取锁
     */
    public boolean tryLock(String key) {
        return redissonClient.getLock(key).tryLock();
    }

    /**
     * 强制解锁：当其他线程持有锁时，使用此方法可以立即释放锁，而不需要等待锁的自然过期或等待解锁操作
     * @param key key
     * @return 是否成功
     */
    public boolean forceUnlock(String key) {
        return redissonClient.getLock(key).forceUnlock();
    }

    /**
     * 释放锁
     * @param key 锁key
     */
    public void unlock(String key) {
        redissonClient.getLock(key).unlock();
    }

    /**
     * 缓存
     *
     * @param time 默认秒
     */
    public void set(String key, Object value, long time) {
        redissonClient.getBucket(key).set(value, Duration.ofSeconds(time));
    }

    /**
     * 缓存
     *
     * @param time 默认秒
     */
    public void setLong(String key, Object value, long time) {
        redissonClient.getBucket(key, LongCodec.INSTANCE).set(value, Duration.ofSeconds(time));
    }

    /**
     * @param timeUnit DAYS 天
     *                 HOURS 小时
     *                 MINUTES 分钟
     *                 SECONDS 秒
     *                 MILLISECONDS 毫秒
     *                 MICROSECONDS 微秒
     *                 NANOSECONDS 纳秒
     */
    public void set(String key, Object value, long time, TimeUnit timeUnit) {
        redissonClient.getBucket(key).set(value, time, timeUnit);
    }

    /**
     * 仅当 key 不存在时写入缓存。
     */
    public boolean setIfAbsent(String key, Object value, long time, TimeUnit timeUnit) {
        return redissonClient.getBucket(key).trySet(value, time, timeUnit);
    }

    public byte[] getBytes(String key) {
        RBucket<byte[]> bucket = redissonClient.getBucket(key, ByteArrayCodec.INSTANCE);
        return bucket.get();
    }

    public void setBytes(String key, byte[] bytes, long timeout, TimeUnit timeUnit) {
        if (bytes == null) {
            return;
        }
        RBucket<byte[]> bucket = redissonClient.getBucket(key, ByteArrayCodec.INSTANCE);
        bucket.set(bytes, timeout, timeUnit);
    }

    public void setBytes(String key, byte[] bytes, long seconds) {
        this.setBytes(key, bytes, seconds, TimeUnit.SECONDS);
    }

    public <T> T get(String key, TypeReference<T> typeReference) {
        Object rawObj = redissonClient.getBucket(key).get();
        if (rawObj == null) {
            return null;
        }
        return objectMapper.convertValue(rawObj, typeReference);
    }

    public <T> T getObject(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        //获取带有原生类型约束的RBucket容器句柄
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public <T> void setByte(String key, T value) {
        if (key == null || value == null) {
            return;
        }
        //获取带有原生类型约束的RBucket容器句柄
        RBucket<T> bucket = redissonClient.getBucket(key);
        //直接存放对象，Redisson 会自动走二进制Codec压缩写入
        bucket.set(value);
    }


    public Object get(String key) {
        return redissonClient.getBucket(key).get();
    }

    /**
     * 获取多key的value
     * @param keys key集合
     * @return list 缓存对象
     */
    public List<Object> get(List<String> keys) {
        return redisTemplate.opsForValue().multiGet(new ArrayList<>(keys));
    }

    /**
     * 获取key的集合
     * @param keyPattern 正则key
     *  如:sys:user:*,获取以sys:user:开头的key集合
     * @return list redis缓存key
     */
    public List<String> getKeys(String keyPattern) {
        Iterable<String> keysByPattern = redissonClient.getKeys().getKeysByPattern(keyPattern);
        List<String> keys = new ArrayList<>();
        keysByPattern.forEach(keys::add);
        return keys;
    }

    public Boolean del(String key) {
        return redissonClient.getBucket(key).delete();
    }

    public long delByPattern(String keyPattern) {
        return redissonClient.getKeys().deleteByPattern(keyPattern);
    }

    public Boolean expire(String key, long time) {
        return expire(key, time, TimeUnit.SECONDS);
    }

    public Boolean expire(String key, long time, TimeUnit timeUnit) {
        return expireObject(redissonClient.getBucket(key), time, timeUnit);
    }

    /**
     * 获取过期时间
     *
     * @return 返回毫秒
     */
    public Long getMillisecondExpire(String key) {
        long remained = redissonClient.getBucket(key).remainTimeToLive();
        if (remained <= 0) {
            return 0L;
        }
        return remained;
    }

    /**
     * 获取过期时间
     *
     * @return 返回秒
     */
    public Long getExpire(String key) {
        long remained = redissonClient.getBucket(key).remainTimeToLive();
        if (remained <= 0) {
            return 0L;
        }
        return (remained / 1000);
    }

    public Boolean hasKey(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 先+1后返回:++i
     * @param key k
     * @return v
     */
    public Long incrAndGet(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    /**
     * 先返回再+1:i++
     * @param key k
     * @return v
     */
    public Long getAndIncr(String key) {
        return redissonClient.getAtomicLong(key).getAndIncrement();
    }

    public Long incrAndGet(String key, long val) {
        return redissonClient.getAtomicLong(key).addAndGet(val);
    }

    public Long getAndIncr(String key, long val) {
        return redissonClient.getAtomicLong(key).getAndAdd(val);
    }

    public Long decrAndGet(String key, long val) {
        return redissonClient.getAtomicLong(key).addAndGet(-val);
    }

    public Long getAndDecr(String key, long val) {
        return redissonClient.getAtomicLong(key).getAndAdd(-val);
    }

    /**
     * 自增操作
     *
     * @param key  自增key
     * @param val  自增val
     * @param time 过期时间
     * @return 自增后的val
     */
    public Long incr(String key, long val, long time) {
        Long result = null;
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            result = atomicLong.addAndGet(val);
            expireObject(atomicLong, time, TimeUnit.SECONDS);
        } catch (Exception e) {
            del(key);
        }
        return result;
    }

    /**
     * 自减操作
     *
     * @param key  自减key
     * @param val  自减val
     * @param time 过期时间
     * @return 自减后的val
     */
    public Long decr(String key, long val, long time) {
        long result = 0;
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            result = atomicLong.addAndGet(-val);
            expireObject(atomicLong, time, TimeUnit.SECONDS);
        } catch (Exception e) {
            del(key);
        }
        return result;
    }

    public Object hGet(String key, String hashKey) {
        return redissonClient.getMap(key).get(hashKey);
    }

    public Boolean hSet(String key, String hashKey, Object value, long time) {
        RMap<Object, Object> map = redissonClient.getMap(key);
        map.put(hashKey, value);
        return expireObject(map, time, TimeUnit.SECONDS);
    }

    public Object hSet(String key, String hashKey, Object value) {
        return redissonClient.getMap(key).put(hashKey, value);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redissonClient.getMap(key).readAllMap();
    }

    public Boolean hSetAll(String key, Map<String, Object> map, long time) {
        RMap<Object, Object> redisMap = redissonClient.getMap(key);
        redisMap.putAll(map);
        return expireObject(redisMap, time, TimeUnit.SECONDS);
    }

    public void hSetAll(String key, Map<String, ?> map) {
        redissonClient.getMap(key).putAll(map);
    }

    public void hDel(String key, String hashKey) {
        redissonClient.getMap(key).remove(hashKey);
    }

    public long hDel(String key, String... hashKey) {
        return redissonClient.getMap(key).fastRemove(hashKey);
    }

    public Boolean hHasKey(String key, String hashKey) {
        return redissonClient.getMap(key).containsKey(hashKey);
    }

    public Set<Object> sMembers(String key) {
        return redissonClient.getSet(key).readAll();
    }

    public boolean sAdd(String key, Object value) {
        return redissonClient.getSet(key).add(value);
    }

    public boolean sAddAll(String key, Set<Object> vals) {
        return redissonClient.getSet(key).addAll(vals);
    }

    public boolean sAdd(String key, Object val, long time) {
        RSet<Object> set = redissonClient.getSet(key);
        set.add(val);
        return expireObject(set, time, TimeUnit.SECONDS);
    }

    public void sAddAll(String key, Set<Object> vals, long time) {
        RSet<Object> set = redissonClient.getSet(key);
        set.addAll(vals);
        expireObject(set, time, TimeUnit.SECONDS);
    }

    public Boolean sIsMember(String key, Object value) {
        return redissonClient.getSet(key).contains(value);
    }

    public int sSize(String key) {
        return redissonClient.getSet(key).size();
    }

    public boolean sRemove(String key, Object val) {
        return redissonClient.getSet(key).remove(val);
    }

    public boolean sRemoveAll(String key, Set<Object> values) {
        return redissonClient.getSet(key).removeAll(values);
    }

    public List<Long> zsetGetLong(String key) {
        return redissonClient.getScoredSortedSet(key).readAll().stream().map(m -> Long.valueOf(String.valueOf(m))).collect(Collectors.toList());
    }

    public List<Integer> zsetGetInteger(String key) {
        return redissonClient.getScoredSortedSet(key).readAll().stream().map(m -> Integer.valueOf(String.valueOf(m))).collect(Collectors.toList());
    }

    public List<String> zsetGetString(String key) {
        return redissonClient.getScoredSortedSet(key).readAll().stream().map(String::valueOf).collect(Collectors.toList());
    }

    public List<Object> zsetGet(String key) {
        return Arrays.asList(redissonClient.getScoredSortedSet(key).readAll().toArray());
    }

    /**
     * zset
     * @param key redis key
     * @param value 元素
     * @param score 得分
     * @param time 过期时间(默认秒)
     * @return 操作状态
     */
    public boolean zsetAdd(String key, Object value, double score, Long time) {
        boolean add = false;
        try {
            add = redissonClient.getScoredSortedSet(key).add(score, value);
            if (add) {
                add = expireObject(redissonClient.getScoredSortedSet(key), time, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("add zset:{} error:{}", key, e.getMessage());
            redissonClient.getBucket(key).delete();
        }
        return add;
    }
    /**
     * zset
     * @param key redis key
     * @param value 元素
     * @param score 得分
     * @param time 过期时间
     * @param timeUnit 时间单位
     * @see TimeUnit
     * @return 操作状态
     */
    public boolean zsetAdd(String key, Object value, double score, Long time, TimeUnit timeUnit) {
        boolean add = false;
        try {
            add = redissonClient.getScoredSortedSet(key).add(score, value);
            if (add) {
                add = expireObject(redissonClient.getScoredSortedSet(key), time, timeUnit);
            }
        } catch (Exception e) {
            log.error("add zset:{} error:{}", key, e.getMessage());
            redissonClient.getBucket(key).delete();
        }
        return add;
    }

    /**
     * zsetAddRetry:设置失败循环重试.直至成功
     * @param key redis key
     * @param value 元素
     * @param score 得分
     * @param time 过期时间(默认秒)
     * @return 操作状态
     */
    public boolean zsetAddRetry(String key, Object value, double score, Long time) {
        boolean add = false;
        int retryTime = 1;
        do {
            try {
                add = redissonClient.getScoredSortedSet(key).add(score, value);
                if (add) {
                    add = expireObject(redissonClient.getScoredSortedSet(key), time, TimeUnit.SECONDS);
                }
                if (retryTime /10 > 0) {
                    Thread.sleep(retryTime * 10 * 1000L);
                }
            } catch (Exception e) {
                log.error("add zsetAddRetry:{},retryTime:{} error:{}", key, retryTime, e.getMessage());
            }
        }while(!add && retryTime++ < 10);
        return add;
    }

    /**
     * zsetAddRetry
     * @param key redis key
     * @param value 元素
     * @param score 得分
     * @param time 过期时间(默认秒)
     * @param retry 重试次数
     * @return 操作状态
     */
    public boolean zsetAddRetry(String key, Object value, double score, Long time, int retry) {
        boolean add = false;
        int retryTime = 1;
        do {
            try {
                add = redissonClient.getScoredSortedSet(key).add(score, value);
                if (add) {
                    add = expireObject(redissonClient.getScoredSortedSet(key), time, TimeUnit.SECONDS);
                }
                if (retryTime /10 > 0) {
                    Thread.sleep(retryTime * 10 * 1000L);
                }
            } catch (Exception e) {
                log.error("retry add zsetAddRetry:{},retryTime:{} error:{}", key, retryTime, e.getMessage());
            }
        }while(!add && retryTime++ < retry);
        return add;
    }

    /**
     * zsetAddRetry
     * @param key redis key
     * @param value 元素
     * @param score 得分
     * @param time 过期时间
     * @param timeUnit 时间单位
     * @see TimeUnit
     * @param retry 重试次数
     * @return 操作状态
     */
    public boolean zsetAddRetry(String key, Object value, double score, Long time, int retry, TimeUnit timeUnit) {
        boolean add = false;
        int retryTime = 1;
        do {
            try {
                add = redissonClient.getScoredSortedSet(key).add(score, value);
                if (add) {
                    add = expireObject(redissonClient.getScoredSortedSet(key), time, timeUnit);
                }
                if (retryTime /10 > 0) {
                    Thread.sleep(retryTime * 10 * 1000L);
                }
            } catch (Exception e) {
                log.error("retry add zsetAddRetry:{},retryTime:{} error:{}", key, retryTime, e.getMessage());
            }
        }while(!add && retryTime++ < retry);
        return add;
    }

    public boolean zsetAdd(String key, Object value, double score) {
        return redissonClient.getScoredSortedSet(key).add(score, value);
    }

    public int zsetAddAll(String key, Map<Object, Double> value) {
        return redissonClient.getScoredSortedSet(key).addAll(value);
    }

    public boolean zsetExists(String key) {
        return redissonClient.getScoredSortedSet(key).isExists();
    }

    public int zsetSize(String key) {
        return redissonClient.getScoredSortedSet(key).size();
    }

    public int zsetRemoveByScore(String key, Double start, Double end, boolean includeStart, boolean includeEnd) {
        return redissonClient.getScoredSortedSet(key).removeRangeByScore(start, includeStart, end, includeEnd);
    }

    public int zsetRemoveByScore(String key, Double start, Double end) {
        return zsetRemoveByScore(key, start, end, true, true);
    }

    public int zsetRemoveByScore(String key, Long start, Long end) {
        return zsetRemoveByScore(key, start.doubleValue(), end.doubleValue(), true, true);
    }

    public boolean zsetRemoveMember(String key, Object... members) {
        var set = redissonClient.getScoredSortedSet(key);
        return set.removeAll(Arrays.asList(members));
    }

    public boolean zsetRemoveMember(String key, List<Object> members) {
        return redissonClient.getScoredSortedSet(key).removeAll(members);
    }

    public List<Object> lReadAll(String key) {
        return redissonClient.getList(key).readAll();
    }

    public int lSize(String key) {
        return redissonClient.getList(key).size();
    }

    public Object lIndex(String key, int index) {
        return redissonClient.getList(key).get(index);
    }

    public List<Object> lSubList(String key, int start, int end) {
        return redissonClient.getList(key).subList(start, end);
    }

    public boolean lContains(String key, Object obj) {
        return redissonClient.getList(key).contains(obj);
    }

    public boolean lContainsAll(String key, List<Object> objs) {
        return redissonClient.getList(key).containsAll(objs);
    }

    public int lIndexOf(String key, Object obj) {
        return redissonClient.getList(key).indexOf(obj);
    }

    public int lLastIndexOf(String key, Object obj) {
        return redissonClient.getList(key).lastIndexOf(obj);
    }

    public boolean lAdd(String key, Object value) {
        return redissonClient.getList(key).add(value);
    }

    public boolean lAdd(String key, Object value, long time) {
        boolean add = false;
        try {
            add = redissonClient.getList(key).add(value);
            return expireObject(redissonClient.getList(key), time, TimeUnit.SECONDS);
        } catch (Exception e){
            del(key);
        }
        return add;
    }

    public boolean lAddAll(String key, List<Object> values) {
        return redissonClient.getList(key).addAll(values);
    }

    public int lAddAfter(String key, Object elementToFind, Object element) {
        return redissonClient.getList(key).addAfter(elementToFind, element);
    }

    public int lAddBefore(String key, Object elementToFind, Object element) {
        return redissonClient.getList(key).addBefore(elementToFind, element);
    }

    public void lAddIndex(String key, int index, Object element) {
        redissonClient.getList(key).add(index, element);
    }

    public boolean lRemove(String key, Object value) {
        return redissonClient.getList(key).remove(value);
    }

    public boolean lRemoveAll(String key, List<Object> values) {
        return redissonClient.getList(key).removeAll(values);
    }

    public <T> Optional<T> tryExecute(String name, Supplier<T> supplier) {
        return tryExecute(name, properties.getWaitTimeSeconds(), properties.getLeaseTimeSeconds(), TimeUnit.SECONDS, supplier);
    }

    public <T> Optional<T> tryExecute(String name, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        boolean locked = tryLock(lockKey(name), waitTime, leaseTime, unit);
        if (!locked) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(supplier.get());
        } finally {
            unlock(lockKey(name));
        }
    }

    public <T> T execute(String name, Supplier<T> supplier) {
        boolean locked = tryLock(lockKey(name), properties.getWaitTimeSeconds(), properties.getLeaseTimeSeconds(), TimeUnit.SECONDS);
        if (!locked) {
            throw new IllegalStateException("Failed to acquire redis lock: " + lockKey(name));
        }
        try {
            return supplier.get();
        } finally {
            unlock(lockKey(name));
        }
    }

    public void execute(String name, Runnable runnable) {
        execute(name, () -> {
            runnable.run();
            return null;
        });
    }

    private String lockKey(String name) {
        String normalizedName = org.springframework.util.StringUtils.hasText(name) ? name.trim() : "default";
        String prefix = org.springframework.util.StringUtils.hasText(properties.getKeyPrefix()) ? properties.getKeyPrefix().trim() : "lock";
        return prefix + ":" + normalizedName;
    }

    private boolean expireObject(RExpirable expirable, long time, TimeUnit timeUnit) {
        if (time <= 0) {
            return expirable.delete();
        }
        return expirable.expire(toDuration(time, timeUnit));
    }

    private Duration toDuration(long time, TimeUnit timeUnit) {
        TimeUnit unit = timeUnit == null ? TimeUnit.SECONDS : timeUnit;
        return Duration.ofNanos(unit.toNanos(time));
    }

}
