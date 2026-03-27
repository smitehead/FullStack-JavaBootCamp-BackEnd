import React, { useState, useMemo, useEffect } from 'react';
import { Search, ShieldAlert, Thermometer, Coins, UserX, UserCheck, Shield, User as UserIcon, ArrowUpDown, Filter, AlertCircle } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { useAppContext } from '@/context/AppContext';
import { User, WithdrawnUser } from '@/types';

type UserStatus = '정상' | '정지' | '영구정지' | '탈퇴';
type SortField = 'mannerTemp' | 'points' | 'joinedAt' | 'role' | 'status' | 'postCount';
type SortOrder = 'asc' | 'desc';

export const UserManagement: React.FC = () => {
  const { users, suspendUser, unsuspendUser, updateUserRole, updateUserManner, updateUserPoints, mannerHistory } = useAppContext();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialSearch = searchParams.get('nickname') || '';

  const [searchTerm, setSearchTerm] = useState(initialSearch);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalType, setModalType] = useState<'manner' | 'suspend' | 'points' | null>(null);

  // Filter & Sort States
  const [filterRole, setFilterRole] = useState<'all' | 'admin' | 'user'>('all');
  const [filterStatus, setFilterStatus] = useState<'all' | UserStatus>('all');
  const [sortField, setSortField] = useState<SortField>('joinedAt');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');

  // Modal Input States
  const [mannerValue, setMannerValue] = useState(36.5);
  const [mannerReason, setMannerReason] = useState('');
  const [suspendDays, setSuspendDays] = useState(7);
  const [suspendReason, setSuspendReason] = useState('');
  const [pointAmount, setPointAmount] = useState(0);

  useEffect(() => {
    const nickname = searchParams.get('nickname');
    if (nickname) {
      setSearchTerm(nickname);
    }
  }, [searchParams]);

  // Only active users for this page
  const allUsers = useMemo(() => {
    return users.map(u => ({
      ...u,
      status: (u.isPermanentlySuspended ? '영구정지' : u.isSuspended ? '정지' : '정상') as UserStatus,
      isWithdrawn: false
    }));
  }, [users]);

  const filteredAndSortedUsers = useMemo(() => {
    return allUsers
      .filter(u => {
        const matchesSearch = u.nickname.toLowerCase().includes(searchTerm.toLowerCase()) ||
          u.email?.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesRole = filterRole === 'all' || (filterRole === 'admin' ? u.isAdmin : !u.isAdmin);
        const matchesStatus = filterStatus === 'all' || u.status === filterStatus;
        return matchesSearch && matchesRole && matchesStatus;
      })
      .sort((a, b) => {
        let comparison = 0;
        if (sortField === 'mannerTemp') comparison = a.mannerTemp - b.mannerTemp;
        else if (sortField === 'points') comparison = a.points - b.points;
        else if (sortField === 'joinedAt') comparison = new Date(a.joinedAt).getTime() - new Date(b.joinedAt).getTime();
        else if (sortField === 'role') comparison = (a.isAdmin ? 1 : 0) - (b.isAdmin ? 1 : 0);
        else if (sortField === 'status') comparison = a.status.localeCompare(b.status);
        else if (sortField === 'postCount') comparison = (a.postCount || 0) - (b.postCount || 0);

        return sortOrder === 'asc' ? comparison : -comparison;
      });
  }, [allUsers, searchTerm, filterRole, filterStatus, sortField, sortOrder]);

  const handleOpenModal = (user: User, type: 'manner' | 'suspend' | 'points') => {
    setSelectedUser(user);
    setModalType(type);
    setIsModalOpen(true);
    if (type === 'manner') {
      setMannerValue(user.mannerTemp);
      setMannerReason('');
    }
    if (type === 'points') setPointAmount(0);
    if (type === 'suspend') {
      setSuspendDays(7);
      setSuspendReason('');
    }
  };

  const handleAction = () => {
    if (!selectedUser) return;

    if (modalType === 'suspend') {
      if (!suspendReason.trim()) {
        alert('정지 사유를 입력해주세요.');
        return;
      }
      suspendUser(selectedUser.id, suspendDays, suspendReason);
    } else if (modalType === 'manner') {
      if (!mannerReason.trim()) {
        alert('변경 사유를 입력해주세요.');
        return;
      }
      updateUserManner(selectedUser.id, mannerValue, mannerReason);
    } else if (modalType === 'points') {
      updateUserPoints(selectedUser.id, pointAmount);
    }

    setIsModalOpen(false);
    setSelectedUser(null);
    setModalType(null);
    setPointAmount(0);
    setSuspendReason('');
    setMannerReason('');
  };

  const toggleSuspension = (user: User) => {
    if (user.isSuspended) {
      if (window.confirm('정지를 해제하시겠습니까?')) {
        unsuspendUser(user.id);
      }
    } else {
      handleOpenModal(user, 'suspend');
    }
  };

  const handleRoleChange = (user: User, newIsAdmin: boolean) => {
    if (user.isAdmin === newIsAdmin) {
      alert(`이미 ${newIsAdmin ? '관리자' : '일반'} 권한입니다.`);
      return;
    }

    if (window.confirm(`정말로 ${newIsAdmin ? '관리자' : '일반'} 권한으로 변경하시겠습니까?`)) {
      updateUserRole(user.id, newIsAdmin);
      alert('권한이 변경되었습니다.');
    }
  };

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('desc');
    }
  };

  const handleUserClick = (nickname: string) => {
    setSearchTerm(nickname);
    setSearchParams({ nickname });
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">사용자 관리</h1>
          <div className="flex items-center gap-2 mt-1">
            <p className="text-gray-500 text-[11px] font-medium">회원 정보 조회 및 제재 조치를 관리합니다.</p>
            <span className="w-1 h-1 bg-gray-300 rounded-full"></span>
            <p className="text-[#FF5A5A] text-[11px] font-black">총 {allUsers.length}명</p>
          </div>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-3">
          <div className="relative w-64 flex items-center h-10">
            <div className="absolute left-3 top-0 bottom-0 flex items-center pointer-events-none">
              <Search className="text-gray-400 w-4 h-4" />
            </div>
            <input
              type="text"
              placeholder="닉네임 또는 이메일 검색"
              className="w-full pl-10 pr-4 h-full bg-white border border-gray-200 rounded-none shadow-sm focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] focus:border-transparent font-bold text-xs"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-none px-4 py-2.5 shadow-sm">
            <Filter className="w-3.5 h-3.5 text-gray-400" />
            <select
              className="bg-transparent text-xs font-bold text-gray-600 focus:outline-none cursor-pointer"
              value={filterRole}
              onChange={(e) => setFilterRole(e.target.value as any)}
            >
              <option value="all">모든 권한</option>
              <option value="user">일반</option>
              <option value="admin">관리자</option>
            </select>
            <div className="w-px h-3 bg-gray-200 mx-1"></div>
            <select
              className="bg-transparent text-xs font-bold text-gray-600 focus:outline-none cursor-pointer"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value as any)}
            >
              <option value="all">모든 상태</option>
              <option value="정상">정상</option>
              <option value="정지">정지</option>
              <option value="영구정지">영구정지</option>
              <option value="탈퇴">탈퇴</option>
            </select>
          </div>
        </div>
      </header>

      {/* User Table */}
      <div className="bg-white rounded-none shadow-sm border border-gray-100 overflow-hidden">
        <table className="w-full text-left border-collapse table-fixed">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-100">
              <th className="w-[20%] px-4 py-3 text-[11px] font-black text-gray-400 uppercase tracking-wider">사용자</th>
              <th
                className="w-[10%] px-4 py-3 text-[11px] font-black text-gray-400 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                onClick={() => toggleSort('role')}
              >
                <div className="flex items-center gap-1">
                  권한
                  <ArrowUpDown className={`w-2.5 h-2.5 ${sortField === 'role' ? 'text-[#FF5A5A]' : 'text-gray-300'}`} />
                </div>
              </th>
              <th
                className="w-[10%] px-4 py-3 text-[11px] font-black text-gray-400 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                onClick={() => toggleSort('mannerTemp')}
              >
                <div className="flex items-center gap-1">
                  매너온도
                  <ArrowUpDown className={`w-2.5 h-2.5 ${sortField === 'mannerTemp' ? 'text-[#FF5A5A]' : 'text-gray-300'}`} />
                </div>
              </th>
              <th
                className="w-[10%] px-4 py-3 text-[11px] font-black text-gray-400 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                onClick={() => toggleSort('points')}
              >
                <div className="flex items-center gap-1">
                  포인트
                  <ArrowUpDown className={`w-2.5 h-2.5 ${sortField === 'points' ? 'text-[#FF5A5A]' : 'text-gray-300'}`} />
                </div>
              </th>
              <th
                className="w-[10%] px-4 py-3 text-[11px] font-black text-gray-400 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                onClick={() => toggleSort('postCount')}
              >
                <div className="flex items-center gap-1">
                  경매 수
                  <ArrowUpDown className={`w-2.5 h-2.5 ${sortField === 'postCount' ? 'text-[#FF5A5A]' : 'text-gray-300'}`} />
                </div>
              </th>
              <th
                className="w-[10%] px-4 py-3 text-[11px] font-black text-gray-400 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                onClick={() => toggleSort('status')}
              >
                <div className="flex items-center gap-1">
                  상태
                  <ArrowUpDown className={`w-2.5 h-2.5 ${sortField === 'status' ? 'text-[#FF5A5A]' : 'text-gray-300'}`} />
                </div>
              </th>
              <th
                className="w-[10%] px-4 py-3 text-[11px] font-black text-gray-400 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                onClick={() => toggleSort('joinedAt')}
              >
                <div className="flex items-center gap-1">
                  가입일
                  <ArrowUpDown className={`w-2.5 h-2.5 ${sortField === 'joinedAt' ? 'text-[#FF5A5A]' : 'text-gray-300'}`} />
                </div>
              </th>
              <th className="w-[14%] px-4 py-3 text-[11px] font-black text-gray-400 uppercase tracking-wider text-right">관리</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {filteredAndSortedUsers.map((user) => (
              <tr key={user.id} className={`hover:bg-gray-50 transition-colors group ${user.isWithdrawn ? 'opacity-60 bg-gray-50/30' : ''}`}>
                <td className="px-4 py-3">
                  <div className="flex items-center space-x-3">
                    <img src={user.profileImage || undefined} alt={user.nickname} className="w-8 h-8 rounded-none object-cover shadow-sm" />
                    <div className="min-w-0">
                      <button
                        onClick={() => handleUserClick(user.nickname)}
                        className="font-bold text-gray-900 text-sm truncate hover:text-[#FF5A5A] transition-colors"
                      >
                        {user.nickname}
                      </button>
                      <p className="text-[10px] font-medium text-gray-400 truncate">{user.email || '이메일 없음'}</p>
                    </div>
                  </div>
                </td>
                <td className="px-4 py-3">
                  {!user.isWithdrawn ? (
                    <div className="relative inline-block text-left group/role">
                      <button className={`inline-flex items-center px-2 py-1 rounded-none text-[10px] font-black transition-colors ${user.isAdmin ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'
                        }`}>
                        {user.isAdmin ? <Shield className="w-2.5 h-2.5 mr-1" /> : <UserIcon className="w-2.5 h-2.5 mr-1" />}
                        {user.isAdmin ? '관리자' : '일반'}
                      </button>
                      <div className="absolute left-0 mt-1 w-24 bg-white border border-gray-100 rounded-none shadow-xl py-1 z-10 opacity-0 invisible group-hover/role:opacity-100 group-hover/role:visible transition-all">
                        <button
                          onClick={() => handleRoleChange(user as any, false)}
                          className="w-full text-left px-3 py-1.5 text-[10px] font-bold text-gray-600 hover:bg-gray-50 hover:text-blue-600"
                        >
                          일반
                        </button>
                        <button
                          onClick={() => handleRoleChange(user as any, true)}
                          className="w-full text-left px-3 py-1.5 text-[10px] font-bold text-gray-600 hover:bg-gray-50 hover:text-purple-600"
                        >
                          관리자
                        </button>
                      </div>
                    </div>
                  ) : (
                    <span className="text-[10px] font-bold text-gray-400">-</span>
                  )}
                </td>
                <td className="px-4 py-3">
                  {!user.isWithdrawn ? (
                    <div className="flex items-center space-x-1">
                      <Thermometer className={`w-3 h-3 ${user.mannerTemp >= 36.5 ? 'text-orange-500' : 'text-blue-500'}`} />
                      <span className="font-bold text-gray-900 text-xs">{user.mannerTemp}°C</span>
                    </div>
                  ) : (
                    <span className="text-xs text-gray-400">-</span>
                  )}
                </td>
                <td className="px-4 py-3">
                  {!user.isWithdrawn ? (
                    <span className="font-bold text-gray-900 text-xs">{user.points.toLocaleString()}P</span>
                  ) : (
                    <span className="text-xs text-gray-400">-</span>
                  )}
                </td>
                <td className="px-4 py-3">
                  <span className="font-bold text-gray-900 text-xs">{user.postCount || 0}</span>
                </td>
                <td className="px-4 py-3">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-none text-[10px] font-black ${user.status === '영구정지' ? 'bg-black text-white' :
                      user.status === '정지' ? 'bg-red-100 text-red-700' :
                        'bg-green-100 text-green-700'
                    }`}>
                    {user.status}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="text-[11px] font-medium text-gray-400">{user.joinedAt.split('T')[0]}</span>
                </td>
                <td className="px-4 py-3 text-right">
                  {!user.isWithdrawn && (
                    <div className="flex items-center justify-end space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => handleOpenModal(user as any, 'manner')}
                        className="p-1.5 hover:bg-orange-100 text-orange-600 rounded-none transition-colors" title="온도 조절"
                      >
                        <Thermometer className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleOpenModal(user as any, 'points')}
                        className="p-1.5 hover:bg-blue-100 text-blue-600 rounded-none transition-colors" title="포인트 지급"
                      >
                        <Coins className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => user.isSuspended ? toggleSuspension(user as any) : handleOpenModal(user as any, 'suspend')}
                        className={`p-1.5 rounded-none transition-colors ${user.isSuspended ? 'hover:bg-green-100 text-green-600' : 'hover:bg-red-100 text-red-600'
                          }`}
                        title={user.isSuspended ? "정지 해제" : "계정 정지"}
                      >
                        {user.isSuspended ? <UserCheck className="w-4 h-4" /> : <UserX className="w-4 h-4" />}
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isModalOpen && selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/60 backdrop-blur-sm">
          <div className="bg-white w-full max-sm rounded-none p-8 shadow-2xl animate-in fade-in zoom-in duration-300">
            <h2 className="text-2xl font-black text-gray-900 mb-1 tracking-tight">
              {modalType === 'manner' && '매너온도 조절'}
              {modalType === 'suspend' && '계정 정지 처리'}
              {modalType === 'points' && '포인트 증감/차감'}
            </h2>
            <p className="text-gray-500 text-sm font-medium mb-6">
              <span className="text-[#FF5A5A] font-black">{selectedUser.nickname}</span> 회원님 처리
            </p>

            <div className="space-y-4 mb-8">
              {modalType === 'manner' && (
                <div className="space-y-6">
                  <div className="space-y-4">
                    <div>
                      <label className="block text-xs font-black text-gray-700 mb-2">새 매너온도 (°C)</label>
                      <input
                        type="number"
                        step="0.1"
                        className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-black text-lg"
                        value={mannerValue}
                        onChange={(e) => setMannerValue(parseFloat(e.target.value))}
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-black text-gray-700 mb-2">변경 사유</label>
                      <textarea
                        className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm resize-none h-24"
                        placeholder="온도 조절 사유를 입력하세요"
                        value={mannerReason}
                        onChange={(e) => setMannerReason(e.target.value)}
                      />
                    </div>
                  </div>

                  {/* Manner History Section */}
                  <div className="pt-6 border-t border-gray-100">
                    <h3 className="text-xs font-black text-gray-900 mb-3 flex items-center gap-2">
                      <Thermometer className="w-3 h-3 text-orange-500" />
                      매너온도 히스토리
                    </h3>
                    <div className="max-h-48 overflow-y-auto pr-2 space-y-2 custom-scrollbar">
                      {mannerHistory.filter(h => h.userId === selectedUser.id).length > 0 ? (
                        mannerHistory
                          .filter(h => h.userId === selectedUser.id)
                          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                          .map((history) => (
                            <div key={history.id} className="p-3 bg-gray-50 border border-gray-100 rounded-none space-y-1">
                              <div className="flex items-center justify-between">
                                <span className="text-[10px] font-black text-gray-400">{history.createdAt.split('T')[0]}</span>
                                <div className="flex items-center gap-1">
                                  <span className="text-[10px] font-bold text-gray-500">{history.previousTemp}°C</span>
                                  <span className="text-[10px] text-gray-300">→</span>
                                  <span className="text-[10px] font-black text-[#FF5A5A]">{history.newTemp}°C</span>
                                </div>
                              </div>
                              <p className="text-[11px] font-medium text-gray-600 leading-relaxed">{history.reason}</p>
                            </div>
                          ))
                      ) : (
                        <div className="py-8 text-center bg-gray-50 border border-dashed border-gray-200">
                          <p className="text-[10px] font-bold text-gray-400">변경 이력이 없습니다.</p>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
              {modalType === 'suspend' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-black text-gray-700 mb-2">정지 기간 (일)</label>
                    <select
                      className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-black text-lg appearance-none"
                      value={suspendDays}
                      onChange={(e) => setSuspendDays(parseInt(e.target.value))}
                    >
                      <option value={3}>3일</option>
                      <option value={7}>7일</option>
                      <option value={15}>15일</option>
                      <option value={30}>30일</option>
                      <option value={999}>영구 정지</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-black text-gray-700 mb-2">정지 사유</label>
                    <textarea
                      className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm resize-none h-24"
                      placeholder="정지 사유를 입력하세요"
                      value={suspendReason}
                      onChange={(e) => setSuspendReason(e.target.value)}
                    />
                  </div>
                </div>
              )}
              {modalType === 'points' && (
                <div>
                  <label className="block text-xs font-black text-gray-700 mb-2">증감/차감 금액 (P)</label>
                  <input
                    type="number"
                    placeholder="예: 10000 또는 -5000"
                    className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-black text-lg"
                    value={pointAmount}
                    onChange={(e) => setPointAmount(parseInt(e.target.value))}
                  />
                </div>
              )}
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setIsModalOpen(false)}
                className="flex-1 py-3 rounded-none font-black text-gray-500 bg-gray-100 hover:bg-gray-200 transition-all"
              >
                취소
              </button>
              <button
                onClick={handleAction}
                className="flex-1 py-3 rounded-none font-black text-white bg-[#FF5A5A] hover:bg-[#E04848] transition-all shadow-lg shadow-red-500/20"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
