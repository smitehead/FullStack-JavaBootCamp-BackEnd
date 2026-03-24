import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react';
import { Notification, ChatRoom, User, Product, WithdrawnUser, NotificationType, Report, MannerHistory, ActivityLog, SystemSettings } from '../types';
import { NOTIFICATIONS as INITIAL_NOTIFICATIONS, MOCK_CHATS as INITIAL_CHATS, CURRENT_USER as MOCK_USER, ADMIN_USER, MOCK_PRODUCTS as INITIAL_PRODUCTS, MOCK_USERS as INITIAL_USERS, MOCK_REPORTS as INITIAL_REPORTS } from '../services/mockData';
import api from '../services/api';

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
  systemSettings: SystemSettings;
  login: (userId: string, password: string) => Promise<boolean>;
  logout: () => void;
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
  const [systemSettings, setSystemSettings] = useState<SystemSettings>({
    isMaintenanceMode: false,
    maintenanceMessage: '현재 시스템 점검 중입니다. 잠시 후 다시 시도해 주세요.',
    lastUpdated: new Date().toISOString()
  });
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

  // 앱 시작 시 localStorage에 저장된 로그인 정보 복원
  useEffect(() => {
    const savedUser = localStorage.getItem('java_user');
    if (savedUser) {
      const parsedUser = JSON.parse(savedUser);
      setUser(parsedUser);
      
      // 새로고침 시 백그라운드에서 최신 포인트/정보 DB 연동
      const memberNo = parseInt(parsedUser.id.replace(/[^0-9]/g, ''), 10);
      if (!isNaN(memberNo)) {
        api.get(`/members/${memberNo}`).then(res => {
          if (res.data) {
            setUser(prev => {
              if (!prev) return prev;
              const updated = { ...prev, points: res.data.points || 0, mannerTemp: res.data.mannerTemp || 36.5 };
              localStorage.setItem('java_user', JSON.stringify(updated));
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
      
      localStorage.setItem('java_token', token);
      
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
         profileImage: 'https://via.placeholder.com/200',
         points: dbPoints,
         mannerTemp: dbMannerTemp,
         joinedAt: new Date().toISOString(),
      };
      
      setUser(loggedInUser);
      localStorage.setItem('java_user', JSON.stringify(loggedInUser));
      return true;
    } catch (error) {
      console.error('Login failed', error);
      return false;
    }
  };

  const logout = () => {
    api.post('/auth/logout').catch(console.error);
    setUser(null);
    localStorage.removeItem('java_user');
    localStorage.removeItem('java_token');
  };

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
      localStorage.setItem('java_user', JSON.stringify(updatedUser));
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
      localStorage.setItem('java_user', JSON.stringify(updatedUser));
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

  const addNotification = (message: string, link: string, type: NotificationType = 'system') => {
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
      localStorage.setItem('java_user', JSON.stringify(updated));
    }
  };

  const toggleMaintenanceMode = (enabled: boolean, message?: string) => {
    setSystemSettings(prev => ({
      ...prev,
      isMaintenanceMode: enabled,
      maintenanceMessage: message || prev.maintenanceMessage,
      lastUpdated: new Date().toISOString()
    }));
    addActivityLog('시스템 설정', `점검 모드 ${enabled ? '활성화' : '비활성화'}`, undefined, 'system');
  };

  const sendAdminMessage = (userId: string, content: string) => {
    // 임시 구현 - 실제 연동 시 채팅방 생성/업데이트 API 호출로 교체 필요
    console.log(`Admin message to ${userId}: ${content}`);
    alert(`${userId}님에게 메시지를 보냈습니다: ${content}`);
    addActivityLog('메시지 발송', `${userId}님에게 관리자 메시지 발송`, userId, 'user');
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
      systemSettings,
      login,
      logout,
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
      sendAdminMessage,
      toggleMaintenanceMode,
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
