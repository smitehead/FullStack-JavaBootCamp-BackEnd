import React, { useState } from 'react';
import { Bell, Send, Link as LinkIcon, Info, Plus, X } from 'lucide-react';
import { useAppContext } from '@/context/AppContext';
import { NotificationType } from '@/types';
import { motion, AnimatePresence } from 'framer-motion';

export const NotificationManagement: React.FC = () => {
  const { addNotification, notifications } = useAppContext();
  const [message, setMessage] = useState('');
  const [link, setLink] = useState('');
  const [type, setType] = useState<NotificationType>('system');
  const [isSending, setIsSending] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim() || !link.trim()) {
      alert('메시지와 링크를 모두 입력해주세요.');
      return;
    }

    setIsSending(true);
    // Simulate network delay
    setTimeout(() => {
      addNotification(message, link, type);
      setMessage('');
      setLink('');
      setType('system');
      setIsSending(false);
      setShowForm(false);
      alert('알림이 성공적으로 전송되었습니다.');
    }, 500);
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">알림 관리</h1>
          <p className="text-gray-500 mt-1 text-[11px] font-medium">사용자에게 새로운 알림을 발송하고 내역을 확인합니다.</p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center justify-center gap-2 px-4 py-2 bg-[#FF5A5A] text-white font-black rounded-none hover:bg-[#E04848] transition-all shadow-lg shadow-red-500/20 active:scale-95 text-sm"
        >
          {showForm ? <X className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
          {showForm ? '닫기' : '새 알림 등록'}
        </button>
      </header>

      <AnimatePresence>
        {showForm && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="bg-white rounded-none p-8 shadow-sm border border-gray-100"
          >
            <h2 className="text-xl font-black text-gray-900 mb-6 flex items-center gap-2">
              <Send className="w-5 h-5 text-[#FF5A5A]" /> 새 알림 발송
            </h2>

            <form onSubmit={handleSend} className="flex flex-col lg:flex-row items-end gap-6">
              <div className="w-full lg:w-48 shrink-0">
                <label className="block text-xs font-black text-gray-700 mb-2 uppercase tracking-widest">알림 유형</label>
                <div className="grid grid-cols-3 gap-2">
                  {(['system', 'activity', 'bid'] as NotificationType[]).map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setType(t)}
                      className={`py-2 text-[10px] font-black rounded-none transition-all ${type === t
                          ? 'bg-[#FF5A5A] text-white shadow-lg shadow-red-100'
                          : 'bg-gray-50 text-gray-400 hover:bg-gray-100'
                        }`}
                    >
                      {t === 'system' && '시스템'}
                      {t === 'activity' && '활동'}
                      {t === 'bid' && '입찰'}
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex-1 w-full">
                <label className="block text-xs font-black text-gray-700 mb-2 uppercase tracking-widest">알림 메시지</label>
                <input
                  type="text"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm"
                  placeholder="사용자에게 보낼 메시지를 입력하세요."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                />
              </div>

              <div className="w-full lg:w-72 shrink-0">
                <label className="block text-xs font-black text-gray-700 mb-2 uppercase tracking-widest">이동 링크 (URL)</label>
                <div className="relative">
                  <LinkIcon className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="text"
                    className="w-full pl-11 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm"
                    placeholder="예: /products/p_123"
                    value={link}
                    onChange={(e) => setLink(e.target.value)}
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isSending}
                className="w-full lg:w-40 py-3 bg-[#FF5A5A] text-white font-black rounded-none hover:bg-[#E04848] transition-all shadow-lg shadow-red-500/20 active:scale-95 disabled:opacity-50 disabled:scale-100"
              >
                {isSending ? '발송 중...' : '발송하기'}
              </button>
            </form>
            <p className="text-[10px] text-gray-400 mt-4 font-medium flex items-center gap-1">
              <Info className="w-3 h-3" /> 내부 경로(/products/...) 또는 외부 URL을 입력하세요.
            </p>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Notification History */}
      <div className="bg-white rounded-none shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-8 py-6 border-b border-gray-50 flex items-center justify-between">
          <h2 className="text-xl font-black text-gray-900 flex items-center gap-2">
            <Bell className="w-5 h-5 text-gray-400" /> 발송 내역
          </h2>
          <span className="text-xs font-bold text-gray-400">최근 50건</span>
        </div>

        <div className="divide-y divide-gray-50 max-h-[600px] overflow-y-auto">
          {notifications.map((noti) => (
            <div key={noti.id} className="px-8 py-5 hover:bg-gray-50 transition-colors group">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-4">
                  <div className={`mt-1 w-8 h-8 rounded-none flex items-center justify-center shrink-0 ${noti.type === 'system' ? 'bg-blue-50 text-blue-500' :
                      noti.type === 'activity' ? 'bg-purple-50 text-purple-500' :
                        'bg-orange-50 text-orange-500'
                    }`}>
                    <Bell className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-gray-900 leading-relaxed">{noti.message}</p>
                    <div className="flex items-center gap-3 mt-2">
                      <span className="text-[10px] font-black uppercase tracking-widest text-gray-400">
                        {noti.type === 'system' ? '시스템' : noti.type === 'activity' ? '활동' : '입찰'}
                      </span>
                      <span className="text-[10px] font-medium text-gray-300">|</span>
                      <span className="text-[10px] font-medium text-gray-400 flex items-center gap-1">
                        <LinkIcon className="w-2.5 h-2.5" /> {noti.link}
                      </span>
                      <span className="text-[10px] font-medium text-gray-300">|</span>
                      <span className="text-[10px] font-medium text-gray-400">{new Date(noti.createdAt).toLocaleString()}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
          {notifications.length === 0 && (
            <div className="px-8 py-20 text-center">
              <p className="text-gray-400 font-bold">발송된 알림이 없습니다.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
