import React, { createContext, useContext, useEffect, useState, useCallback, useMemo } from 'react';
import { AuthAPIClient } from '@/lib/api/AuthAPIClient';
import { useRouter } from 'next/navigation';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isChecking, setIsChecking] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const savedUsername = localStorage.getItem('username');
    const savedToken = localStorage.getItem('token');
    if (savedUsername && savedToken) {
      setUser(savedUsername);
      setToken(savedToken);
    }
    setIsChecking(false);
  }, []);

  // 토큰을 최신 상태로 반환하는 콜백 (APIClient에 전달할 용도)
  const getToken = useCallback(() => token, [token]);

  // API 클라이언트 인스턴스 생성 및 토큰 변경 시 갱신
  const authAPI = useMemo(() => new AuthAPIClient(getToken), [getToken]);

  const isAuthenticated = !!user && !!token;

  const persist = (username, token) => {
    localStorage.setItem('username', username);
    localStorage.setItem('token', token);
    setUser(username);
    setToken(token);
  };

  const clear = () => {
    localStorage.removeItem('username');
    localStorage.removeItem('token');
    setUser(null);
    setToken(null);
  };

  const login = async (credentials) => {
    const res = await authAPI.login(credentials);
    if (res.token && res.username) {
      persist(res.username, res.token);
    } else {
      throw new Error('로그인 실패: 유효한 토큰 또는 사용자 정보 없음');
    }
  };

  const signup = async (userData) => {
    const res = await authAPI.signup(userData);
    if (res.token && res.username) {
      persist(res.username, res.token);  // 로그인 호출 없이 바로 로그인 상태 진입
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
        user,
        token,
        isAuthenticated,
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
