import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react';
import { Notification, ChatRoom, User, Product, WithdrawnUser, NotificationType, Report, MannerHistory, ActivityLog } from '@/types';
import { NOTIFICATIONS as INITIAL_NOTIFICATIONS, MOCK_CHATS as INITIAL_CHATS, CURRENT_USER as MOCK_USER, ADMIN_USER, MOCK_PRODUCTS as INITIAL_PRODUCTS, MOCK_USERS as INITIAL_USERS, MOCK_REPORTS as INITIAL_REPORTS } from '@/services/mockData';
import api from '@/services/api';
import { BACKEND_URL } from '@/utils/imageUtils';
import { getMemberNo } from '@/utils/memberUtils';

interface AppContextType {
  user: User | null;
  users: User[];
  products: Product[];
  notifications: Notification[];
  chats: ChatRoom[];
  withdrawnUsers: WithdrawnUser[];
  reports: Report[];
  mannerHistory: MannerHistory[];
  activityLogs: ActivityLog[];
  login: (userId: string, password: string) => Promise<boolean>;
  logout: () => void;
  forceLogoutModalOpen: boolean;
  closeForceLogoutModal: () => void;
  suspendUser: (userId: string, days: number, reason: string) => void;
  unsuspendUser: (userId: string) => void;
  addProduct: (newProduct: Product) => void;
  cancelAuction: (productId: string, reason: string) => void;
  resolveReport: (reportId: string, action: string) => void;
  markNotificationAsRead: (id: string) => void;
  markChatAsRead: (id: string) => void;
  addNotification: (message: string, link: string, type?: NotificationType) => void;
  updateUserRole: (userId: string, isAdmin: boolean) => void;
  updateUserManner: (userId: string, mannerTemp: number, reason: string) => void;
  updateUserPoints: (userId: string, points: number) => void;
  updateCurrentUserPoints: (points: number) => void;
  sendAdminMessage: (userId: string, content: string) => void;
  toggleMaintenanceMode: (enabled: boolean, message?: string) => void;
  addActivityLog: (action: string, details: string, targetId?: string, targetType?: ActivityLog['targetType']) => void;
  unreadNotificationsCount: number;
  unreadChatsCount: number;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [users, setUsers] = useState<User[]>(INITIAL_USERS);
  const [products, setProducts] = useState<Product[]>(INITIAL_PRODUCTS);
  const [notifications, setNotifications] = useState<Notification[]>(INITIAL_NOTIFICATIONS);
  const [chats, setChats] = useState<ChatRoom[]>(INITIAL_CHATS);
  const [reports, setReports] = useState<Report[]>(INITIAL_REPORTS);
  const [mannerHistory, setMannerHistory] = useState<MannerHistory[]>([]);
  const [activityLogs, setActivityLogs] = useState<ActivityLog[]>([]);
  const [forceLogoutModalOpen, setForceLogoutModalOpen] = useState(false);
  const [withdrawnUsers, setWithdrawnUsers] = useState<WithdrawnUser[]>([
    {
      id: 'w1',
      nickname: '탈퇴회원1',
      email: 'user1@example.com',
      reason: '서비스 불만족',
      details: 'UI가 너무 복잡해요.',
      withdrawnAt: '2024-03-20T10:00:00Z'
    },
    {
      id: 'w2',
      nickname: '탈퇴왕',
      email: 'king@example.com',
      reason: '타 서비스 이용',
      details: '다른 경매 앱이 더 편해요.',
      withdrawnAt: '2024-03-21T15:30:00Z'
    }
  ]);

  // 전역 SSE 구독 - 로그인한 사용자의 포인트/알림 실시간 업데이트
  useEffect(() => {
    if (!user) return;

    const memberNo = getMemberNo(user);
    if (!memberNo) return;

    // BACKEND_URL을 사용하여 하드코딩 제거 (imageUtils.ts에서 환경변수 기반 관리)
    const eventSource = new EventSource(`${BACKEND_URL}/api/sse/subscribe?clientId=${String(memberNo)}`);

    eventSource.addEventListener('pointUpdate', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        if (data && typeof data.points === 'number') {
          setUser(prev => {
            if (!prev) return prev;
            // 포인트 값이 실제로 변경된 경우에만 업데이트
            if (prev.points === data.points) return prev;
            const updated = { ...prev, points: data.points };
            // sessionStorage도 동기화하여 새로고침 후에도 유지
            sessionStorage.setItem('java_user', JSON.stringify(updated));
            return updated;
          });
        }
      } catch (e) {
        console.error('[SSE] pointUpdate 파싱 오류', e);
      }
    });

    // 다른 기기에서 로그인 시 즉시 강제 로그아웃 처리
    eventSource.addEventListener('forceLogout', () => {
      eventSource.close();
      sessionStorage.removeItem('java_token');
      sessionStorage.removeItem('java_user');
      setUser(null);
      setForceLogoutModalOpen(true);
    });

