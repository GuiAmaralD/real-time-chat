package com.guiamaral.real_time_chat.controller;

import java.time.Instant;

import com.guiamaral.real_time_chat.dto.message.MessageResponse;
import com.guiamaral.real_time_chat.dto.message.SendMessageRequest;
import com.guiamaral.real_time_chat.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageWebSocketControllerTest {

	@Mock
	private MessageService messageService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private MessageWebSocketController messageWebSocketController;

	@BeforeEach
	void setUp() {
		messageWebSocketController = new MessageWebSocketController(messageService, messagingTemplate);
	}

	@Test
	void sendMessageShouldPublishToRoomTopic() {
		String roomId = "room-1";
		SendMessageRequest request = new SendMessageRequest("user-1", "hello");
		MessageResponse response = new MessageResponse(
				"1-0",
				roomId,
				"user-1",
				"gui",
				"hello",
				Instant.parse("2026-03-12T16:00:00Z")
		);
		when(messageService.send(roomId, request)).thenReturn(response);

		messageWebSocketController.sendMessage(roomId, request);

		verify(messageService).send(roomId, request);
		verify(messagingTemplate).convertAndSend("/topic/rooms/room-1/messages", response);
	}
}
