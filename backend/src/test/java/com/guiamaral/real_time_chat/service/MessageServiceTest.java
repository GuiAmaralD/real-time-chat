package com.guiamaral.real_time_chat.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.guiamaral.real_time_chat.dto.message.MessageResponse;
import com.guiamaral.real_time_chat.dto.message.SendMessageRequest;
import com.guiamaral.real_time_chat.exception.ApiException;
import com.guiamaral.real_time_chat.model.Message;
import com.guiamaral.real_time_chat.model.Room;
import com.guiamaral.real_time_chat.model.User;
import com.guiamaral.real_time_chat.repository.MessageRepository;
import com.guiamaral.real_time_chat.repository.RoomRepository;
import com.guiamaral.real_time_chat.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private UserRepository userRepository;

	private MessageService messageService;

	@BeforeEach
	void setUp() {
		messageService = new MessageService(messageRepository, roomRepository, userRepository);
	}

	@Test
	void sendShouldPersistMessageWhenUserIsMember() {
		Room room = room("room-1", Set.of("owner-1", "user-1"));
		when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
		when(userRepository.findById("user-1")).thenReturn(Optional.of(new User("user-1", "nick")));
		when(messageRepository.append(any(Message.class))).thenAnswer(invocation -> {
			Message message = invocation.getArgument(0);
			message.setId("1700000000000-0");
			message.setSentAt(Instant.parse("2026-03-12T16:00:00Z"));
			return message;
		});

		MessageResponse response = messageService.send("room-1", new SendMessageRequest("user-1", " hello "));

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(messageRepository).append(messageCaptor.capture());
		Message persisted = messageCaptor.getValue();

		assertEquals("room-1", persisted.getRoomId());
		assertEquals("user-1", persisted.getUserId());
		assertEquals("hello", persisted.getContent());
		assertEquals("1700000000000-0", response.id());
		assertEquals("nick", response.userNickname());
	}

	@Test
	void sendShouldThrowForbiddenWhenUserIsNotRoomMember() {
		Room room = room("room-1", Set.of("owner-1"));
		when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
		when(userRepository.findById("user-1")).thenReturn(Optional.of(new User("user-1", "nick")));

		ApiException exception = assertThrows(
				ApiException.class,
				() -> messageService.send("room-1", new SendMessageRequest("user-1", "hello"))
		);

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
		assertEquals("user is not a room member", exception.getMessage());
	}

	@Test
	void listRecentShouldUseDefaultLimitWhenNotProvided() {
		Room room = room("room-1", Set.of("owner-1"));
		when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
		when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "owner")));
		when(messageRepository.findRecentByRoomId("room-1", 50)).thenReturn(List.of());

		messageService.listRecent("room-1", "owner-1", null);

		verify(messageRepository).findRecentByRoomId("room-1", 50);
	}

	@Test
	void listRecentShouldCapLimitToMax() {
		Room room = room("room-1", Set.of("owner-1"));
		when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
		when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "owner")));
		when(messageRepository.findRecentByRoomId("room-1", 200)).thenReturn(List.of());

		messageService.listRecent("room-1", "owner-1", 999);

		verify(messageRepository).findRecentByRoomId("room-1", 200);
	}

	@Test
	void listRecentShouldKeepMessageOrderFromRepository() {
		Room room = room("room-1", Set.of("owner-1"));
		when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
		when(userRepository.findById("owner-1")).thenReturn(Optional.of(new User("owner-1", "owner")));

		List<Message> messages = List.of(
				new Message("1-0", "room-1", "owner-1", "first", Instant.parse("2026-03-12T16:00:00Z")),
				new Message("2-0", "room-1", "owner-1", "second", Instant.parse("2026-03-12T16:00:01Z"))
		);
		when(messageRepository.findRecentByRoomId("room-1", 2)).thenReturn(messages);
		when(userRepository.findAllById(Set.of("owner-1"))).thenReturn(List.of(new User("owner-1", "owner")));

		List<MessageResponse> response = messageService.listRecent("room-1", "owner-1", 2);

		assertEquals(2, response.size());
		assertEquals("first", response.get(0).content());
		assertEquals("second", response.get(1).content());
		assertEquals("owner", response.get(0).userNickname());
		assertTrue(response.get(0).sentAt().isBefore(response.get(1).sentAt()));
	}

	private Room room(String id, Set<String> members) {
		return new Room(id, "General", "GEN", "owner-1", new LinkedHashSet<>(members));
	}
}
