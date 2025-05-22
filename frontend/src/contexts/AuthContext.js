import React, { createContext, useContext, useEffect, useState, useCallback, useMemo } from 'react';
import { AuthAPIClient } from '@/lib/api/AuthAPIClient';
import { useRouter } from 'next/navigation';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [userId, setUserId] = useState(null);
  const [username, setUsername] = useState(null);
  const [token, setToken] = useState(null);
  const [isChecking, setIsChecking] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const savedUsername = localStorage.getItem('username');
    const savedToken = localStorage.getItem('token');
    if (savedUsername && savedToken) {
      setUsername(savedUsername);
      setToken(savedToken);
    }
    setIsChecking(false);
  }, []);

  // 토큰을 최신 상태로 반환하는 콜백 (APIClient에 전달할 용도)
  const getToken = useCallback(() => token, [token]);

  // API 클라이언트 인스턴스 생성 및 토큰 변경 시 갱신
  const authAPI = useMemo(() => new AuthAPIClient(getToken), [getToken]);

  const isAuthenticated = !!username && !!token;

  const persist = (userId, username, token) => {
    console.log('Persisting auth data:', { userId, username, token }); // 디버깅용
    localStorage.setItem('username', username);
    console.log('Username saved:', localStorage.getItem('username')); // 디버깅용
    localStorage.setItem('token', token);
    console.log('Token saved:', localStorage.getItem('token')); // 디버깅용
    setUserId(userId);
    setUsername(username);
    setToken(token);
  };

  const clear = () => {
    localStorage.removeItem('username');
    localStorage.removeItem('token');
    setUserId(null);
    setUsername(null);
    setToken(null);
  };

  const login = async (credentials) => {
    console.log('Login attempt with:', credentials); // 디버깅용
    const res = await authAPI.login(credentials);
    console.log('Login response:', res); // 디버깅용
    if (res.token && res.username && res.userId) {
      console.log('Login valid response:', res); // 디버깅용
      persist(res.userId, res.username, res.token);
      console.log('Auth state after persist:', { 
        userId: res.userId, 
        username: res.username, 
        token: res.token 
      }); // 디버깅용
    } else {
      console.error('Invalid login response:', res); // 디버깅용
      throw new Error('로그인 실패: 유효한 토큰 또는 사용자 정보 없음');
    }
  };

  const signup = async (userData) => {
    console.log('Signup attempt with:', userData); // 디버깅용
    const res = await authAPI.signup(userData);
    console.log('Signup response:', res); // 디버깅용
    if (res.token && res.username && res.userId) {
      console.log('Signup valid response:', res); // 디버깅용
      persist(res.userId, res.username, res.token);  // 로그인 호출 없이 바로 로그인 상태 진입
      return { ...res, autoLoginSuccess: true };
    }
    return { ...res, autoLoginSuccess: false };
  };

  const logout = () => {
    clear();
    router.push('/');
  };

  return (
    <AuthContext.Provider
      value={{
        userId,
        username,
        token,
        getToken,
        isAuthenticated,
        isChecking,
        login,
        signup,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
