export interface SendMessageRequest {
  userId: string;
  content: string;
}

export interface MessageResponse {
  id: string;
  roomId: string;
  userId: string;
  nickname: string;
  content: string;
  sentAt: string;
}
