import { api, ENDPOINTS } from './api';

export const authService = {
  // 토큰 관리
  getToken: () => {
    const token = localStorage.getItem('token');
    //console.log('Getting token:', token);
    return token;
  },
  setToken: (token) => {
    console.log('Setting token:', token);
    localStorage.setItem('token', token);
  },
  removeToken: () => localStorage.removeItem('token'),

  // 사용자 정보 관리
  getUsername: () => {
    const username = localStorage.getItem('username');
    //console.log('Getting username:', username);
    return username;
  },
  setUsername: (username) => {
    console.log('Setting username:', username);
    localStorage.setItem('username', username);
  },
  removeUsername: () => localStorage.removeItem('username'),

  // 로그인 상태 확인
  checkAuth: () => {
    const token = authService.getToken();
    const username = authService.getUsername();
    return !!(token && username); // 토큰과 사용자명이 모두 있으면 true, 아니면 false
  },

  // 인증 상태 확인 (checkAuth와 동일 - 더 직관적인 이름)
  isAuthenticated: () => {
    return authService.checkAuth();
  },

  // 회원가입
  signup: async (credentials) => {
    try {
      // 회원가입 요청
      const signupData = await api.post(ENDPOINTS.AUTH.SIGNUP, credentials);
      console.log('Signup response:', signupData);
      
      // 회원가입 성공 후 자동으로 로그인 시도
      try {
        const loginResponse = await authService.login(credentials);
        console.log('Auto login after signup successful, token received:', !!loginResponse.token);
        return { 
          ...signupData, 
          token: loginResponse.token,
          autoLoginSuccess: true 
        };
      } catch (loginError) {
        console.error('Auto login failed after signup:', loginError);
        // 로그인 실패 시에도 회원가입 데이터 반환
        return { 
          ...signupData, 
          autoLoginSuccess: false
        };
      }
    } catch (signupError) {
      console.error('Signup failed:', signupError);
      throw signupError; // 회원가입 실패 시 에러 전파
    }
  },

  // 로그인
  login: async (credentials) => {
    const response = await api.post(ENDPOINTS.AUTH.LOGIN, credentials);
    console.log('Login response:', response);
    
    // 응답 구조가 { status: 'SUCCESS', data: { token: '...' }, error: null } 형태인지 확인
    if (response.status === 'SUCCESS' && response.data && response.data.token) {
      const token = response.data.token;
      authService.setToken(token);
      authService.setUsername(credentials.username);
      return { ...response, token };
    } else {
      console.error('Login response missing token or invalid format:', response);
      throw new Error('로그인은 성공했으나 유효한 토큰을 받지 못했습니다.');
    }
  },

  // 로그아웃
  logout: () => {
    authService.removeToken();
    authService.removeUsername();
  }
}; 