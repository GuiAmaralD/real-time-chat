package com.guiamaral.real_time_chat.config;

import java.util.List;

import com.guiamaral.real_time_chat.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceEventListener {

	private final PresenceService presenceService;
	private final SimpMessagingTemplate messagingTemplate;

	public WebSocketPresenceEventListener(PresenceService presenceService, SimpMessagingTemplate messagingTemplate) {
		this.presenceService = presenceService;
		this.messagingTemplate = messagingTemplate;
	}

	@EventListener
	public void onDisconnect(SessionDisconnectEvent event) {
		List<PresenceService.PresenceUpdate> updates = presenceService.disconnect(event.getSessionId());
		for (PresenceService.PresenceUpdate update : updates) {
			messagingTemplate.convertAndSend(destination(update.roomId()), update.payload());
		}
	}

	private String destination(String roomId) {
		return "/topic/rooms/" + roomId + "/presence";
	}
}
