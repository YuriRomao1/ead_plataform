package com.yuriromao.ead.authuser.application.event;

import java.util.Objects;
import java.util.UUID;

public record UserCreatedPayload(
		UUID userId,
		String name,
		String email) {

	public UserCreatedPayload {
		userId = Objects.requireNonNull(userId, "userId must not be null");
		name = requireText(name, "name");
		email = requireText(email, "email");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be null or empty");
		}

		return value;
	}
}
