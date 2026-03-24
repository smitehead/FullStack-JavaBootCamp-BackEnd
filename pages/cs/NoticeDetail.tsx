import React from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Calendar, Eye, Info, AlertTriangle, List } from 'lucide-react';
import { MOCK_NOTICES } from '@/services/mockData';
import { format } from 'date-fns';

export const NoticeDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const noticeIndex = MOCK_NOTICES.findIndex(n => n.id === id);
  const notice = MOCK_NOTICES[noticeIndex];
  
  const prevNotice = noticeIndex > 0 ? MOCK_NOTICES[noticeIndex - 1] : null;
  const nextNotice = noticeIndex < MOCK_NOTICES.length - 1 ? MOCK_NOTICES[noticeIndex + 1] : null;

  if (!notice) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <p className="text-gray-400">존재하지 않는 공지사항입니다.</p>
        <button
          onClick={() => navigate('/notice')}
          className="px-6 py-2 bg-gray-900 text-white rounded-xl font-bold hover:bg-gray-800 transition-colors"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-[1000px] mx-auto px-6 py-12">
      {/* Back Button */}
      <Link to="/notice" className="inline-flex items-center gap-2 text-sm font-bold text-red-500 hover:text-red-600 transition-colors mb-8">
        <ChevronLeft className="w-4 h-4" />
        공지사항 목록
      </Link>

      <div className="bg-white border border-gray-100 rounded-3xl overflow-hidden shadow-sm">
        {/* Header Section */}
        <div className="p-10 border-b border-gray-50">
          <div className="flex items-center gap-2 mb-4">
            <span className={`px-3 py-1 text-xs font-bold rounded-full ${
              notice.category === '점검' ? 'bg-blue-50 text-blue-500' :
              notice.category === '업데이트' ? 'bg-green-50 text-green-500' :
              notice.category === '이벤트' ? 'bg-purple-50 text-purple-500' :
              'bg-gray-50 text-gray-500'
            }`}>
              {notice.category}
            </span>
          </div>
          <h1 className="text-3xl font-bold text-gray-900 mb-6">{notice.title}</h1>
          <div className="flex items-center gap-6 text-sm text-gray-400 font-medium">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4" />
              {format(new Date(notice.createdAt), 'yyyy.MM.dd')}
            </div>
            <div className="flex items-center gap-2">
              <Eye className="w-4 h-4" />
              {notice.viewCount.toLocaleString()}
            </div>
          </div>
        </div>

        {/* Content Section */}
        <div className="p-10">
          <div className="prose prose-gray max-w-none text-gray-600 leading-relaxed whitespace-pre-wrap min-h-[300px]">
            {notice.content}
          </div>

          {/* Info Box (Example based on design) */}
          {notice.category === '점검' && (
            <div className="mt-10 p-8 bg-gray-50 rounded-2xl border border-gray-100">
              <div className="flex items-center gap-3 mb-4 text-blue-500">
                <Info className="w-5 h-5" />
                <span className="font-bold">점검 상세 안내</span>
              </div>
              <div className="space-y-3 text-sm">
                <div className="flex gap-4">
                  <span className="w-20 text-gray-400 font-bold">점검 일시</span>
                  <span className="text-gray-900">2024년 5월 22일(수) 오전 02:00 ~ 06:00 (약 4시간)</span>
                </div>
                <div className="flex gap-4">
                  <span className="w-20 text-gray-400 font-bold">점검 대상</span>
                  <span className="text-gray-900">웹사이트 전체 및 모바일 애플리케이션 전 서비스</span>
                </div>
                <div className="flex gap-4">
                  <span className="w-20 text-gray-400 font-bold">점검 내용</span>
                  <span className="text-gray-900">서버 안정화 작업, 데이터베이스 최적화 및 보안 업데이트</span>
                </div>
              </div>
            </div>
          )}

          {/* Warning Box (Example based on design) */}
          {notice.category === '점검' && (
            <div className="mt-6 p-6 bg-amber-50 rounded-2xl border border-amber-100 flex items-start gap-4">
              <AlertTriangle className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
              <p className="text-sm text-amber-700 leading-relaxed">
                경매 종료 시간이 점검 시간과 겹치는 물품의 경우, 점검 시간만큼 경매 종료 시간이 자동으로 연장될 예정입니다. 입찰 참여 시 이 점 유의하시기 바랍니다.
              </p>
            </div>
          )}
        </div>

        {/* Navigation Footer */}
        <div className="border-t border-gray-100">
          <div className="grid grid-cols-1 md:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-gray-100">
            {/* Previous */}
            <div className="p-6 flex items-center">
              {prevNotice ? (
                <Link to={`/notice/${prevNotice.id}`} className="group flex items-center gap-4 w-full">
                  <div className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center shrink-0 group-hover:bg-red-50 group-hover:text-red-500 transition-all">
                    <ChevronLeft className="w-5 h-5" />
                  </div>
                  <div className="flex flex-col gap-1 overflow-hidden">
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">PREV</span>
                    <span className="text-sm font-bold text-gray-900 group-hover:text-red-500 transition-colors line-clamp-1">
                      {prevNotice.title}
                    </span>
                  </div>
                </Link>
              ) : (
                <div className="flex items-center gap-4 opacity-30">
                  <div className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center shrink-0">
                    <ChevronLeft className="w-5 h-5" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">PREV</span>
                    <span className="text-sm font-bold text-gray-900">이전 글이 없습니다.</span>
                  </div>
                </div>
              )}
            </div>

            {/* Back to List */}
            <div className="p-6 flex items-center justify-center bg-gray-50/30">
              <Link
                to="/notice"
                className="flex items-center gap-2 px-6 py-3 bg-white border border-gray-200 rounded-full text-sm font-bold text-gray-600 hover:text-red-500 hover:border-red-200 hover:shadow-lg hover:shadow-red-500/5 transition-all"
              >
                <List className="w-4 h-4" />
                목록으로
              </Link>
            </div>

            {/* Next */}
            <div className="p-6 flex items-center justify-end">
              {nextNotice ? (
                <Link to={`/notice/${nextNotice.id}`} className="group flex items-center gap-4 w-full text-right justify-end">
                  <div className="flex flex-col gap-1 overflow-hidden">
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">NEXT</span>
                    <span className="text-sm font-bold text-gray-900 group-hover:text-red-500 transition-colors line-clamp-1">
                      {nextNotice.title}
                    </span>
                  </div>
                  <div className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center shrink-0 group-hover:bg-red-50 group-hover:text-red-500 transition-all">
                    <ChevronRight className="w-5 h-5" />
                  </div>
                </Link>
              ) : (
                <div className="flex items-center gap-4 opacity-30 text-right justify-end">
                  <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">NEXT</span>
                    <span className="text-sm font-bold text-gray-900">다음 글이 없습니다.</span>
                  </div>
                  <div className="w-10 h-10 rounded-full bg-gray-50 flex items-center justify-center shrink-0">
                    <ChevronRight className="w-5 h-5" />
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
