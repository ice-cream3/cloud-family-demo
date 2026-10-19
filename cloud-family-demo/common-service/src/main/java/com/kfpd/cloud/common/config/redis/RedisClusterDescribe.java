package com.kfpd.cloud.common.config.redis;

/**
 * redis集群安装:
 *     mkdir -p ~/redis-cluster
 *     cd ~/redis-cluster
 *     cd ~/redis-cluster
 *     生成脚本(脚本使用时要统一顶格输写,避免有语法问题):
 *         for port in {7000..7005}; do
 *         mkdir -p "/usr/local/redis-cluster/$port"
 *
 *         cat > "/usr/local/redis-cluster/$port/redis.conf" <<EOF
 *         bind 127.0.0.1
 *         port $port
 *         daemonize yes
 *         protected-mode yes
 *
 *         # Redis Cluster
 *         cluster-enabled yes
 *         cluster-config-file nodes.conf
 *         cluster-node-timeout 5000
 *
 *         # 认证配置
 *         requirepass 123456
 *         masterauth 123456
 *
 *         # 持久化
 *         appendonly yes
 *         dir /usr/local/redis-cluster/$port
 *         logfile /usr/local/redis-cluster/$port/redis.log
 *         pidfile /usr/local/redis-cluster/$port/redis.pid
 *         EOF
 *
 *         done
 *
 *     启动 6 个 Redis 节点:
 *         for port in {7000..7005}; do
 *             redis-server /usr/local/redis-cluster/$port/redis.conf
 *         done
 *     检查是否启动：
 *         for port in {7000..7005}; do
 *             redis-cli -p $port ping
 *         done
 *     创建集群:
 *         redis-cli -a 123456 --cluster create \
 *             127.0.0.1:7000 \
 *             127.0.0.1:7001 \
 *             127.0.0.1:7002 \
 *             127.0.0.1:7003 \
 *             127.0.0.1:7004 \
 *             127.0.0.1:7005 \
 *             --cluster-replicas 1
 *         参数 --cluster-replicas 1 表示每个主节点配置一个副本节点
 *         如果执行报错:[ERR] Node 127.0.0.1:7001 is not empty. Either the node already knows other nodes (check with CLUSTER NODES) or contains some key in database 0.
 *         则需要清理节点缓存:
 *             redis-cli -a 123456 -p 7000 FLUSHALL
 *             redis-cli -a 123456 -p 7000 CLUSTER RESET
 *             redis-cli -a 123456 -p 7001 FLUSHALL
 *             redis-cli -a 123456 -p 7001 CLUSTER RESET
 *             redis-cli -a 123456 -p 7002 FLUSHALL
 *             redis-cli -a 123456 -p 7002 CLUSTER RESET
 *             redis-cli -a 123456 -p 7003 FLUSHALL
 *             redis-cli -a 123456 -p 7003 CLUSTER RESET
 *             redis-cli -a 123456 -p 7004 FLUSHALL
 *             redis-cli -a 123456 -p 7004 CLUSTER RESET
 *             redis-cli -a 123456 -p 7005 FLUSHALL
 *             redis-cli -a 123456 -p 7005 CLUSTER RESET
 *     关闭全部 Redis 节点：
 *         for port in {7000..7005}; do
 *             redis-cli -p $port shutdown
 *         done
 *
 *     重新启动：
 *         for port in {7000..7005}; do
 *             redis-server /usr/local/redis-cluster/$port/redis.conf
 *         done
 *
 *     已经创建的集群正常关闭后，再次启动不需要重新执行 --cluster create
 *
 *     验证节点:
 *          redis-cli -a 123456 -c -h 127.0.0.1 -p 7000
 */
public class RedisClusterDescribe {

}
