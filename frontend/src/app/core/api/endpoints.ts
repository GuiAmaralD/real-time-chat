const encoded = (value: string): string => encodeURIComponent(value);

export const API_ENDPOINTS = {
  users: {
    create: '/users',
    pingRedis: '/users/redis/ping',
    disconnect: (userId: string): string => `/users/${encoded(userId)}/disconnect`
  },
  rooms: {
    create: '/rooms',
    join: '/rooms/join',
    leave: (roomId: string): string => `/rooms/${encoded(roomId)}/leave`,
    byCode: (code: string): string => `/rooms/code/${encoded(code)}`,
    users: (roomId: string): string => `/rooms/${encoded(roomId)}/users`,
    messages: {
      list: (roomId: string): string => `/rooms/${encoded(roomId)}/messages`,
      send: (roomId: string): string => `/rooms/${encoded(roomId)}/messages`
    }
  },
  websocket: {
    endpoint: '/ws',
    appSendRoomMessage: (roomId: string): string => `/app/rooms/${encoded(roomId)}/messages`,
    topicRoomMessages: (roomId: string): string => `/topic/rooms/${encoded(roomId)}/messages`,
    appJoinRoomPresence: (roomId: string): string => `/app/rooms/${encoded(roomId)}/presence/join`,
    appLeaveRoomPresence: (roomId: string): string => `/app/rooms/${encoded(roomId)}/presence/leave`,
    topicRoomPresence: (roomId: string): string => `/topic/rooms/${encoded(roomId)}/presence`
  }
} as const;
