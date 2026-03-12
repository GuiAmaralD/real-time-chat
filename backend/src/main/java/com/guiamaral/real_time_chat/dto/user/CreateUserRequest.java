package com.guiamaral.real_time_chat.dto.user;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
		@NotBlank(message = "nickname is required") String nickname
) {
}
