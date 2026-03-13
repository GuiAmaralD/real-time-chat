package com.guiamaral.real_time_chat.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.guiamaral.real_time_chat.model.Message;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisMessageRepository implements MessageRepository {

	private static final Duration MESSAGE_RETENTION = Duration.ofHours(24);
	private static final Duration STREAM_KEY_TTL = Duration.ofHours(24);

	private final StringRedisTemplate redisTemplate;

	public RedisMessageRepository(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public Message append(Message message) {
		Instant now = Instant.now();
		String streamKey = streamKey(message.getRoomId());

		Map<String, String> fields = new HashMap<>();
		fields.put("roomId", message.getRoomId());
		fields.put("userId", message.getUserId());
		fields.put("userNickname", safeString(message.getUserNickname()));
		fields.put("content", message.getContent());
		fields.put("sentAt", Long.toString(now.toEpochMilli()));

		RecordId minId = RecordId.of(now.minus(MESSAGE_RETENTION).toEpochMilli(), 0);
		XAddOptions addOptions = XAddOptions.none().minId(minId);

		RecordId recordId = redisTemplate.opsForStream()
				.add(MapRecord.create(streamKey, fields), addOptions);

		if (recordId == null) {
			throw new IllegalStateException("Unable to append message to Redis stream.");
		}

		redisTemplate.expire(streamKey, STREAM_KEY_TTL);

		message.setId(recordId.getValue());
		message.setSentAt(now);
		return message;
	}

	@Override
	public List<Message> findRecentByRoomId(String roomId, int limit) {
		if (limit <= 0) {
			return List.of();
		}

		String streamKey = streamKey(roomId);
		List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
				.reverseRange(streamKey, Range.unbounded(), Limit.limit().count(limit));

		if (records == null || records.isEmpty()) {
			return List.of();
		}

		List<Message> messages = new ArrayList<>(records.size());
		Instant cutoff = Instant.now().minus(MESSAGE_RETENTION);
		for (int i = records.size() - 1; i >= 0; i--) {
			MapRecord<String, Object, Object> record = records.get(i);
			Map<Object, Object> values = record.getValue();

			Message message = new Message();
			message.setId(record.getId().getValue());
			message.setRoomId(stringValue(values.get("roomId"), roomId));
			message.setUserId(stringValue(values.get("userId"), ""));
			message.setUserNickname(stringValue(values.get("userNickname"), ""));
			message.setContent(stringValue(values.get("content"), ""));
			message.setSentAt(toSentAt(values.get("sentAt"), record.getId()));

			if (!message.getSentAt().isBefore(cutoff)) {
				messages.add(message);
			}
		}

		return messages;
	}

	private String streamKey(String roomId) {
		return "room:" + roomId + ":messages";
	}

	private String stringValue(Object value, String defaultValue) {
		return value == null ? defaultValue : value.toString();
	}

	private String safeString(String value) {
		return value == null ? "" : value;
	}

	private Instant toSentAt(Object value, RecordId recordId) {
		if (value != null) {
			try {
				return Instant.ofEpochMilli(Long.parseLong(value.toString()));
			} catch (NumberFormatException ignored) {
			}
		}
		Long timestamp = recordId.getTimestamp();
		return Instant.ofEpochMilli(timestamp == null ? 0 : timestamp);
	}
}
