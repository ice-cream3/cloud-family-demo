package com.kfpd.cloud.common.utils;

import com.kfpd.cloud.common.constant.CacheKeyConsts;

/**
 * @ClassName: RedisKeyUtil
 * @Description:redis业务key统一管理
 */
public class RedisKeyUtil {

    public static String redisKeyDemo(Integer sign) {
        return CacheKeyUtil.getJoinKey(CacheKeyConsts.DEMO_KEY, ":", String.valueOf(sign));
    }

}
