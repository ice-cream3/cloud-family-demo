package com.kfpd.cloud.common.utils;

import com.kfpd.cloud.common.constant.RedisKeyConsts;

/**
 * @ClassName: RedisKeyUtil
 * @Description:redis业务key统一管理
 */
public class RedisKeyUtil {

    public static String redisKeyDemo(Integer sign) {
        return RedisKeyConsts.getJoinKey(RedisKeyConsts.DEMO_KEY, ":", String.valueOf(sign));
    }

}
