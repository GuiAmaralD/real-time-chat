package com.guiamaral.real_time_chat.dto.room;

import jakarta.validation.constraints.NotBlank;

public record CreateRoomRequest(
		@NotBlank(message = "name is required") String name,
		@NotBlank(message = "code is required") String code,
		@NotBlank(message = "ownerId is required") String ownerId
) {
}
