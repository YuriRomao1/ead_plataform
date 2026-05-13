package com.yuriromao.ead.authuser.application.port;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;

/**
 * Application port for publishing domain events after local state changes.
 *
 * <p>Use cases depend on this boundary so event publication can move between RabbitMQ, outbox, or
 * another mechanism without changing application rules.
 */
public interface EventPublisher {

  /** Publishes a domain event to the configured asynchronous messaging mechanism. */
  void publish(UserCreatedEvent event);
}
