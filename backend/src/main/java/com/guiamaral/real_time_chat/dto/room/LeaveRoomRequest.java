package com.guiamaral.real_time_chat.dto.room;

import jakarta.validation.constraints.NotBlank;

public record LeaveRoomRequest(
		@NotBlank(message = "userId is required") String userId
) {
}
