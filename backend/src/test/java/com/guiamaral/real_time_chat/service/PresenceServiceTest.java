package com.guiamaral.real_time_chat.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.guiamaral.real_time_chat.dto.presence.RoomPresenceResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private UserRepository userRepository;

	private PresenceService presenceService;

	@BeforeEach
	void setUp() {
		presenceService = new PresenceService(roomRepository, userRepository);
	}

	@Test
	void joinShouldRegisterPresenceAndReturnSnapshot() {
		stubRoom("room-1", Set.of("user-1"));
		stubUser("user-1", "Gui");
		stubFindAllById(Map.of("user-1", "Gui"));

		List<PresenceService.PresenceUpdate> updates = presenceService.join("room-1", "user-1", "session-1");

		assertEquals(1, updates.size());
		PresenceService.PresenceUpdate update = updates.get(0);
		assertEquals("room-1", update.roomId());
		assertEquals(1, update.payload().onlineCount());
		assertEquals(1, update.payload().members().size());
		assertEquals("user-1", update.payload().members().get(0).id());
		assertEquals("Gui", update.payload().members().get(0).nickname());
	}

	@Test
	void joinShouldMoveSessionToAnotherRoomAndReturnBothRoomUpdates() {
		stubRoom("room-1", Set.of("user-1"));
		stubRoom("room-2", Set.of("user-1"));
		stubUser("user-1", "Gui");
		stubFindAllById(Map.of("user-1", "Gui"));

		presenceService.join("room-1", "user-1", "session-1");
		List<PresenceService.PresenceUpdate> updates = presenceService.join("room-2", "user-1", "session-1");

		assertEquals(2, updates.size());
		Map<String, RoomPresenceResponse> payloadByRoom = updates.stream()
				.collect(Collectors.toMap(PresenceService.PresenceUpdate::roomId, PresenceService.PresenceUpdate::payload));

		assertEquals(0, payloadByRoom.get("room-1").onlineCount());
		assertEquals(1, payloadByRoom.get("room-2").onlineCount());
		assertEquals("user-1", payloadByRoom.get("room-2").members().get(0).id());
	}

	@Test
	void leaveShouldThrowForbiddenWhenSessionIsNotRegisteredForProvidedUser() {
		stubRoom("room-1", Set.of("user-1"));
		stubUser("user-1", "Gui");
		stubFindAllById(Map.of("user-1", "Gui"));
		presenceService.join("room-1", "user-1", "session-1");

		ApiException exception = assertThrows(
				ApiException.class,
				() -> presenceService.leave("room-1", "user-2", "session-1")
		);

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
		assertEquals("session is not registered for provided room/user", exception.getMessage());
	}

	@Test
	void disconnectShouldRemoveSessionAndReturnEmptyPresenceSnapshot() {
		stubRoom("room-1", Set.of("user-1"));
		stubUser("user-1", "Gui");
		stubFindAllById(Map.of("user-1", "Gui"));
		presenceService.join("room-1", "user-1", "session-1");

		List<PresenceService.PresenceUpdate> updates = presenceService.disconnect("session-1");

		assertEquals(1, updates.size());
		assertEquals("room-1", updates.get(0).roomId());
		assertEquals(0, updates.get(0).payload().onlineCount());
		assertEquals(0, updates.get(0).payload().members().size());
	}

	@Test
	void removeUserFromAllRoomsShouldKeepOtherUsersOnline() {
		stubRoom("room-1", Set.of("user-1", "user-2"));
		stubUser("user-1", "Gui");
		stubUser("user-2", "Ana");
		stubFindAllById(Map.of("user-1", "Gui", "user-2", "Ana"));
		presenceService.join("room-1", "user-1", "session-1");
		presenceService.join("room-1", "user-2", "session-2");

		List<PresenceService.PresenceUpdate> updates = presenceService.removeUserFromAllRooms("user-1");

		assertEquals(1, updates.size());
		assertEquals("room-1", updates.get(0).roomId());
		assertEquals(1, updates.get(0).payload().onlineCount());
		assertEquals(1, updates.get(0).payload().members().size());
		assertEquals("user-2", updates.get(0).payload().members().get(0).id());
		assertEquals("Ana", updates.get(0).payload().members().get(0).nickname());
	}

	private void stubRoom(String roomId, Set<String> members) {
		Room room = new Room(roomId, "General", "GEN-" + roomId, "owner-1", new LinkedHashSet<>(members));
		when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
	}

	private void stubUser(String userId, String nickname) {
		when(userRepository.findById(userId)).thenReturn(Optional.of(new User(userId, nickname)));
	}

	private void stubFindAllById(Map<String, String> nicknamesByUserId) {
		when(userRepository.findAllById(any())).thenAnswer(invocation -> {
			Iterable<String> ids = invocation.getArgument(0);
			List<User> users = new ArrayList<>();
			for (String id : ids) {
				users.add(new User(id, nicknamesByUserId.getOrDefault(id, "")));
			}
			return users;
		});
	}
}
