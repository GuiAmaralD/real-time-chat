package com.guiamaral.real_time_chat.controller;

import java.util.List;

import com.guiamaral.real_time_chat.dto.presence.PresenceJoinRequest;
import com.guiamaral.real_time_chat.dto.presence.RoomOwnershipResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class PresenceWebSocketController {

	private final PresenceService presenceService;
	private final RoomRepository roomRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public PresenceWebSocketController(
			PresenceService presenceService,
			RoomRepository roomRepository,
			SimpMessagingTemplate messagingTemplate
	) {
		this.presenceService = presenceService;
		this.roomRepository = roomRepository;
		this.messagingTemplate = messagingTemplate;
	}

	@MessageMapping("/rooms/{roomId}/presence/join")
	public void join(
			@DestinationVariable String roomId,
			@Valid @Payload PresenceJoinRequest request,
			SimpMessageHeaderAccessor headerAccessor
	) {
		String sessionId = requireSessionId(headerAccessor);
		List<PresenceService.PresenceUpdate> updates = presenceService.join(roomId, request.userId(), sessionId);
		broadcastPresenceUpdates(updates);
	}

	@MessageMapping("/rooms/{roomId}/presence/leave")
	public void leave(
			@DestinationVariable String roomId,
			@Valid @Payload PresenceJoinRequest request,
			SimpMessageHeaderAccessor headerAccessor
	) {
		String sessionId = requireSessionId(headerAccessor);
		List<PresenceService.PresenceUpdate> updates = presenceService.leave(roomId, request.userId(), sessionId);
		broadcastPresenceUpdates(updates);
	}

	public void broadcastPresenceUpdates(List<PresenceService.PresenceUpdate> updates) {
		for (PresenceService.PresenceUpdate update : updates) {
			String roomId = update.roomId();
			messagingTemplate.convertAndSend(presenceDestination(roomId), update.payload());
			messagingTemplate.convertAndSend(ownershipDestination(roomId), buildOwnershipPayload(roomId));
		}
	}

	private String requireSessionId(SimpMessageHeaderAccessor headerAccessor) {
		String sessionId = headerAccessor.getSessionId();
		if (sessionId == null || sessionId.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "websocket session id is required");
		}
		return sessionId;
	}

	private RoomOwnershipResponse buildOwnershipPayload(String roomId) {
		String ownerId = roomRepository.findById(roomId)
				.map(Room::getOwnerId)
				.orElse(null);
		return new RoomOwnershipResponse(roomId, ownerId);
	}

	private String presenceDestination(String roomId) {
		return "/topic/rooms/" + roomId + "/presence";
	}

	private String ownershipDestination(String roomId) {
		return "/topic/rooms/" + roomId + "/ownership";
	}
}
