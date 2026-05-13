package com.yuriromao.ead.authuser.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for querying and updating transactional outbox records. */
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

  /** Finds a single outbox record by its globally unique event id. */
  Optional<OutboxEventJpaEntity> findByEventId(UUID eventId);

  /** Finds pending events whose retry time has arrived, ordered oldest first and page-limited. */
  List<OutboxEventJpaEntity> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
      OutboxEventStatus status, LocalDateTime nextAttemptAt, Pageable pageable);
}
