import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, MessageSquare, CheckCircle2, Trash2, Send, User } from 'lucide-react';
import { MOCK_INQUIRIES, MOCK_USERS } from '@/services/mockData';
import { Inquiry } from '@/types';

export const InquiryManagement: React.FC = () => {
  const navigate = useNavigate();
  const [inquiries, setInquiries] = useState<Inquiry[]>(MOCK_INQUIRIES);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedInquiry, setSelectedInquiry] = useState<Inquiry | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [inquiryToDelete, setInquiryToDelete] = useState<string | null>(null);
  const [answer, setAnswer] = useState('');
  const [activeTab, setActiveTab] = useState<'전체' | '답변 대기중' | '답변 완료'>('전체');
  const [selectedCategory, setSelectedCategory] = useState<string>('전체');

  const filteredInquiries = inquiries.filter(i => {
    const matchesSearch = i.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      i.content.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesTab = activeTab === '전체' || i.status === activeTab;
    const matchesCategory = selectedCategory === '전체' || i.category === selectedCategory;
    return matchesSearch && matchesTab && matchesCategory;
  });

  const handleSelectInquiry = (inquiry: Inquiry) => {
    setSelectedInquiry(inquiry);
    setAnswer(inquiry.answer || '');
  };

  const handleSaveAnswer = () => {
    if (!selectedInquiry) return;

    setInquiries(prev => prev.map(i => i.id === selectedInquiry.id ? {
      ...i, 
      status: '답변 완료', 
      answer, 
      answeredAt: new Date().toISOString()
    } : i));

    setSelectedInquiry(null);
    setAnswer('');
  };

  const handleDelete = (id: string) => {
    setInquiryToDelete(id);
    setIsDeleteModalOpen(true);
  };

  const confirmDelete = () => {
    if (inquiryToDelete) {
      setInquiries(prev => prev.filter(i => i.id !== inquiryToDelete));
      if (selectedInquiry?.id === inquiryToDelete) setSelectedInquiry(null);
      setInquiryToDelete(null);
      setIsDeleteModalOpen(false);
    }
  };

  const getUserNickname = (userId: string) => {
    return MOCK_USERS.find(u => u.id === userId)?.nickname || '알 수 없는 사용자';
  };

  return (
    <div className="flex h-[calc(100vh-140px)] space-x-6">
      {/* Inquiry List */}
      <div className="w-1/2 flex flex-col space-y-4">
        <header className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-black text-gray-900 tracking-tight">문의사항 관리</h1>
            <p className="text-gray-500 mt-1 text-[11px] font-medium">사용자들의 문의에 답변을 작성합니다.</p>
          </div>
        </header>

        <div className="flex flex-col space-y-3">
          <div className="flex items-center space-x-6 border-b border-gray-200 w-full px-1">
            {(['전체', '답변 대기중', '답변 완료'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => {
                  setActiveTab(tab);
                  setSelectedCategory('전체');
                }}
                className={`pb-3 text-xs font-black transition-all relative ${
                  activeTab === tab 
                    ? 'text-[#FF5A5A]' 
                    : 'text-gray-400 hover:text-gray-600'
                }`}
              >
                {tab}
                {activeTab === tab && (
                  <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#FF5A5A]" />
                )}
              </button>
            ))}
          </div>

          {(activeTab === '답변 대기중' || activeTab === '답변 완료') && (
            <div className="flex items-center space-x-2 overflow-x-auto pb-1 scrollbar-hide">
              {['전체', '버그 신고', '환불 문의', '계정 문의', '기타'].map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedCategory(cat);
                  }}
                  className={`px-3 py-1.5 rounded-none text-[10px] font-black whitespace-nowrap transition-all border ${
                    selectedCategory === cat
                      ? 'bg-[#FF5A5A] text-white border-[#FF5A5A] shadow-sm'
                      : 'bg-white text-gray-500 border-gray-100 hover:border-gray-200'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="flex justify-end">
          <div className="relative flex items-center h-10 w-64">
            <div className="absolute left-3 top-0 bottom-0 flex items-center pointer-events-none">
              <Search className="text-gray-400 w-4 h-4" />
            </div>
            <input 
              type="text" 
              placeholder="문의 제목 또는 내용 검색"
              className="w-full pl-10 pr-4 h-full bg-white border border-gray-200 rounded-none shadow-sm focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-bold text-xs"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto space-y-3 pr-2">
          {filteredInquiries.map((inquiry) => (
            <button
              key={inquiry.id}
              onClick={() => handleSelectInquiry(inquiry)}
              className={`w-full text-left p-4 rounded-none border-2 transition-all ${
                selectedInquiry?.id === inquiry.id 
                  ? 'bg-white border-[#FF5A5A] shadow-lg shadow-red-900/5' 
                  : 'bg-white border-gray-100 shadow-sm hover:shadow-md'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className={`px-2 py-0.5 rounded-none text-[10px] font-black ${
                  inquiry.status === '답변 완료' ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'
                }`}>
                  {inquiry.status}
                </span>
                <span className="text-[10px] font-medium text-gray-400">{inquiry.createdAt.split('T')[0]}</span>
              </div>
              <h3 className="text-sm font-black text-gray-900 mb-1 line-clamp-1">{inquiry.title}</h3>
              <div className="flex items-center space-x-2 text-[11px] text-gray-500 font-medium">
                <User className="w-3 h-3" />
                <button 
                  onClick={(e) => {
                    e.stopPropagation();
                    navigate(`/admin/users?nickname=${getUserNickname(inquiry.userId)}`);
                  }}
                  className="hover:text-[#FF5A5A] hover:underline transition-colors"
                >
                  {getUserNickname(inquiry.userId)}
                </button>
                <span>•</span>
                <span>{inquiry.category}</span>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Inquiry Detail & Answer */}
      <div className="w-1/2 bg-white rounded-none shadow-sm border border-gray-100 flex flex-col overflow-hidden">
        {selectedInquiry ? (
          <div className="flex-1 flex flex-col p-8 overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-black text-gray-900">문의 상세 내용</h2>
              <button 
                onClick={() => handleDelete(selectedInquiry.id)}
                className="p-2 hover:bg-red-100 text-red-600 rounded-none transition-colors"
              >
                <Trash2 className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-6 mb-8">
              <div className="p-5 bg-gray-50 rounded-none border border-gray-100">
                <div className="flex items-center space-x-2 mb-3">
                  <div className="w-8 h-8 bg-white rounded-none flex items-center justify-center shadow-sm">
                    <User className="w-4 h-4 text-gray-400" />
                  </div>
                  <div>
                    <button 
                      onClick={() => navigate(`/admin/users?nickname=${getUserNickname(selectedInquiry.userId)}`)}
                      className="text-sm font-black text-gray-900 hover:text-[#FF5A5A] hover:underline transition-colors block text-left"
                    >
                      {getUserNickname(selectedInquiry.userId)}
                    </button>
                    <p className="text-[10px] font-medium text-gray-500">{selectedInquiry.createdAt}</p>
                  </div>
                </div>
                <h4 className="text-base font-black text-gray-900 mb-1">{selectedInquiry.title}</h4>
                <p className="text-sm text-gray-700 font-medium leading-relaxed whitespace-pre-wrap">{selectedInquiry.content}</p>
              </div>

              {selectedInquiry.status === '답변 완료' && (
                <div className="p-5 bg-[#FF5A5A]/5 rounded-none border border-[#FF5A5A]/10">
                  <div className="flex items-center space-x-2 mb-3">
                    <div className="w-8 h-8 bg-[#FF5A5A] rounded-none flex items-center justify-center shadow-lg shadow-red-900/10">
                      <CheckCircle2 className="w-4 h-4 text-white" />
                    </div>
                    <div>
                      <p className="text-sm font-black text-gray-900">관리자 답변</p>
                      <p className="text-[10px] font-medium text-gray-500">{selectedInquiry.answeredAt}</p>
                    </div>
                  </div>
                  <p className="text-sm text-gray-700 font-medium leading-relaxed whitespace-pre-wrap">{selectedInquiry.answer}</p>
                </div>
              )}
            </div>

            <div className="mt-auto pt-6 border-t border-gray-100">
              <h3 className="text-base font-black text-gray-900 mb-3">답변 작성</h3>
              <textarea 
                rows={4}
                placeholder="답변 내용을 입력하세요"
                className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium resize-none mb-4 text-sm"
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
              />
              <button 
                onClick={handleSaveAnswer}
                disabled={!answer.trim()}
                className="w-full py-3 rounded-none font-black text-white bg-[#FF5A5A] hover:bg-[#E04848] transition-all flex items-center justify-center shadow-lg shadow-red-500/10 active:scale-95 disabled:opacity-50 disabled:active:scale-100 text-sm"
              >
                <Send className="w-4 h-4 mr-2" /> 확인
              </button>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
            <div className="w-20 h-20 bg-gray-50 rounded-none flex items-center justify-center mb-4">
              <MessageSquare className="w-8 h-8 text-gray-300" />
            </div>
            <h3 className="text-lg font-black text-gray-900 mb-1">문의를 선택해주세요</h3>
            <p className="text-gray-400 text-sm font-medium">왼쪽 목록에서 답변할 문의사항을 선택하세요.</p>
          </div>
        )}
      </div>
      {/* Delete Confirmation Modal */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-6 bg-black/60 backdrop-blur-sm">
          <div className="bg-white w-full max-w-sm rounded-none p-8 shadow-2xl animate-in fade-in zoom-in duration-300">
            <div className="w-16 h-16 bg-red-50 rounded-none flex items-center justify-center mb-4 mx-auto">
              <Trash2 className="w-8 h-8 text-red-500" />
            </div>
            <h2 className="text-xl font-black text-gray-900 mb-2 text-center tracking-tight">
              정말 삭제하시겠습니까?
            </h2>
            <p className="text-gray-500 font-medium text-center mb-8 text-sm">
              삭제된 문의사항은 복구할 수 없습니다.<br />신중하게 결정해주세요.
            </p>

            <div className="flex gap-3">
              <button 
                onClick={() => setIsDeleteModalOpen(false)}
                className="flex-1 py-3 rounded-none font-black text-gray-500 bg-gray-100 hover:bg-gray-200 transition-all text-sm"
              >
                취소
              </button>
              <button 
                onClick={confirmDelete}
                className="flex-1 py-3 rounded-none font-black text-white bg-[#FF5A5A] hover:bg-[#E04848] transition-all shadow-lg shadow-red-500/20 text-sm"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
