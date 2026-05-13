package com.yuriromao.ead.authuser.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ producer configuration for auth-user-service domain events.
 *
 * <p>ADR-007 defines the initial topology: publish domain events to a durable topic exchange and
 * use event-specific routing keys.
 */
@Configuration
public class RabbitMqConfig {

  /** Declares the durable domain-event exchange used by producers in local and runtime profiles. */
  @Bean
  TopicExchange domainEventsExchange(
      @Value("${auth-user-service.messaging.rabbitmq.exchange}") String exchangeName) {
    return new TopicExchange(exchangeName, true, false);
  }

  /** Configures RabbitMQ messages to use JSON serialization for domain-event envelopes. */
  @Bean
  MessageConverter jacksonMessageConverter() {
    return new JacksonJsonMessageConverter(JsonMapper.builder().findAndAddModules().build());
  }
}
