import React, { useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Users, Bell, MessageSquare, Image, Home, LayoutDashboard, LogOut, UserX, Megaphone, Gavel, Thermometer, AlertTriangle, Settings, History } from 'lucide-react';
import { useAppContext } from '../../context/AppContext';

interface AdminLayoutProps {
  children: React.ReactNode;
}

export const AdminLayout: React.FC<AdminLayoutProps> = ({ children }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAppContext();

  useEffect(() => {
    // Check if user is admin
    if (!user || !user.isAdmin) {
      alert('관리자 권한이 필요합니다.');
      navigate('/');
    }
  }, [user, navigate]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user || !user.isAdmin) {
    return null; // Don't render anything while redirecting
  }

  const menuGroups = [
    {
      title: '대시보드',
      items: [
        { path: '/admin', icon: LayoutDashboard, label: '대시보드' },
      ]
    },
    {
      title: '사용자 관리',
      items: [
        { path: '/admin/users', icon: Users, label: '사용자 관리' },
        { path: '/admin/manner-history', icon: Thermometer, label: '매너온도 히스토리' },
      ]
    },
    {
      title: '콘텐츠 관리',
      items: [
        { path: '/admin/auctions', icon: Gavel, label: '경매 관리' },
        { path: '/admin/reports', icon: AlertTriangle, label: '신고 관리' },
        { path: '/admin/notices', icon: Megaphone, label: '공지사항 관리' },
        { path: '/admin/inquiries', icon: MessageSquare, label: '문의사항 관리' },
        { path: '/admin/notifications', icon: Bell, label: '알림 관리' },
        { path: '/admin/banners', icon: Image, label: '배너 관리' },
      ]
    },
    {
      title: '시스템',
      items: [
        { path: '/admin/activity-logs', icon: History, label: '관리자 활동 로그' },
      ]
    }
  ];

  return (
    <div className="flex min-h-screen bg-gray-200">
      {/* Sidebar */}
      <aside className="w-60 bg-[#1A1D21] text-white flex flex-col shrink-0">
        <div className="p-5 border-b border-white/5">
          <Link to="/" className="flex items-center space-x-2">
            <span className="text-xl font-black text-[#FF5A5A] tracking-tighter">JAVAJAVA</span>
            <span className="text-[10px] font-bold bg-white/10 px-1.5 py-0.5 rounded-none text-gray-300">ADMIN</span>
          </Link>
        </div>
        
        <nav className="flex-1 py-4 px-3 space-y-6 overflow-y-auto scrollbar-hide">
          {menuGroups.map((group) => (
            <div key={group.title} className="space-y-1">
              <h3 className="px-4 text-[10px] font-bold text-gray-500 uppercase tracking-widest mb-2">
                {group.title}
              </h3>
              <div className="space-y-0.5">
                {group.items.map((item) => {
                  const Icon = item.icon;
                  const isActive = location.pathname === item.path;
                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={`flex items-center space-x-3 px-4 py-2 rounded-none transition-all ${
                        isActive 
                          ? 'bg-[#FF5A5A] text-white shadow-md shadow-red-900/20' 
                          : 'text-gray-400 hover:bg-white/5 hover:text-white'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                      <span className="text-[13px] font-bold">{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        <div className="p-3 border-t border-white/5 bg-[#141619]">
          <Link 
            to="/" 
            className="flex items-center space-x-3 px-4 py-2 text-gray-400 hover:text-white transition-colors text-xs"
          >
            <Home className="w-4 h-4" />
            <span className="font-bold">사용자 페이지</span>
          </Link>
          <button 
            onClick={handleLogout}
            className="w-full flex items-center space-x-3 px-4 py-2 text-gray-400 hover:text-white transition-colors mt-0.5 text-xs"
          >
            <LogOut className="w-4 h-4" />
            <span className="font-bold">로그아웃</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-8 overflow-y-auto">
        <div className="max-w-7xl mx-auto">
          {children}
        </div>
      </main>
    </div>
  );
};
