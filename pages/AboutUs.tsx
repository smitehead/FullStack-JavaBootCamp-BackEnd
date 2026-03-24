import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Gavel, ShieldCheck, Eye, CreditCard, Sparkles } from 'lucide-react';

type AboutTab = 'intro' | 'privacy' | 'terms' | 'policy';

export const AboutUs: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<AboutTab>('intro');

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const tab = params.get('tab') as AboutTab;
    if (tab && ['intro', 'privacy', 'terms', 'policy'].includes(tab)) {
      setActiveTab(tab);
    }
    // Scroll to top when tab changes or on initial load from footer
    window.scrollTo(0, 0);
  }, [location]);

  const handleTabChange = (tab: AboutTab) => {
    setActiveTab(tab);
    navigate(`/about?tab=${tab}`, { replace: true });
  };

  const tabs = [
    { id: 'intro', label: '서비스 소개' },
    { id: 'privacy', label: '개인정보처리방침' },
    { id: 'terms', label: '이용약관' },
    { id: 'policy', label: '운영정책' },
  ];

  const introFeatures = [
    {
      title: '실시간 입찰',
      description: '박진감 넘치는 실시간 경매 시스템으로 원하는 상품을 쟁취하세요.',
      icon: Gavel,
      color: 'bg-red-50',
      iconColor: 'text-red-500',
      borderColor: 'border-red-100'
    },
    {
      title: '안전한 거래',
      description: '검증된 판매자와 안전 결제 시스템으로 믿고 거래할 수 있습니다.',
      icon: ShieldCheck,
      color: 'bg-gray-50',
      iconColor: 'text-gray-500',
      borderColor: 'border-gray-100'
    },
    {
      title: '투명한 경매',
      description: '모든 입찰 내역이 투명하게 공개되어 공정한 경쟁이 가능합니다.',
      icon: Eye,
      color: 'bg-gray-50',
      iconColor: 'text-gray-500',
      borderColor: 'border-gray-100'
    },
    {
      title: '간편한 결제',
      description: '다양한 결제 수단을 지원하여 낙찰 후 빠르게 결제할 수 있습니다.',
      icon: CreditCard,
      color: 'bg-red-50',
      iconColor: 'text-red-500',
      borderColor: 'border-red-100'
    }
  ];

  return (
    <div className="min-h-screen bg-white pb-20">
      {/* Hero Section */}
      <div className="py-24 text-center border-b border-gray-50 bg-gray-50/30 relative overflow-hidden">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-full opacity-[0.03] pointer-events-none">
          <div className="grid grid-cols-6 gap-4 rotate-12 scale-150">
            {Array.from({ length: 24 }).map((_, i) => (
              <Sparkles key={i} className="w-12 h-12" />
            ))}
          </div>
        </div>
        <h1 className="text-6xl font-black text-gray-900 mb-6 tracking-tighter uppercase italic">About Us</h1>
        <p className="text-gray-500 font-bold text-lg">JAVAJAVA 서비스와 정책을 안내해 드립니다.</p>
      </div>

      {/* Tab Navigation */}
      <div className="max-w-[1200px] mx-auto px-6 -mt-8 relative z-10">
        <div className="bg-white p-1.5 rounded-[28px] flex items-center justify-center max-w-3xl mx-auto border border-gray-100 shadow-2xl">
          {tabs.map((tab, index) => (
            <React.Fragment key={tab.id}>
              <button
                onClick={() => handleTabChange(tab.id as AboutTab)}
                className={`flex-1 py-2.5 rounded-[22px] text-[13px] font-black transition-all flex items-center justify-center ${
                  activeTab === tab.id
                    ? 'bg-gray-900 text-white shadow-md'
                    : 'text-gray-400 hover:text-gray-600 hover:bg-gray-50'
                }`}
              >
                {tab.label}
              </button>
              {index < tabs.length - 1 && (
                <div className="w-px h-3 bg-gray-200 mx-1 opacity-50" />
              )}
            </React.Fragment>
          ))}
        </div>
      </div>

      <div className="max-w-[1100px] mx-auto px-6 mt-16">
        <div className="bg-white border border-gray-100 rounded-[40px] p-12 shadow-sm h-auto relative overflow-hidden">
          {activeTab === 'intro' && (
            <div className="animate-in fade-in slide-in-from-bottom-8 duration-700">
              <div className="flex items-center gap-4 mb-10">
                <div className="w-10 h-10 bg-gray-900 rounded-xl flex items-center justify-center">
                  <Sparkles className="w-5 h-5 text-white" />
                </div>
                <h2 className="text-3xl font-black text-gray-900 tracking-tight">서비스 소개</h2>
              </div>
              
              <div className="prose prose-gray max-w-none">
                <p className="text-lg text-gray-500 font-medium leading-relaxed mb-12 whitespace-nowrap">
                  JAVAJAVA는 누구나 쉽고 안전하게 참여할 수 있는 <span className="text-gray-900 font-black underline decoration-[#FF5A5A] decoration-4 underline-offset-4">실시간 경매 마켓플레이스</span>입니다.
                </p>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {introFeatures.map((feature, idx) => (
                    <div 
                      key={idx}
                      className={`group p-7 ${feature.color} rounded-[32px] border ${feature.borderColor} transition-all duration-300 hover:shadow-lg hover:-translate-y-1`}
                    >
                      <div className={`w-12 h-12 bg-white rounded-xl flex items-center justify-center mb-6 shadow-sm group-hover:scale-110 transition-transform`}>
                        <feature.icon className={`w-6 h-6 ${feature.iconColor}`} />
                      </div>
                      <h3 className="text-xl font-black text-gray-900 mb-3 tracking-tight">{feature.title}</h3>
                      <p className="text-gray-600 font-bold leading-relaxed text-base opacity-80 group-hover:opacity-100 transition-opacity">
                        {feature.description}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'privacy' && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
              <h2 className="text-3xl font-black text-gray-900 mb-8">개인정보처리방침</h2>
              <div className="bg-gray-50 p-8 rounded-[32px] text-gray-600 text-sm leading-relaxed space-y-6">
                <p>JAVAJAVA(이하 '회사')는 고객님의 개인정보를 중요시하며, "정보통신망 이용촉진 및 정보보호"에 관한 법률을 준수하고 있습니다.</p>
                <div>
                  <h4 className="font-black text-gray-900 mb-2">1. 수집하는 개인정보 항목</h4>
                  <p>회사는 회원가입, 상담, 서비스 신청 등등을 위해 아래와 같은 개인정보를 수집하고 있습니다.</p>
                  <ul className="list-disc ml-5 mt-2">
                    <li>수집항목: 이름, 생년월일, 성별, 로그인ID, 비밀번호, 휴대전화번호, 이메일, 주소</li>
                  </ul>
                </div>
                <div>
                  <h4 className="font-black text-gray-900 mb-2">2. 개인정보의 수집 및 이용목적</h4>
                  <p>회사는 수집한 개인정보를 다음의 목적을 위해 활용합니다.</p>
                  <ul className="list-disc ml-5 mt-2">
                    <li>서비스 제공에 관한 계약 이행 및 서비스 제공에 따른 요금정산</li>
                    <li>회원 관리: 회원제 서비스 이용에 따른 본인확인, 개인 식별, 불량회원의 부정 이용 방지와 비인가 사용 방지</li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'terms' && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
              <h2 className="text-3xl font-black text-gray-900 mb-8">이용약관</h2>
              <div className="bg-gray-50 p-8 rounded-[32px] text-gray-600 text-sm leading-relaxed space-y-6">
                <div>
                  <h4 className="font-black text-gray-900 mb-2">제1조 (목적)</h4>
                  <p>이 약관은 JAVAJAVA가 운영하는 웹사이트에서 제공하는 인터넷 관련 서비스(이하 "서비스"라 한다)를 이용함에 있어 사이버 몰과 이용자의 권리·의무 및 책임사항을 규정함을 목적으로 합니다.</p>
                </div>
                <div>
                  <h4 className="font-black text-gray-900 mb-2">제2조 (정의)</h4>
                  <p>1. "몰"이란 회사가 재화 또는 용역을 이용자에게 제공하기 위하여 컴퓨터 등 정보통신설비를 이용하여 재화 등을 거래할 수 있도록 설정한 가상의 영업장을 말하며, 아울러 사이버몰을 운영하는 사업자의 의미로도 사용합니다.</p>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'policy' && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
              <h2 className="text-3xl font-black text-gray-900 mb-8">운영정책</h2>
              <div className="bg-gray-50 p-8 rounded-[32px] text-gray-600 text-sm leading-relaxed space-y-6">
                <div>
                  <h4 className="font-black text-gray-900 mb-2">1. 경매 운영 원칙</h4>
                  <p>모든 경매는 공정하게 진행되어야 하며, 허위 입찰이나 담합 행위는 엄격히 금지됩니다.</p>
                </div>
                <div>
                  <h4 className="font-black text-gray-900 mb-2">2. 판매자 준수사항</h4>
                  <p>판매자는 상품의 상태를 정확하게 기재해야 하며, 낙찰된 상품은 정해진 기한 내에 발송해야 합니다.</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
