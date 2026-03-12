package com.guiamaral.real_time_chat.repository;

import java.util.List;

import com.guiamaral.real_time_chat.model.Message;

public interface MessageRepository {
	Message append(Message message);
	List<Message> findRecentByRoomId(String roomId, int limit);
}
