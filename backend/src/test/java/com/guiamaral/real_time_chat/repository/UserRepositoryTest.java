package com.guiamaral.real_time_chat.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.guiamaral.real_time_chat.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.redis.verify-on-startup=false")
@ActiveProfiles("dev")
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	private final List<String> createdIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		for (String id : createdIds) {
			userRepository.deleteById(id);
		}
		createdIds.clear();
	}

	@Test
	void shouldSaveAndFindUserById() {
		String id = UUID.randomUUID().toString();
		User user = new User(id, "nick-save-find");
		createdIds.add(id);

		userRepository.save(user);
		Optional<User> loadedUser = userRepository.findById(id);

		assertTrue(loadedUser.isPresent());
		assertEquals(id, loadedUser.get().getId());
		assertEquals("nick-save-find", loadedUser.get().getNickname());
	}

	@Test
	void shouldDeleteUserById() {
		String id = UUID.randomUUID().toString();
		User user = new User(id, "nick-delete");

		userRepository.save(user);
		userRepository.deleteById(id);

		Optional<User> loadedUser = userRepository.findById(id);
		assertFalse(loadedUser.isPresent());
	}
}
