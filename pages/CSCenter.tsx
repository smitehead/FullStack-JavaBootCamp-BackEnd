import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Search, ChevronRight, MessageSquare, Plus, ChevronLeft, ChevronDown, Info, Megaphone, HelpCircle } from 'lucide-react';
import { MOCK_NOTICES, MOCK_INQUIRIES } from '../services/mockData';
import { NoticeCategory, InquiryCategory } from '../types';
import { format } from 'date-fns';
import { FAQ } from './FAQ';

type CSTab = 'notice' | 'faq' | 'inquiry';

export const CSCenter: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [activeTab, setActiveTab] = useState<CSTab>('notice');
  
  // Notice state
  const [noticeCategory, setNoticeCategory] = useState<NoticeCategory | '전체'>('전체');
  const [noticeSearch, setNoticeSearch] = useState('');
  
  // Inquiry state
  const [inquirySearch, setInquirySearch] = useState('');
  const [inquiryCategory, setInquiryCategory] = useState<InquiryCategory | '전체'>('전체');

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const tab = params.get('tab') as CSTab;
    if (tab && ['notice', 'faq', 'inquiry'].includes(tab)) {
      setActiveTab(tab);
    }
  }, [location]);

  const handleTabChange = (tab: CSTab) => {
    setActiveTab(tab);
    navigate(`/cs?tab=${tab}`, { replace: true });
  };

  const filteredNotices = MOCK_NOTICES.filter(notice => {
    const matchesCategory = noticeCategory === '전체' || notice.category === noticeCategory;
    const matchesSearch = notice.title.toLowerCase().includes(noticeSearch.toLowerCase());
    return matchesCategory && matchesSearch;
  }).sort((a, b) => {
    if (a.isImportant && !b.isImportant) return -1;
    if (!a.isImportant && b.isImportant) return 1;
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });

  const filteredInquiries = MOCK_INQUIRIES.filter(inquiry => {
    const matchesSearch = inquiry.title.toLowerCase().includes(inquirySearch.toLowerCase()) || 
                         inquiry.content.toLowerCase().includes(inquirySearch.toLowerCase());
    const matchesCategory = inquiryCategory === '전체' || inquiry.category === inquiryCategory;
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="min-h-screen bg-white pb-20">
      {/* Hero Section */}
      <div className="py-20 text-center border-b border-gray-50">
        <h1 className="text-5xl font-black text-gray-900 mb-4 tracking-normal uppercase">CS Center</h1>
        <p className="text-gray-500 font-medium">궁금한 모든 것을 확인해보세요.</p>
      </div>

      {/* Tab Navigation */}
      <div className="max-w-[1200px] mx-auto px-6 -mt-8">
        <div className="bg-gray-50/50 p-2 rounded-[32px] flex items-center justify-center max-w-2xl mx-auto border border-gray-100 shadow-sm">
          <button
            onClick={() => handleTabChange('notice')}
            className={`flex-1 py-4 rounded-[24px] text-sm font-bold transition-all ${
              activeTab === 'notice'
                ? 'bg-white text-gray-900 shadow-md ring-1 ring-black/5'
                : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            공지사항
          </button>
          <div className="w-px h-4 bg-gray-200 mx-2 opacity-50" />
          <button
            onClick={() => handleTabChange('faq')}
            className={`flex-1 py-4 rounded-[24px] text-sm font-bold transition-all ${
              activeTab === 'faq'
                ? 'bg-white text-gray-900 shadow-md ring-1 ring-black/5'
                : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            자주 묻는 질문
          </button>
          <div className="w-px h-4 bg-gray-200 mx-2 opacity-50" />
          <button
            onClick={() => handleTabChange('inquiry')}
            className={`flex-1 py-4 rounded-[24px] text-sm font-bold transition-all ${
              activeTab === 'inquiry'
                ? 'bg-white text-gray-900 shadow-md ring-1 ring-black/5'
                : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            1:1 문의
          </button>
        </div>
      </div>

      <div className="max-w-[1000px] mx-auto px-6 mt-16">
        {/* Notice Content */}
        {activeTab === 'notice' && (
          <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="flex items-center justify-between mb-8">
              <div className="flex flex-wrap gap-2">
                {['전체', '업데이트', '이벤트', '점검', '정책'].map(cat => (
                  <button
                    key={cat}
                    onClick={() => setNoticeCategory(cat as any)}
                    className={`px-5 py-2 rounded-full text-xs font-bold transition-all ${
                      noticeCategory === cat
                        ? 'bg-gray-900 text-white'
                        : 'bg-white border border-gray-100 text-gray-400 hover:border-gray-200'
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
              <div className="relative w-64 hidden md:block">
                <input
                  type="text"
                  placeholder="공지사항 검색"
                  className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border-none rounded-xl text-xs focus:ring-2 focus:ring-gray-200 outline-none"
                  value={noticeSearch}
                  onChange={(e) => setNoticeSearch(e.target.value)}
                />
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              </div>
            </div>

            <div className="bg-white border border-gray-100 rounded-[32px] overflow-hidden shadow-sm">
              {filteredNotices.length > 0 ? (
                <div className="divide-y divide-gray-50">
                  {filteredNotices.map(notice => (
                    <Link
                      key={notice.id}
                      to={`/notice/${notice.id}`}
                      className="flex items-center justify-between p-8 hover:bg-gray-50 transition-colors group"
                    >
                      <div className="flex items-center gap-6">
                        <div className="flex flex-col items-center min-w-[60px]">
                          <span className="text-[10px] font-black text-gray-300 uppercase tracking-widest mb-1">
                            {format(new Date(notice.createdAt), 'MMM')}
                          </span>
                          <span className="text-2xl font-black text-gray-900 tracking-tighter">
                            {format(new Date(notice.createdAt), 'dd')}
                          </span>
                        </div>
                        <div>
                          <div className="flex items-center gap-2 mb-1.5">
                            {notice.isImportant && (
                              <span className="px-2 py-0.5 bg-red-50 text-red-500 text-[10px] font-bold rounded-md">IMPORTANT</span>
                            )}
                            <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{notice.category}</span>
                          </div>
                          <h3 className={`text-lg text-gray-900 group-hover:underline underline-offset-4 decoration-2 transition-all ${notice.isImportant ? 'font-black' : 'font-bold'}`}>
                            {notice.title}
                          </h3>
                        </div>
                      </div>
                      <ChevronRight className="w-5 h-5 text-gray-300 group-hover:text-gray-900 transition-colors" />
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="py-20 text-center">
                  <Megaphone className="w-12 h-12 text-gray-100 mx-auto mb-4" />
                  <p className="text-gray-400 font-bold">공지사항이 없습니다.</p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* FAQ Content */}
        {activeTab === 'faq' && (
          <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            <FAQ isEmbedded />
          </div>
        )}

        {/* Inquiry Content */}
        {activeTab === 'inquiry' && (
          <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="flex flex-col md:flex-row gap-4 mb-8">
              <div className="relative flex-1">
                <input
                  type="text"
                  placeholder="문의 내역 검색"
                  className="w-full h-14 pl-12 pr-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-gray-200 transition-all outline-none text-sm font-medium"
                  value={inquirySearch}
                  onChange={(e) => setInquirySearch(e.target.value)}
                />
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              </div>
              <button 
                onClick={() => navigate('/inquiry/create')}
                className="px-8 py-4 bg-gray-900 text-white rounded-2xl font-bold hover:bg-black transition-all shadow-xl shadow-gray-200 flex items-center justify-center gap-2 shrink-0"
              >
                <Plus className="w-5 h-5" />
                문의 작성하기
              </button>
            </div>

            <div className="bg-white border border-gray-100 rounded-[32px] overflow-hidden shadow-sm">
              {filteredInquiries.length > 0 ? (
                <div className="divide-y divide-gray-50">
                  {filteredInquiries.map(inquiry => (
                    <Link
                      key={inquiry.id}
                      to={`/inquiry/${inquiry.id}`}
                      className="block p-8 hover:bg-gray-50 transition-colors group"
                    >
                      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <span className="px-2.5 py-0.5 text-[10px] font-black rounded-md uppercase tracking-wider bg-gray-50 text-gray-500">
                              {inquiry.category}
                            </span>
                            <span className="text-xs text-gray-400 font-bold">
                              {format(new Date(inquiry.createdAt), 'yyyy.MM.dd')}
                            </span>
                          </div>
                          <h3 className="text-lg font-bold text-gray-900 group-hover:underline underline-offset-4 transition-all">
                            {inquiry.title}
                          </h3>
                        </div>
                        
                        <div className="flex items-center gap-4">
                          {inquiry.status === '답변 완료' ? (
                            <div className="px-3 py-1.5 bg-emerald-50 text-emerald-600 rounded-full text-xs font-bold">
                              답변 완료
                            </div>
                          ) : (
                            <div className="px-3 py-1.5 bg-amber-50 text-amber-600 rounded-full text-xs font-bold">
                              답변 대기중
                            </div>
                          )}
                          <ChevronRight className="w-5 h-5 text-gray-300 group-hover:text-gray-900 transition-colors" />
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="py-20 text-center">
                  <MessageSquare className="w-12 h-12 text-gray-100 mx-auto mb-4" />
                  <p className="text-gray-400 font-bold">문의 내역이 없습니다.</p>
                </div>
              )}
            </div>

            <div className="mt-12 p-10 bg-gray-50 rounded-[40px] flex flex-col md:flex-row items-center justify-between gap-8 border border-gray-100">
              <div className="flex items-center gap-6">
                <div className="w-16 h-16 bg-white rounded-2xl flex items-center justify-center shadow-sm">
                  <HelpCircle className="w-8 h-8 text-gray-900" />
                </div>
                <div>
                  <h4 className="text-xl font-black text-gray-900 mb-1">전화 상담이 필요하신가요?</h4>
                  <p className="text-gray-500 text-sm font-medium">평일 09:00 - 18:00 (주말/공휴일 제외)</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-3xl font-black text-gray-900 tracking-tighter">1588-0000</p>
                <p className="text-gray-400 text-xs font-bold mt-1 uppercase tracking-widest">Customer Support</p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
