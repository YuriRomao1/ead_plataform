package com.yuriromao.ead.authuser.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for direct persistence operations on user JPA entities. */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

  /** Uses the database index/constraint path to support the email uniqueness rule. */
  boolean existsByEmail(String email);
}
