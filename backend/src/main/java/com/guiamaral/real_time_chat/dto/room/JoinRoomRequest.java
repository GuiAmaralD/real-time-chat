package com.guiamaral.real_time_chat.dto.room;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
		@NotBlank(message = "code is required") String code,
		@NotBlank(message = "userId is required") String userId
) {
}
