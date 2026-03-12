package com.guiamaral.real_time_chat.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.StringRedisTemplate;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserRepository userRepository;
	private final StringRedisTemplate redisTemplate;

	public UserController(UserRepository userRepository, StringRedisTemplate redisTemplate) {
		this.userRepository = userRepository;
		this.redisTemplate = redisTemplate;
	}

	@PostMapping
	public ResponseEntity<User> create(@Valid @RequestBody User user) {
		if (user.getId() == null || user.getId().isBlank()) {
			user.setId(UUID.randomUUID().toString());
		}
		User savedUser = userRepository.save(user);
        System.out.println(savedUser.toString());
		return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
	}


	@GetMapping("/redis/ping")
	public Map<String, String> pingRedis() {
		RedisConnectionFactory connectionFactory = Objects.requireNonNull(
				redisTemplate.getConnectionFactory(),
				"RedisConnectionFactory nao foi inicializada."
		);

		String response;
		try (RedisConnection connection = connectionFactory.getConnection()) {
			response = connection.ping();
		}

		return Map.of("redis", response == null ? "NO_RESPONSE" : response);
	}
}
