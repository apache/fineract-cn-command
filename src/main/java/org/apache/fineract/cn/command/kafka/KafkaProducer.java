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


import com.google.gson.Gson;
import org.apache.fineract.cn.command.util.CommandConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component(CommandConstants.KAFKA_PRODUCER_CUSTOM)
public class KafkaProducer {

    private final Logger logger;
    private final Gson gson;
    private final KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    public KafkaProducer(@Qualifier(CommandConstants.LOGGER_NAME) final Logger logger,
                         @Qualifier(CommandConstants.KAFKA_TEMPLATE_CUSTOM) final KafkaTemplate<String, String> kafkaTemplate,
                         @Qualifier(CommandConstants.SERIALIZER) final Gson gson) {

        this.logger = logger;
        this.gson = gson;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String key, Object message) {

        // TODO inverstigar reduccion de tamaño objecto
        String payload = this.gson.toJson(message);
        logger.info(String.format("$$ -> Producing json message --> %s", payload));

        ListenableFuture<SendResult<String, String>> future = this.kafkaTemplate.send(topic, key, payload);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
                System.out.println("Sent message=[" + message.toString() + "] with offset=[" + result.getRecordMetadata().offset() + "] ; topic:=[" + result.getRecordMetadata().topic() + "]");
            }
            @Override
            public void onFailure(Throwable ex) {
                System.out.println("Unable to send message=[" + message.toString() + "] due to : " + ex.getMessage());
            }
        });
    }
}
