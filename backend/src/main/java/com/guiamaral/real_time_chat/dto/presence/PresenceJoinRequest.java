package com.guiamaral.real_time_chat.dto.presence;

import jakarta.validation.constraints.NotBlank;

public record PresenceJoinRequest(
		@NotBlank(message = "userId is required") String userId
) {
}
