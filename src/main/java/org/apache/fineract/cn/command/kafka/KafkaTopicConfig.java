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

    @Value(value = "${kafka.bootstrapAddress:localhost:2181}")
    private String bootstrapAddress;
    private static final Integer NUM_PARTITIONS = 3;
    private static final Short REPLICATION_FACTOR = 1;

    // TODO parametrizar topics por cloud config
    @Value(value = "${kafka.topic.customer:topic_customer}")
    private String topicCustomer;

    @Value(value = "${kafka.topic.error.customer:topic_error_customer}")
    private String topicErrorCustomer;

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

    @Bean
    public NewTopic topicErrorCustomer() {
        return new NewTopic(topicErrorCustomer, NUM_PARTITIONS, REPLICATION_FACTOR);
    }
}
