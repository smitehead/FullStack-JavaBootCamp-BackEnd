import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';
import { MOCK_NOTICES } from '@/services/mockData';
import { NoticeCategory } from '@/types';
import { format } from 'date-fns';
import { CustomerCenterSidebar } from '@/pages/cs/CustomerCenterSidebar';

export const NoticeList: React.FC = () => {
  const [activeCategory, setActiveCategory] = useState<NoticeCategory | '전체'>('전체');
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const categories: (NoticeCategory | '전체')[] = ['전체', '업데이트', '이벤트', '점검', '정책'];

  const filteredNotices = MOCK_NOTICES.filter(notice => {
    const matchesCategory = activeCategory === '전체' || notice.category === activeCategory;
    const matchesSearch = notice.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      notice.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  // Sort: Important first, then by date desc
  const sortedNotices = [...filteredNotices].sort((a, b) => {
    if (a.isImportant && !b.isImportant) return -1;
    if (!a.isImportant && b.isImportant) return 1;
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-12">
      <div className="mb-10">
        <h1 className="text-xl font-bold text-gray-900 mb-2">고객센터</h1>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        <CustomerCenterSidebar />

        <div className="flex-1">
          <div className="mb-6">
            <h2 className="text-xl font-bold text-gray-900">공지사항</h2>
            <p className="text-gray-500 mt-1 text-sm">경매 마켓플레이스의 새로운 소식과 안내를 확인하세요.</p>
          </div>

          {/* Search Bar */}
          <div className="relative mb-8">
            <input
              type="text"
              placeholder="공지사항 제목이나 내용을 검색하세요"
              className="w-full h-14 pl-12 pr-4 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-gray-200 transition-all outline-none text-sm text-gray-900"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <div className="absolute left-0 top-0 bottom-0 w-12 flex items-center justify-center">
              <Search className="w-5 h-5 text-gray-400" />
            </div>
          </div>

          {/* Category Tabs */}
          <div className="flex flex-wrap gap-2 mb-8">
            {categories.map(category => (
              <button
                key={category}
                onClick={() => {
                  setActiveCategory(category);
                  setCurrentPage(1);
                }}
                className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all ${activeCategory === category
                  ? 'bg-red-500 text-white shadow-lg shadow-red-200'
                  : 'bg-white border border-gray-100 text-gray-500 hover:bg-gray-50'
                  }`}
              >
                {category}
              </button>
            ))}
          </div>

          {/* Notice List */}
          <div className="bg-white border border-gray-100 rounded-3xl overflow-hidden shadow-sm">
            {sortedNotices.length > 0 ? (
              <div className="divide-y divide-gray-50">
                {sortedNotices.map(notice => (
                  <Link
                    key={notice.id}
                    to={`/notice/${notice.id}`}
                    className="block px-8 py-5 transition-colors group"
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-20 shrink-0">
                        {notice.isImportant ? (
                          <span className="px-2 py-0.5 bg-gray-900 text-white text-[10px] font-bold rounded-md inline-block">공지</span>
                        ) : (
                          <span className="text-xs font-bold text-gray-400">{notice.category}</span>
                        )}
                      </div>
                      <h3 className={`flex-1 text-sm text-gray-900 group-hover:underline underline-offset-4 transition-all line-clamp-1 ${notice.isImportant ? 'font-black' : 'font-bold'}`}>
                        {notice.title}
                      </h3>
                      <span className="w-24 text-sm text-gray-400 font-medium shrink-0 text-right">
                        {format(new Date(notice.createdAt), 'yyyy.MM.dd')}
                      </span>
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="py-20 text-center">
                <p className="text-gray-400">검색 결과가 없습니다.</p>
              </div>
            )}
          </div>

          {/* Pagination */}
          <div className="flex justify-center items-center gap-2 mt-10">
            <button className="p-2 border border-gray-100 rounded-xl hover:bg-gray-50 transition-colors">
              <ChevronLeft className="w-4 h-4 text-gray-400" />
            </button>
            {[1, 2, 3, 4, 5].map(page => (
              <button
                key={page}
                className={`w-10 h-10 rounded-xl text-sm font-bold transition-all ${page === currentPage
                  ? 'bg-red-500 text-white shadow-lg shadow-red-200'
                  : 'bg-white border border-gray-100 text-gray-500 hover:bg-gray-50'
                  }`}
              >
                {page}
              </button>
            ))}
            <button className="p-2 border border-gray-100 rounded-xl hover:bg-gray-50 transition-colors">
              <ChevronRight className="w-4 h-4 text-gray-400" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
