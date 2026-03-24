import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, Plus, ChevronRight, MessageSquare, Clock, CheckCircle2, Filter } from 'lucide-react';
import { MOCK_INQUIRIES } from '@/services/mockData';
import { InquiryCategory, InquiryStatus } from '@/types';
import { format } from 'date-fns';
import { CustomerCenterSidebar } from '@/pages/cs/CustomerCenterSidebar';

export const InquiryList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<InquiryCategory | '전체'>('전체');
  const navigate = useNavigate();

  const categories: (InquiryCategory | '전체')[] = ['전체', '버그 신고', '환불 문의', '계정 문의', '기타'];

  const filteredInquiries = MOCK_INQUIRIES.filter(inquiry => {
    const matchesSearch = inquiry.title.toLowerCase().includes(searchTerm.toLowerCase()) || 
                         inquiry.content.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = selectedCategory === '전체' || inquiry.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-12">
      {/* Header Section */}
      <div className="mb-10">
        <h1 className="text-xl font-bold text-gray-900 mb-2">고객센터</h1>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        <CustomerCenterSidebar />

        <div className="flex-1">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900">문의하기</h2>
              <p className="text-gray-500 mt-1 text-sm">서비스 이용 중 궁금하신 점이나 불편한 사항을 남겨주세요.</p>
            </div>
            <button 
              onClick={() => navigate('/inquiry/create')}
              className="px-6 py-3 bg-red-500 text-white rounded-2xl font-bold hover:bg-red-600 transition-all shadow-lg shadow-red-500/20 flex items-center justify-center shrink-0 text-sm"
            >
              문의 작성하기
            </button>
          </div>

          {/* Search Bar */}
          <div className="relative mb-8">
            <input
              type="text"
              placeholder="문의 제목이나 내용을 검색해보세요"
              className="w-full h-14 pl-12 pr-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-gray-200 transition-all outline-none text-sm text-gray-900"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <div className="absolute left-0 top-0 bottom-0 w-12 flex items-center justify-center">
              <Search className="w-5 h-5 text-gray-400" />
            </div>
          </div>

          {/* Category Tabs */}
          <div className="flex items-center gap-2 mb-8 overflow-x-auto pb-2 no-scrollbar">
            {categories.map(cat => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                  selectedCategory === cat
                    ? 'bg-red-500 text-white shadow-lg shadow-red-200'
                    : 'bg-white text-gray-500 border border-gray-100 hover:bg-gray-50'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Inquiry List */}
          <div className="bg-white border border-gray-100 rounded-3xl overflow-hidden shadow-sm">
            {filteredInquiries.length > 0 ? (
              <div className="divide-y divide-gray-50">
                {filteredInquiries.map(inquiry => (
                  <Link
                    key={inquiry.id}
                    to={`/inquiry/${inquiry.id}`}
                    className="block px-8 py-5 transition-colors group"
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-20 shrink-0">
                        <span className="text-xs font-bold text-gray-400">{inquiry.category}</span>
                      </div>
                      <div className="flex-1 flex items-center gap-4 min-w-0">
                        <h3 className="flex-1 text-sm text-gray-900 group-hover:underline underline-offset-4 transition-all line-clamp-1 font-bold">
                          {inquiry.title}
                        </h3>
                        <div className="w-20 shrink-0 text-center">
                          {inquiry.status === '답변 완료' ? (
                            <span className="px-2 py-0.5 bg-green-50 text-green-600 text-[10px] font-bold rounded-md inline-block">답변완료</span>
                          ) : (
                            <span className="px-2 py-0.5 bg-amber-50 text-amber-600 text-[10px] font-bold rounded-md inline-block">답변대기</span>
                          )}
                        </div>
                      </div>
                      <span className="w-24 text-sm text-gray-400 font-medium shrink-0 text-right">
                        {format(new Date(inquiry.createdAt), 'yyyy.MM.dd')}
                      </span>
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="py-20 text-center">
                <div className="w-16 h-16 bg-gray-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
                  <MessageSquare className="w-8 h-8 text-gray-200" />
                </div>
                <p className="text-gray-400 font-bold">문의 내역이 없습니다.</p>
              </div>
            )}
          </div>

          {/* Help Banner */}
          <div className="mt-12 p-8 bg-gray-900 rounded-3xl text-white flex flex-col md:flex-row items-center justify-between gap-6">
            <div>
              <h4 className="text-xl font-bold mb-2">도움이 더 필요하신가요?</h4>
              <p className="text-gray-400 text-sm">자주 묻는 질문(FAQ)에서 빠르게 답을 찾아보실 수 있습니다.</p>
            </div>
            <Link to="/faq" className="px-6 py-3 bg-white/10 hover:bg-white/20 rounded-xl text-sm font-bold transition-colors">
              FAQ 바로가기
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
