package com.guiamaral.real_time_chat.controller;

import java.util.List;
import java.util.Set;

import com.guiamaral.real_time_chat.dto.room.CreateRoomRequest;
import com.guiamaral.real_time_chat.dto.room.JoinRoomRequest;
import com.guiamaral.real_time_chat.dto.room.RoomResponse;
import com.guiamaral.real_time_chat.dto.room.RoomUserResponse;
import com.guiamaral.real_time_chat.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

	@Mock
	private RoomService roomService;

	private RoomController roomController;

	@BeforeEach
	void setUp() {
		roomController = new RoomController(roomService);
	}

	@Test
	void createShouldReturnCreatedRoom() {
		CreateRoomRequest request = new CreateRoomRequest("General", "GEN01", "owner-1");
		RoomResponse createdRoom = roomResponse("room-1", "General", "GEN01", "owner-1", Set.of("owner-1"));
		when(roomService.create(request)).thenReturn(createdRoom);

		ResponseEntity<RoomResponse> response = roomController.create(request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("room-1", response.getBody().id());
		verify(roomService).create(request);
	}

	@Test
	void joinByCodeShouldReturnRoom() {
		JoinRoomRequest request = new JoinRoomRequest("GEN01", "member-1");
		RoomResponse joinedRoom = roomResponse("room-1", "General", "GEN01", "owner-1", Set.of("owner-1", "member-1"));
		when(roomService.joinByCode(request)).thenReturn(joinedRoom);

		ResponseEntity<RoomResponse> response = roomController.joinByCode(request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(2, response.getBody().memberIds().size());
		verify(roomService).joinByCode(request);
	}

	@Test
	void findByCodeShouldReturnRoom() {
		RoomResponse room = roomResponse("room-1", "General", "GEN01", "owner-1", Set.of("owner-1"));
		when(roomService.findByCode("GEN01")).thenReturn(room);

		ResponseEntity<RoomResponse> response = roomController.findByCodeEndpoint("GEN01");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("GEN01", response.getBody().code());
		verify(roomService).findByCode("GEN01");
	}

	@Test
	void listRoomUsersShouldReturnUsers() {
		List<RoomUserResponse> users = List.of(
				new RoomUserResponse("owner-1", "ownerNick", "owner"),
				new RoomUserResponse("member-1", "memberNick", "member")
		);
		when(roomService.listRoomUsers("room-1")).thenReturn(users);

		ResponseEntity<List<RoomUserResponse>> response = roomController.listRoomUsers("room-1");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(2, response.getBody().size());
		assertEquals("owner", response.getBody().get(0).role());
		verify(roomService).listRoomUsers("room-1");
	}

	private static RoomResponse roomResponse(String id, String name, String code, String ownerId, Set<String> memberIds) {
		return new RoomResponse(id, name, code, ownerId, memberIds);
	}
}
