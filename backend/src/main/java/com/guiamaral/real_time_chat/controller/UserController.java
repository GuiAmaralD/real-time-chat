package com.guiamaral.real_time_chat.controller;

import com.guiamaral.real_time_chat.dto.user.CreateUserRequest;
import com.guiamaral.real_time_chat.dto.user.RedisPingResponse;
import com.guiamaral.real_time_chat.dto.user.UserResponse;
import com.guiamaral.real_time_chat.service.PresenceService;
import com.guiamaral.real_time_chat.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;
	private final SimpMessagingTemplate messagingTemplate;

	public UserController(UserService userService, SimpMessagingTemplate messagingTemplate) {
		this.userService = userService;
		this.messagingTemplate = messagingTemplate;
	}

	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
	}

	@PostMapping("/{userId}/disconnect")
	public ResponseEntity<Void> disconnect(@PathVariable String userId) {
		return disconnectAndBroadcast(userId);
	}

	@GetMapping("/redis/ping")
	public RedisPingResponse pingRedis() {
		return userService.pingRedis();
	}

	private ResponseEntity<Void> disconnectAndBroadcast(String userId) {
		var updates = userService.disconnectAndDelete(userId);
		for (PresenceService.PresenceUpdate update : updates) {
			messagingTemplate.convertAndSend("/topic/rooms/" + update.roomId() + "/presence", update.payload());
		}
		return ResponseEntity.noContent().build();
	}
}
