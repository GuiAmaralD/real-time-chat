package com.guiamaral.real_time_chat.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.guiamaral.real_time_chat.dto.message.MessageResponse;
import com.guiamaral.real_time_chat.dto.message.SendMessageRequest;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Message;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.MessageRepository;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 200;

	private final MessageRepository messageRepository;
	private final RoomRepository roomRepository;
	private final UserRepository userRepository;

	public MessageService(
			MessageRepository messageRepository,
			RoomRepository roomRepository,
			UserRepository userRepository
	) {
		this.messageRepository = messageRepository;
		this.roomRepository = roomRepository;
		this.userRepository = userRepository;
	}

	public MessageResponse send(String roomId, SendMessageRequest request) {
		Room room = findRoomOrThrow(roomId);
		User sender = findUserOrThrow(request.userId(), "user not found");
		ensureRoomMembership(room, request.userId());

		Message message = new Message();
		message.setRoomId(roomId);
		message.setUserId(request.userId());
		message.setContent(request.content().trim());

		Message savedMessage = messageRepository.append(message);
		return toResponse(savedMessage, sender.getNickname());
	}

	public List<MessageResponse> listRecent(String roomId, String userId, Integer limit) {
		Room room = findRoomOrThrow(roomId);
		ensureUserExists(userId, "user not found");
		ensureRoomMembership(room, userId);

		int resolvedLimit = resolveLimit(limit);
		List<Message> messages = messageRepository.findRecentByRoomId(roomId, resolvedLimit);
		Map<String, String> nicknamesByUserId = resolveNicknamesByUserId(messages);

		return messages.stream()
				.map(message -> toResponse(
						message,
						nicknamesByUserId.getOrDefault(message.getUserId(), "")
				))
				.toList();
	}

	private Room findRoomOrThrow(String roomId) {
		return roomRepository.findById(roomId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "room not found"));
	}

	private void ensureUserExists(String userId, String errorMessage) {
		if (userRepository.findById(userId).isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, errorMessage);
		}
	}

	private User findUserOrThrow(String userId, String errorMessage) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, errorMessage));
	}

	private void ensureRoomMembership(Room room, String userId) {
		Set<String> memberIds = room.getMemberIds() == null
				? Set.of()
				: new LinkedHashSet<>(room.getMemberIds());

		if (!memberIds.contains(userId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "user is not a room member");
		}
	}

	private int resolveLimit(Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		if (limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private Map<String, String> resolveNicknamesByUserId(List<Message> messages) {
		Set<String> userIds = messages.stream()
				.map(Message::getUserId)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (userIds.isEmpty()) {
			return Map.of();
		}

		return StreamSupport.stream(userRepository.findAllById(userIds).spliterator(), false)
				.collect(Collectors.toMap(User::getId, User::getNickname));
	}

	private MessageResponse toResponse(Message message, String userNickname) {
		return new MessageResponse(
				message.getId(),
				message.getRoomId(),
				message.getUserId(),
				userNickname,
				message.getContent(),
				message.getSentAt()
		);
	}
}
