import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronDown, ChevronUp, Search, HelpCircle, MessageCircle, Phone } from 'lucide-react';
import { CustomerCenterSidebar } from '@/pages/cs/CustomerCenterSidebar';

interface FAQItem {
  id: string;
  question: string;
  answer: string;
  category: 'auction' | 'payment' | 'delivery' | 'account';
}

const FAQ_DATA: FAQItem[] = [
  {
    id: '1',
    category: 'auction',
    question: '경매는 어떻게 참여하나요?',
    answer: '원하는 상품 상세 페이지에서 "입찰하기" 버튼을 눌러 참여할 수 있습니다. 현재 최고가보다 높은 금액을 입력해야 하며, 입찰 시 포인트가 차감됩니다. 낙찰되지 않을 경우 입찰금은 즉시 반환됩니다.'
  },
  {
    id: '2',
    category: 'auction',
    question: '낙찰 후 취소할 수 있나요?',
    answer: '경매 낙찰 후 취소는 원칙적으로 불가능합니다. 부득이한 사정으로 취소할 경우 페널티가 부과될 수 있으며, 입찰 보증금이 차감될 수 있으니 신중하게 입찰해 주세요.'
  },
  {
    id: '3',
    category: 'payment',
    question: '포인트 충전은 어떻게 하나요?',
    answer: '마이페이지 > 포인트 관리에서 충전하기 버튼을 통해 가능합니다. 카드 결제, 계좌 이체 등 다양한 결제 수단을 지원합니다.'
  },
  {
    id: '4',
    category: 'delivery',
    question: '배송 기간은 얼마나 걸리나요?',
    answer: '낙찰 상품의 배송은 판매자가 상품을 발송한 후 영업일 기준 3~5일 정도 소요됩니다. 배송 현황은 마이페이지 > 구매 내역에서 확인하실 수 있습니다.'
  },
  {
    id: '5',
    category: 'account',
    question: '비밀번호를 잊어버렸어요.',
    answer: '로그인 화면 하단의 "아이디/비밀번호 찾기"를 통해 이메일 인증 후 비밀번호를 재설정하실 수 있습니다.'
  },
  {
    id: '6',
    category: 'payment',
    question: '포인트 출금은 언제 되나요?',
    answer: '출금 신청 후 영업일 기준 1~2일 이내에 등록하신 계좌로 입금됩니다. 주말 및 공휴일은 처리가 지연될 수 있습니다.'
  }
];

export const FAQ: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<'all' | FAQItem['category']>('all');
  const [openId, setOpenId] = useState<string | null>(null);

  const filteredFaqs = FAQ_DATA.filter(faq => {
    const matchesSearch = faq.question.toLowerCase().includes(searchQuery.toLowerCase()) || 
                         faq.answer.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = activeCategory === 'all' || faq.category === activeCategory;
    return matchesSearch && matchesCategory;
  });

  const categories = [
    { id: 'all', label: '전체' },
    { id: 'auction', label: '경매/입찰' },
    { id: 'payment', label: '결제/포인트' },
    { id: 'delivery', label: '배송' },
    { id: 'account', label: '계정/인증' },
  ];

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-12">
      {/* Header Section */}
      <div className="mb-10">
        <h1 className="text-xl font-bold text-gray-900 mb-2">고객센터</h1>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        <CustomerCenterSidebar />

        <div className="flex-1">
          <div className="mb-6">
            <h2 className="text-xl font-bold text-gray-900">자주 묻는 질문</h2>
            <p className="text-gray-500 mt-1 text-sm">궁금하신 점을 검색해 보세요.</p>
          </div>

          {/* Search Bar */}
          <div className="relative mb-8">
            <input
              type="text"
              placeholder="검색어를 입력하세요"
              className="w-full h-14 pl-12 pr-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-gray-200 transition-all outline-none text-sm text-gray-900"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <div className="absolute left-0 top-0 bottom-0 w-12 flex items-center justify-center">
              <Search className="w-5 h-5 text-gray-400" />
            </div>
          </div>

          {/* Category Tabs */}
          <div className="flex gap-2 mb-8 overflow-x-auto pb-2 no-scrollbar">
            {categories.map(cat => (
              <button
                key={cat.id}
                onClick={() => setActiveCategory(cat.id as any)}
                className={`px-6 py-2.5 rounded-full text-sm font-bold whitespace-nowrap transition-all ${
                  activeCategory === cat.id 
                    ? 'bg-red-500 text-white shadow-lg shadow-red-200' 
                    : 'bg-white text-gray-500 hover:bg-gray-50 border border-gray-100'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>

          {/* FAQ List */}
          <div className="space-y-4">
            {filteredFaqs.length > 0 ? (
              filteredFaqs.map(faq => (
                <div 
                  key={faq.id}
                  className="bg-white border border-gray-100 rounded-[24px] overflow-hidden transition-all hover:shadow-md"
                >
                  <button
                    onClick={() => setOpenId(openId === faq.id ? null : faq.id)}
                    className="w-full px-8 py-5 flex items-center justify-between text-left"
                  >
                    <div className="flex items-center gap-4">
                      <HelpCircle className="w-5 h-5 text-gray-400 shrink-0" />
                      <span className="text-sm font-bold text-gray-900">{faq.question}</span>
                    </div>
                    {openId === faq.id ? (
                      <ChevronDown className="w-5 h-5 text-gray-400 rotate-180 transition-transform" />
                    ) : (
                      <ChevronDown className="w-5 h-5 text-gray-400 transition-transform" />
                    )}
                  </button>
                  {openId === faq.id && (
                    <div className="px-8 pb-8 animate-in slide-in-from-top-2 duration-200">
                      <div className="pt-4 border-t border-gray-50 text-gray-600 leading-relaxed text-sm">
                        {faq.answer}
                      </div>
                    </div>
                  )}
                </div>
              ))
            ) : (
              <div className="text-center py-20 bg-white rounded-[32px] border border-dashed border-gray-200">
                <p className="text-gray-400">검색 결과가 없습니다.</p>
              </div>
            )}
          </div>

          {/* Contact Support */}
          <div className="mt-16 p-8 bg-gray-900 rounded-[32px] text-white flex flex-col md:flex-row items-center justify-between gap-6">
            <div>
              <h3 className="text-xl font-bold mb-2">원하시는 답변을 찾지 못하셨나요?</h3>
              <p className="text-gray-400 text-sm">고객센터로 문의해 주시면 친절하게 안내해 드리겠습니다.</p>
            </div>
            <Link 
              to="/cs/inquiry" 
              className="px-6 py-3 bg-white/10 hover:bg-white/20 rounded-xl text-sm font-bold transition-colors">
              문의하기
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
