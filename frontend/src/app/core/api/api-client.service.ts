import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { ApiErrorResponse } from '../models/common.model';
import { apiConfig } from '../config/api.config';

type QueryValue = string | number | boolean | null | undefined;

@Injectable({ providedIn: 'root' })
export class ApiClientService {
  private readonly http = inject(HttpClient);

  get<TResponse>(path: string, query?: Record<string, QueryValue>): Observable<TResponse> {
    return this.http
      .get<TResponse>(this.buildUrl(path), { params: this.buildParams(query) })
      .pipe(catchError((error: HttpErrorResponse) => this.handleError(error)));
  }

  post<TRequest, TResponse>(path: string, body: TRequest): Observable<TResponse> {
    return this.http
      .post<TResponse>(this.buildUrl(path), body)
      .pipe(catchError((error: HttpErrorResponse) => this.handleError(error)));
  }

  private buildUrl(path: string): string {
    return `${apiConfig.restBaseUrl}${path}`;
  }

  private buildParams(query?: Record<string, QueryValue>): HttpParams {
    let params = new HttpParams();
    if (!query) {
      return params;
    }

    for (const [key, value] of Object.entries(query)) {
      if (value === null || value === undefined) {
        continue;
      }

      params = params.set(key, String(value));
    }

    return params;
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    if (error.error && typeof error.error === 'object' && 'error' in error.error) {
      const apiError = error.error as ApiErrorResponse;
      return throwError(() => new Error(apiError.error));
    }

    return throwError(() => new Error(error.message || 'Unexpected API error'));
  }
}
