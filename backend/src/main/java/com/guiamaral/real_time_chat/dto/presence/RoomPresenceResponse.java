package com.guiamaral.real_time_chat.dto.presence;

import java.util.List;

public record RoomPresenceResponse(
		String roomId,
		int onlineCount,
		List<PresenceMemberResponse> members
) {
}
