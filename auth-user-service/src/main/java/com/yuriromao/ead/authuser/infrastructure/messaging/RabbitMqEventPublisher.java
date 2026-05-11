package com.yuriromao.ead.authuser.infrastructure.messaging;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.EventPublisher;
import java.util.Objects;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ adapter for publishing application domain events.
 *
 * <p>The adapter receives already sanitized event models and only translates them to the configured
 * RabbitMQ exchange and routing key.
 */
@Component
public class RabbitMqEventPublisher implements EventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final String exchangeName;
  private final String userCreatedRoutingKey;

  @Autowired
  public RabbitMqEventPublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${auth-user-service.messaging.rabbitmq.exchange}") String exchangeName,
      @Value("${auth-user-service.messaging.rabbitmq.user-created-routing-key}")
          String userCreatedRoutingKey) {
    this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate must not be null");
    this.exchangeName = requireText(exchangeName, "exchangeName");
    this.userCreatedRoutingKey = requireText(userCreatedRoutingKey, "userCreatedRoutingKey");
  }

  @Override
  public void publish(UserCreatedEvent event) {
    rabbitTemplate.convertAndSend(
        exchangeName,
        userCreatedRoutingKey,
        Objects.requireNonNull(event, "event must not be null"));
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be null or empty");
    }

    return value;
  }
}
