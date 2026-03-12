import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService } from '../api/api-client.service';
import { API_ENDPOINTS } from '../api/endpoints';
import { MessageResponse, SendMessageRequest } from '../models/message.model';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly apiClient = inject(ApiClientService);

  send(roomId: string, payload: SendMessageRequest): Observable<MessageResponse> {
    return this.apiClient.post<SendMessageRequest, MessageResponse>(
      API_ENDPOINTS.rooms.messages.send(roomId),
      payload
    );
  }

  listRecent(roomId: string, userId: string, limit?: number): Observable<MessageResponse[]> {
    return this.apiClient.get<MessageResponse[]>(API_ENDPOINTS.rooms.messages.list(roomId), {
      userId,
      limit
    });
  }
}
