package com.yuriromao.ead.authuser.application.port;

import com.yuriromao.ead.authuser.domain.model.User;

/**
 * Application port for user persistence owned by auth-user-service.
 *
 * Use cases depend on this boundary so persistence details stay in infrastructure
 * and no other service database is accessed directly.
 */
public interface UserRepository {

	User save(User user);

	boolean existsByEmail(String email);
}
