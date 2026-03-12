package com.guiamaral.real_time_chat.dto.room;

import java.util.Set;

public record RoomResponse(
		String id,
		String name,
		String code,
		String ownerId,
		Set<String> memberIds
) {
}
