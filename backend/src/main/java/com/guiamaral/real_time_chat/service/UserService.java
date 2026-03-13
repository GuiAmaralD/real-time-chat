package com.guiamaral.real_time_chat.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import com.guiamaral.real_time_chat.dto.user.CreateUserRequest;
import com.guiamaral.real_time_chat.dto.user.RedisPingResponse;
import com.guiamaral.real_time_chat.dto.user.UserResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RoomRepository roomRepository;
	private final PresenceService presenceService;
	private final StringRedisTemplate redisTemplate;

	public UserService(
			UserRepository userRepository,
			RoomRepository roomRepository,
			PresenceService presenceService,
			StringRedisTemplate redisTemplate
	) {
		this.userRepository = userRepository;
		this.roomRepository = roomRepository;
		this.presenceService = presenceService;
		this.redisTemplate = redisTemplate;
	}

	public UserResponse create(CreateUserRequest request) {
		String normalizedNickname = request.nickname().trim();
		ensureNicknameAvailable(normalizedNickname);

		User user = new User();
		user.setId(UUID.randomUUID().toString());
		user.setNickname(normalizedNickname);

		User savedUser = userRepository.save(user);
		return new UserResponse(savedUser.getId(), savedUser.getNickname());
	}

	public List<PresenceService.PresenceUpdate> disconnectAndDelete(String userId) {
		findUserOrThrow(userId, "user not found");

		List<PresenceService.PresenceUpdate> presenceUpdates = presenceService.removeUserFromAllRooms(userId);
		disconnectUserFromRooms(userId);
		userRepository.deleteById(userId);

		return presenceUpdates;
	}

	private void disconnectUserFromRooms(String userId) {
		for (Room room : roomRepository.findAll()) {
			Set<String> members = room.getMemberIds() == null
					? new LinkedHashSet<>()
					: new LinkedHashSet<>(room.getMemberIds());

			boolean removedFromMembers = members.remove(userId);
			boolean isOwner = userId.equals(room.getOwnerId());
			if (!removedFromMembers && !isOwner) {
				continue;
			}

			if (isOwner) {
				if (members.isEmpty()) {
					roomRepository.deleteById(room.getId());
					continue;
				}
				room.setOwnerId(members.iterator().next());
			}

			room.setMemberIds(members);
			roomRepository.save(room);
		}
	}

	private void ensureNicknameAvailable(String nickname) {
		boolean nicknameExists = StreamSupport.stream(userRepository.findAll().spliterator(), false)
				.map(User::getNickname)
				.filter(Objects::nonNull)
				.anyMatch(existingNickname -> existingNickname.equalsIgnoreCase(nickname));

		if (nicknameExists) {
			throw new ApiException(HttpStatus.CONFLICT, "nickname already exists");
		}
	}

	private User findUserOrThrow(String userId, String errorMessage) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, errorMessage));
	}

	public RedisPingResponse pingRedis() {
		RedisConnectionFactory connectionFactory = Objects.requireNonNull(
				redisTemplate.getConnectionFactory(),
				"RedisConnectionFactory nao foi inicializada."
		);

		String response;
		try (RedisConnection connection = connectionFactory.getConnection()) {
			response = connection.ping();
		}

		return new RedisPingResponse(response == null ? "NO_RESPONSE" : response);
	}
}
