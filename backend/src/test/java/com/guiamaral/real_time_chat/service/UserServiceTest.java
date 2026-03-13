package com.guiamaral.real_time_chat.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.guiamaral.real_time_chat.dto.presence.RoomPresenceResponse;
import com.guiamaral.real_time_chat.dto.user.CreateUserRequest;
import com.guiamaral.real_time_chat.dto.user.RedisPingResponse;
import com.guiamaral.real_time_chat.dto.user.UserResponse;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private PresenceService presenceService;

	@Mock
	private RedisConnectionFactory redisConnectionFactory;

	@Mock
	private RedisConnection redisConnection;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, roomRepository, presenceService, redisTemplate);
	}

	@Test
	void createShouldGenerateIdAndPersistUser() {
		when(userRepository.findAll()).thenReturn(List.of());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		CreateUserRequest request = new CreateUserRequest("gui");

		UserResponse response = userService.create(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User persistedUser = userCaptor.getValue();

		assertNotNull(persistedUser.getId());
		assertFalse(persistedUser.getId().isBlank());
		assertEquals("gui", persistedUser.getNickname());
		assertEquals(persistedUser.getId(), response.id());
		assertEquals("gui", response.nickname());
	}

	@Test
	void createShouldThrowConflictWhenNicknameAlreadyExists() {
		when(userRepository.findAll()).thenReturn(List.of(new User("user-1", "Gui")));

		ApiException exception = assertThrows(
				ApiException.class,
				() -> userService.create(new CreateUserRequest(" gui "))
		);

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		assertEquals("nickname already exists", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void pingRedisShouldReturnPong() {
		when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
		when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
		when(redisConnection.ping()).thenReturn("PONG");

		RedisPingResponse response = userService.pingRedis();

		assertEquals("PONG", response.redis());
		verify(redisConnection).close();
	}

	@Test
	void pingRedisShouldReturnNoResponseWhenNull() {
		when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
		when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
		when(redisConnection.ping()).thenReturn(null);

		RedisPingResponse response = userService.pingRedis();

		assertEquals("NO_RESPONSE", response.redis());
		verify(redisConnection).close();
	}

	@Test
	void disconnectAndDeleteShouldRemoveUserPresenceAndRooms() {
		User user = new User("user-1", "Gui");
		when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
		Room ownerRoom = new Room("room-1", "Room 1", "R1", "user-1", Set.of("user-1", "user-2"));
		Room memberRoom = new Room("room-2", "Room 2", "R2", "user-3", Set.of("user-1", "user-3"));
		Room orphanRoom = new Room("room-3", "Room 3", "R3", "user-1", Set.of("user-1"));
		when(roomRepository.findAll()).thenReturn(List.of(ownerRoom, memberRoom, orphanRoom));
		var updates = List.of(new PresenceService.PresenceUpdate("room-1", new RoomPresenceResponse("room-1", 1, List.of())));
		when(presenceService.removeUserFromAllRooms("user-1")).thenReturn(updates);

		List<PresenceService.PresenceUpdate> response = userService.disconnectAndDelete("user-1");

		assertEquals(updates, response);
		verify(presenceService).removeUserFromAllRooms("user-1");
		verify(roomRepository).deleteById("room-3");
		verify(roomRepository).save(argThat(room ->
				room.getId().equals("room-1")
						&& room.getOwnerId().equals("user-2")
						&& room.getMemberIds().equals(Set.of("user-2"))
		));
		verify(roomRepository).save(argThat(room ->
				room.getId().equals("room-2")
						&& room.getOwnerId().equals("user-3")
						&& room.getMemberIds().equals(Set.of("user-3"))
		));
		verify(userRepository).deleteById("user-1");
	}

	@Test
	void disconnectAndDeleteShouldFailWhenUserDoesNotExist() {
		when(userRepository.findById("user-1")).thenReturn(Optional.empty());

		ApiException exception = assertThrows(ApiException.class, () -> userService.disconnectAndDelete("user-1"));

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
		assertEquals("user not found", exception.getMessage());
		verifyNoInteractions(presenceService);
		verify(roomRepository, never()).findAll();
		verify(userRepository, never()).deleteById(eq("user-1"));
	}
}
