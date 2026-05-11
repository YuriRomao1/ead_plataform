package com.yuriromao.ead.authuser.application.port;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;

/**
 * Application port for recording domain events in the local transaction.
 *
 * <p>Use cases write events through this boundary so the database commit controls both the
 * aggregate change and the durable publication intent.
 */
public interface DomainEventRecorder {

  void record(UserCreatedEvent event);
}
