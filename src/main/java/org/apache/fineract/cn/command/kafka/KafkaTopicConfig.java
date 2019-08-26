package org.apache.fineract.cn.command.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value(value = "${kafka.bootstrapAddress:localhost:2181}")
    private String bootstrapAddress;
    private static final Integer NUM_PARTITIONS = 3;
    private static final Short REPLICATION_FACTOR = 1;

    // TODO parametrizar topics por cloud config
    @Value(value = "${kafka.topic.customer:topic_customer}")
    private String topicCustomer;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);

        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic topicCustomer() {
        return new NewTopic(topicCustomer, NUM_PARTITIONS, REPLICATION_FACTOR);
    }
}
