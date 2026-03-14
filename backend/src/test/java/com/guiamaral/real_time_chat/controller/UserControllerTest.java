package com.guiamaral.real_time_chat.controller;

import com.guiamaral.real_time_chat.dto.user.CreateUserRequest;
import com.guiamaral.real_time_chat.dto.presence.RoomPresenceResponse;
import com.guiamaral.real_time_chat.dto.user.RedisPingResponse;
import com.guiamaral.real_time_chat.dto.user.UserResponse;
import com.guiamaral.real_time_chat.service.PresenceService;
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

	@Mock
	private PresenceWebSocketController presenceWebSocketController;

	private UserController userController;

	@BeforeEach
	void setUp() {
		userController = new UserController(userService, presenceWebSocketController);
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

	@Test
	void disconnectShouldDeleteUserAndBroadcastPresenceUpdates() {
		RoomPresenceResponse payload = new RoomPresenceResponse("room-1", 0, java.util.List.of());
		var updates = java.util.List.of(new PresenceService.PresenceUpdate("room-1", payload));
		when(userService.disconnectAndDelete("user-1")).thenReturn(updates);

		ResponseEntity<Void> response = userController.disconnect("user-1");

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		verify(userService).disconnectAndDelete("user-1");
		verify(presenceWebSocketController).broadcastPresenceUpdates(updates);
	}

}
