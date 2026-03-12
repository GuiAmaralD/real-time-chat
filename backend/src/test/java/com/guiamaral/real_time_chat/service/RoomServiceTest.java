package com.guiamaral.real_time_chat.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.guiamaral.real_time_chat.dto.room.CreateRoomRequest;
import com.guiamaral.real_time_chat.dto.room.JoinRoomRequest;
import com.guiamaral.real_time_chat.dto.room.RoomResponse;
import com.guiamaral.real_time_chat.dto.room.RoomUserResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private UserRepository userRepository;

	private RoomService roomService;

	@BeforeEach
	void setUp() {
		roomService = new RoomService(roomRepository, userRepository);
	}

	@Nested
	class CreateTests {

		@Test
		void createShouldPersistRoomWhenRequestIsValid() {
			CreateRoomRequest request = new CreateRoomRequest("General", "GEN01", "owner-1");

			when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "owner")));
			when(roomRepository.findByCode("GEN01")).thenReturn(Optional.empty());
			when(roomRepository.findAll()).thenReturn(List.of());
			when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

			RoomResponse response = roomService.create(request);

			ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
			verify(roomRepository).save(roomCaptor.capture());
			Room persistedRoom = roomCaptor.getValue();

			assertNotNull(persistedRoom.getId());
			assertEquals("owner-1", persistedRoom.getOwnerId());
			assertTrue(persistedRoom.getMemberIds().contains("owner-1"));
			assertEquals("GEN01", response.code());
		}

		@Test
		void createShouldThrowConflictWhenCodeAlreadyExists() {
			CreateRoomRequest request = new CreateRoomRequest("General", "GEN01", "owner-1");

			when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "owner")));
			when(roomRepository.findByCode("GEN01")).thenReturn(Optional.of(room("r1", "Existing", "GEN01", "owner-x", Set.of("owner-x"))));

			ApiException exception = assertThrows(ApiException.class, () -> roomService.create(request));

			assertEquals(HttpStatus.CONFLICT, exception.getStatus());
			verify(roomRepository, never()).save(any(Room.class));
		}

		@Test
		void createShouldThrowConflictWhenOwnerAlreadyHasThreeRooms() {
			CreateRoomRequest request = new CreateRoomRequest("General", "GEN04", "owner-1");
			Room r1 = room("r1", "A", "A1", "owner-1", Set.of("owner-1"));
			Room r2 = room("r2", "B", "B1", "owner-1", Set.of("owner-1"));
			Room r3 = room("r3", "C", "C1", "owner-1", Set.of("owner-1"));

			when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "owner")));
			when(roomRepository.findByCode("GEN04")).thenReturn(Optional.empty());
			when(roomRepository.findAll()).thenReturn(List.of(r1, r2, r3));

			ApiException exception = assertThrows(ApiException.class, () -> roomService.create(request));

			assertEquals(HttpStatus.CONFLICT, exception.getStatus());
			verify(roomRepository, never()).save(any(Room.class));
		}
	}

	@Nested
	class JoinTests {

		@Test
		void joinShouldAddMemberWhenLimitAllows() {
			String userId = "member-1";
			Room room = room("room-1", "General", "GEN01", "owner-1", Set.of("owner-1"));

			when(userRepository.findById(userId)).thenReturn(Optional.of(new User(userId, "member")));
			when(roomRepository.findByCode("GEN01")).thenReturn(Optional.of(room));
			when(roomRepository.findAll()).thenReturn(List.of(room));
			when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

			RoomResponse response = roomService.joinByCode(new JoinRoomRequest("GEN01", userId));

			assertTrue(response.memberIds().contains(userId));
			verify(roomRepository).save(any(Room.class));
		}

		@Test
		void joinShouldThrowConflictWhenUserAlreadyHasThreeRooms() {
			String userId = "member-1";
			Room targetRoom = room("r4", "D", "ROOM99", "owner-9", Set.of("owner-9"));
			Room r1 = room("r1", "A", "A1", "owner-1", Set.of(userId));
			Room r2 = room("r2", "B", "B1", "owner-2", Set.of(userId));
			Room r3 = room("r3", "C", "C1", "owner-3", Set.of(userId));

			when(userRepository.findById(userId)).thenReturn(Optional.of(new User(userId, "member")));
			when(roomRepository.findByCode("ROOM99")).thenReturn(Optional.of(targetRoom));
			when(roomRepository.findAll()).thenReturn(List.of(targetRoom, r1, r2, r3));

			ApiException exception = assertThrows(
					ApiException.class,
					() -> roomService.joinByCode(new JoinRoomRequest("ROOM99", userId))
			);

			assertEquals(HttpStatus.CONFLICT, exception.getStatus());
			verify(roomRepository, never()).save(any(Room.class));
		}
	}

	@Test
	void listRoomUsersShouldReturnOwnerAndMembers() {
		LinkedHashSet<String> members = new LinkedHashSet<>();
		members.add("owner-1");
		members.add("member-1");
		Room room = room("room-1", "General", "GEN01", "owner-1", members);

		when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
		when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "ownerNick")));
		when(userRepository.findById("member-1")).thenReturn(Optional.of(new User("member-1", "memberNick")));

		List<RoomUserResponse> users = roomService.listRoomUsers("room-1");

		assertEquals(2, users.size());
		assertEquals("owner", users.get(0).role());
		assertEquals("member", users.get(1).role());
	}

	private static Room room(String id, String name, String code, String ownerId, Set<String> members) {
		return new Room(id, name, code, ownerId, members);
	}
}
