package com.guiamaral.real_time_chat.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
			CreateRoomRequest request = new CreateRoomRequest("General", "owner-1");

			when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "owner")));
			when(roomRepository.findByCode(any(String.class))).thenReturn(Optional.empty());
			when(roomRepository.findAll()).thenReturn(List.of());
			when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

			RoomResponse response = roomService.create(request);

			ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
			verify(roomRepository).save(roomCaptor.capture());
			Room persistedRoom = roomCaptor.getValue();

			assertNotNull(persistedRoom.getId());
			assertEquals("owner-1", persistedRoom.getOwnerId());
			assertTrue(persistedRoom.getMemberIds().contains("owner-1"));
			assertNotNull(response.code());
			assertEquals(response.code(), persistedRoom.getCode());
			assertEquals(response.code(), UUID.fromString(response.code()).toString());
		}

		@Test
		void createShouldThrowConflictWhenOwnerAlreadyHasThreeRooms() {
			CreateRoomRequest request = new CreateRoomRequest("General", "owner-1");
			Room r1 = room("r1", "A", "A1", "owner-1", Set.of("owner-1"));
			Room r2 = room("r2", "B", "B1", "owner-1", Set.of("owner-1"));
			Room r3 = room("r3", "C", "C1", "owner-1", Set.of("owner-1"));

			when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "owner")));
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
			when(roomRepository.findByCode("gen01")).thenReturn(Optional.of(room));
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
			when(roomRepository.findByCode("room99")).thenReturn(Optional.of(targetRoom));
			when(roomRepository.findAll()).thenReturn(List.of(targetRoom, r1, r2, r3));

			ApiException exception = assertThrows(
					ApiException.class,
					() -> roomService.joinByCode(new JoinRoomRequest("ROOM99", userId))
			);

			assertEquals(HttpStatus.CONFLICT, exception.getStatus());
			verify(roomRepository, never()).save(any(Room.class));
		}

		@Test
		void joinShouldFindRoomByCodeIgnoringCaseAndWhitespace() {
			String userId = "member-1";
			String storedCode = "f4e0c21e-8a71-4f8f-9df0-3c267be4cd7b";
			Room room = room("room-1", "General", storedCode, "owner-1", Set.of("owner-1"));

			when(userRepository.findById(userId)).thenReturn(Optional.of(new User(userId, "member")));
			when(roomRepository.findByCode(storedCode)).thenReturn(Optional.of(room));
			when(roomRepository.findAll()).thenReturn(List.of(room));
			when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

			RoomResponse response = roomService.joinByCode(new JoinRoomRequest("  F4E0C21E-8A71-4F8F-9DF0-3C267BE4CD7B  ", userId));

			assertTrue(response.memberIds().contains(userId));
			verify(roomRepository).findByCode(storedCode);
		}
	}

	@Nested
	class LeaveTests {

		@Test
		void leaveShouldRemoveMemberAndKeepOwner() {
			String roomId = "room-1";
			String userId = "member-1";
			Room room = room(roomId, "General", "GEN01", "owner-1", Set.of("owner-1", userId));

			when(userRepository.findById(userId)).thenReturn(Optional.of(new User(userId, "member")));
			when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
			when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

			roomService.leave(roomId, userId);

			ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
			verify(roomRepository).save(captor.capture());
			assertEquals("owner-1", captor.getValue().getOwnerId());
			assertEquals(Set.of("owner-1"), captor.getValue().getMemberIds());
		}

		@Test
		void leaveShouldTransferOwnershipWhenOwnerLeaves() {
			String roomId = "room-1";
			String userId = "owner-1";
			Room room = room(roomId, "General", "GEN01", userId, new LinkedHashSet<>(Set.of(userId, "member-1")));

			when(userRepository.findById(userId)).thenReturn(Optional.of(new User(userId, "owner")));
			when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
			when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

			roomService.leave(roomId, userId);

			ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
			verify(roomRepository).save(captor.capture());
			assertEquals("member-1", captor.getValue().getOwnerId());
			assertEquals(Set.of("member-1"), captor.getValue().getMemberIds());
		}

		@Test
		void leaveShouldTransferOwnershipToOldestRemainingMember() {
			String roomId = "room-1";
			String ownerId = "owner-1";
			LinkedHashSet<String> members = new LinkedHashSet<>();
			members.add(ownerId);
			members.add("member-oldest");
			members.add("member-newest");
			Room room = room(roomId, "General", "GEN01", ownerId, members);

			when(userRepository.findById(ownerId)).thenReturn(Optional.of(new User(ownerId, "owner")));
			when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
			when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

			roomService.leave(roomId, ownerId);

			ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
			verify(roomRepository).save(captor.capture());
			assertEquals("member-oldest", captor.getValue().getOwnerId());
		}

		@Test
		void leaveShouldDeleteRoomWhenLastMemberLeaves() {
			String roomId = "room-1";
			String userId = "owner-1";
			Room room = room(roomId, "General", "GEN01", userId, Set.of(userId));

			when(userRepository.findById(userId)).thenReturn(Optional.of(new User(userId, "owner")));
			when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

			roomService.leave(roomId, userId);

			verify(roomRepository).deleteById(roomId);
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

	@Test
	void findByCodeShouldIgnoreCaseAndWhitespace() {
		String storedCode = "8f3578cc-6961-4478-8915-6f51dd2caed2";
		Room room = room("room-1", "General", storedCode, "owner-1", Set.of("owner-1"));
		when(roomRepository.findByCode(storedCode)).thenReturn(Optional.of(room));

		RoomResponse response = roomService.findByCode("  8F3578CC-6961-4478-8915-6F51DD2CAED2 ");

		assertEquals(storedCode, response.code());
		verify(roomRepository).findByCode(storedCode);
	}

	private static Room room(String id, String name, String code, String ownerId, Set<String> members) {
		return new Room(id, name, code, ownerId, members);
	}
}
