package com.guiamaral.real_time_chat.service;

import com.guiamaral.real_time_chat.dto.user.CreateUserRequest;
import com.guiamaral.real_time_chat.dto.user.RedisPingResponse;
import com.guiamaral.real_time_chat.dto.user.UserResponse;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private RedisConnectionFactory redisConnectionFactory;

	@Mock
	private RedisConnection redisConnection;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, redisTemplate);
	}

	@Test
	void createShouldGenerateIdAndPersistUser() {
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateUserRequest request = new CreateUserRequest("gui");

		UserResponse response = userService.create(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User persistedUser = userCaptor.getValue();

		assertNotNull(persistedUser.getId());
		assertFalse(persistedUser.getId().isBlank());
		assertEquals("gui", persistedUser.getNickname());
		assertEquals(persistedUser.getId(), response.id());
		assertEquals("gui", response.nickname());
	}

	@Test
	void pingRedisShouldReturnPong() {
		when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
		when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
		when(redisConnection.ping()).thenReturn("PONG");

		RedisPingResponse response = userService.pingRedis();

		assertEquals("PONG", response.redis());
		verify(redisConnection).close();
	}

	@Test
	void pingRedisShouldReturnNoResponseWhenNull() {
		when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
		when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
		when(redisConnection.ping()).thenReturn(null);

		RedisPingResponse response = userService.pingRedis();

		assertEquals("NO_RESPONSE", response.redis());
		verify(redisConnection).close();
	}
}
