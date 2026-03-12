package com.guiamaral.real_time_chat.controller;

import com.guiamaral.real_time_chat.dto.message.MessageResponse;
import com.guiamaral.real_time_chat.dto.message.SendMessageRequest;
import com.guiamaral.real_time_chat.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class MessageWebSocketController {

	private final MessageService messageService;
	private final SimpMessagingTemplate messagingTemplate;

	public MessageWebSocketController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
		this.messageService = messageService;
		this.messagingTemplate = messagingTemplate;
	}

	@MessageMapping("/rooms/{roomId}/messages")
	public void sendMessage(
			@DestinationVariable String roomId,
			@Valid @Payload SendMessageRequest request
	) {
		MessageResponse response = messageService.send(roomId, request);
		messagingTemplate.convertAndSend(destination(roomId), response);
	}

	private String destination(String roomId) {
		return "/topic/rooms/" + roomId + "/messages";
	}
}
