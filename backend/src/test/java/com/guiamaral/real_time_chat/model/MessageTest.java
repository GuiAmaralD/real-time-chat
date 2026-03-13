package com.guiamaral.real_time_chat.model;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTest {

	@Test
	void shouldSetAndGetAllFields() {
		Instant sentAt = Instant.now();
		Message message = new Message();
		message.setId("msg-1");
		message.setRoomId("room-1");
		message.setUserId("user-1");
		message.setUserNickname("Gui");
		message.setContent("hello");
		message.setSentAt(sentAt);

		assertEquals("msg-1", message.getId());
		assertEquals("room-1", message.getRoomId());
		assertEquals("user-1", message.getUserId());
		assertEquals("Gui", message.getUserNickname());
		assertEquals("hello", message.getContent());
		assertEquals(sentAt, message.getSentAt());
	}

	@Test
	void shouldCreateWithConstructor() {
		Instant sentAt = Instant.now();
		Message message = new Message("msg-2", "room-2", "user-2", "Ana", "hi", sentAt);

		assertEquals("msg-2", message.getId());
		assertEquals("room-2", message.getRoomId());
		assertEquals("user-2", message.getUserId());
		assertEquals("Ana", message.getUserNickname());
		assertEquals("hi", message.getContent());
		assertEquals(sentAt, message.getSentAt());
	}
}
