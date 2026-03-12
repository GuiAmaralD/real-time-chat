package com.guiamaral.real_time_chat.controller;

import com.guiamaral.real_time_chat.dto.user.CreateUserRequest;
import com.guiamaral.real_time_chat.dto.user.RedisPingResponse;
import com.guiamaral.real_time_chat.dto.user.UserResponse;
import com.guiamaral.real_time_chat.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserService userService;

	private UserController userController;

	@BeforeEach
	void setUp() {
		userController = new UserController(userService);
	}

	@Test
	void createShouldReturnCreatedUser() {
		CreateUserRequest request = new CreateUserRequest("gui");
		UserResponse createdUser = new UserResponse("user-1", "gui");
		when(userService.create(request)).thenReturn(createdUser);

		ResponseEntity<UserResponse> response = userController.create(request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals("user-1", response.getBody().id());
		assertEquals("gui", response.getBody().nickname());
		verify(userService).create(request);
	}

	@Test
	void pingRedisShouldReturnServiceResponse() {
		RedisPingResponse pingResponse = new RedisPingResponse("PONG");
		when(userService.pingRedis()).thenReturn(pingResponse);

		RedisPingResponse response = userController.pingRedis();

		assertEquals("PONG", response.redis());
		verify(userService).pingRedis();
	}
}
