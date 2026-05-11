package com.yuriromao.ead.authuser.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.event.UserCreatedPayload;

import tools.jackson.databind.json.JsonMapper;

class RabbitMqEventPublisherTest {

	private static final String EXCHANGE_NAME = "ead.domain.events";
	private static final String USER_CREATED_ROUTING_KEY = "auth-user.user-created";
	private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");

	@Test
	void shouldConfigureDomainEventsExchange() {
		RabbitMqConfig config = new RabbitMqConfig();

		TopicExchange exchange = config.domainEventsExchange(EXCHANGE_NAME);

		assertAll(
				() -> assertEquals(EXCHANGE_NAME, exchange.getName()),
				() -> assertEquals("topic", exchange.getType()),
				() -> assertEquals(true, exchange.isDurable()),
				() -> assertEquals(false, exchange.isAutoDelete()));
	}

	@Test
	void shouldConfigureJsonMessageConverter() {
		RabbitMqConfig config = new RabbitMqConfig();

		MessageConverter messageConverter = config.jacksonMessageConverter();

		assertNotNull(messageConverter);
	}

	@Test
	void shouldPublishUserCreatedEventToConfiguredExchangeAndRoutingKey() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		RabbitMqEventPublisher eventPublisher = new RabbitMqEventPublisher(
				rabbitTemplate,
				EXCHANGE_NAME,
				USER_CREATED_ROUTING_KEY);
		UserCreatedEvent event = userCreatedEvent();

		eventPublisher.publish(event);

		ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
		verify(rabbitTemplate).convertAndSend(eq(EXCHANGE_NAME), eq(USER_CREATED_ROUTING_KEY), eventCaptor.capture());
		assertEquals(event, eventCaptor.getValue());
	}

	@Test
	void shouldRejectInvalidPublisherArguments() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		RabbitMqEventPublisher eventPublisher = new RabbitMqEventPublisher(
				rabbitTemplate,
				EXCHANGE_NAME,
				USER_CREATED_ROUTING_KEY);

		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new RabbitMqEventPublisher(null, EXCHANGE_NAME, USER_CREATED_ROUTING_KEY)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new RabbitMqEventPublisher(rabbitTemplate, null, USER_CREATED_ROUTING_KEY)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new RabbitMqEventPublisher(rabbitTemplate, " ", USER_CREATED_ROUTING_KEY)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new RabbitMqEventPublisher(rabbitTemplate, EXCHANGE_NAME, null)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new RabbitMqEventPublisher(rabbitTemplate, EXCHANGE_NAME, "")),
				() -> assertThrows(NullPointerException.class, () -> eventPublisher.publish(null)));
	}

	@Test
	void shouldSerializeUserCreatedEventWithoutSensitiveData() throws Exception {
		JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

		String json = objectMapper.writeValueAsString(userCreatedEvent());

		assertAll(
				() -> assertFalse(json.toLowerCase().contains("password")),
				() -> assertFalse(json.toLowerCase().contains("hash")),
				() -> assertFalse(json.contains("plainPassword")),
				() -> assertFalse(json.contains("$2a$")),
				() -> assertEquals(true, json.contains(UserCreatedEvent.EVENT_TYPE)),
				() -> assertEquals(true, json.contains(USER_ID.toString())));
	}

	private UserCreatedEvent userCreatedEvent() {
		UserCreatedPayload payload = new UserCreatedPayload(USER_ID, "User Name", "user@email.com");
		return UserCreatedEvent.create(payload);
	}
}
