export interface CreateUserRequest {
  nickname: string;
}

export interface UserResponse {
  id: string;
  nickname: string;
}

export interface RedisPingResponse {
  response: string;
}
