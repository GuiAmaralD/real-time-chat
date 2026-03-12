import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService } from '../api/api-client.service';
import { API_ENDPOINTS } from '../api/endpoints';
import { CreateUserRequest, RedisPingResponse, UserResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiClient = inject(ApiClientService);

  create(payload: CreateUserRequest): Observable<UserResponse> {
    return this.apiClient.post<CreateUserRequest, UserResponse>(API_ENDPOINTS.users.create, payload);
  }

  pingRedis(): Observable<RedisPingResponse> {
    return this.apiClient.get<RedisPingResponse>(API_ENDPOINTS.users.pingRedis);
  }
}