    eventSource.onerror = (err) => {
      console.error(`[SSE] 연결 오류 (memberNo: ${memberNo})`, err);
      // close() 미호출 - EventSource 스펙상 오류 시 자동 재연결됨
    };

    return () => {
      eventSource.close();
    };
  }, [user?.id]); // 사용자 ID가 바뀔 때(로그인/로그아웃) 재연결

  // 앱 시작 시 sessionStorage에 저장된 로그인 정보 복원
  useEffect(() => {
    const savedUser = sessionStorage.getItem('java_user');
    if (savedUser) {
      const parsedUser = JSON.parse(savedUser);
      setUser(parsedUser);

      const memberNo = parseInt(parsedUser.id.replace(/[^0-9]/g, ''), 10);
      if (!isNaN(memberNo)) {
        api.get(`/members/${memberNo}`).then(res => {
          if (res.data) {
            setUser(prev => {
              if (!prev) return prev;
              const updated = { ...prev, points: res.data.points || 0, mannerTemp: res.data.mannerTemp || 36.5 };
              sessionStorage.setItem('java_user', JSON.stringify(updated));
              return updated;
            });
          }
        }).catch(err => console.error("Failed to sync user data in AppContext", err));
      }
    }
  }, []);

  const login = async (userId: string, password: string): Promise<boolean> => {
    try {
      const response = await api.post('/auth/login', { userId, password });
      const { token, memberNo, nickname } = response.data;

      sessionStorage.setItem('java_token', token);

      // FETCH REAL POINTS HERE
      let dbPoints = 0;
      let dbMannerTemp = 36.5;
      try {
        const memberRes = await api.get(`/members/${memberNo}`);
        dbPoints = memberRes.data.points || 0;
        dbMannerTemp = memberRes.data.mannerTemp || 36.5;
      } catch (err) {
        console.error("Failed to fetch user points during login", err);
      }

      const loggedInUser: User = {
        id: `user_${memberNo}`,
        nickname: nickname || userId,
        profileImage: '',
        points: dbPoints,
        mannerTemp: dbMannerTemp,
        joinedAt: new Date().toISOString(),
      };

      setUser(loggedInUser);
      sessionStorage.setItem('java_user', JSON.stringify(loggedInUser));
      return true;
    } catch (error) {
      console.error('Login failed', error);
      return false;
    }
  };

  const logout = () => {
    api.post('/auth/logout').catch(console.error);
    setUser(null);
    sessionStorage.removeItem('java_user');
    sessionStorage.removeItem('java_token');
  };

  const closeForceLogoutModal = () => setForceLogoutModalOpen(false);

  // api.ts의 401 인터셉터에서 발생시키는 커스텀 이벤트 처리 (SSE 미연결 상태 백업)
  useEffect(() => {
    const handleForceLogout = () => {
      sessionStorage.removeItem('java_token');
      sessionStorage.removeItem('java_user');
      setUser(null);
      setForceLogoutModalOpen(true);
    };
    window.addEventListener('forceLogout', handleForceLogout);
    return () => window.removeEventListener('forceLogout', handleForceLogout);
  }, []);

  const addActivityLog = (action: string, details: string, targetId?: string, targetType?: ActivityLog['targetType']) => {
    if (!user?.isAdmin) return;
    const newLog: ActivityLog = {
      id: `log_${Date.now()}`,
      adminId: user.id,
      adminNickname: user.nickname,
      action,
      details,
      targetId,
      targetType,
      createdAt: new Date().toISOString()
    };
    setActivityLogs(prev => [newLog, ...prev]);
  };

  const suspendUser = (userId: string, days: number, reason: string) => {
    const isPermanent = days === 999;
    let endDateStr: string | undefined = undefined;

    if (!isPermanent) {
      const endDate = new Date();
      endDate.setDate(endDate.getDate() + days);
      endDateStr = endDate.toISOString();
    }

    // 전체 유저 목록에 정지 상태 반영
    setUsers(prev => prev.map(u =>
      u.id === userId ? {
        ...u,
        isSuspended: true,
        suspensionEndDate: endDateStr,
        suspensionReason: reason,
        isPermanentlySuspended: isPermanent
      } : u
    ));

    // 현재 로그인한 유저가 정지 대상이면 본인 상태도 갱신
    if (user?.id === userId) {
      const updatedUser = {
        ...user,
        isSuspended: true,
        suspensionEndDate: endDateStr,
        suspensionReason: reason,
        isPermanentlySuspended: isPermanent
      };
      setUser(updatedUser);
      sessionStorage.setItem('java_user', JSON.stringify(updatedUser));
    }

    // 해당 유저의 진행 중인 경매 강제 종료
    setProducts(prev => prev.map(p =>
      p.seller.id === userId && p.status === 'active' ? { ...p, status: 'canceled' as const } : p
    ));

    const suspendedUser = users.find(u => u.id === userId);
    addActivityLog('사용자 정지', `${suspendedUser?.nickname}님 정지 처리 (${isPermanent ? '영구' : days + '일'}): ${reason}`, userId, 'user');
  };

  const unsuspendUser = (userId: string) => {
    setUsers(prev => prev.map(u =>
      u.id === userId ? {
        ...u,
        isSuspended: false,
        suspensionEndDate: undefined,
        suspensionReason: undefined,
        isPermanentlySuspended: false
      } : u
    ));

    if (user?.id === userId) {
      const updatedUser = {
        ...user,
        isSuspended: false,
        suspensionEndDate: undefined,
        suspensionReason: undefined,
        isPermanentlySuspended: false
      };
      setUser(updatedUser);
      sessionStorage.setItem('java_user', JSON.stringify(updatedUser));
    }

    const unsuspendedUser = users.find(u => u.id === userId);
    addActivityLog('사용자 정지 해제', `${unsuspendedUser?.nickname}님 정지 해제`, userId, 'user');
  };

  const addProduct = (newProduct: Product) => {
    setProducts(prev => [newProduct, ...prev]);
  };

  const cancelAuction = (productId: string, reason: string) => {
    setProducts(prev => prev.map(p =>
      p.id === productId ? { ...p, status: 'canceled' as const } : p
    ));
    const product = products.find(p => p.id === productId);
    addActivityLog('경매 강제 종료', `경매 종료: ${product?.title} (${reason})`, productId, 'product');
  };

  const resolveReport = (reportId: string, action: string) => {
    setReports(prev => prev.map(r =>
      r.id === reportId ? { ...r, status: 'resolved' as const } : r
    ));
    addActivityLog('신고 처리', `신고 처리: ${reportId} (${action})`, reportId, 'report');
  };

  const markNotificationAsRead = (id: string) => {
    setNotifications(prev =>
      prev.map(noti => noti.id === id ? { ...noti, read: true } : noti)
    );
  };

  const markChatAsRead = (id: string) => {
    setChats(prev =>
      prev.map(chat => chat.id === id ? { ...chat, unreadCount: 0 } : chat)
    );
  };

  const addNotification = (message: string, link: string, type: NotificationType = 'activity') => {
    const newNoti: Notification = {
      id: `n_${Date.now()}`,
      message,
      link,
      type,
      read: false,
      createdAt: new Date().toISOString()
    };
    setNotifications(prev => [newNoti, ...prev]);
  };

  const updateUserRole = (userId: string, isAdmin: boolean) => {
    setUsers(prev => prev.map(u => u.id === userId ? { ...u, isAdmin } : u));
    const targetUser = users.find(u => u.id === userId);
    addActivityLog('권한 변경', `${targetUser?.nickname}님 권한 변경: ${isAdmin ? '관리자' : '일반'}`, userId, 'user');
  };

  const updateUserManner = (userId: string, mannerTemp: number, reason: string) => {
    const targetUser = users.find(u => u.id === userId);
    if (!targetUser) return;

    const newHistory: MannerHistory = {
      id: `mh_${Date.now()}`,
      userId,
      userNickname: targetUser.nickname,
      previousTemp: targetUser.mannerTemp,
      newTemp: mannerTemp,
      reason,
      adminId: user?.id || 'admin',
      createdAt: new Date().toISOString()
    };

    setMannerHistory(prev => [newHistory, ...prev]);
    setUsers(prev => prev.map(u => u.id === userId ? { ...u, mannerTemp } : u));
    addActivityLog('매너온도 변경', `${targetUser.nickname}님 매너온도 변경: ${targetUser.mannerTemp} -> ${mannerTemp}`, userId, 'user');
  };

  const updateUserPoints = (userId: string, points: number) => {
    setUsers(prev => prev.map(u => u.id === userId ? { ...u, points: u.points + points } : u));
    const targetUser = users.find(u => u.id === userId);
    addActivityLog('포인트 변경', `${targetUser?.nickname}님 포인트 변경: ${points > 0 ? '+' : ''}${points}P`, userId, 'user');
  };

  const updateCurrentUserPoints = (points: number) => {
    if (user) {
      const updated = { ...user, points };
      setUser(updated);
      sessionStorage.setItem('java_user', JSON.stringify(updated));
    }
  };

  const unreadNotificationsCount = notifications.filter(n => !n.read).length;
  const unreadChatsCount = chats.filter(c => c.unreadCount > 0).length;

  return (
    <AppContext.Provider value={{
      user,
      users,
      products,
      notifications,
      chats,
      withdrawnUsers,
      reports,
      mannerHistory,
      activityLogs,
      login,
      logout,
      forceLogoutModalOpen,
      closeForceLogoutModal,
      suspendUser,
      unsuspendUser,
      addProduct,
      cancelAuction,
      resolveReport,
      markNotificationAsRead,
      markChatAsRead,
      addNotification,
      updateUserRole,
      updateUserManner,
      updateUserPoints,
      updateCurrentUserPoints,
      addActivityLog,
      unreadNotificationsCount,
      unreadChatsCount
    }}>
      {children}
    </AppContext.Provider>
  );
};

export const useAppContext = () => {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error('useAppContext must be used within an AppProvider');
  }
  return context;
};
