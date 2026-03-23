import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react';
import { Notification, ChatRoom, User, Product, WithdrawnUser, NotificationType, Report, MannerHistory, ActivityLog, SystemSettings } from '../types';
import { NOTIFICATIONS as INITIAL_NOTIFICATIONS, MOCK_CHATS as INITIAL_CHATS, CURRENT_USER as MOCK_USER, ADMIN_USER, MOCK_PRODUCTS as INITIAL_PRODUCTS, MOCK_USERS as INITIAL_USERS, MOCK_REPORTS as INITIAL_REPORTS } from '../services/mockData';

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
  login: (userId: string, password: string) => boolean;
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

  // Load user from localStorage if exists
  useEffect(() => {
    const savedUser = localStorage.getItem('java_user');
    if (savedUser) {
      const parsedUser = JSON.parse(savedUser);
      setUser(parsedUser);
    }
  }, []);

  const login = (userId: string, password: string) => {
    let foundUser: User | null = null;
    if (userId === 'admin1' && password === 'admin1234') {
      foundUser = ADMIN_USER;
    } else if (userId === 'testuser' && password === 'password123') {
      foundUser = MOCK_USER;
    }

    if (foundUser) {
      setUser(foundUser);
      localStorage.setItem('java_user', JSON.stringify(foundUser));
      return true;
    }
    return false;
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('java_user');
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

    // Update users list
    setUsers(prev => prev.map(u => 
      u.id === userId ? { 
        ...u, 
        isSuspended: true, 
        suspensionEndDate: endDateStr, 
        suspensionReason: reason,
        isPermanentlySuspended: isPermanent 
      } : u
    ));

    // Update current user if it's the one being suspended
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

    // End all ongoing auctions for this user
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
    // Mock sending a message - in a real app, this would create/update a ChatRoom
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
