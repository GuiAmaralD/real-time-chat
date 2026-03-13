package com.guiamaral.real_time_chat.config;

import java.util.List;

import com.guiamaral.real_time_chat.dto.presence.PresenceMemberResponse;
import com.guiamaral.real_time_chat.dto.presence.RoomPresenceResponse;
import com.guiamaral.real_time_chat.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketPresenceEventListenerTest {

	@Mock
	private PresenceService presenceService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private SessionDisconnectEvent disconnectEvent;

	private WebSocketPresenceEventListener listener;

	@BeforeEach
	void setUp() {
		listener = new WebSocketPresenceEventListener(presenceService, messagingTemplate);
	}

	@Test
	void onDisconnectShouldBroadcastPresenceUpdates() {
		RoomPresenceResponse payload = new RoomPresenceResponse(
				"room-1",
				1,
				List.of(new PresenceMemberResponse("user-1", "Gui"))
		);
		List<PresenceService.PresenceUpdate> updates = List.of(new PresenceService.PresenceUpdate("room-1", payload));
		when(disconnectEvent.getSessionId()).thenReturn("session-1");
		when(presenceService.disconnect("session-1")).thenReturn(updates);

		listener.onDisconnect(disconnectEvent);

		verify(messagingTemplate).convertAndSend("/topic/rooms/room-1/presence", payload);
	}

	@Test
	void onDisconnectShouldNotBroadcastWhenNoUpdates() {
		when(disconnectEvent.getSessionId()).thenReturn("session-1");
		when(presenceService.disconnect("session-1")).thenReturn(List.of());

		listener.onDisconnect(disconnectEvent);

		verifyNoInteractions(messagingTemplate);
	}
}
