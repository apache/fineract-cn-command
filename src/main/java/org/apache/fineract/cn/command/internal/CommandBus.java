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
import org.apache.fineract.cn.command.annotation.*;
import org.apache.fineract.cn.command.domain.CommandHandlerHolder;
import org.apache.fineract.cn.command.domain.CommandProcessingException;
import org.apache.fineract.cn.command.domain.NotificationMs;
import org.apache.fineract.cn.command.repository.CommandSource;
import org.apache.fineract.cn.command.util.CommandConstants;
import org.apache.fineract.cn.cassandra.core.TenantAwareEntityTemplate;
import org.apache.fineract.cn.lang.TenantContextHolder;
import org.apache.fineract.cn.lang.config.TenantHeaderFilter;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Component
public class CommandBus implements ApplicationContextAware {

  private final Environment environment;
  private final Logger logger;
  private final Gson gson;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;
  private final JmsTemplate jmsTemplate;

  private final ConcurrentHashMap<Class, CommandHandlerHolder> cachedCommandHandlers = new ConcurrentHashMap<>();
  private ApplicationContext applicationContext;

  @Autowired
  public CommandBus(final Environment environment,
                    @Qualifier(CommandConstants.LOGGER_NAME) final Logger logger,
                    @Qualifier(CommandConstants.SERIALIZER) final Gson gson,
                    @SuppressWarnings("SpringJavaAutowiringInspection") TenantAwareEntityTemplate tenantAwareEntityTemplate,
                    final JmsTemplate jmsTemplate) {
    super();
    this.environment = environment;
    this.logger = logger;
    this.gson = gson;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
    this.jmsTemplate = jmsTemplate;
  }

  @Async
  public <C> void dispatch(final C command) {
    this.logger.debug("CommandBus::dispatch-async called.");
    final CommandSource commandSource = this.storeCommand(command);
    CommandHandlerHolder commandHandlerHolder = null;
    try {
      commandHandlerHolder = this.findCommandHandler(command);
      commandHandlerHolder.logStart(command);

      final Object result = commandHandlerHolder.method().invoke(commandHandlerHolder.aggregate(), command);
      this.updateCommandSource(commandSource, null);

      commandHandlerHolder.logFinish(result);

      if (commandHandlerHolder.eventEmitter() != null) {
        this.fireEvent(result, commandHandlerHolder.eventEmitter());
        this.checkNotification(command, result, commandHandlerHolder.eventEmitter());
      }
    } catch (final Throwable th) {
      //noinspection ThrowableResultOfMethodCallIgnored
      this.handle(th, commandSource, (commandHandlerHolder != null ? commandHandlerHolder.exceptionTypes() : null));
    }
  }

  private <C> void checkNotification(C command, Object identifier, EventEmitter eventEmitter) {

    if (eventEmitter.selectorNotifyMs().equals(Notification.NOTIFY.toString())) {
      String commandName = command.getClass().getCanonicalName();
      NotificationMs notificationMs = new NotificationMs(commandName, identifier, command);
      this.saveNotificationMs(notificationMs);
    }
  }

  private void saveNotificationMs(NotificationMs notificationMs) {
    this.tenantAwareEntityTemplate.save(notificationMs);
  }

  @Async
  public <C, T> Future<T> dispatch(final C command, final Class<T> clazz) throws CommandProcessingException {
    this.logger.debug("CommandBus::dispatch-sync called.");
    // store command
    final CommandSource commandSource = this.storeCommand(command);
    CommandHandlerHolder commandHandlerHolder = null;
    try {
      // find command handling method
      commandHandlerHolder = this.findCommandHandler(command);
      commandHandlerHolder.logStart(command);

      final Object result = commandHandlerHolder.method().invoke(commandHandlerHolder.aggregate(), command);
      this.updateCommandSource(commandSource, null);

      commandHandlerHolder.logFinish(result);

      if (commandHandlerHolder.eventEmitter() != null) {
        this.fireEvent(result, commandHandlerHolder.eventEmitter());
      }

      return new AsyncResult<>(clazz.cast(result));
    } catch (final Throwable th) {
      throw this.handle(th, commandSource, (commandHandlerHolder != null ? commandHandlerHolder.exceptionTypes() : null));
    }
  }

  private <C> CommandHandlerHolder findCommandHandler(final C command) {
    this.logger.debug("CommandBus::findCommandHandler called for {}.", command.getClass().getSimpleName());
    final Class<?> commandClass = command.getClass();
    this.cachedCommandHandlers.computeIfAbsent(commandClass, findHandler -> {
      final Map<String, Object> aggregates = this.applicationContext.getBeansWithAnnotation(Aggregate.class);
      for (Object aggregate : aggregates.values()) {
        final CommandHandlerHolder commandHandlerHolder = this.getCommandHandlerMethodFromClass(commandClass, aggregate);
        if (commandHandlerHolder != null) {
          return commandHandlerHolder;
        }
      }
      this.logger.info("Could not find command handler for {}.", commandClass.getSimpleName());
      throw new IllegalArgumentException("No command handler found.");
    });
    return this.cachedCommandHandlers.get(commandClass);
  }

