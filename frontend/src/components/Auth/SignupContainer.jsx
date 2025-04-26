import { useState } from 'react';
import { useRouter } from 'next/navigation';
import SignupForm from './SignupForm';
import { AuthAPIClient } from '@/lib/api/AuthAPIClient';

const authClient = AuthAPIClient.getInstance();

export default function SignupContainer() {
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const router = useRouter();

  const handleSignup = async (userData) => {
    if (isSubmitting) return;
    
    setIsSubmitting(true);
    setError('');
    setMessage('');

    try {
      const response = await authClient.signup(userData);
      
      // 자동 로그인 성공 여부 확인
      if (response.autoLoginSuccess) {
        setMessage('회원가입 및 자동 로그인이 완료되었습니다. 잠시 후 거래 페이지로 이동합니다.');
        setTimeout(() => {
          router.push('/trading');
        }, 1500);
      } else {
        // 회원가입은 성공했지만 자동 로그인 실패
        setMessage('회원가입이 완료되었습니다.');
        setError('자동 로그인에 실패했습니다. 로그인 페이지에서 다시 시도해주세요.');
        setTimeout(() => {
          router.push('/auth/login');
        }, 2500);
      }
    } catch (err) {
      console.error('회원가입 에러:', err);
      if (err.status === 409) {
        setError('이미 존재하는 아이디입니다.');
      } else if (err.data && err.data.message) {
        setError(err.data.message);
      } else {
        setError(err.message || '서버와의 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
      }
      // 에러 발생 시 토큰 제거
      authClient.logout();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
      <div className="w-full max-w-md p-8 space-y-8 bg-white rounded-lg shadow-md">
        <div className="text-center">
          <h2 className="mt-6 text-3xl font-extrabold text-gray-900">회원가입</h2>
          <p className="mt-2 text-sm text-gray-600">
            새 계정을 생성하세요
          </p>
        </div>
        {message && (
          <div className="text-center p-2 bg-green-100 text-green-800 rounded">
            {message}
          </div>
        )}
        <SignupForm onSubmit={handleSignup} error={error} isSubmitting={isSubmitting} />
      </div>
    </div>
  );
} 