package com.guiamaral.real_time_chat.dto.message;

import java.time.Instant;

public record MessageResponse(
		String id,
		String roomId,
		String userId,
		String userNickname,
		String content,
		Instant sentAt
) {
}
