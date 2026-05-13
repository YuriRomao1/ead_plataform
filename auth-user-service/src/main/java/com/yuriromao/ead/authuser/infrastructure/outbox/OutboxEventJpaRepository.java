package com.yuriromao.ead.authuser.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

  Optional<OutboxEventJpaEntity> findByEventId(UUID eventId);

  List<OutboxEventJpaEntity> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
      OutboxEventStatus status, LocalDateTime nextAttemptAt, Pageable pageable);
}
