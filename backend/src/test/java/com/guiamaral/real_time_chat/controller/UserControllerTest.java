package com.guiamaral.real_time_chat.controller;

import java.util.Map;

import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private RedisConnectionFactory redisConnectionFactory;

	@Mock
	private RedisConnection redisConnection;

	private UserController userController;

	@BeforeEach
	void setUp() {
		userController = new UserController(userRepository, redisTemplate);
	}

	@Nested
	class CreateTests {

		@BeforeEach
		void setUpCreate() {
			when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		}

		@Test
		void createShouldGenerateIdWhenMissing() {
			User request = new User();
			request.setNickname("gui");

			ResponseEntity<User> response = userController.create(request);

			ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).save(userCaptor.capture());
			User persistedUser = userCaptor.getValue();

			assertEquals(HttpStatus.CREATED, response.getStatusCode());
			assertNotNull(persistedUser.getId());
			assertFalse(persistedUser.getId().isBlank());
			assertEquals("gui", persistedUser.getNickname());
			assertEquals(persistedUser.getId(), response.getBody().getId());
		}

		@Test
		void createShouldKeepProvidedId() {
			User request = new User();
			request.setId("user-123");
			request.setNickname("gui");

			ResponseEntity<User> response = userController.create(request);

			ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
			verify(userRepository).save(userCaptor.capture());
			User persistedUser = userCaptor.getValue();

			assertEquals(HttpStatus.CREATED, response.getStatusCode());
			assertEquals("user-123", persistedUser.getId());
			assertEquals("user-123", response.getBody().getId());
			assertEquals("gui", response.getBody().getNickname());
		}
	}

	@Nested
	class PingRedisTests {

		@BeforeEach
		void setUpPing() {
			when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
			when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
		}

		@Test
		void pingRedisShouldReturnPong() {
			when(redisConnection.ping()).thenReturn("PONG");

			Map<String, String> response = userController.pingRedis();

			assertEquals("PONG", response.get("redis"));
			verify(redisConnection).close();
		}

		@Test
		void pingRedisShouldReturnNoResponseWhenPingIsNull() {
			when(redisConnection.ping()).thenReturn(null);

			Map<String, String> response = userController.pingRedis();

			assertEquals("NO_RESPONSE", response.get("redis"));
			verify(redisConnection).close();
		}
	}
}
