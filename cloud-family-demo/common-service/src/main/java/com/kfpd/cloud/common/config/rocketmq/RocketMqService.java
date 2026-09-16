package com.kfpd.cloud.common.config.rocketmq;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class RocketMqService {

    private final DefaultMQProducer producer;
    private final RocketMqProperties properties;
    private final List<DefaultMQPushConsumer> consumers = new CopyOnWriteArrayList<>();

    public RocketMqService(DefaultMQProducer producer, RocketMqProperties properties) {
        this.producer = producer;
        this.properties = properties;
    }

    public SendResult syncSend(String topic, String body)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return syncSend(topic, null, null, body);
    }

    public SendResult syncSend(String topic, String tag, String key, String body)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return syncSend(topic, tag, key, body, StandardCharsets.UTF_8);
    }

    public SendResult syncSend(String topic, String tag, String key, String body, Charset charset)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return producer.send(buildMessage(topic, tag, key, body, charset));
    }

    public void asyncSend(String topic, String body, SendCallback callback)
            throws MQClientException, RemotingException, InterruptedException {
        asyncSend(topic, null, null, body, callback);
    }

    public void asyncSend(String topic, String tag, String key, String body, SendCallback callback)
            throws MQClientException, RemotingException, InterruptedException {
        asyncSend(topic, tag, key, body, StandardCharsets.UTF_8, callback);
    }

    public void asyncSend(String topic, String tag, String key, String body, Charset charset, SendCallback callback)
            throws MQClientException, RemotingException, InterruptedException {
        Assert.notNull(callback, "SendCallback must not be null");
        producer.send(buildMessage(topic, tag, key, body, charset), callback);
    }

    public void oneWaySend(String topic, String body)
            throws MQClientException, RemotingException, InterruptedException {
        oneWaySend(topic, null, null, body);
    }

    public void oneWaySend(String topic, String tag, String key, String body)
            throws MQClientException, RemotingException, InterruptedException {
        oneWaySend(topic, tag, key, body, StandardCharsets.UTF_8);
    }

    public void oneWaySend(String topic, String tag, String key, String body, Charset charset)
            throws MQClientException, RemotingException, InterruptedException {
        producer.sendOneway(buildMessage(topic, tag, key, body, charset));
    }

    public DefaultMQPushConsumer subscribe(String topic, MessageListenerConcurrently listener) throws MQClientException {
        return subscribe(properties.getConsumerGroup(), topic, "*", listener);
    }

    public DefaultMQPushConsumer subscribe(String topic, String tagExpression, MessageListenerConcurrently listener)
            throws MQClientException {
        return subscribe(properties.getConsumerGroup(), topic, tagExpression, listener);
    }

    public DefaultMQPushConsumer subscribe(String consumerGroup, String topic, String tagExpression,
            MessageListenerConcurrently listener) throws MQClientException {
        Assert.hasText(consumerGroup, "Consumer group must not be blank");
        Assert.hasText(topic, "Topic must not be blank");
        Assert.notNull(listener, "MessageListenerConcurrently must not be null");

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(properties.getNamesrvAddr());
        consumer.setConsumeThreadMin(properties.getConsumeThreadMin());
        consumer.setConsumeThreadMax(properties.getConsumeThreadMax());
        consumer.setConsumeMessageBatchMaxSize(properties.getConsumeMessageBatchMaxSize());
        consumer.subscribe(topic, StringUtils.hasText(tagExpression) ? tagExpression : "*");
        consumer.registerMessageListener(listener);
        consumer.start();
        consumers.add(consumer);
        return consumer;
    }

    public void shutdown() {
        for (DefaultMQPushConsumer consumer : consumers) {
            consumer.shutdown();
        }
        consumers.clear();
    }

    private Message buildMessage(String topic, String tag, String key, String body, Charset charset) {
        Assert.hasText(topic, "Topic must not be blank");
        Assert.notNull(body, "Message body must not be null");
        Charset messageCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        Message message = new Message(topic, body.getBytes(messageCharset));
        if (StringUtils.hasText(tag)) {
            message.setTags(tag);
        }
        if (StringUtils.hasText(key)) {
            message.setKeys(key);
        }
        return message;
    }
}
