import React, { useState } from 'react';
import AuthInput from '../../Common/AuthInput';
import Link from 'next/link';

const LoginForm = ({ onSubmit, error }) => {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="mt-8 space-y-6">
      <div className="space-y-4">
        <AuthInput
          label="아이디"
          name="username"
          type="text"
          placeholder="아이디를 입력하세요"
          value={formData.username}
          onChange={handleChange}
          required={true}
        />
        <AuthInput
          label="비밀번호"
          name="password"
          type="password"
          placeholder="비밀번호를 입력하세요"
          value={formData.password}
          onChange={handleChange}
          required={true}
        />
      </div>

      {error && (
        <div className="text-red-500 text-sm mt-2">
          {error}
        </div>
      )}

      <div>
        <button
          type="submit"
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
        >
          로그인
        </button>
      </div>

      <div className="text-sm text-center mt-4">
        <p className="text-gray-600">
          계정이 없으신가요?{" "}
          <Link 
            href="/auth/signup"
            className="font-medium text-blue-600 hover:text-blue-500"
          >
            회원가입
          </Link>
        </p>
      </div>
    </form>
  );
};

export default LoginForm;
