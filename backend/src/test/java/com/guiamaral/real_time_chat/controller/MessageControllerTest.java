package com.guiamaral.real_time_chat.controller;

import java.time.Instant;
import java.util.List;

import com.guiamaral.real_time_chat.dto.message.MessageResponse;
import com.guiamaral.real_time_chat.dto.message.SendMessageRequest;
import com.guiamaral.real_time_chat.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

	@Mock
	private MessageService messageService;

	private MessageController messageController;

	@BeforeEach
	void setUp() {
		messageController = new MessageController(messageService);
	}

	@Test
	void sendShouldReturnCreatedMessage() {
		SendMessageRequest request = new SendMessageRequest("user-1", "hello");
		MessageResponse message = new MessageResponse(
				"1700000000000-0",
				"room-1",
				"user-1",
				"hello",
				Instant.parse("2026-03-12T16:00:00Z")
		);
		when(messageService.send("room-1", request)).thenReturn(message);

		ResponseEntity<MessageResponse> response = messageController.send("room-1", request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("hello", response.getBody().content());
		verify(messageService).send("room-1", request);
	}

	@Test
	void listRecentShouldReturnMessages() {
		List<MessageResponse> messages = List.of(
				new MessageResponse("1-0", "room-1", "user-1", "a", Instant.parse("2026-03-12T16:00:00Z")),
				new MessageResponse("2-0", "room-1", "user-2", "b", Instant.parse("2026-03-12T16:01:00Z"))
		);
		when(messageService.listRecent("room-1", "user-1", 20)).thenReturn(messages);

		ResponseEntity<List<MessageResponse>> response = messageController.listRecent("room-1", "user-1", 20);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(2, response.getBody().size());
		verify(messageService).listRecent("room-1", "user-1", 20);
	}
}
