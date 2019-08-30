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
package org.apache.fineract.cn.command.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.domain.CommandHandlerHolder;
import org.apache.fineract.cn.command.domain.CommandProcessingException;
import org.apache.fineract.cn.command.fixture.DomainAggregate;
import org.apache.fineract.cn.command.fixture.DomainException;
import org.apache.fineract.cn.command.fixture.DomainObject;
import org.apache.fineract.cn.command.fixture.ErroneousDomainCommand;
import org.apache.fineract.cn.command.fixture.ReturningCommand;
import org.apache.fineract.cn.command.fixture.ReturningWithEventCommand;
import org.apache.fineract.cn.command.fixture.VoidCommand;
import org.apache.fineract.cn.command.fixture.VoidWithEventCommand;
import org.apache.fineract.cn.command.kafka.KafkaProducer;
import org.apache.fineract.cn.command.repository.CommandSource;
import org.apache.fineract.cn.command.util.CommandConstants;
import org.apache.fineract.cn.cassandra.core.TenantAwareEntityTemplate;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import java.util.HashMap;
import java.util.concurrent.Future;

public class CommandBusTest {

  public CommandBusTest() {
    super();
  }

  private static TestHarness createTestHarness() {
    final Environment mockedEnvironment = Mockito.mock(Environment.class);
    Mockito.when(mockedEnvironment.getProperty(CommandConstants.APPLICATION_NAME_PROP,
        CommandConstants.APPLICATION_NAME_DEFAULT))
        .thenReturn(CommandConstants.APPLICATION_NAME_DEFAULT);

    final Logger mockedLogger = Mockito.mock(Logger.class);

    final Gson gson = new GsonBuilder().create();

    final TenantAwareEntityTemplate mockedTenantAwareEntityTemplate = Mockito.mock(TenantAwareEntityTemplate.class);

    final JmsTemplate mockedJmsTemplate = Mockito.mock(JmsTemplate.class);

    final KafkaProducer kafkaProducer = Mockito.mock(KafkaProducer.class);
    final NewTopic topicDeathLetter = Mockito.mock(NewTopic.class);

    final ApplicationContext mockedApplicationContext = Mockito.mock(ApplicationContext.class);
    final HashMap<String, Object> mockedBeans = new HashMap<>();
    mockedBeans.put(DomainAggregate.class.getSimpleName(), new DomainAggregate());
    Mockito.when(mockedApplicationContext.getBeansWithAnnotation(Mockito.eq(Aggregate.class)))
        .thenReturn(mockedBeans);
    Mockito.when(mockedApplicationContext.getBean(Mockito.eq(DomainAggregate.class)))
        .thenReturn((DomainAggregate) mockedBeans.get(DomainAggregate.class.getSimpleName()));

    final CommandBus commandBus =
        new CommandBus(mockedEnvironment, mockedLogger, gson, mockedTenantAwareEntityTemplate, mockedJmsTemplate, kafkaProducer, topicDeathLetter);
    commandBus.setApplicationContext(mockedApplicationContext);

    return new TestHarness(commandBus, mockedTenantAwareEntityTemplate, mockedJmsTemplate);
  }

  @Test
  public void shouldProcessVoidCommand() {
    final TestHarness testHarness = createTestHarness();
    testHarness.commandBus.dispatch(new VoidCommand());

    Mockito.verify(testHarness.mockedTenantAwareEntityTemplate, Mockito.times(2)).save(Mockito.any(CommandSource.class));
    Mockito.verify(testHarness.jmsTemplate, Mockito.times(0)).convertAndSend(Mockito.any(Object.class), Mockito.any(MessagePostProcessor.class));
  }

  @Test
  public void shouldProcessReturningCommand() throws Exception {
    final TestHarness testHarness = createTestHarness();
    final DomainObject domainObject = new DomainObject("test", 1L);

    final Future<DomainObject> future = testHarness.commandBus.dispatch(new ReturningCommand(domainObject), DomainObject.class);
    final DomainObject returnValue = future.get();

    Assert.assertNotNull(returnValue);
    Assert.assertEquals(domainObject.getStringProperty(), returnValue.getStringProperty());
    Assert.assertEquals(domainObject.getLongProperty(), returnValue.getLongProperty());

    Mockito.verify(testHarness.mockedTenantAwareEntityTemplate, Mockito.times(2)).save(Mockito.any(CommandSource.class));
    Mockito.verify(testHarness.jmsTemplate, Mockito.times(0)).convertAndSend(Mockito.any(Object.class), Mockito.any(MessagePostProcessor.class));
  }

