import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService } from '../api/api-client.service';
import { API_ENDPOINTS } from '../api/endpoints';
import {
  CreateRoomRequest,
  JoinRoomRequest,
  RoomResponse,
  RoomUserResponse
} from '../models/room.model';

@Injectable({ providedIn: 'root' })
export class RoomService {
  private readonly apiClient = inject(ApiClientService);

  create(payload: CreateRoomRequest): Observable<RoomResponse> {
    return this.apiClient.post<CreateRoomRequest, RoomResponse>(API_ENDPOINTS.rooms.create, payload);
  }

  joinByCode(payload: JoinRoomRequest): Observable<RoomResponse> {
    return this.apiClient.post<JoinRoomRequest, RoomResponse>(API_ENDPOINTS.rooms.join, payload);
  }

  findByCode(code: string): Observable<RoomResponse> {
    return this.apiClient.get<RoomResponse>(API_ENDPOINTS.rooms.byCode(code));
  }

  listUsers(roomId: string): Observable<RoomUserResponse[]> {
    return this.apiClient.get<RoomUserResponse[]>(API_ENDPOINTS.rooms.users(roomId));
  }
}
