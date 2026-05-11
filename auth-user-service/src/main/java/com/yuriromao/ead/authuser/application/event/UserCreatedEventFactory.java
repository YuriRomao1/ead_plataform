package com.yuriromao.ead.authuser.application.event;

import java.util.Objects;

import com.yuriromao.ead.authuser.domain.model.User;

/**
 * Builds the UserCreated event from the persisted user aggregate.
 *
 * Keeping event construction here makes it explicit that only non-sensitive
 * user data is allowed to leave the application boundary.
 */
public final class UserCreatedEventFactory {

	private UserCreatedEventFactory() {
	}

	public static UserCreatedEvent from(User user) {
		Objects.requireNonNull(user, "user must not be null");

		UserCreatedPayload payload = new UserCreatedPayload(user.getId(), user.getName(), user.getEmail());
		return UserCreatedEvent.create(payload);
	}
}
