package com.guiamaral.real_time_chat.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.guiamaral.real_time_chat.model.Room;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.redis.verify-on-startup=false")
@ActiveProfiles("dev")
class RoomRepositoryTest {

	@Autowired
	private RoomRepository roomRepository;

	private final List<String> createdRoomIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		for (String roomId : createdRoomIds) {
			roomRepository.deleteById(roomId);
		}
		createdRoomIds.clear();
	}

	@Test
	void shouldSaveAndFindRoomById() {
		String roomId = UUID.randomUUID().toString();
		Room room = new Room(roomId, "General", "GEN-" + roomId.substring(0, 8), "owner-1", Set.of("owner-1"));
		createdRoomIds.add(roomId);

		roomRepository.save(room);
		Optional<Room> loadedRoom = roomRepository.findById(roomId);

		assertTrue(loadedRoom.isPresent());
		assertEquals("General", loadedRoom.get().getName());
		assertEquals("owner-1", loadedRoom.get().getOwnerId());
	}

	@Test
	void shouldPersistMembersSet() {
		String roomId = UUID.randomUUID().toString();
		LinkedHashSet<String> members = new LinkedHashSet<>();
		members.add("owner-2");
		members.add("member-2");
		Room room = new Room(roomId, "Dev", "DEV-" + roomId.substring(0, 8), "owner-2", members);
		createdRoomIds.add(roomId);

		roomRepository.save(room);
		Optional<Room> loadedRoom = roomRepository.findById(roomId);

		assertTrue(loadedRoom.isPresent());
		assertEquals(2, loadedRoom.get().getMemberIds().size());
		assertTrue(loadedRoom.get().getMemberIds().contains("member-2"));
	}

	@Test
	void shouldDeleteRoomById() {
		String roomId = UUID.randomUUID().toString();
		Room room = new Room(roomId, "Delete", "DEL-" + roomId.substring(0, 8), "owner-3", Set.of("owner-3"));

		roomRepository.save(room);
		roomRepository.deleteById(roomId);

		Optional<Room> loadedRoom = roomRepository.findById(roomId);
		assertFalse(loadedRoom.isPresent());
	}

	@Test
	void shouldFindRoomByCode() {
		String roomId = UUID.randomUUID().toString();
		String code = "CODE-" + roomId.substring(0, 8);
		Room room = new Room(roomId, "Lookup", code, "owner-4", Set.of("owner-4"));
		createdRoomIds.add(roomId);

		roomRepository.save(room);
		Optional<Room> loadedRoom = roomRepository.findByCode(code);

		assertTrue(loadedRoom.isPresent());
		assertEquals(roomId, loadedRoom.get().getId());
		assertEquals(code, loadedRoom.get().getCode());
	}
}
