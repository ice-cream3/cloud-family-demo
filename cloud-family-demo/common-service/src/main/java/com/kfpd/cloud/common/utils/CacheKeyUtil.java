package com.kfpd.cloud.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * CacheKeyUtil
 * @Description 缓存工具类
 */
public class CacheKeyUtil {

    /**
     * @param keys 数组类型
     *             按照顺序放置需要拼装的成员，例如：new String[]{"val1","val2","val3"},结果：val1+val2+val3
     * @return key
     * @Description 生成缓存key
     */
    public static String getJoinKey(String... keys) {
        StringBuffer buffer = new StringBuffer();
        if (keys != null && keys.length > 0) {
            Arrays.stream(keys).forEach(e -> {
                if (StringUtils.isNotBlank(e)) {
                    buffer.append(e);
                }
            });
        }
        return buffer.toString().toUpperCase();
    }

    public static String getJoinKey(String separator, String[] keys) {
        StringBuffer buffer = new StringBuffer();
        if (keys != null && keys.length > 0) {
            Arrays.stream(keys).forEach(e -> {
                if (StringUtils.isNotBlank(e)) {
                    buffer.append(e).append(separator);
                }
            });
        }
        return buffer.substring(0, buffer.length()-1).toUpperCase();
    }

    /**
     * @param prefixKey 前缀key
     * @param orderNo   订单号
     * @return
     * @Description 获取生成订单缓存锁的key
     */
    public static String getGeneratorOrderKey(String prefixKey, String orderNo) {
        StringBuilder buffer = new StringBuilder(prefixKey);
        if (!prefixKey.endsWith(":")) {
            buffer.append(":");
        }
        buffer.append(orderNo);
        return buffer.toString().toUpperCase();
    }

}
