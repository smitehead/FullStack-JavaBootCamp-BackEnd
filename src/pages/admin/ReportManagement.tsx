import React, { useState } from 'react';
import { AlertTriangle, Search, Filter, CheckCircle2, MoreVertical, User, Gavel, MessageSquare, ShieldAlert, X } from 'lucide-react';
import { useAppContext } from '@/context/AppContext';
import { Report } from '@/types';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import { showToast } from '@/components/toastService';

export const ReportManagement: React.FC = () => {
  const { reports, resolveReport, users, products } = useAppContext();
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'pending' | 'resolved'>('all');
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [resolveAction, setResolveAction] = useState('');
  const [showResolveModal, setShowResolveModal] = useState(false);

  const filteredReports = reports.filter(r => {
    const matchesSearch = r.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.reason.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'all' || r.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const handleResolve = () => {
    if (!selectedReport) return;
    if (!resolveAction.trim()) {
      showToast('처리 내용 및 제재 사유를 입력해주세요.', 'error');
      return;
    }
    resolveReport(selectedReport.id, resolveAction.trim());
    setShowResolveModal(false);
    setSelectedReport(null);
    setResolveAction('');
    showToast('신고 처리가 완료되었습니다.', 'success');
  };

  const getTargetInfo = (report: Report) => {
    if (report.targetType === 'user') {
      const user = users.find(u => u.id === report.targetId);
      return {
        name: user?.nickname || '알 수 없는 사용자',
        icon: User,
        color: 'text-blue-500',
        bgColor: 'bg-blue-50',
        link: `/admin/users?nickname=${user?.nickname}`
      };
    } else if (report.targetType === 'product') {
      const product = products.find(p => p.id === report.targetId);
      return {
        name: product?.title || '알 수 없는 상품',
        icon: Gavel,
        color: 'text-orange-500',
        bgColor: 'bg-orange-50',
        link: `/admin/auctions?search=${product?.title}`
      };
    }
    return {
      name: '알 수 없는 대상',
      icon: MessageSquare,
      color: 'text-gray-500',
      bgColor: 'bg-gray-50',
      link: '#'
    };
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">신고 관리</h1>
          <p className="text-gray-500 mt-1 text-[11px] font-medium">사용자 및 상품에 대한 신고 내역을 검토하고 처리합니다.</p>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-3">
          <div className="relative w-64 flex items-center h-10">
            <div className="absolute left-3 top-0 bottom-0 flex items-center pointer-events-none">
              <Search className="text-gray-400 w-4 h-4" />
            </div>
            <input
              type="text"
              placeholder="신고 ID 또는 사유 검색"
              className="w-full pl-10 pr-4 h-full bg-white border border-gray-200 rounded-none shadow-sm focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] focus:border-transparent font-bold text-xs"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-none px-4 py-2.5 shadow-sm">
            <Filter className="w-3.5 h-3.5 text-gray-400" />
            <select
              className="bg-transparent text-xs font-bold text-gray-600 focus:outline-none cursor-pointer"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
            >
              <option value="all">전체 상태</option>
              <option value="pending">처리 대기</option>
              <option value="resolved">처리 완료</option>
            </select>
          </div>
        </div>
      </header>

      {/* Report List Table */}
      <div className="bg-white rounded-none shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50/50 border-b border-gray-100">
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">신고 정보</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">신고 대상</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">신고 사유</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-center">신고 일시</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-center">상태</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-right">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filteredReports.map((report) => {
                const target = getTargetInfo(report);
                const reporter = users.find(u => u.id === report.reporterId);
                return (
                  <tr key={report.id} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">ID: {report.id}</span>
                        <Link
                          to={`/admin/users?nickname=${reporter?.nickname}`}
                          className="text-sm font-bold text-gray-700 hover:text-[#FF5A5A] transition-colors"
                        >
                          신고자: {reporter?.nickname || '알 수 없음'}
                        </Link>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <div className={`w-8 h-8 rounded-none ${target.bgColor} flex items-center justify-center shrink-0`}>
                          <target.icon className={`w-4 h-4 ${target.color}`} />
                        </div>
                        <Link
                          to={target.link}
                          className="text-sm font-bold text-gray-700 hover:text-[#FF5A5A] transition-colors line-clamp-1"
                        >
                          {target.name}
                        </Link>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm font-bold text-gray-900">{report.reason}</p>
                      <p className="text-[10px] text-gray-400 font-medium line-clamp-1">{report.details}</p>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className="text-[10px] font-medium text-gray-400">{new Date(report.createdAt).toLocaleString()}</span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className={`inline-flex px-2 py-1 rounded-none text-[10px] font-black ${report.status === 'pending' ? 'bg-orange-50 text-orange-600' : 'bg-green-50 text-green-600'
                        }`}>
                        {report.status === 'pending' ? '대기중' : '처리됨'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {report.status === 'pending' && (
                        <button
                          onClick={() => {
                            setSelectedReport(report);
                            setShowResolveModal(true);
                          }}
                          className="p-2 text-gray-400 hover:text-[#FF5A5A] hover:bg-red-50 rounded-none transition-all"
                          title="신고 처리"
                        >
                          <ShieldAlert className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {filteredReports.length === 0 && (
          <div className="py-20 text-center">
            <AlertTriangle className="w-12 h-12 text-gray-100 mx-auto mb-4" />
            <p className="text-gray-400 font-bold">검색 결과가 없습니다.</p>
          </div>
        )}
      </div>

      {/* Resolve Report Modal */}
      <AnimatePresence>
        {showResolveModal && selectedReport && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => { setShowResolveModal(false); setResolveAction(''); }}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              className="relative w-full max-w-md bg-white rounded-none p-8 shadow-2xl overflow-hidden"
            >
              <div className="absolute top-0 left-0 w-full h-2 bg-[#FF5A5A]" />

              <div className="flex items-center gap-4 mb-6">
                <div className="w-12 h-12 rounded-none bg-red-50 flex items-center justify-center shrink-0">
                  <ShieldAlert className="w-6 h-6 text-[#FF5A5A]" />
                </div>
                <div>
                  <h3 className="text-xl font-black text-gray-900">신고 처리</h3>
                  <p className="text-sm text-gray-500 font-medium">신고 내용을 검토하고 조치를 취합니다.</p>
                </div>
              </div>

              <div className="space-y-4 mb-6">
                <div className="bg-gray-50 rounded-none p-4">
                  <p className="text-xs font-black text-gray-400 uppercase tracking-widest mb-1">신고 사유</p>
                  <p className="text-sm font-bold text-gray-900">{selectedReport.reason}</p>
                </div>
                <div className="bg-gray-50 rounded-none p-4">
                  <p className="text-xs font-black text-gray-400 uppercase tracking-widest mb-1">상세 내용</p>
                  <p className="text-sm font-medium text-gray-600">{selectedReport.details}</p>
                </div>
                <div>
                  <label className="block text-xs font-black text-gray-700 mb-2">처리 내용 / 제재 사유 <span className="text-[#FF5A5A]">*</span></label>
                  <textarea
                    className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm resize-none h-24"
                    placeholder="처리 내용 및 피신고자에게 전달할 제재 사유를 입력하세요"
                    value={resolveAction}
                    onChange={(e) => setResolveAction(e.target.value)}
                  />
                </div>
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  onClick={() => { setShowResolveModal(false); setResolveAction(''); }}
                  className="flex-1 py-4 bg-gray-100 text-gray-500 font-black rounded-none hover:bg-gray-200 transition-all active:scale-95"
                >
                  취소
                </button>
                <button
                  onClick={handleResolve}
                  className="flex-1 py-4 bg-[#FF5A5A] text-white font-black rounded-none hover:bg-[#E04848] transition-all shadow-lg shadow-red-500/20 active:scale-95"
                >
                  처리 완료
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};
