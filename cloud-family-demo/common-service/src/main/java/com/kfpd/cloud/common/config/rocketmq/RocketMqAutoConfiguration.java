package com.kfpd.cloud.common.config.rocketmq;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(MQProducer.class)
@EnableConfigurationProperties(RocketMqProperties.class)
public class RocketMqAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "demo.rocketmq", name = "enabled", havingValue = "true")
    DefaultMQProducer rocketMqProducer(RocketMqProperties properties) throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer(properties.getProducerGroup());
        producer.setNamesrvAddr(properties.getNamesrvAddr());
        producer.setSendMsgTimeout(properties.getSendMessageTimeoutMillis());
        producer.setRetryTimesWhenSendFailed(properties.getRetryTimesWhenSendFailed());
        producer.start();
        return producer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "demo.rocketmq", name = "enabled", havingValue = "true")
    RocketMqService rocketMqService(DefaultMQProducer producer) {
        return new RocketMqService(producer);
    }
}
