package com.yuriromao.ead.authuser.infrastructure.outbox;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

  Optional<OutboxEventJpaEntity> findByEventId(UUID eventId);
}
