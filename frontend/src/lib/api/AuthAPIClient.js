import { BaseAPIClient } from './BaseAPIClient';
import { ENDPOINTS } from '@/config/constants';

export class AuthAPIClient extends BaseAPIClient {
  constructor(getToken) {
    super({ getToken });
  }

  async login(credentials) {
    return this.post(ENDPOINTS.AUTH.LOGIN, credentials);
  }

  async signup(userData) {
    return this.post(ENDPOINTS.AUTH.SIGNUP, userData);
  }
}
