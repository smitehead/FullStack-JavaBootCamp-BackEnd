import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CURRENT_USER, MOCK_POINT_HISTORY } from '../services/mockData';
import { Wallet, Plus, Minus, ChevronDown, ChevronUp, ArrowUpRight, ArrowDownLeft, CreditCard, ArrowLeft, Filter } from 'lucide-react';

export const Points: React.FC = () => {
  const navigate = useNavigate();
  const [dateFilter, setDateFilter] = useState<'all' | '1m' | '3m' | '6m'>('all');
  const [typeFilter, setTypeFilter] = useState<'all' | 'charge' | 'withdraw' | 'use'>('all');
  const [showDateFilters, setShowDateFilters] = useState(false);
  const [showTypeFilters, setShowTypeFilters] = useState(false);
  const [visibleCount, setVisibleCount] = useState(10);
  const [isLoading, setIsLoading] = useState(false);
  const loaderRef = useRef<HTMLDivElement>(null);

  const filteredHistory = MOCK_POINT_HISTORY.filter(item => {
    // Date filter
    let dateMatch = true;
    if (dateFilter !== 'all') {
      const now = new Date();
      const itemDate = new Date(item.createdAt);
      const diffMonths = (now.getFullYear() - itemDate.getFullYear()) * 12 + (now.getMonth() - itemDate.getMonth());
      if (dateFilter === '1m') dateMatch = diffMonths < 1;
      else if (dateFilter === '3m') dateMatch = diffMonths < 3;
      else if (dateFilter === '6m') dateMatch = diffMonths < 6;
    }

    // Type filter
    let typeMatch = true;
    if (typeFilter !== 'all') {
      typeMatch = item.type === typeFilter;
    }

    return dateMatch && typeMatch;
  });

  const visibleHistory = filteredHistory.slice(0, visibleCount);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && visibleCount < filteredHistory.length && !isLoading) {
          setIsLoading(true);
          setTimeout(() => {
            setVisibleCount(prev => prev + 10);
            setIsLoading(false);
          }, 500);
        }
      },
      { threshold: 1.0 }
    );

    if (loaderRef.current) {
      observer.observe(loaderRef.current);
    }

    return () => observer.disconnect();
  }, [visibleCount, filteredHistory.length, isLoading]);

  const formatDescription = (desc: string) => {
    const parts = desc.split('(');
    if (parts.length > 1) {
      return {
        main: parts[0].trim(),
        sub: parts[1].replace(')', '').trim()
      };
    }
    return { main: desc, sub: '' };
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const m = (date.getMonth() + 1).toString().padStart(2, '0');
    const d = date.getDate().toString().padStart(2, '0');
    const h = date.getHours().toString().padStart(2, '0');
    const min = date.getMinutes().toString().padStart(2, '0');
    const s = date.getSeconds().toString().padStart(2, '0');
    return `${m}.${d} ${h}:${min}:${s}`;
  };

  const getTypeLabel = (type: string) => {
    switch (type) {
      case 'charge': return '충전';
      case 'withdraw': return '출금';
      case 'use': return '사용';
      default: return '기타';
    }
  };
  return (
    <div className="max-w-2xl mx-auto px-6 py-12">
      <div className="flex items-center justify-between mb-10">
        <div className="flex items-center gap-4">
          <button 
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
          >
            <ArrowLeft className="w-6 h-6 text-gray-900" />
          </button>
          <h2 className="text-3xl font-black text-gray-900 tracking-normal">포인트</h2>
        </div>
        <Link 
          to="/settings?tab=card"
          className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 text-gray-600 text-xs font-bold rounded-xl hover:bg-gray-50 transition-all"
        >
          <CreditCard className="w-3.5 h-3.5" /> 카드 관리
        </Link>
      </div>

      {/* Current Balance Card */}
      <div className="bg-gray-900 rounded-[32px] p-8 text-white mb-10 shadow-2xl shadow-indigo-100 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-indigo-500/10 rounded-full -mr-16 -mt-16"></div>
        <div className="relative z-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div>
            <p className="text-xs font-bold text-indigo-300 uppercase tracking-widest mb-2">현재 보유 포인트</p>
            <h3 className="text-4xl font-black tracking-tight">{CURRENT_USER.points.toLocaleString()}<span className="text-xl ml-1 text-indigo-400">P</span></h3>
          </div>
          
          <div className="flex gap-2">
            <Link 
              to="/points/charge"
              className="px-6 py-2.5 bg-indigo-600 text-white text-xs font-bold rounded-xl hover:bg-indigo-700 transition-all shadow-lg shadow-indigo-900/20"
            >
              충전
            </Link>
            <Link 
              to="/points/withdraw"
              className="px-6 py-2.5 bg-white/10 border border-white/10 text-white text-xs font-bold rounded-xl hover:bg-white/20 transition-all"
            >
              출금
            </Link>
          </div>
        </div>
      </div>

      {/* History Section */}
      <div className="space-y-6">
        <div className="flex items-center justify-between px-2">
          <h4 className="text-sm font-bold text-gray-400 uppercase tracking-wider">포인트 내역</h4>
          <div className="flex items-center gap-4">
            {/* Type Filter */}
            <div className="relative">
              <button 
                onClick={() => {
                  setShowTypeFilters(!showTypeFilters);
                  setShowDateFilters(false);
                }}
                className="text-xs font-bold text-gray-400 hover:text-gray-600 flex items-center gap-1"
              >
                {typeFilter === 'all' ? '전체유형' : getTypeLabel(typeFilter)}
                {showTypeFilters ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
              </button>
              
              {showTypeFilters && (
                <div className="absolute right-0 mt-2 w-24 bg-white border border-gray-100 rounded-xl shadow-xl z-10 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                  {[
                    { label: '전체', value: 'all' },
                    { label: '입금(충전)', value: 'charge' },
                    { label: '출금', value: 'withdraw' },
                    { label: '입찰(사용)', value: 'use' }
                  ].map((opt) => (
                    <button
                      key={opt.value}
                      onClick={() => {
                        setTypeFilter(opt.value as any);
                        setShowTypeFilters(false);
                        setVisibleCount(10);
                      }}
                      className={`w-full px-4 py-2 text-xs font-bold text-left hover:bg-gray-50 transition-colors ${typeFilter === opt.value ? 'text-indigo-600 bg-indigo-50/50' : 'text-gray-600'}`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Date Filter */}
            <div className="relative">
              <button 
                onClick={() => {
                  setShowDateFilters(!showDateFilters);
                  setShowTypeFilters(false);
                }}
                className="text-xs font-bold text-gray-400 hover:text-gray-600 flex items-center gap-1"
              >
                {dateFilter === 'all' ? '전체기간' : dateFilter === '1m' ? '1개월' : dateFilter === '3m' ? '3개월' : '6개월'}
                {showDateFilters ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
              </button>
              
              {showDateFilters && (
                <div className="absolute right-0 mt-2 w-24 bg-white border border-gray-100 rounded-xl shadow-xl z-10 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                  {[
                    { label: '전체', value: 'all' },
                    { label: '1개월', value: '1m' },
                    { label: '3개월', value: '3m' },
                    { label: '6개월', value: '6m' }
                  ].map((opt) => (
                    <button
                      key={opt.value}
                      onClick={() => {
                        setDateFilter(opt.value as any);
                        setShowDateFilters(false);
                        setVisibleCount(10);
                      }}
                      className={`w-full px-4 py-2 text-xs font-bold text-left hover:bg-gray-50 transition-colors ${dateFilter === opt.value ? 'text-indigo-600 bg-indigo-50/50' : 'text-gray-600'}`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="bg-white rounded-3xl border border-gray-100 overflow-hidden shadow-sm">
          {visibleHistory.length > 0 ? (
            <div className="divide-y divide-gray-50">
              {visibleHistory.map(item => (
                <div key={item.id} className="p-6 flex items-center justify-between hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0">
                  <div className="flex flex-col gap-1">
                    <p className="text-xs text-gray-400 font-medium">
                      {formatDate(item.createdAt)} {formatDescription(item.description).sub && <span className="mx-1 text-gray-200">|</span>} {formatDescription(item.description).sub}
                    </p>
                    <p className="text-lg font-bold text-gray-900">{formatDescription(item.description).main}</p>
                  </div>
                  <div className="text-right flex flex-col gap-1">
                    <p className={`text-xl font-bold ${
                      item.type === 'charge' ? 'text-blue-500' : 'text-gray-900'
                    }`}>
                      {item.type === 'charge' ? '+' : '-'}{item.amount.toLocaleString()}원
                    </p>
                    <p className="text-base text-gray-400 font-medium">{item.balance.toLocaleString()}원</p>
                  </div>
                </div>
              ))}
              {visibleCount < filteredHistory.length && (
                <div ref={loaderRef} className="py-8 flex justify-center">
                  <div className="w-6 h-6 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
                </div>
              )}
            </div>
          ) : (
            <div className="py-20 text-center">
              <p className="text-gray-400 font-medium">포인트 내역이 없습니다.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
