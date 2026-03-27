import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Package, Mail, Lock, AlertCircle, ChevronRight } from 'lucide-react';
import { useAppContext } from '@/context/AppContext';

export const Login: React.FC = () => {
  const navigate = useNavigate();
  const { login, user } = useAppContext();
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  // 이미 로그인된 상태면 홈으로 리다이렉트 (중복 로그인 방지)
  useEffect(() => {
    if (user) {
      navigate('/', { replace: true });
    }
  }, [user, navigate]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const trimmedId = userId.trim();
    const trimmedPw = password.trim();

    // 1. 입력값 검증
    if (trimmedId.length === 0 || trimmedPw.length === 0) {
      setError('아이디와 비밀번호를 모두 입력해주세요.');
      return;
    }

    // 2. Context API를 통한 로그인 요청
    const success = await login(trimmedId, trimmedPw);

    if (success) {
      navigate('/');
    } else {
      // 로그인 실패
      alert('아이디 혹은 비밀번호가 잘못되었습니다.');
      setError('아이디 혹은 비밀번호가 일치하지 않습니다.');
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)] bg-gray-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-[32px] shadow-xl border border-gray-100">
        <div className="text-center">
          <div className="mx-auto h-16 w-16 bg-[#FF5A5A] rounded-2xl flex items-center justify-center shadow-lg shadow-red-100">
             <Package className="h-10 w-10 text-white" />
          </div>
          <h2 className="mt-6 text-3xl font-black text-gray-900 tracking-tight">
            로그인
          </h2>
          <p className="mt-2 text-sm text-gray-500 font-medium">
            LiveBid에 오신 것을 환영합니다.
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleLogin}>
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1.5 ml-1">아이디</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type="text"
                  required
                  className="block w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                  placeholder="아이디를 입력하세요"
                  value={userId}
                  onChange={(e) => setUserId(e.target.value)}
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1.5 ml-1">비밀번호</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type="password"
                  required
                  className="block w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                  placeholder="비밀번호를 입력하세요"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
            </div>
          </div>

          {error && (
            <div className="flex items-center gap-2 text-red-500 text-xs font-bold bg-red-50 p-3 rounded-xl animate-shake">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          <div className="flex items-center justify-between text-xs font-bold">
            <div className="flex items-center space-x-4">
              <Link to="/find-id" className="text-gray-400 hover:text-gray-600 transition-colors">아이디 찾기</Link>
              <span className="text-gray-200">|</span>
              <Link to="/find-pw" className="text-gray-400 hover:text-gray-600 transition-colors">비밀번호 찾기</Link>
            </div>
          </div>

          <div>
            <button
              type="submit"
              className="group relative w-full flex justify-center py-4 px-4 border border-transparent text-sm font-black rounded-2xl text-white bg-[#FF5A5A] hover:bg-[#FF4545] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#FF5A5A] transition-all shadow-lg shadow-red-100 active:scale-95"
            >
              로그인
            </button>
          </div>
        </form>

        <div className="pt-6 border-t border-gray-50 text-center">
          <p className="text-sm text-gray-500 font-medium">
            아직 회원이 아니신가요?
          </p>
          <Link 
            to="/signup" 
            className="mt-4 inline-flex items-center justify-center w-full py-3.5 px-4 border-2 border-gray-100 text-sm font-bold rounded-2xl text-gray-700 hover:bg-gray-50 hover:border-gray-200 transition-all group"
          >
            회원가입 하러가기
            <ChevronRight className="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
          </Link>
        </div>
      </div>
    </div>
  );
};
