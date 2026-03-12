package com.guiamaral.real_time_chat.service;

import java.util.Objects;
import java.util.UUID;

import com.guiamaral.real_time_chat.dto.user.CreateUserRequest;
import com.guiamaral.real_time_chat.dto.user.RedisPingResponse;
import com.guiamaral.real_time_chat.dto.user.UserResponse;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final StringRedisTemplate redisTemplate;

	public UserService(UserRepository userRepository, StringRedisTemplate redisTemplate) {
		this.userRepository = userRepository;
		this.redisTemplate = redisTemplate;
	}

	public UserResponse create(CreateUserRequest request) {
		User user = new User();
		user.setId(UUID.randomUUID().toString());
		user.setNickname(request.nickname());

		User savedUser = userRepository.save(user);
		return new UserResponse(savedUser.getId(), savedUser.getNickname());
	}

	public RedisPingResponse pingRedis() {
		RedisConnectionFactory connectionFactory = Objects.requireNonNull(
				redisTemplate.getConnectionFactory(),
				"RedisConnectionFactory nao foi inicializada."
		);

		String response;
		try (RedisConnection connection = connectionFactory.getConnection()) {
			response = connection.ping();
		}

		return new RedisPingResponse(response == null ? "NO_RESPONSE" : response);
	}
}
