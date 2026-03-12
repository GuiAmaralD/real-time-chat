import { environment } from '../../../environments/environment';

const trimTrailingSlash = (value: string): string => value.replace(/\/+$/, '');

export const apiConfig = {
  restBaseUrl: trimTrailingSlash(environment.restApiBaseUrl),
  websocketBaseUrl: trimTrailingSlash(environment.websocketBaseUrl)
} as const;
