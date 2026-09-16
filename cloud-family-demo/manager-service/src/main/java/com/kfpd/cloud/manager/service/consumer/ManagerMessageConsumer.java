package com.kfpd.cloud.manager.service.consumer;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.kfpd.cloud.common.config.rocketmq.RocketMqService;

import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnBean(RocketMqService.class)
public class ManagerMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(ManagerMessageConsumer.class);

    private final RocketMqService rocketMqService;

    @Value("${demo.rocketmq.manager-consumer.topic:manager-topic}")
    private String topic;

    @Value("${demo.rocketmq.manager-consumer.tag:*}")
    private String tag;

    @Value("${demo.rocketmq.manager-consumer.group:manager-service-consumer}")
    private String consumerGroup;

    public ManagerMessageConsumer(RocketMqService rocketMqService) {
        this.rocketMqService = rocketMqService;
    }

    @PostConstruct
    public void subscribe() throws MQClientException {
        rocketMqService.subscribe(consumerGroup, topic, tag, (messages, context) -> {
            for (MessageExt message : messages) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                log.info("Manager service consumed RocketMQ message. topic={}, tags={}, keys={}, msgId={}, body={}",
                        message.getTopic(), message.getTags(), message.getKeys(), message.getMsgId(), body);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        log.info("Manager service RocketMQ consumer subscribed. group={}, topic={}, tag={}", consumerGroup, topic, tag);
    }
}
