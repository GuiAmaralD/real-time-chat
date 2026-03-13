export interface CreateRoomRequest {
  name: string;
  ownerId: string;
}

export interface JoinRoomRequest {
  code: string;
  userId: string;
}

export interface LeaveRoomRequest {
  userId: string;
}

export interface RoomResponse {
  id: string;
  name: string;
  code: string;
  ownerId: string;
  memberIds: string[];
}

export interface RoomUserResponse {
  id: string;
  nickname: string;
  role: 'owner' | 'member';
}
