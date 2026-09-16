package com.kfpd.cloud.common.config.rocketmq;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.util.StringUtils;

public class RocketMqService {

    private final DefaultMQProducer producer;

    public RocketMqService(DefaultMQProducer producer) {
        this.producer = producer;
    }

    public SendResult syncSend(String topic, String body) throws Exception {
        return syncSend(topic, null, null, body);
    }

    public SendResult syncSend(String topic, String tag, String key, String body) throws Exception {
        Message message = new Message(topic, body.getBytes(StandardCharsets.UTF_8));
        if (StringUtils.hasText(tag)) {
            message.setTags(tag);
        }
        if (StringUtils.hasText(key)) {
            message.setKeys(key);
        }
        return producer.send(message);
    }
}
