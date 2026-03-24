import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Bell, HelpCircle, MessageSquare } from 'lucide-react';

export const CustomerCenterSidebar: React.FC = () => {
  const location = useLocation();
  const activePath = location.pathname;

  const menuItems = [
    { path: '/notice', label: '공지사항', icon: Bell },
    { path: '/faq', label: '자주 묻는 질문', icon: HelpCircle },
    { path: '/inquiry', label: '문의하기', icon: MessageSquare },
  ];

  return (
    <div className="w-full md:w-64 flex-shrink-0">
      <nav className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        {menuItems.map((item) => {
          const isActive = activePath === item.path || (item.path === '/notice' && activePath.startsWith('/notice/')) || (item.path === '/inquiry' && activePath.startsWith('/inquiry/'));
          const Icon = item.icon;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${
                isActive
                  ? 'bg-red-50 text-red-900'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              <Icon className="w-5 h-5 mr-3" /> {item.label}
            </Link>
          );
        })}
      </nav>
    </div>
  );
};
