package com.guiamaral.real_time_chat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.guiamaral.real_time_chat.dto.presence.PresenceMemberResponse;
import com.guiamaral.real_time_chat.dto.presence.RoomPresenceResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

	private final RoomRepository roomRepository;
	private final UserRepository userRepository;

	private final Object lock = new Object();
	private final Map<String, SessionPresence> sessionsById = new HashMap<>();
	private final Map<String, Map<String, Integer>> roomUserSessionCounts = new HashMap<>();

	public PresenceService(RoomRepository roomRepository, UserRepository userRepository) {
		this.roomRepository = roomRepository;
		this.userRepository = userRepository;
	}

	public List<PresenceUpdate> join(String roomId, String userId, String sessionId) {
		Room room = findRoomOrThrow(roomId);
		findUserOrThrow(userId, "user not found");
		ensureRoomMembership(room, userId);

		synchronized (lock) {
			List<PresenceUpdate> updates = new ArrayList<>();
			updates.addAll(removeSessionLocked(sessionId));

			sessionsById.put(sessionId, new SessionPresence(roomId, userId));
			roomUserSessionCounts
					.computeIfAbsent(roomId, ignored -> new LinkedHashMap<>())
					.merge(userId, 1, Integer::sum);

			updates.add(new PresenceUpdate(roomId, buildRoomPresenceLocked(roomId)));
			return mergeUpdatesByRoom(updates);
		}
	}

	public List<PresenceUpdate> leave(String roomId, String userId, String sessionId) {
		synchronized (lock) {
			SessionPresence activeSession = sessionsById.get(sessionId);
			if (activeSession == null) {
				return List.of();
			}

			if (!activeSession.roomId().equals(roomId) || !activeSession.userId().equals(userId)) {
				throw new ApiException(HttpStatus.FORBIDDEN, "session is not registered for provided room/user");
			}

			return removeSessionLocked(sessionId);
		}
	}

	public List<PresenceUpdate> disconnect(String sessionId) {
		synchronized (lock) {
			return removeSessionLocked(sessionId);
		}
	}

	private List<PresenceUpdate> removeSessionLocked(String sessionId) {
		SessionPresence previous = sessionsById.remove(sessionId);
		if (previous == null) {
			return List.of();
		}

		Map<String, Integer> sessionCounts = roomUserSessionCounts.get(previous.roomId());
		if (sessionCounts != null) {
			Integer current = sessionCounts.get(previous.userId());
			if (current != null) {
				if (current <= 1) {
					sessionCounts.remove(previous.userId());
				} else {
					sessionCounts.put(previous.userId(), current - 1);
				}
			}

			if (sessionCounts.isEmpty()) {
				roomUserSessionCounts.remove(previous.roomId());
			}
		}

		return List.of(new PresenceUpdate(previous.roomId(), buildRoomPresenceLocked(previous.roomId())));
	}

	private RoomPresenceResponse buildRoomPresenceLocked(String roomId) {
		Map<String, Integer> countsByUserId = roomUserSessionCounts.get(roomId);
		if (countsByUserId == null || countsByUserId.isEmpty()) {
			return new RoomPresenceResponse(roomId, 0, List.of());
		}

		Set<String> userIds = countsByUserId.entrySet().stream()
				.filter(entry -> entry.getValue() != null && entry.getValue() > 0)
				.map(Map.Entry::getKey)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (userIds.isEmpty()) {
			return new RoomPresenceResponse(roomId, 0, List.of());
		}

		Map<String, String> nicknamesByUserId = StreamSupport.stream(userRepository.findAllById(userIds).spliterator(), false)
				.collect(Collectors.toMap(User::getId, User::getNickname));

		List<PresenceMemberResponse> members = userIds.stream()
				.map(userId -> new PresenceMemberResponse(userId, nicknamesByUserId.getOrDefault(userId, "")))
				.toList();

		return new RoomPresenceResponse(roomId, members.size(), members);
	}

	private Room findRoomOrThrow(String roomId) {
		return roomRepository.findById(roomId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "room not found"));
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

	private List<PresenceUpdate> mergeUpdatesByRoom(List<PresenceUpdate> updates) {
		Map<String, PresenceUpdate> byRoom = new LinkedHashMap<>();
		for (PresenceUpdate update : updates) {
			byRoom.put(update.roomId(), update);
		}
		return new ArrayList<>(byRoom.values());
	}

	private record SessionPresence(String roomId, String userId) {
	}

	public record PresenceUpdate(String roomId, RoomPresenceResponse payload) {
	}
}
