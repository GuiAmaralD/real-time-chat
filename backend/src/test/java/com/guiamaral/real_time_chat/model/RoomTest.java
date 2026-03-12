package com.guiamaral.real_time_chat.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomTest {

	@Test
	void shouldCreateRoomWithConstructor() {
		Set<String> members = new LinkedHashSet<>();
		members.add("user-1");
		members.add("user-2");

		Room room = new Room("room-1", "General", "GEN01", "user-1", members);

		assertEquals("room-1", room.getId());
		assertEquals("General", room.getName());
		assertEquals("GEN01", room.getCode());
		assertEquals("user-1", room.getOwnerId());
		assertEquals(2, room.getMemberIds().size());
		assertTrue(room.getMemberIds().contains("user-2"));
	}

	@Test
	void shouldSetAndGetFields() {
		Room room = new Room();
		room.setId("room-2");
		room.setName("Gaming");
		room.setCode("GAM01");
		room.setOwnerId("user-9");
		room.setMemberIds(Set.of("user-9"));

		assertEquals("room-2", room.getId());
		assertEquals("Gaming", room.getName());
		assertEquals("GAM01", room.getCode());
		assertEquals("user-9", room.getOwnerId());
		assertTrue(room.getMemberIds().contains("user-9"));
	}

	@Test
	void shouldKeepMembersAsEmptySetWhenNullProvided() {
		Room room = new Room();
		room.setMemberIds(null);

		assertNotNull(room.getMemberIds());
		assertTrue(room.getMemberIds().isEmpty());
	}
}
