export class TokenManager {
  static instance = null;

  static getInstance() {
    if (!TokenManager.instance) {
      TokenManager.instance = new TokenManager();
    }
    return TokenManager.instance;
  }

  getToken() {
    return localStorage.getItem('token');
  }

  setToken(token) {
    if (token) {
      localStorage.setItem('token', token);
    } else {
      this.removeToken();
    }
  }

  removeToken() {
    localStorage.removeItem('token');
  }

  getUsername() {
    return localStorage.getItem('username');
  }

  setUsername(username) {
    if (username) {
      localStorage.setItem('username', username);
    } else {
      this.removeUsername();
    }
  }

  removeUsername() {
    localStorage.removeItem('username');
  }

  isAuthenticated() {
    const token = this.getToken();
    const username = this.getUsername();
    return !!(token && username);
  }
} 