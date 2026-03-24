import React from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, Calendar, Clock, CheckCircle2, MessageSquare, User, ShieldCheck, AlertCircle, Info, List } from 'lucide-react';
import { MOCK_INQUIRIES } from '../../services/mockData';
import { format } from 'date-fns';

export const InquiryDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const inquiry = MOCK_INQUIRIES.find(inq => inq.id === id);

  if (!inquiry) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <p className="text-gray-400 font-bold">존재하지 않는 문의 내역입니다.</p>
        <button
          onClick={() => navigate('/inquiry')}
          className="px-8 py-3 bg-gray-900 text-white rounded-2xl font-bold hover:bg-gray-800 transition-colors shadow-lg shadow-gray-200"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-[1000px] mx-auto px-6 py-12">
      {/* Back Button */}
      <Link to="/inquiry" className="inline-flex items-center gap-2 text-sm font-bold text-red-500 hover:text-red-600 transition-colors mb-8">
        <ChevronLeft className="w-4 h-4" />
        문의 목록으로
      </Link>

      <div className="bg-white border border-gray-100 rounded-3xl overflow-hidden shadow-sm">
        {/* Header Section */}
        <div className="p-10 border-b border-gray-50">
          <div className="flex items-center justify-between gap-4 mb-6">
            <div className="flex items-center gap-3">
              <span className="px-3 py-1 text-xs font-black rounded-md uppercase tracking-wider bg-gray-50 text-gray-500">
                {inquiry.category}
              </span>
              {inquiry.bugType && (
                <span className="px-3 py-1 bg-blue-50 text-blue-500 text-xs font-black rounded-md uppercase tracking-wider">
                  {inquiry.bugType}
                </span>
              )}
            </div>
            
            {inquiry.status === '답변 완료' ? (
              <div className="flex items-center gap-1.5 px-4 py-2 bg-green-50 text-green-600 rounded-full text-xs font-black">
                <CheckCircle2 className="w-4 h-4" />
                답변 완료
              </div>
            ) : (
              <div className="flex items-center gap-1.5 px-4 py-2 bg-amber-50 text-amber-600 rounded-full text-xs font-black">
                <Clock className="w-4 h-4" />
                답변 대기중
              </div>
            )}
          </div>

          <h1 className="text-3xl font-black text-gray-900 mb-6 tracking-tight leading-tight">{inquiry.title}</h1>
          
          <div className="flex items-center gap-6 text-sm text-gray-400 font-bold">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4" />
              작성일: {format(new Date(inquiry.createdAt), 'yyyy.MM.dd HH:mm')}
            </div>
          </div>
        </div>

        {/* Content Section */}
        <div className="p-10">
          <div className="flex items-start gap-4 mb-8">
            <div className="w-10 h-10 rounded-2xl bg-gray-50 flex items-center justify-center shrink-0">
              <User className="w-5 h-5 text-gray-400" />
            </div>
            <div className="flex-1">
              <div className="text-sm font-black text-gray-900 mb-2">문의 내용</div>
              <div className="text-gray-600 leading-relaxed whitespace-pre-wrap font-medium">
                {inquiry.content}
              </div>
              
              {/* Images (if any) */}
              {inquiry.images && inquiry.images.length > 0 && (
                <div className="mt-8 flex flex-wrap gap-4">
                  {inquiry.images.map((img, index) => (
                    <img
                      key={index}
                      src={img || undefined}
                      alt={`inquiry-img-${index}`}
                      className="w-32 h-32 object-cover rounded-2xl border border-gray-100 hover:scale-105 transition-transform cursor-pointer"
                    />
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Answer Section */}
          {inquiry.status === '답변 완료' && inquiry.answer && (
            <div className="mt-12 p-8 bg-gray-50 rounded-3xl border border-gray-100 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <div className="flex items-start gap-4">
                <div className="w-10 h-10 rounded-2xl bg-red-500 flex items-center justify-center shrink-0 shadow-lg shadow-red-500/20">
                  <ShieldCheck className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between gap-4 mb-4">
                    <div className="text-sm font-black text-gray-900">운영팀 답변</div>
                    {inquiry.answeredAt && (
                      <div className="text-xs text-gray-400 font-bold">
                        답변일: {format(new Date(inquiry.answeredAt), 'yyyy.MM.dd HH:mm')}
                      </div>
                    )}
                  </div>
                  <div className="text-gray-700 leading-relaxed whitespace-pre-wrap font-medium">
                    {inquiry.answer}
                  </div>
                  
                  <div className="mt-8 pt-6 border-t border-gray-200/50 flex items-center gap-2 text-xs text-gray-400 font-bold italic">
                    <Info className="w-3.5 h-3.5" />
                    추가 문의가 필요하신 경우 새로운 문의를 작성해 주시기 바랍니다.
                  </div>
                </div>
              </div>
            </div>
          )}
          
          {inquiry.status === '답변 대기중' && (
            <div className="mt-12 p-8 bg-amber-50/50 rounded-3xl border border-dashed border-amber-200 flex flex-col items-center justify-center text-center gap-4">
              <div className="w-12 h-12 bg-white rounded-2xl flex items-center justify-center shadow-sm">
                <Clock className="w-6 h-6 text-amber-500 animate-pulse" />
              </div>
              <div>
                <p className="text-amber-800 font-bold mb-1">답변을 준비 중입니다.</p>
                <p className="text-amber-600/70 text-xs font-medium">최대한 신속하게 확인하여 답변해 드리겠습니다.</p>
              </div>
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div className="border-t border-gray-100">
          <div className="p-6 flex items-center justify-center bg-gray-50/30">
            <Link
              to="/inquiry"
              className="flex items-center gap-2 px-6 py-3 bg-white border border-gray-200 rounded-full text-sm font-bold text-gray-600 hover:text-red-500 hover:border-red-200 hover:shadow-lg hover:shadow-red-500/5 transition-all"
            >
              <List className="w-4 h-4" />
              목록으로
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
