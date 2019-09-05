/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

    // TODO parametrizar TODO por cloud config
    @Value(value = "${kafka.bootstrapAddress:http://kafka.default:9092}")
    private String bootstrapAddress;

    @Value(value = "${kafka.num.partitions:3}")
    private Integer numPartitions;

    @Value(value = "${kafka.replica.factor:1}")
    private Short replicaFactor;


    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);

        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic topicCustomer() {
        return new NewTopic(KafkaTopicConstants.TOPIC_CUSTOMER, numPartitions, replicaFactor);
    }

    @Bean
    public NewTopic topicErrorCustomer() {
        return new NewTopic(KafkaTopicConstants.TOPIC_ERROR_CUSTOMER, numPartitions, replicaFactor);
    }

    @Bean
    public NewTopic topicProductDeposit() {
        return new NewTopic(KafkaTopicConstants.TOPIC_PRODUCT_DEPOSIT, numPartitions, replicaFactor);
    }

    @Bean
    public NewTopic topicErrorProductDeposit() {
        return new NewTopic(KafkaTopicConstants.TOPIC_ERROR_PRODUCT_DEPOSIT, numPartitions, replicaFactor);
    }

    @Bean
    public NewTopic topicIdentityUser() {
        return new NewTopic(KafkaTopicConstants.TOPIC_IDENTITY_USER, numPartitions, replicaFactor);
    }

    @Bean
    public NewTopic topicErrorIdentityUser() {
        return new NewTopic(KafkaTopicConstants.TOPIC_ERROR_IDENTITY_USER, numPartitions, replicaFactor);
    }

    @Bean
    public NewTopic topicDeathLetter() {
        return new NewTopic(KafkaTopicConstants.TOPIC_DEATH_LETTER, numPartitions, replicaFactor);
    }


}