  CommandHandlerHolder getCommandHandlerMethodFromClass(final Class<?> commandClass, final Object aggregate) {
    final Method[] methods = aggregate.getClass().getDeclaredMethods();
    for (final Method method : methods) {
      final CommandHandler commandHandlerAnnotation = AnnotationUtils.findAnnotation(method, CommandHandler.class);
      if (commandHandlerAnnotation != null
          && method.getParameterCount() == 1
          && method.getParameterTypes()[0].isAssignableFrom(commandClass)) {
        this.logger.debug("CommandBus::findCommandHandler added method for {}.", commandClass.getSimpleName());

        //Note that as much of the logic of determining how to log as possible is moved into the creation of the
        //handler holder rather than performing it in the process of handling the command.  Creation of the command
        //handler holder is not performance critical, but execution of the command is.
        final Consumer<Object> logStart = getLogHandler(commandHandlerAnnotation.logStart(),
                "Handling command of type " + commandClass.getCanonicalName() + " for tenant {}, -> command {}");

        final Consumer<Object> logFinish = getLogHandler(commandHandlerAnnotation.logFinish(),
                "Handled command of type " + commandClass.getCanonicalName() + " for tenant {}, -> result {}");

        return new CommandHandlerHolder(aggregate, method, AnnotationUtils.findAnnotation(method, EventEmitter.class),
            method.getExceptionTypes(), logStart, logFinish);
      }
    }
    return null;
  }

  private Consumer<Object> getLogHandler(final CommandLogLevel level, final String formatString) {
    switch (level) {
      case INFO:
        return (x) -> logger.info(formatString, TenantContextHolder.identifier().orElse("none"), x);
      case DEBUG:
        return (x) -> logger.debug(formatString, TenantContextHolder.identifier().orElse("none"), x);
      case TRACE:
        return (x) -> logger.trace(formatString, TenantContextHolder.identifier().orElse("none"), x);
      default:
      case NONE:
        return (x) -> { };
    }
  }

  private <C> CommandSource storeCommand(final C command) {
    this.logger.debug("CommandBus::storeCommand called.");
    final LocalDateTime now = LocalDateTime.now();

    final CommandSource commandSource = new CommandSource();
    commandSource.setSource(
        this.environment.getProperty(
            CommandConstants.APPLICATION_NAME_PROP,
            CommandConstants.APPLICATION_NAME_DEFAULT
        )
    );
    commandSource.setBucket(now.format(DateTimeFormatter.ISO_LOCAL_DATE));
    commandSource.setCreatedOn(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
    commandSource.setCommand(this.gson.toJson(command));

    this.tenantAwareEntityTemplate.save(commandSource);

    return commandSource;
  }

  private void updateCommandSource(final CommandSource commandSource, final String failureMessage) {
    this.logger.debug("CommandBus::updateCommandSource called.");
    if (failureMessage != null) {
      commandSource.setFailed(Boolean.TRUE);
      commandSource.setFailureMessage(failureMessage);
    } else {
      commandSource.setProcessed(Boolean.TRUE);
    }

    this.tenantAwareEntityTemplate.save(commandSource);
  }

  private <T> void fireEvent(final T eventPayload, final EventEmitter eventEmitter) {
    if (eventPayload != null) {
      this.jmsTemplate.convertAndSend(
          this.gson.toJson(eventPayload),
          message -> {
            if (TenantContextHolder.identifier().isPresent()) {
              message.setStringProperty(
                  TenantHeaderFilter.TENANT_HEADER,
                  TenantContextHolder.checkedGetIdentifier());
            }
            message.setStringProperty(
                eventEmitter.selectorName(),
                eventEmitter.selectorValue()
            );
            return message;
          }
      );
    }
  }

  private CommandProcessingException handle(final Throwable th, final CommandSource commandSource,
                                            final Class<?>[] declaredExceptions) {
    final Throwable cause;
    if (th.getClass().isAssignableFrom(InvocationTargetException.class)) {
      cause = th.getCause();
    } else {
      cause = th;
    }

    final String failureMessage = cause.getClass().getSimpleName() + ": "
        + (cause.getMessage() != null ? cause.getMessage() : "no details available");

    this.logger.warn("Error while processing command. {}", failureMessage);

    this.updateCommandSource(commandSource, failureMessage);

    if (declaredExceptions != null) {
      if (Arrays.asList(declaredExceptions).contains(cause.getClass())) {
        if (cause instanceof RuntimeException) {
          throw RuntimeException.class.cast(cause);
        } else {
          this.logger.info("Exception {} is not a runtime exception.", cause.getClass().getSimpleName());
        }
      }
    }
    return new CommandProcessingException(cause.getMessage(), cause);
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
