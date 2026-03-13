package com.guiamaral.real_time_chat.controller;

import java.util.List;

import com.guiamaral.real_time_chat.dto.room.CreateRoomRequest;
import com.guiamaral.real_time_chat.dto.room.JoinRoomRequest;
import com.guiamaral.real_time_chat.dto.room.LeaveRoomRequest;
import com.guiamaral.real_time_chat.dto.room.RoomResponse;
import com.guiamaral.real_time_chat.dto.room.RoomUserResponse;
import com.guiamaral.real_time_chat.service.PresenceService;
import com.guiamaral.real_time_chat.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {

	private final RoomService roomService;
	private final PresenceService presenceService;
	private final SimpMessagingTemplate messagingTemplate;

	public RoomController(
			RoomService roomService,
			PresenceService presenceService,
			SimpMessagingTemplate messagingTemplate
	) {
		this.roomService = roomService;
		this.presenceService = presenceService;
		this.messagingTemplate = messagingTemplate;
	}

	@PostMapping
	public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
	}

	@PostMapping("/join")
	public ResponseEntity<RoomResponse> joinByCode(@Valid @RequestBody JoinRoomRequest request) {
		return ResponseEntity.ok(roomService.joinByCode(request));
	}

	@PostMapping("/{roomId}/leave")
	public ResponseEntity<Void> leave(
			@PathVariable String roomId,
			@Valid @RequestBody LeaveRoomRequest request
	) {
		roomService.leave(roomId, request.userId());

		var updates = presenceService.removeUserFromRoom(roomId, request.userId());
		for (PresenceService.PresenceUpdate update : updates) {
			messagingTemplate.convertAndSend("/topic/rooms/" + update.roomId() + "/presence", update.payload());
		}

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/code/{code}")
	public ResponseEntity<RoomResponse> findByCodeEndpoint(@PathVariable String code) {
		return ResponseEntity.ok(roomService.findByCode(code));
	}

	@GetMapping("/{roomId}/users")
	public ResponseEntity<List<RoomUserResponse>> listRoomUsers(@PathVariable String roomId) {
		return ResponseEntity.ok(roomService.listRoomUsers(roomId));
	}
}
