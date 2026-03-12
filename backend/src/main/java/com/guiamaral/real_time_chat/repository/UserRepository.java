package com.guiamaral.real_time_chat.repository;

import com.guiamaral.real_time_chat.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {
}