  @Test
  public void shouldProcessVoidWithEventCommand() {
    final TestHarness testHarness = createTestHarness();
    testHarness.commandBus.dispatch(new VoidWithEventCommand());

    Mockito.verify(testHarness.mockedTenantAwareEntityTemplate, Mockito.times(2)).save(Mockito.any(CommandSource.class));
    Mockito.verify(testHarness.jmsTemplate, Mockito.times(1)).convertAndSend(Mockito.any(Object.class), Mockito.any(MessagePostProcessor.class));
  }

  @Test
  public void shouldProcessReturningWithEventCommand() throws Exception {
    final TestHarness testHarness = createTestHarness();
    final DomainObject domainObject = new DomainObject("test", 2L);

    final Future<DomainObject> future = testHarness.commandBus.dispatch(new ReturningWithEventCommand(domainObject), DomainObject.class);
    final DomainObject returnValue = future.get();

    Assert.assertNotNull(returnValue);
    Assert.assertEquals(domainObject.getStringProperty(), returnValue.getStringProperty());
    Assert.assertEquals(domainObject.getLongProperty(), returnValue.getLongProperty());

    Mockito.verify(testHarness.mockedTenantAwareEntityTemplate, Mockito.times(2)).save(Mockito.any(CommandSource.class));
    Mockito.verify(testHarness.jmsTemplate, Mockito.times(1)).convertAndSend(Mockito.any(Object.class), Mockito.any(MessagePostProcessor.class));
  }

  @Test
  public void shouldNotProcessCommandRethrowDeclaredException()
      throws CommandProcessingException {

    final TestHarness testHarness = createTestHarness();

    final ErroneousDomainCommand erroneousDomainCommand = new ErroneousDomainCommand();
    try {
      testHarness.commandBus.dispatch(erroneousDomainCommand, Void.class);
      Assert.fail();
    } catch (final DomainException ex) {
      // do nothing
    }

    Mockito.verify(testHarness.mockedTenantAwareEntityTemplate, Mockito.times(2)).save(Mockito.any(CommandSource.class));
  }

  @Test()
  public void getCommandHandlerMethodWithoutReturnValue() {
    final TestHarness testHarness = createTestHarness();
    final TestCommandHandler aggregateHandler = new TestCommandHandler();
    final CommandHandlerHolder commandHandlerHolder = testHarness.commandBus
        .getCommandHandlerMethodFromClass(TestCommandClass1.class, aggregateHandler);
    Assert.assertNotNull(commandHandlerHolder);
  }

  @Test()
  public void getCommandHandlerMethodWithReturnValue() {
    final TestHarness testHarness = createTestHarness();
    final TestCommandHandler aggregateHandler = new TestCommandHandler();
    final CommandHandlerHolder commandHandlerHolder = testHarness.commandBus
        .getCommandHandlerMethodFromClass(TestCommandClass2.class, aggregateHandler);
    Assert.assertNotNull(commandHandlerHolder);
  }

  private static class TestHarness {

    CommandBus commandBus;
    TenantAwareEntityTemplate mockedTenantAwareEntityTemplate;
    JmsTemplate jmsTemplate;

    private TestHarness(final CommandBus commandBus,
                        final TenantAwareEntityTemplate mockedTenantAwareEntityTemplate,
                        final JmsTemplate jmsTemplate) {
      this.commandBus = commandBus;
      this.mockedTenantAwareEntityTemplate = mockedTenantAwareEntityTemplate;
      this.jmsTemplate = jmsTemplate;
    }
  }

  private static class TestCommandClass1 {

  }

  private static class TestCommandClass2 {

  }

  private static class TestCommandResponse {

  }

  private static class TestCommandHandler {

    @SuppressWarnings("unused")
    @CommandHandler
    void process(final TestCommandClass1 command) {
    }

    @SuppressWarnings("unused")
    @CommandHandler
    TestCommandResponse process(final TestCommandClass2 command) {
      return new TestCommandResponse();
    }
  }
}
