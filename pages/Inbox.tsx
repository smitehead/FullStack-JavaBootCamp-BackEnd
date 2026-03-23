import React, { useState, useEffect } from 'react';
import { Bell, MessageSquare, ChevronRight, Package, Clock } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { useAppContext } from '../context/AppContext';
import { CURRENT_USER } from '../services/mockData';

export const Inbox: React.FC = () => {
  const { notifications, chats, markNotificationAsRead, markChatAsRead } = useAppContext();
  const location = useLocation();
  const [activeTab, setActiveTab] = useState<'noti' | 'chat'>('noti');
  const [notiFilter, setNotiFilter] = useState<'all' | 'bid' | 'activity'>('all');
  const [chatFilter, setChatFilter] = useState<'all' | 'seller' | 'buyer'>('all');
  const [visibleNotisCount, setVisibleNotisCount] = useState(10);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const tab = params.get('tab');
    if (tab === 'noti' || tab === 'chat') {
      setActiveTab(tab as 'noti' | 'chat');
    }
  }, [location]);

  // Infinite Scroll Logic
  useEffect(() => {
    const handleScroll = () => {
      if (
        window.innerHeight + document.documentElement.scrollTop >=
        document.documentElement.offsetHeight - 100 &&
        !isLoading &&
        activeTab === 'noti'
      ) {
        loadMore();
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [isLoading, activeTab]);

  const loadMore = () => {
    setIsLoading(true);
    // Simulate API delay
    setTimeout(() => {
      setVisibleNotisCount(prev => prev + 10);
      setIsLoading(false);
    }, 500);
  };

  const filteredNotis = notifications.filter(n => {
    if (notiFilter === 'all') return true;
    return n.type === notiFilter;
  }).slice(0, visibleNotisCount);

  const filteredChats = chats.filter(c => {
    // Filter out blocked users
    if (CURRENT_USER.blockedUserIds?.includes(c.otherUser.id)) return false;
    
    if (chatFilter === 'all') return true;
    return c.otherUser.role === chatFilter;
  });

  return (
    <div className="max-w-[800px] mx-auto px-6 py-12">
      <div className="flex items-center justify-center mb-10">
        <h2 className="text-3xl font-black text-gray-900 tracking-tight">알림함</h2>
      </div>

      {/* Main Tabs */}
      <div className="flex border-b border-gray-100 mb-8">
        <button
          onClick={() => setActiveTab('noti')}
          className={`flex-1 py-4 text-lg font-black transition-all relative ${
            activeTab === 'noti' ? 'text-gray-900' : 'text-gray-400 hover:text-gray-600'
          }`}
        >
          알림
          {activeTab === 'noti' && (
            <div className="absolute bottom-0 left-0 w-full h-1 bg-gray-900 rounded-full"></div>
          )}
        </button>
        <button
          onClick={() => setActiveTab('chat')}
          className={`flex-1 py-4 text-lg font-black transition-all relative ${
            activeTab === 'chat' ? 'text-gray-900' : 'text-gray-400 hover:text-gray-600'
          }`}
        >
          대화
          {activeTab === 'chat' && (
            <div className="absolute bottom-0 left-0 w-full h-1 bg-gray-900 rounded-full"></div>
          )}
        </button>
      </div>

      {/* Sub Filters (Chips) */}
      <div className="flex gap-2 mb-8 overflow-x-auto pb-2 scrollbar-hide">
        {activeTab === 'noti' ? (
          <>
            <button
              onClick={() => setNotiFilter('all')}
              className={`px-5 py-2 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                notiFilter === 'all' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
              }`}
            >
              전체
            </button>
            <button
              onClick={() => setNotiFilter('bid')}
              className={`px-5 py-2 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                notiFilter === 'bid' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
              }`}
            >
              입찰소식
            </button>
            <button
              onClick={() => setNotiFilter('activity')}
              className={`px-5 py-2 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                notiFilter === 'activity' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
              }`}
            >
              활동알림
            </button>
          </>
        ) : (
          <>
            <button
              onClick={() => setChatFilter('all')}
              className={`px-5 py-2 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                chatFilter === 'all' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
              }`}
            >
              전체
            </button>
            <button
              onClick={() => setChatFilter('seller')}
              className={`px-5 py-2 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                chatFilter === 'seller' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
              }`}
            >
              판매자
            </button>
            <button
              onClick={() => setChatFilter('buyer')}
              className={`px-5 py-2 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                chatFilter === 'buyer' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
              }`}
            >
              구매자
            </button>
          </>
        )}
      </div>

      {/* List Content */}
      <div className="space-y-4">
        {activeTab === 'noti' ? (
          filteredNotis.length > 0 ? (
            filteredNotis.map(noti => (
              <Link
                key={noti.id}
                to={noti.link}
                onClick={() => markNotificationAsRead(noti.id)}
                className={`flex items-start gap-4 p-5 rounded-3xl border transition-all hover:shadow-md ${
                  noti.read ? 'bg-white border-gray-100' : 'bg-indigo-50/30 border-indigo-100'
                }`}
              >
                <div className={`p-3 rounded-2xl ${
                  noti.type === 'bid' ? 'bg-emerald-50 text-emerald-600' :
                  noti.type === 'activity' ? 'bg-indigo-50 text-indigo-600' :
                  'bg-gray-50 text-gray-600'
                }`}>
                  <Bell className="w-6 h-6" />
                </div>
                <div className="flex-1">
                  <p className="text-gray-900 font-bold leading-snug mb-2">{noti.message}</p>
                  <div className="flex items-center gap-2 text-xs text-gray-400 font-medium">
                    <Clock className="w-3.5 h-3.5" />
                    {new Date(noti.createdAt).toLocaleString()}
                  </div>
                </div>
                <ChevronRight className="w-5 h-5 text-gray-300 self-center" />
              </Link>
            ))
          ) : (
            <div className="text-center py-20 bg-gray-50 rounded-[40px] border border-dashed border-gray-200">
              <Bell className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500 font-bold">알림이 없습니다.</p>
            </div>
          )
        ) : (
          filteredChats.length > 0 ? (
            filteredChats.map(chat => (
              <Link
                key={chat.id}
                to={`/chat/${chat.id}`}
                onClick={() => markChatAsRead(chat.id)}
                className="flex items-center gap-4 p-5 bg-white rounded-3xl border border-gray-100 transition-all hover:shadow-md"
              >
                <div className="relative">
                  <img
                    src={chat.otherUser.profileImage || undefined}
                    alt={chat.otherUser.nickname}
                    className="w-14 h-14 rounded-2xl object-cover border border-gray-100"
                  />
                  {chat.unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 bg-[#FF5A5A] text-white text-[10px] font-black px-1.5 py-0.5 rounded-full border-2 border-white">
                      {chat.unreadCount}
                    </span>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-black text-gray-900 truncate">{chat.otherUser.nickname}</span>
                    <span className={`text-[10px] font-black px-2 py-0.5 rounded-full uppercase tracking-widest ${
                      chat.otherUser.role === 'seller' ? 'bg-indigo-100 text-indigo-600' : 'bg-emerald-100 text-emerald-600'
                    }`}>
                      {chat.otherUser.role === 'seller' ? '판매자' : '구매자'}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500 truncate font-medium">{chat.lastMessage}</p>
                  <div className="flex items-center gap-2 mt-2 text-[10px] text-gray-400 font-bold uppercase tracking-wider">
                    <Package className="w-3 h-3" />
                    <span className="truncate">{chat.productTitle}</span>
                    <span className="mx-1">•</span>
                    <span>{new Date(chat.lastMessageAt).toLocaleDateString()}</span>
                  </div>
                </div>
                <ChevronRight className="w-5 h-5 text-gray-300" />
              </Link>
            ))
          ) : (
            <div className="text-center py-20 bg-gray-50 rounded-[40px] border border-dashed border-gray-200">
              <MessageSquare className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500 font-bold">대화 내역이 없습니다.</p>
            </div>
          )
        )}
      </div>

      {isLoading && (
        <div className="flex justify-center py-8">
          <div className="w-8 h-8 border-4 border-gray-200 border-t-gray-900 rounded-full animate-spin"></div>
        </div>
      )}
    </div>
  );
};
