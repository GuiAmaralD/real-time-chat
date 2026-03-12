package com.guiamaral.real_time_chat.dto.message;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
		@NotBlank(message = "userId is required") String userId,
		@NotBlank(message = "content is required") String content
) {
}
