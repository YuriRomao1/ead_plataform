package com.yuriromao.ead.authuser.application.port;

import com.yuriromao.ead.authuser.domain.model.User;

public interface UserRepository {

	User save(User user);

	boolean existsByEmail(String email);
}
