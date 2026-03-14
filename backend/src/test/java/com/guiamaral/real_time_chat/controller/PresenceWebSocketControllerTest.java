package com.guiamaral.real_time_chat.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.guiamaral.real_time_chat.dto.presence.PresenceJoinRequest;
import com.guiamaral.real_time_chat.dto.presence.PresenceMemberResponse;
import com.guiamaral.real_time_chat.dto.presence.RoomOwnershipResponse;
import com.guiamaral.real_time_chat.dto.presence.RoomPresenceResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceWebSocketControllerTest {

	@Mock
	private PresenceService presenceService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private SimpMessageHeaderAccessor headerAccessor;

	private PresenceWebSocketController presenceWebSocketController;

	@BeforeEach
	void setUp() {
		presenceWebSocketController = new PresenceWebSocketController(presenceService, roomRepository, messagingTemplate);
	}

	@Test
	void joinShouldBroadcastPresenceUpdates() {
		PresenceJoinRequest request = new PresenceJoinRequest("user-1");
		RoomPresenceResponse payload = new RoomPresenceResponse(
				"room-1",
				1,
				List.of(new PresenceMemberResponse("user-1", "Gui"))
		);
		List<PresenceService.PresenceUpdate> updates = List.of(new PresenceService.PresenceUpdate("room-1", payload));
		when(headerAccessor.getSessionId()).thenReturn("session-1");
		when(presenceService.join("room-1", "user-1", "session-1")).thenReturn(updates);
		when(roomRepository.findById("room-1"))
				.thenReturn(Optional.of(new Room("room-1", "General", "GEN01", "owner-1", Set.of("owner-1", "user-1"))));

		presenceWebSocketController.join("room-1", request, headerAccessor);

		verify(presenceService).join("room-1", "user-1", "session-1");
		verify(messagingTemplate).convertAndSend("/topic/rooms/room-1/presence", payload);
		verify(messagingTemplate).convertAndSend(
				"/topic/rooms/room-1/ownership",
				new RoomOwnershipResponse("room-1", "owner-1")
		);
	}

	@Test
	void leaveShouldBroadcastPresenceUpdates() {
		PresenceJoinRequest request = new PresenceJoinRequest("user-1");
		RoomPresenceResponse payload = new RoomPresenceResponse("room-1", 0, List.of());
		List<PresenceService.PresenceUpdate> updates = List.of(new PresenceService.PresenceUpdate("room-1", payload));
		when(headerAccessor.getSessionId()).thenReturn("session-1");
		when(presenceService.leave("room-1", "user-1", "session-1")).thenReturn(updates);
		when(roomRepository.findById("room-1"))
				.thenReturn(Optional.of(new Room("room-1", "General", "GEN01", "owner-1", Set.of("owner-1"))));

		presenceWebSocketController.leave("room-1", request, headerAccessor);

		verify(presenceService).leave("room-1", "user-1", "session-1");
		verify(messagingTemplate).convertAndSend("/topic/rooms/room-1/presence", payload);
		verify(messagingTemplate).convertAndSend(
				"/topic/rooms/room-1/ownership",
				new RoomOwnershipResponse("room-1", "owner-1")
		);
	}

	@Test
	void joinShouldThrowBadRequestWhenSessionIdIsMissing() {
		when(headerAccessor.getSessionId()).thenReturn(" ");

		ApiException exception = assertThrows(
				ApiException.class,
				() -> presenceWebSocketController.join("room-1", new PresenceJoinRequest("user-1"), headerAccessor)
		);

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("websocket session id is required", exception.getMessage());
		verifyNoInteractions(presenceService);
		verifyNoInteractions(roomRepository);
		verifyNoInteractions(messagingTemplate);
	}
}
