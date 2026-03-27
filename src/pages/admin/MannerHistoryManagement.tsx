import React, { useState } from 'react';
import { Thermometer, Search, User, Calendar, ArrowRight, TrendingUp, TrendingDown } from 'lucide-react';
import { useAppContext } from '@/context/AppContext';
import { motion } from 'framer-motion';

export const MannerHistoryManagement: React.FC = () => {
  const { mannerHistory, users } = useAppContext();
  const [searchTerm, setSearchTerm] = useState('');

  const getNickname = (userId: string) => {
    return users.find(u => u.id === userId)?.nickname || '알 수 없는 사용자';
  };

  const filteredHistory = mannerHistory.filter(history => 
    getNickname(history.userId).toLowerCase().includes(searchTerm.toLowerCase()) || 
    history.reason.toLowerCase().includes(searchTerm.toLowerCase())
  ).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  return (
    <div className="space-y-6">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">매너온도 히스토리</h1>
          <p className="text-gray-500 mt-1 text-[11px] font-medium">사용자들의 매너온도 변경 이력을 모니터링합니다.</p>
        </div>
        <div className="relative w-64 flex items-center h-10">
          <div className="absolute left-3 top-0 bottom-0 flex items-center pointer-events-none">
            <Search className="text-gray-400 w-4 h-4" />
          </div>
          <input 
            type="text" 
            placeholder="닉네임 또는 사유 검색"
            className="w-full pl-10 pr-4 h-full bg-white border border-gray-200 rounded-none shadow-sm focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] focus:border-transparent font-bold text-xs"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </header>

      {/* History List */}
      <div className="bg-white rounded-none shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50/50 border-b border-gray-100">
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">사용자</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-center">온도 변화</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">변경 사유</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-right">일시</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filteredHistory.length > 0 ? (
                filteredHistory.map((history) => {
                  const isIncrease = history.newTemp > history.previousTemp;
                  return (
                    <tr key={history.id} className="hover:bg-gray-50/50 transition-colors group">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 rounded-none bg-gray-100 flex items-center justify-center shrink-0">
                            <User className="w-4 h-4 text-gray-400" />
                          </div>
                          <span className="text-sm font-bold text-gray-900">{getNickname(history.userId)}</span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center justify-center gap-3">
                          <span className="text-xs font-medium text-gray-400">{history.previousTemp.toFixed(1)}°C</span>
                          <ArrowRight className="w-3 h-3 text-gray-300" />
                          <div className={`flex items-center gap-1 px-2 py-1 rounded-none text-xs font-black ${
                            isIncrease ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-600'
                          }`}>
                            {isIncrease ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                            {history.newTemp.toFixed(1)}°C
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <p className="text-sm font-medium text-gray-600">{history.reason}</p>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex flex-col items-end">
                          <span className="text-xs font-bold text-gray-900">
                            {new Date(history.createdAt).toLocaleDateString()}
                          </span>
                          <span className="text-[10px] text-gray-400 font-medium">
                            {new Date(history.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </div>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={4} className="px-6 py-12 text-center">
                    <div className="flex flex-col items-center gap-2">
                      <Thermometer className="w-8 h-8 text-gray-200" />
                      <p className="text-gray-400 text-sm font-medium">검색 결과가 없습니다.</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
