package com.guiamaral.real_time_chat.controller;

import java.util.List;

import com.guiamaral.real_time_chat.dto.message.MessageResponse;
import com.guiamaral.real_time_chat.dto.message.SendMessageRequest;
import com.guiamaral.real_time_chat.service.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/rooms/{roomId}/messages")
public class MessageController {

	private final MessageService messageService;

	public MessageController(MessageService messageService) {
		this.messageService = messageService;
	}

	@PostMapping
	public ResponseEntity<MessageResponse> send(
			@PathVariable String roomId,
			@Valid @RequestBody SendMessageRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(messageService.send(roomId, request));
	}

	@GetMapping
	public ResponseEntity<List<MessageResponse>> listRecent(
			@PathVariable String roomId,
			@RequestParam @NotBlank(message = "userId is required") String userId,
			@RequestParam(required = false) Integer limit
	) {
		return ResponseEntity.ok(messageService.listRecent(roomId, userId, limit));
	}
}
