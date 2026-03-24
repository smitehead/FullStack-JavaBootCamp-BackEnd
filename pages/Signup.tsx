import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { Package, Check, ChevronRight, Mail, User, Lock, ShieldCheck, MapPin, Phone, Calendar, AlertCircle, Send, CheckCircle2, X } from 'lucide-react';

type SignupStep = 'terms' | 'info' | 'success';

export const Signup: React.FC = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState<SignupStep>('terms');
  
  // 약관 동의 상태
  const [terms, setTerms] = useState({
    service: false,
    privacy: false,
    purpose: false,
    termsDef: false,
    auction: false,
    prohibited: false,
    marketing: false
  });

  // 회원정보 입력 상태
  const [formData, setFormData] = useState({
    userId: '',
    password: '',
    confirmPassword: '',
    nickname: '',
    email: '',
    address: '',
    addrDetail: '',
    phoneNum: '',
    birthDate: '' // 6자리 숫자 (YYMMDD)
  });

  // 아이디 중복확인 상태
  const [idCheckMessage, setIdCheckMessage] = useState<{ text: string; isError: boolean } | null>(null);
  const [isIdChecked, setIsIdChecked] = useState(false);

  // 약관 아코디언 펼침 상태
  const [expandedTerm, setExpandedTerm] = useState<string | null>(null);

  const termContents: Record<string, string> = {
    service: 'LiveBid 서비스 이용약관입니다. 본 약관은 회원이 서비스를 이용함에 있어 필요한 권리, 의무 및 책임사항을 규정합니다. 회원은 가입 시 본 약관에 동의함으로써 서비스 이용 권한을 부여받습니다.',
    privacy: '개인정보 처리방침입니다. 회사는 회원의 개인정보를 소중히 취급하며, 관련 법령을 준수합니다. 수집된 정보는 서비스 제공 및 품질 개선을 위해서만 사용됩니다.',
    purpose: '서비스 이용 목적에 대한 동의입니다. 본 서비스는 실시간 경매를 통한 투명한 거래 문화를 지향하며, 건전한 경매 생태계 조성을 목적으로 합니다.',
    termsDef: '용어의 정의에 대한 동의입니다. 서비스 내에서 사용되는 주요 용어들에 대한 명확한 정의를 안내하여 서비스 이용의 혼선을 방지합니다.',
    auction: '서비스 이용 및 낙찰에 대한 동의입니다. 경매 참여 방법 및 낙찰 시 의무사항에 대해 규정하며, 낙찰 후 정당한 사유 없는 취소는 제한될 수 있습니다.',
    prohibited: '금지물품에 대한 동의입니다. 법령에 위반되거나 서비스 운영상 부적절한 물품의 등록을 금지하며, 위반 시 서비스 이용이 제한될 수 있습니다.',
    marketing: '마케팅 정보 수신 동의입니다. 이벤트, 할인 혜택 등 다양한 소식을 받아보실 수 있습니다. 동의하지 않으셔도 서비스 이용은 가능합니다.'
  };

  const toggleTermExpand = (key: string) => {
    setExpandedTerm(prev => prev === key ? null : key);
  };

  const handleIdCheck = async () => {
    if (!formData.userId) {
      setIdCheckMessage({ text: '아이디를 입력해주세요.', isError: true });
      return;
    }
    const idRegex = /^[a-z0-9]{5,20}$/;
    if (!idRegex.test(formData.userId)) {
      setIdCheckMessage({ text: '영문 소문자, 숫자 포함 5~20자여야 합니다.', isError: true });
      return;
    }

    try {
      const res = await api.get(`/members/check-userid?userId=${formData.userId}`);
      if (res.data.duplicate) {
        setIdCheckMessage({ text: '이미 사용 중인 아이디입니다.', isError: true });
        setIsIdChecked(false);
      } else {
        setIdCheckMessage({ text: '사용 가능한 아이디입니다.', isError: false });
        setIsIdChecked(true);
      }
    } catch {
      setIdCheckMessage({ text: '중복확인 중 오류가 발생했습니다.', isError: true });
    }
  };

  // 이메일 인증 상태
  const [emailCode, setEmailCode] = useState('');
  const [sentCode, setSentCode] = useState<string | null>(null);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [verificationError, setVerificationError] = useState<string | null>(null);
  const [timer, setTimer] = useState(0);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (timer > 0) {
      interval = setInterval(() => {
        setTimer(prev => prev - 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [timer]);

  const isAllRequiredTermsChecked = 
    terms.service && terms.privacy && terms.purpose && 
    terms.termsDef && terms.auction && terms.prohibited;

  const handleTermToggle = (key: keyof typeof terms) => {
    setTerms(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const handleAllTermsToggle = () => {
    const allChecked = Object.values(terms).every(v => v);
    setTerms({
      service: !allChecked,
      privacy: !allChecked,
      purpose: !allChecked,
      termsDef: !allChecked,
      auction: !allChecked,
      prohibited: !allChecked,
      marketing: !allChecked
    });
  };

  const sendVerificationCode = () => {
    if (!formData.email.includes('@')) {
      alert('올바른 이메일 형식을 입력해주세요.');
      return;
    }
    // 6자리 랜덤 인증번호 생성
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    setSentCode(code);
    setTimer(180); // 3 minutes
    alert(`인증번호가 발송되었습니다: ${code} (실제 서비스에서는 이메일로 발송됩니다)`);
  };

  const verifyCode = () => {
    if (emailCode === sentCode) {
      setIsEmailVerified(true);
      setVerificationError(null);
      alert('이메일 인증이 완료되었습니다.');
    } else {
      setVerificationError('인증번호가 틀렸습니다.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 1. ID 중복확인 여부 체크
    if (!isIdChecked) {
      alert('아이디 중복확인을 완료해주세요.');
      return;
    }

    // 2. 아이디 유효성 검사 (영문 소문자·숫자 5~20자)
    const idRegex = /^[a-z0-9]{5,20}$/;
    if (!idRegex.test(formData.userId)) {
      alert('아이디는 영문 소문자, 숫자 포함 5~20자여야 합니다.');
      return;
    }

    // 3. 비밀번호 유효성 검사 (8자 이상)
    if (formData.password.length < 8) {
      alert('비밀번호는 최소 8자 이상이어야 합니다.');
      return;
    }

    // 4. 비밀번호 일치 확인
    if (formData.password !== formData.confirmPassword) {
      alert('비밀번호가 일치하지 않습니다.');
      return;
    }

    // 5. 이메일 인증 완료 여부 확인
    if (!isEmailVerified) {
      alert('이메일 인증을 완료해주세요.');
      return;
    }

    // 6. birthDate: YYMMDD → yyyy-MM-dd 변환
    const yy = parseInt(formData.birthDate.substring(0, 2), 10);
    const currentYY = new Date().getFullYear() % 100;
    const fullYear = yy > currentYY ? `19${formData.birthDate.substring(0, 2)}` : `20${formData.birthDate.substring(0, 2)}`;
    const birthDateFormatted = `${fullYear}-${formData.birthDate.substring(2, 4)}-${formData.birthDate.substring(4, 6)}`;

    try {
      await api.post('/members', {
        userId: formData.userId,
        password: formData.password,
        nickname: formData.nickname,
        email: formData.email,
        phoneNum: formData.phoneNum,
        emdNo: 1, // TODO: 주소 검색 API 연동 후 실제 읍면동 번호로 교체 필요
        addrDetail: formData.addrDetail,
        birthDate: birthDateFormatted,
        marketingAgree: terms.marketing ? 1 : 0,
      });
      setStep('success');
    } catch (error: any) {
      if (error.response?.status === 409) {
        alert('이미 사용 중인 아이디, 닉네임, 또는 이메일입니다.');
      } else if (error.response?.status === 400) {
        alert(error.response.data?.message || '입력 정보를 다시 확인해주세요.');
      } else {
        alert('회원가입에 실패했습니다. 잠시 후 다시 시도해주세요.');
      }
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)] bg-gray-50 py-12 px-4">
      <div className="max-w-xl mx-auto">
        {/* Progress Header */}
        <div className="flex items-center justify-between mb-10 px-4">
          <div className="flex flex-col items-center gap-2">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center font-black text-sm transition-all ${step === 'terms' ? 'bg-[#FF5A5A] text-white shadow-lg shadow-red-100 scale-110' : 'bg-white text-gray-300 border border-gray-100'}`}>1</div>
            <span className={`text-[10px] font-black uppercase tracking-wider ${step === 'terms' ? 'text-[#FF5A5A]' : 'text-gray-300'}`}>약관동의</span>
          </div>
          <div className="flex-1 h-px bg-gray-100 mx-4"></div>
          <div className="flex flex-col items-center gap-2">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center font-black text-sm transition-all ${step === 'info' ? 'bg-[#FF5A5A] text-white shadow-lg shadow-red-100 scale-110' : 'bg-white text-gray-300 border border-gray-100'}`}>2</div>
            <span className={`text-[10px] font-black uppercase tracking-wider ${step === 'info' ? 'text-[#FF5A5A]' : 'text-gray-300'}`}>정보입력</span>
          </div>
          <div className="flex-1 h-px bg-gray-100 mx-4"></div>
          <div className="flex flex-col items-center gap-2">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center font-black text-sm transition-all ${step === 'success' ? 'bg-[#FF5A5A] text-white shadow-lg shadow-red-100 scale-110' : 'bg-white text-gray-300 border border-gray-100'}`}>3</div>
            <span className={`text-[10px] font-black uppercase tracking-wider ${step === 'success' ? 'text-[#FF5A5A]' : 'text-gray-300'}`}>가입완료</span>
          </div>
        </div>

        {step === 'terms' && (
          <div className="bg-white p-10 rounded-[32px] shadow-xl border border-gray-100">
            <div className="text-left mb-10">
              <h2 className="text-3xl font-black text-gray-900 tracking-tight">약관 동의</h2>
              <p className="mt-2 text-sm text-gray-500 font-medium">LiveBid 서비스 이용을 위해 약관에 동의해주세요.</p>
            </div>

            <div className="space-y-4">
              <button 
                onClick={handleAllTermsToggle}
                className="w-full flex items-center justify-between p-5 bg-gray-50 rounded-2xl border border-gray-100 hover:bg-gray-100 transition-all group"
              >
                <div className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center transition-all ${Object.values(terms).every(v => v) ? 'bg-[#FF5A5A] text-white' : 'bg-white text-gray-200 border border-gray-200'}`}>
                    <Check className="w-4 h-4" />
                  </div>
                  <span className="font-black text-gray-800">전체 동의하기</span>
                </div>
              </button>

              <div className="space-y-2 pt-4">
                {[
                  { key: 'service', label: '이용약관 동의', required: true },
                  { key: 'privacy', label: '개인정보 처리방침 동의', required: true },
                  { key: 'purpose', label: '목적 동의', required: true },
                  { key: 'termsDef', label: '용어의 정의 동의', required: true },
                  { key: 'auction', label: '서비스 이용 및 낙찰 동의', required: true },
                  { key: 'prohibited', label: '금지물품 동의', required: true },
                  { key: 'marketing', label: '마케팅 정보 수신 동의', required: false },
                ].map((item) => (
                  <div key={item.key} className="p-4 rounded-2xl hover:bg-gray-50 transition-colors">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <button 
                          onClick={() => handleTermToggle(item.key as keyof typeof terms)}
                          className={`w-5 h-5 rounded-full flex items-center justify-center transition-all ${terms[item.key as keyof typeof terms] ? 'bg-[#FF5A5A] text-white' : 'bg-white text-gray-200 border border-gray-200'}`}
                        >
                          <Check className="w-3 h-3" />
                        </button>
                        <span className={`text-sm font-bold ${item.required ? 'text-gray-700' : 'text-gray-400'}`}>
                          {item.label} {item.required ? <span className="text-[#FF5A5A]">(필수)</span> : '(선택)'}
                        </span>
                      </div>
                      <button 
                        onClick={() => toggleTermExpand(item.key)}
                        className="text-[10px] font-black text-gray-300 uppercase tracking-widest hover:text-[#FF5A5A] transition-colors"
                      >
                        {expandedTerm === item.key ? '닫기' : '보기'}
                      </button>
                    </div>
                    {expandedTerm === item.key && (
                      <div className="mt-3 p-4 bg-gray-50 rounded-xl text-xs text-gray-500 leading-relaxed animate-in slide-in-from-top-2 duration-200">
                        {termContents[item.key]}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            <button 
              disabled={!isAllRequiredTermsChecked}
              onClick={() => setStep('info')}
              className={`w-full mt-10 py-4 rounded-2xl font-black text-sm transition-all flex items-center justify-center gap-2 ${isAllRequiredTermsChecked ? 'bg-[#FF5A5A] text-white shadow-lg shadow-red-100 hover:bg-[#FF4545] active:scale-95' : 'bg-gray-100 text-gray-400 cursor-not-allowed'}`}
            >
              다음 단계로
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}

        {step === 'info' && (
          <div className="bg-white p-10 rounded-[32px] shadow-xl border border-gray-100">
            <h2 className="text-2xl font-black text-gray-900 mb-8 tracking-tight">회원정보 입력</h2>
            
            <form onSubmit={handleSubmit} className="space-y-8">
              {/* ID & PW */}
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">아이디</label>
                  <div className="flex gap-2">
                    <div className="relative flex-1">
                      <input
                        type="text"
                        required
                        className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                        placeholder="영문 소문자, 숫자 포함 5~20자"
                        value={formData.userId}
                        onChange={(e) => {
                          setFormData({...formData, userId: e.target.value});
                          setIdCheckMessage(null);
                          setIsIdChecked(false);
                        }}
                      />
                    </div>
                    <button 
                      type="button" 
                      onClick={handleIdCheck}
                      className="px-5 py-3.5 bg-gray-900 text-white text-xs font-bold rounded-2xl hover:bg-black transition-all"
                    >
                      중복확인
                    </button>
                  </div>
                  {idCheckMessage && (
                    <p className={`text-[10px] mt-2 ml-1 font-bold ${idCheckMessage.isError ? 'text-red-500' : 'text-emerald-500'}`}>
                      {idCheckMessage.text}
                    </p>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">비밀번호</label>
                    <div className="relative">
                      <input
                        type="password"
                        required
                        className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                        placeholder="최소 8자 이상"
                        value={formData.password}
                        onChange={(e) => setFormData({...formData, password: e.target.value})}
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">비밀번호 확인</label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <ShieldCheck className={`h-5 w-5 ${formData.confirmPassword === '' ? 'text-gray-400' : (formData.password === formData.confirmPassword ? 'text-emerald-500' : 'text-red-500')}`} />
                      </div>
                      <input
                        type="password"
                        required
                        className="block w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                        placeholder="비밀번호 재입력"
                        value={formData.confirmPassword}
                        onChange={(e) => setFormData({...formData, confirmPassword: e.target.value})}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* Nickname & Email */}
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">닉네임</label>
                  <input
                    type="text"
                    required
                    className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                    placeholder="한글/영문 15자 이내"
                    value={formData.nickname}
                    onChange={(e) => setFormData({...formData, nickname: e.target.value})}
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">이메일 인증</label>
                  <div className="flex gap-2 mb-2">
                    <div className="relative flex-1">
                      <input
                        type="email"
                        required
                        disabled={isEmailVerified}
                        className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none disabled:opacity-50"
                        placeholder="example@email.com"
                        value={formData.email}
                        onChange={(e) => setFormData({...formData, email: e.target.value})}
                      />
                    </div>
                    <button 
                      type="button" 
                      onClick={sendVerificationCode}
                      disabled={isEmailVerified}
                      className="px-5 py-3.5 bg-gray-900 text-white text-xs font-bold rounded-2xl hover:bg-black transition-all disabled:bg-gray-200"
                    >
                      코드전송
                    </button>
                  </div>
                  {!isEmailVerified && sentCode && (
                    <div className="space-y-2 animate-in fade-in slide-in-from-top-2">
                      <div className="flex gap-2">
                        <div className="relative flex-1">
                          <input
                            type="text"
                            className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                            placeholder="인증번호 6자리 입력"
                            value={emailCode}
                            onChange={(e) => setEmailCode(e.target.value)}
                          />
                          {timer > 0 && (
                            <div className="absolute right-4 top-1/2 -translate-y-1/2 text-[10px] font-bold text-[#FF5A5A]">
                              {Math.floor(timer / 60)}:{(timer % 60).toString().padStart(2, '0')}
                            </div>
                          )}
                        </div>
                        <button 
                          type="button" 
                          onClick={verifyCode}
                          className="px-5 py-3.5 bg-[#FF5A5A] text-white text-xs font-bold rounded-2xl hover:bg-[#FF4545] transition-all"
                        >
                          확인
                        </button>
                      </div>
                      <div className="flex justify-center px-1">
                        <button 
                          type="button"
                          onClick={sendVerificationCode}
                          className="text-[10px] font-bold text-gray-400 hover:text-gray-600 transition-colors underline underline-offset-2"
                        >
                          인증번호 재전송
                        </button>
                      </div>
                    </div>
                  )}
                  {verificationError && <p className="text-[10px] text-red-500 mt-1 ml-1 font-bold">{verificationError}</p>}
                  {isEmailVerified && <p className="text-[10px] text-emerald-500 mt-1 ml-1 font-bold flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> 인증이 완료되었습니다.</p>}
                </div>
              </div>

              {/* Address & Phone & Birth */}
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">주소</label>
                  <div className="flex gap-2 mb-2">
                    <div className="relative flex-1">
                      <input
                        type="text"
                        required
                        readOnly
                        className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm outline-none"
                        placeholder="주소 검색을 이용해주세요"
                        value={formData.address}
                      />
                    </div>
                    <button type="button" onClick={() => setFormData({...formData, address: '서울 강남구 테헤란로 123'})} className="px-5 py-3.5 bg-gray-900 text-white text-xs font-bold rounded-2xl hover:bg-black transition-all">검색</button>
                  </div>
                  <input
                    type="text"
                    required
                    className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                    placeholder="상세주소를 입력하세요"
                    value={formData.addrDetail}
                    onChange={(e) => setFormData({...formData, addrDetail: e.target.value})}
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">휴대폰 번호</label>
                    <div className="relative">
                      <input
                        type="tel"
                        required
                        className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                        placeholder="010-0000-0000"
                        value={formData.phoneNum}
                        onChange={(e) => setFormData({...formData, phoneNum: e.target.value})}
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">생년월일 (6자리)</label>
                    <div className="relative">
                      <input
                        type="text"
                        required
                        maxLength={6}
                        className="block w-full px-5 py-3.5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none"
                        placeholder="YYMMDD"
                        value={formData.birthDate}
                        onChange={(e) => {
                          const val = e.target.value.replace(/[^0-9]/g, '');
                          if (val.length <= 6) {
                            setFormData({...formData, birthDate: val});
                          }
                        }}
                      />
                    </div>
                  </div>
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-5 bg-[#FF5A5A] text-white font-black rounded-2xl hover:bg-[#FF4545] transition-all shadow-xl shadow-red-100 active:scale-95 flex items-center justify-center gap-2"
              >
                가입 완료하기
                <ChevronRight className="w-5 h-5" />
              </button>
            </form>
          </div>
        )}

        {step === 'success' && (
          <div className="bg-white p-12 rounded-[40px] shadow-2xl text-center border border-gray-100 animate-in zoom-in-95 duration-500">
            <div className="w-24 h-24 bg-red-50 rounded-[32px] flex items-center justify-center mx-auto mb-8 shadow-inner">
              <CheckCircle2 className="w-12 h-12 text-[#FF5A5A]" />
            </div>
            <h2 className="text-4xl font-black text-gray-900 mb-4 tracking-tight">환영합니다!</h2>
            <p className="text-gray-500 font-medium leading-relaxed mb-10">
              회원가입이 성공적으로 완료되었습니다.<br/>
              지금 바로 LiveBid의 실시간 경매를 시작해보세요.
            </p>
            <div className="space-y-3">
              <Link 
                to="/login" 
                className="block w-full py-5 bg-[#FF5A5A] text-white font-black rounded-2xl hover:bg-[#FF4545] transition-all shadow-xl shadow-red-100 active:scale-95"
              >
                로그인 하러가기
              </Link>
              <Link 
                to="/" 
                className="block w-full py-5 bg-gray-50 text-gray-600 font-black rounded-2xl hover:bg-gray-100 transition-all"
              >
                메인으로 이동
              </Link>
            </div>
          </div>
        )}
      </div>

    </div>
  );
};
