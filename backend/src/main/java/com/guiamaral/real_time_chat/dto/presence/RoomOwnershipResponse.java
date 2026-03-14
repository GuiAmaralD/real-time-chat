package com.guiamaral.real_time_chat.dto.presence;

public record RoomOwnershipResponse(
		String roomId,
		String ownerId
) {
}
