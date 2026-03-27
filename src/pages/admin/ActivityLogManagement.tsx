import React, { useState } from 'react';
import { History, Search, Filter, ShieldCheck, User, Gavel, AlertTriangle, Settings, Calendar, Info } from 'lucide-react';
import { useAppContext } from '@/context/AppContext';
import { ActivityLog } from '@/types';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';

export const ActivityLogManagement: React.FC = () => {
  const { activityLogs } = useAppContext();
  const [searchTerm, setSearchTerm] = useState('');

  const filteredLogs = activityLogs.filter(log =>
    log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
    log.details.toLowerCase().includes(searchTerm.toLowerCase()) ||
    log.adminNickname.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getTargetIcon = (type?: ActivityLog['targetType']) => {
    switch (type) {
      case 'user': return User;
      case 'product': return Gavel;
      case 'report': return AlertTriangle;
      default: return Info;
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">관리자 활동 로그</h1>
          <p className="text-gray-500 mt-1 text-[11px] font-medium">관리자들의 모든 활동 내역을 기록하고 모니터링합니다.</p>
        </div>
        <div className="relative w-64 flex items-center h-10">
          <div className="absolute left-3 top-0 bottom-0 flex items-center pointer-events-none">
            <Search className="text-gray-400 w-4 h-4" />
          </div>
          <input
            type="text"
            placeholder="관리자 닉네임, 활동 내용 또는 상세 정보 검색"
            className="w-full pl-10 pr-4 h-full bg-white border border-gray-200 rounded-none shadow-sm focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] focus:border-transparent font-bold text-xs"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </header>

      {/* Log List */}
      <div className="bg-white rounded-none shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50/50 border-b border-gray-100">
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">관리자</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">활동 유형</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">상세 내용</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-center">대상 유형</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-right">일시</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filteredLogs.map((log) => {
                const TargetIcon = getTargetIcon(log.targetType);
                return (
                  <tr key={log.id} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-none bg-red-50 flex items-center justify-center shrink-0">
                          <ShieldCheck className="w-4 h-4 text-[#FF5A5A]" />
                        </div>
                        <div className="flex flex-col">
                          <span className="text-sm font-bold text-gray-900">{log.adminNickname}</span>
                          <span className="text-[10px] text-gray-400 font-medium">ID: {log.adminId}</span>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-sm font-black text-gray-900">{log.action}</span>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm font-medium text-gray-600 line-clamp-1">{log.details}</p>
                      {log.targetId && (
                        <span className="text-[10px] text-gray-400 font-medium">Target ID: {log.targetId}</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className="inline-flex items-center gap-1 px-2 py-1 bg-gray-100 text-gray-600 rounded-none text-[10px] font-black">
                        <TargetIcon className="w-3 h-3" />
                        {log.targetType === 'user' ? '사용자' :
                          log.targetType === 'product' ? '상품/경매' :
                            log.targetType === 'report' ? '신고' : '기타'}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-1 text-[10px] font-medium text-gray-400">
                        <Calendar className="w-3 h-3" />
                        {new Date(log.createdAt).toLocaleString()}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {filteredLogs.length === 0 && (
          <div className="py-20 text-center">
            <History className="w-12 h-12 text-gray-100 mx-auto mb-4" />
            <p className="text-gray-400 font-bold">활동 로그가 없습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
};
