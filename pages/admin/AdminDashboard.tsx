import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Bell, MessageSquare, AlertCircle, TrendingUp, Gavel, DollarSign, BarChart3, Clock } from 'lucide-react';
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer,
  LineChart, Line
} from 'recharts';
import { MOCK_INQUIRIES, MOCK_NOTICES } from '@/services/mockData';
import { useAppContext } from '@/context/AppContext';
import { Category } from '@/types';

const MONTHLY_DATA = [
  { name: '1월', users: 400 },
  { name: '2월', users: 300 },
  { name: '3월', users: 500 },
  { name: '4월', users: 280 },
  { name: '5월', users: 590 },
  { name: '6월', users: 800 },
];

const HOURLY_DATA = [
  { time: '00:00', visitors: 120 },
  { time: '04:00', visitors: 80 },
  { time: '08:00', visitors: 250 },
  { time: '12:00', visitors: 450 },
  { time: '16:00', visitors: 580 },
  { time: '20:00', visitors: 720 },
  { time: '23:59', visitors: 300 },
];

const BID_TRAFFIC_DATA = [
  { time: '10:00', bids: 45 },
  { time: '11:00', bids: 52 },
  { time: '12:00', bids: 89 },
  { time: '13:00', bids: 64 },
  { time: '14:00', bids: 78 },
  { time: '15:00', bids: 95 },
];

export const AdminDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { reports } = useAppContext();

  const unprocessedInquiries = MOCK_INQUIRIES.filter(i => i.status === '답변 대기중').length;
  const unprocessedReports = reports.filter(r => r.status === 'pending').length;

  const stats = [
    { label: '미처리 신고', value: unprocessedReports, icon: AlertCircle, color: 'bg-red-500', path: '/admin/reports' },
    { label: '미처리 문의', value: unprocessedInquiries, icon: MessageSquare, color: 'bg-orange-500', path: '/admin/inquiries' },
  ];

  const popularCategories = [
    { name: Category.DIGITAL, count: 40, color: '#FF5A5A' },
    { name: Category.CLOTHING, count: 25, color: '#4F46E5' },
    { name: Category.FURNITURE, count: 15, color: '#10B981' },
    { name: Category.BOOKS, count: 10, color: '#F59E0B' },
    { name: '기타', count: 10, color: '#9CA3AF' },
  ];

  return (
    <div className="space-y-6 pb-10">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-gray-900 tracking-tight">관리자 대시보드</h1>
          <p className="text-gray-500 mt-1 text-xs font-medium">서비스 현황을 확인하세요.</p>
        </div>
      </header>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <button 
              key={stat.label} 
              onClick={() => navigate(stat.path)}
              className="bg-white p-5 rounded-none shadow-sm border border-gray-200 flex items-center space-x-4 hover:shadow-md transition-all text-left group"
            >
              <div className={`${stat.color} p-3 rounded-none text-white shadow-lg group-hover:scale-110 transition-transform`}>
                <Icon className="w-6 h-6" />
              </div>
              <div>
                <p className="text-[11px] font-bold text-gray-500 mb-0.5">{stat.label}</p>
                <p className="text-xl font-black text-gray-900">{stat.value}</p>
              </div>
            </button>
          );
        })}
      </div>

      <div className="flex flex-col lg:flex-row gap-6">
        {/* Popular Categories */}
        <section className="lg:w-1/3 bg-white p-6 rounded-none shadow-sm border border-gray-200">
          <h2 className="text-lg font-black text-gray-900 mb-6">인기 카테고리</h2>
          <div className="h-[200px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={popularCategories}
                  cx="50%"
                  cy="50%"
                  innerRadius={40}
                  outerRadius={70}
                  paddingAngle={5}
                  dataKey="count"
                >
                  {popularCategories.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ borderRadius: '0px', border: '1px solid #e5e7eb', fontWeight: 'bold', fontSize: '12px' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2 mt-4">
            {popularCategories.map((cat) => (
              <div key={cat.name} className="flex items-center space-x-1.5 whitespace-nowrap">
                <div className="w-2 h-2 shrink-0" style={{ backgroundColor: cat.color }}></div>
                <span className="text-[10px] font-bold text-gray-600">{cat.name} ({cat.count}%)</span>
              </div>
            ))}
          </div>
        </section>

        {/* Recent Inquiries */}
        <section className="lg:flex-1 bg-white p-6 rounded-none shadow-sm border border-gray-200">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-black text-gray-900">최근 문의사항</h2>
            <button 
              onClick={() => navigate('/admin/inquiries')}
              className="text-xs font-bold text-[#FF5A5A] hover:underline"
            >
              전체보기
            </button>
          </div>
          <div className="space-y-3">
            {MOCK_INQUIRIES.slice(0, 5).map((inquiry) => (
              <div key={inquiry.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-none">
                <div className="flex items-center space-x-3 overflow-hidden">
                  <div className={`w-1.5 h-1.5 rounded-none shrink-0 ${inquiry.status === '답변 완료' ? 'bg-green-500' : 'bg-orange-500'}`}></div>
                  <div className="overflow-hidden">
                    <p className="text-xs font-bold text-gray-900 truncate">{inquiry?.title || '제목 없음'}</p>
                    <p className="text-[10px] font-medium text-gray-400">{inquiry?.createdAt?.split('T')[0] || '-'}</p>
                  </div>
                </div>
                <span className={`text-[10px] font-black px-2 py-0.5 rounded-none shrink-0 ${
                  inquiry.status === '답변 완료' ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'
                }`}>
                  {inquiry.status}
                </span>
              </div>
            ))}
          </div>
        </section>

        {/* Recent Notices */}
        <section className="lg:flex-1 bg-white p-6 rounded-none shadow-sm border border-gray-200">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-black text-gray-900">최근 공지사항</h2>
            <button 
              onClick={() => navigate('/admin/notices')}
              className="text-xs font-bold text-[#FF5A5A] hover:underline"
            >
              전체보기
            </button>
          </div>
          <div className="space-y-3">
            {MOCK_NOTICES.slice(0, 5).map((notice) => (
              <div key={notice.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-none">
                <div className="flex items-center space-x-3 overflow-hidden">
                  <div className={`w-1.5 h-1.5 rounded-none shrink-0 ${notice.isImportant ? 'bg-red-500' : 'bg-gray-300'}`}></div>
                  <div className="overflow-hidden">
                    <p className="text-xs font-bold text-gray-900 truncate">{notice.title}</p>
                    <p className="text-[10px] font-medium text-gray-400">{notice.createdAt.split('T')[0]}</p>
                  </div>
                </div>
                <span className={`text-[10px] font-black px-2 py-0.5 rounded-none shrink-0 ${
                  notice.category === '점검' ? 'bg-orange-100 text-orange-700' :
                  notice.category === '업데이트' ? 'bg-blue-100 text-blue-700' :
                  'bg-gray-100 text-gray-700'
                }`}>
                  {notice.category}
                </span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
};
