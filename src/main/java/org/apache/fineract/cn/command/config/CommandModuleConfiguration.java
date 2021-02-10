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
package org.apache.fineract.cn.command.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.fineract.cn.command.util.CommandConstants;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.fineract.cn.lang.ApplicationName;
import org.apache.fineract.cn.lang.config.EnableApplicationName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Configuration
@EnableApplicationName
@ComponentScan(basePackages = {
    "org.apache.fineract.cn.command.internal",
    "org.apache.fineract.cn.command.gateway"
})
public class CommandModuleConfiguration {

  private final Environment environment;

  @Autowired
  public CommandModuleConfiguration(Environment environment) {
    super();
    this.environment = environment;
  }

  @Bean(name = CommandConstants.SERIALIZER)
  public Gson gson() {
    return new GsonBuilder().create();
  }

  @Bean(name = CommandConstants.LOGGER_NAME)
  public Logger loggerBean() {
    return LoggerFactory.getLogger(CommandConstants.LOGGER_NAME);
  }

  @Bean
  public PooledConnectionFactory jmsFactory() {
    final PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
    final ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
    activeMQConnectionFactory.setBrokerURL(
        this.environment.getProperty(
            CommandConstants.ACTIVEMQ_BROKER_URL_PROP,
            CommandConstants.ACTIVEMQ_BROKER_URL_DEFAULT));
    if (StringUtils.hasLength(this.environment.getProperty(CommandConstants.ACTIVEMQ_USERNAME))) {
      Assert.hasLength(this.environment.getProperty(CommandConstants.ACTIVEMQ_PASSWORD),"Amqp password is not supplied");
      activeMQConnectionFactory.setUserName(this.environment.getProperty(CommandConstants.ACTIVEMQ_USERNAME));
      activeMQConnectionFactory.setPassword(this.environment.getProperty(CommandConstants.ACTIVEMQ_PASSWORD));
    }
    pooledConnectionFactory.setConnectionFactory(activeMQConnectionFactory);
    return pooledConnectionFactory;
  }

  @Bean
  public JmsListenerContainerFactory jmsListenerContainerFactory(final PooledConnectionFactory jmsFactory) {
    final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setPubSubDomain(true);
    factory.setConnectionFactory(jmsFactory);
    factory.setConcurrency(
        this.environment.getProperty(
            CommandConstants.ACTIVEMQ_CONCURRENCY_PROP,
            CommandConstants.ACTIVEMQ_CONCURRENCY_DEFAULT
        )
    );
    return factory;
  }

  @Bean
  public JmsTemplate jmsTemplate(final ApplicationName applicationName, final PooledConnectionFactory jmsFactory) {
    final ActiveMQTopic activeMQTopic = new ActiveMQTopic(applicationName.toString());
    final JmsTemplate jmsTemplate = new JmsTemplate();
    jmsTemplate.setPubSubDomain(true);
    jmsTemplate.setConnectionFactory(jmsFactory);
    jmsTemplate.setDefaultDestination(activeMQTopic);
    return jmsTemplate;
  }
}
