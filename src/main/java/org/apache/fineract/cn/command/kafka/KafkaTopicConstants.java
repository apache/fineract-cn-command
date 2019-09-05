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

public interface KafkaTopicConstants {

    String TOPIC_CUSTOMER = "topic_customer";
    String TOPIC_ERROR_CUSTOMER = "topic_error_customer";

    String TOPIC_IDENTITY_USER = "topic_identity_user";
    String TOPIC_ERROR_IDENTITY_USER = "topic_error_identity_user";

    String TOPIC_PRODUCT_DEPOSIT = "topic_product_deposit";
    String TOPIC_ERROR_PRODUCT_DEPOSIT = "topic_error_product_deposit";

    String TOPIC_DEATH_LETTER = "topic_death_letter";

}

