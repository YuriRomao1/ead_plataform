package com.yuriromao.ead.authuser.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OutboxMigrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void shouldCreateOutboxEventsTable() {
		Integer tableCount = jdbcTemplate.queryForObject("""
				select count(*)
				from information_schema.tables
				where table_schema = 'public'
				  and table_name = 'outbox_events'
				""", Integer.class);

		assertTrue(tableCount != null && tableCount == 1);
	}

	@Test
	void shouldCreateRequiredOutboxEventColumns() {
		Map<String, String> columns = jdbcTemplate.queryForList("""
				select column_name, data_type
				from information_schema.columns
				where table_schema = 'public'
				  and table_name = 'outbox_events'
				""").stream()
				.collect(Collectors.toMap(
						row -> (String) row.get("column_name"),
						row -> (String) row.get("data_type")));

		assertAll(
				() -> assertTrue(columns.entrySet().contains(Map.entry("event_id", "uuid"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("event_type", "character varying"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("occurred_at", "timestamp without time zone"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("payload", "jsonb"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("status", "character varying"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("attempts", "integer"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("last_error", "text"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("created_at", "timestamp without time zone"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("updated_at", "timestamp without time zone"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("published_at", "timestamp without time zone"))),
				() -> assertTrue(columns.entrySet().contains(Map.entry("next_attempt_at", "timestamp without time zone"))));
	}

	@Test
	void shouldCreateOutboxConstraintsAndIndexes() {
		Set<String> constraintNames = jdbcTemplate.queryForList("""
				select constraint_name
				from information_schema.table_constraints
				where table_schema = 'public'
				  and table_name = 'outbox_events'
				""", String.class).stream().collect(Collectors.toSet());

		Set<String> indexNames = jdbcTemplate.queryForList("""
				select indexname
				from pg_indexes
				where schemaname = 'public'
				  and tablename = 'outbox_events'
				""", String.class).stream().collect(Collectors.toSet());

		assertAll(
				() -> assertTrue(constraintNames.contains("outbox_events_pkey")),
				() -> assertTrue(constraintNames.contains("ck_outbox_events_status")),
				() -> assertTrue(constraintNames.contains("ck_outbox_events_attempts")),
				() -> assertTrue(indexNames.contains("idx_outbox_events_status_next_attempt_at")),
				() -> assertTrue(indexNames.contains("idx_outbox_events_event_type")));
	}
}
