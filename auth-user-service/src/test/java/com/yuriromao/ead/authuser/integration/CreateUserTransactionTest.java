package com.yuriromao.ead.authuser.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.yuriromao.ead.authuser.application.event.UserCreatedEvent;
import com.yuriromao.ead.authuser.application.port.DomainEventRecorder;
import com.yuriromao.ead.authuser.application.usecase.CreateUserCommand;
import com.yuriromao.ead.authuser.application.usecase.CreateUserUseCase;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.infrastructure.persistence.UserJpaRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {"delete from outbox_events", "delete from user_roles", "delete from users"})
class CreateUserTransactionTest {

  private static final String NAME = "Transactional User";
  private static final String EMAIL = "transactional-user@email.com";
  private static final String PASSWORD = "plainPassword";

  @Autowired private CreateUserUseCase createUserUseCase;

  @Autowired private UserJpaRepository userJpaRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private DomainEventRecorder domainEventRecorder;

  @Test
  void shouldRollbackUserWhenOutboxRecordingFails() {
    doThrow(new IllegalStateException("outbox unavailable"))
        .when(domainEventRecorder)
        .record(any(UserCreatedEvent.class));

    assertThrows(
        IllegalStateException.class,
        () ->
            createUserUseCase.execute(
                new CreateUserCommand(NAME, EMAIL, PASSWORD, Set.of(UserRole.STUDENT))));

    assertEquals(0, userJpaRepository.count());
    assertEquals(0, outboxEventCount());
  }

  private int outboxEventCount() {
    Integer count =
        jdbcTemplate.queryForObject("select count(*) from outbox_events", Integer.class);
    return count == null ? 0 : count;
  }
}
