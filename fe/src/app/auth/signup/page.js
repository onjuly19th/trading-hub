"use client";
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

export default function SignupPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      console.log('회원가입 요청 시작:', formData);
      
      const response = await fetch('http://localhost:8080/api/auth/signup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      console.log('서버 응답 상태:', response.status);
      const data = await response.json();
      console.log('서버 응답:', data);

      if (!response.ok) {
        throw new Error(data.message || 'Signup failed');
      }

      // 토큰 저장
      localStorage.setItem('token', data.token);
      localStorage.setItem('username', data.username);

      setMessage(data.message);
      // 3초 후 로그인 페이지로 이동
      setTimeout(() => {
        router.push('/trading');  // 바로 거래 페이지로 이동
      }, 3000);
      
    } catch (err) {
      console.error('회원가입 에러:', err);
      setError(err.message || 'Failed to create account');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100">
      <div className="max-w-md w-full p-8 bg-white rounded-lg shadow-lg relative z-0">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Create your account
          </h2>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="text-red-500 text-center">{error}</div>
          )}
          {message && (
            <div className="text-green-500 text-center">{message}</div>
          )}
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <input
                type="text"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="Username"
                value={formData.username}
                onChange={(e) => setFormData({...formData, username: e.target.value})}
              />
            </div>
            <div>
              <input
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="Password"
                value={formData.password}
                onChange={(e) => setFormData({...formData, password: e.target.value})}
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              className="w-full bg-black text-white font-bold py-3 px-4 rounded-md text-lg cursor-pointer"
            >
              Sign up
            </button>
          </div>
        </form>

        <div className="text-center mt-4 bg-white p-4 rounded-lg">
          <p className="text-sm text-gray-600">
            Already have an account?{' '}
            <Link href="/auth/login" className="font-semibold text-black hover:text-blue-700 underline">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
} 