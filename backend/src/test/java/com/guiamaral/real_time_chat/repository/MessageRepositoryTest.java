package com.guiamaral.real_time_chat.repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.guiamaral.real_time_chat.model.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.redis.verify-on-startup=false")
@ActiveProfiles("dev")
class MessageRepositoryTest {

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@AfterEach
	void cleanUp() {
		var keys = redisTemplate.keys("room:*:messages");
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}
	}

	@Test
	void shouldAppendAndReadMessagesInAscendingOrder() {
		String roomId = "room-stream-order";

		Message first = new Message();
		first.setRoomId(roomId);
		first.setUserId("user-1");
		first.setUserNickname("Gui");
		first.setContent("first");

		Message second = new Message();
		second.setRoomId(roomId);
		second.setUserId("user-1");
		second.setUserNickname("Gui");
		second.setContent("second");

		messageRepository.append(first);
		messageRepository.append(second);

		List<Message> messages = messageRepository.findRecentByRoomId(roomId, 10);

		assertEquals(2, messages.size());
		assertEquals("first", messages.get(0).getContent());
		assertEquals("second", messages.get(1).getContent());
		assertEquals("Gui", messages.get(0).getUserNickname());
		assertTrue(messages.get(0).getSentAt().compareTo(messages.get(1).getSentAt()) <= 0);
	}

	@Test
	void shouldKeepOnlyLast24HoursWhenAppendingNewMessage() {
		String roomId = "room-stream-retention";
		String streamKey = "room:" + roomId + ":messages";

		long oldTimestamp = Instant.now().minusSeconds(25 * 3600).toEpochMilli();
		MapRecord<String, String, String> oldRecord = MapRecord.create(
				streamKey,
				Map.of(
						"roomId", roomId,
						"userId", "user-1",
						"userNickname", "Gui",
						"content", "old",
						"sentAt", Long.toString(oldTimestamp)
				)
		).withId(RecordId.of(oldTimestamp, 0));
		redisTemplate.opsForStream().add(oldRecord);

		Message recent = new Message();
		recent.setRoomId(roomId);
		recent.setUserId("user-1");
		recent.setUserNickname("Gui");
		recent.setContent("recent");
		messageRepository.append(recent);

		List<Message> messages = messageRepository.findRecentByRoomId(roomId, 10);

		assertEquals(1, messages.size());
		assertEquals("recent", messages.get(0).getContent());
		assertFalse(messages.stream().anyMatch(message -> "old".equals(message.getContent())));
	}
}
