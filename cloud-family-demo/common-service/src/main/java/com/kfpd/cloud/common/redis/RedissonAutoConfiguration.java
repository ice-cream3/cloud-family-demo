package com.kfpd.cloud.common.redis;

import java.util.List;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "spring.data.redis.cluster", name = "nodes")
public class RedissonAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    RedissonClient redissonClient(@Value("${spring.data.redis.cluster.nodes}") List<String> nodes,
                                  @Value("${spring.data.redis.password:}") String password) {
        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers();
        clusterServersConfig.addNodeAddress(nodes.stream()
                .map(this::redisAddress)
                .toArray(String[]::new));
        if (StringUtils.hasText(password)) {
            clusterServersConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    RedissonReactiveClient redissonReactiveClient(RedissonClient redissonClient) {
        return redissonClient.reactive();
    }

    private String redisAddress(String node) {
        String trimmed = node.trim();
        if (trimmed.startsWith("redis://") || trimmed.startsWith("rediss://")) {
            return trimmed;
        }
        return "redis://" + trimmed;
    }
}
