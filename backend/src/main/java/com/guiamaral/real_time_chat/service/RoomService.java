package com.guiamaral.real_time_chat.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import com.guiamaral.real_time_chat.dto.room.CreateRoomRequest;
import com.guiamaral.real_time_chat.dto.room.JoinRoomRequest;
import com.guiamaral.real_time_chat.dto.room.RoomResponse;
import com.guiamaral.real_time_chat.dto.room.RoomUserResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

	private static final int MAX_ROOMS_PER_USER = 3;

	private final RoomRepository roomRepository;
	private final UserRepository userRepository;

	public RoomService(RoomRepository roomRepository, UserRepository userRepository) {
		this.roomRepository = roomRepository;
		this.userRepository = userRepository;
	}

	public RoomResponse create(CreateRoomRequest request) {
		findUserOrThrow(request.ownerId(), "owner user not found");

		if (countRoomsForUser(request.ownerId()) >= MAX_ROOMS_PER_USER) {
			throw new ApiException(HttpStatus.CONFLICT, "user reached max rooms limit");
		}

		Room room = new Room();
		room.setId(UUID.randomUUID().toString());
		room.setName(request.name());
		room.setCode(generateUniqueRoomCode());
		room.setOwnerId(request.ownerId());
		room.setMemberIds(new LinkedHashSet<>(Set.of(request.ownerId())));

		return toRoomResponse(roomRepository.save(room));
	}

	public RoomResponse joinByCode(JoinRoomRequest request) {
		findUserOrThrow(request.userId(), "user not found");
		Room room = findRoomByCodeOrThrow(request.code());

		Set<String> members = room.getMemberIds() == null
				? new LinkedHashSet<>()
				: new LinkedHashSet<>(room.getMemberIds());

		if (members.contains(request.userId())) {
			return toRoomResponse(room);
		}

		if (countRoomsForUser(request.userId()) >= MAX_ROOMS_PER_USER) {
			throw new ApiException(HttpStatus.CONFLICT, "user reached max rooms limit");
		}

		members.add(request.userId());
		room.setMemberIds(members);
		return toRoomResponse(roomRepository.save(room));
	}

	public RoomResponse findByCode(String code) {
		Room room = findRoomByCodeOrThrow(code);
		return toRoomResponse(room);
	}

	public void leave(String roomId, String userId) {
		findUserOrThrow(userId, "user not found");
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "room not found"));

		Set<String> members = room.getMemberIds() == null
				? new LinkedHashSet<>()
				: new LinkedHashSet<>(room.getMemberIds());

		if (!members.contains(userId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "user is not a room member");
		}

		members.remove(userId);
		if (members.isEmpty()) {
			roomRepository.deleteById(roomId);
			return;
		}

		if (userId.equals(room.getOwnerId())) {
			room.setOwnerId(selectOldestMember(members));
		}

		room.setMemberIds(members);
		roomRepository.save(room);
	}

	public List<RoomUserResponse> listRoomUsers(String roomId) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "room not found"));

		Set<String> members = room.getMemberIds() == null ? Set.of() : room.getMemberIds();
		return members.stream()
				.map(memberId -> toRoomUserResponse(room, memberId))
				.filter(Objects::nonNull)
				.toList();
	}

	private User findUserOrThrow(String userId, String message) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, message));
	}

	private String generateUniqueRoomCode() {
		String code;
		do {
			code = UUID.randomUUID().toString();
		} while (roomRepository.findByCode(code).isPresent());
		return code;
	}

	private Room findRoomByCodeOrThrow(String code) {
		String normalizedCode = normalizeRoomCode(code);
		return roomRepository.findByCode(normalizedCode)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "room not found"));
	}

	private String normalizeRoomCode(String code) {
		return code == null ? null : code.trim().toLowerCase(Locale.ROOT);
	}

	private String selectOldestMember(Set<String> members) {
		return members.iterator().next();
	}

	private long countRoomsForUser(String userId) {
		return StreamSupport.stream(roomRepository.findAll().spliterator(), false)
				.filter(room -> room.getMemberIds() != null && room.getMemberIds().contains(userId))
				.count();
	}

	private RoomUserResponse toRoomUserResponse(Room room, String memberId) {
		return userRepository.findById(memberId)
				.map(user -> new RoomUserResponse(
						user.getId(),
						user.getNickname(),
						memberId.equals(room.getOwnerId()) ? "owner" : "member"
				))
				.orElse(null);
	}

	private RoomResponse toRoomResponse(Room room) {
		Set<String> memberIds = room.getMemberIds() == null
				? Set.of()
				: new LinkedHashSet<>(room.getMemberIds());
		return new RoomResponse(
				room.getId(),
				room.getName(),
				room.getCode(),
				room.getOwnerId(),
				memberIds
		);
	}
}
