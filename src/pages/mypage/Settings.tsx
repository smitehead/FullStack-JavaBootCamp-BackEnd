import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { CURRENT_USER, BLOCKED_USERS, MOCK_PRODUCTS } from '@/services/mockData';
import { Bell, Shield, ShieldCheck, LogOut, UserMinus, ChevronRight, X, AlertCircle, CheckCircle2, User, Phone, Mail, MapPin, CreditCard, Settings as SettingsIcon, ArrowLeft, GripVertical } from 'lucide-react';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
  TouchSensor
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
  useSortable
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { showToast } from '@/components/toastService';

// Sortable Card Component
interface SortableCardProps {
  card: {
    id: string;
    bank: string;
    account: string;
    isDefault: boolean;
  };
  onDelete: (id: string) => void;
}

const SortableCard: React.FC<SortableCardProps> = ({ card, onDelete }) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging
  } = useSortable({ id: card.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isDragging ? 10 : 1,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="group relative flex items-center justify-between p-6 bg-gray-50 rounded-2xl border border-gray-100 hover:border-gray-200 transition-all"
    >
      <div className="flex items-center gap-4">
        <div {...attributes} {...listeners} className="cursor-grab active:cursor-grabbing p-1 hover:bg-white rounded-lg transition-colors">
          <GripVertical className="w-5 h-5 text-gray-300" />
        </div>
        <div className="w-12 h-12 bg-white rounded-xl flex items-center justify-center shadow-sm">
          <CreditCard className="w-6 h-6 text-gray-400" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <p className="font-bold text-gray-900">{card.bank}</p>
            {card.isDefault && (
              <span className="px-2 py-0.5 bg-indigo-50 text-indigo-600 text-[10px] font-bold rounded-md">기본</span>
            )}
          </div>
          <p className="text-sm text-gray-400 font-medium">{card.account}</p>
        </div>
      </div>

      <button
        onClick={() => onDelete(card.id)}
        className="opacity-0 group-hover:opacity-100 p-2 text-gray-300 hover:text-red-400 transition-all"
        title="삭제"
      >
        <X className="w-5 h-5" />
      </button>
    </div>
  );
};

export const Settings: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const initialTab = queryParams.get('tab') as 'notification' | 'block' | 'profile' | 'card' | null;

  const [activeTab, setActiveTab] = useState<'notification' | 'block' | 'profile' | 'card'>(initialTab || 'notification');

  useEffect(() => {
    if (initialTab) {
      setActiveTab(initialTab);
    }
  }, [initialTab]);
  const [settings, setSettings] = useState(CURRENT_USER.settings || {
    auctionEnd: true,
    newBid: true,
    marketing: false,
    chat: true
  });
  const [blockedUsers, setBlockedUsers] = useState(BLOCKED_USERS);
  const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);
  const [isProfileSaveModalOpen, setIsProfileSaveModalOpen] = useState(false);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [isEmailModalOpen, setIsEmailModalOpen] = useState(false);
  const [isCardDeleteModalOpen, setIsCardDeleteModalOpen] = useState(false);
  const [cardToDelete, setCardToDelete] = useState<string | null>(null);
  const [withdrawStep, setWithdrawStep] = useState<'confirm' | 'reason' | 'input' | 'success'>('confirm');
  const [withdrawReason, setWithdrawReason] = useState('');
  const [withdrawReasonDetail, setWithdrawReasonDetail] = useState('');
  const [emailStep, setEmailStep] = useState<'input' | 'verify' | 'success'>('input');
  const [passwordStep, setPasswordStep] = useState<'verify' | 'input' | 'success'>('verify');
  const [withdrawInput, setWithdrawInput] = useState('');
  const [withdrawError, setWithdrawError] = useState<string | null>(null);

  // Profile Edit state
  const [profileData, setProfileData] = useState({
    nickname: CURRENT_USER.nickname,
    email: CURRENT_USER.email,
    phoneNum: CURRENT_USER.phoneNum || '010-1234-5678',
    address: CURRENT_USER.address || '서울시 강남구 테헤란로 123',
    addrDetail: '4층 402호',
    paymentCard: '신한카드 (****-****-****-1234)'
  });

  const [newEmail, setNewEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [isCodeSent, setIsCodeSent] = useState(false);
  const [timer, setTimer] = useState(0);

  const [passwordData, setPasswordData] = useState({
    current: '',
    new: '',
    confirm: ''
  });

  const [cards, setCards] = useState([
    { id: 'card_1', bank: '신한은행', account: '110-123-456789', isDefault: true },
    { id: 'card_2', bank: '국민은행', account: '432102-01-987654', isDefault: false },
  ] as { id: string; bank: string; account: string; isDefault: boolean }[]);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
    useSensor(TouchSensor)
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      setCards((items) => {
        const oldIndex = items.findIndex((item) => item.id === active.id);
        const newIndex = items.findIndex((item) => item.id === over.id);
        const newItems = arrayMove(items, oldIndex, newIndex);

        // If the item moved to the top, it becomes the default
        return newItems.map((item: any, index: number) => ({
          ...item,
          isDefault: index === 0
        }));
      });
    }
  };

  const [isProfileSaving, setIsProfileSaving] = useState(false);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (timer > 0) {
      interval = setInterval(() => {
        setTimer(prev => prev - 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [timer]);

  const handleProfileSave = () => {
    setIsProfileSaving(true);
    setTimeout(() => {
      setIsProfileSaving(false);
      setIsProfileSaveModalOpen(true);
    }, 1000);
  };

  const handlePasswordChange = () => {
    if (passwordStep === 'verify') {
      if (passwordData.current === '1234') {
        setPasswordStep('input');
      } else {
        showToast('현재 비밀번호가 일치하지 않습니다.');
      }
      return;
    }

    if (passwordData.new !== passwordData.confirm) {
      showToast('새 비밀번호가 일치하지 않습니다.');
      return;
    }
    if (!passwordData.new) {
      showToast('새 비밀번호를 입력해주세요.');
      return;
    }
    // Mock password change
    setPasswordStep('success');
    setTimeout(() => {
      setIsPasswordModalOpen(false);
      setPasswordStep('verify');
      setPasswordData({ current: '', new: '', confirm: '' });
      setVerificationCode('');
      setIsCodeSent(false);
    }, 2000);
  };

  const sendVerificationCode = (type: 'email' | 'password') => {
    setIsCodeSent(true);
    setTimer(180); // 3 minutes
    showToast('인증번호가 발송되었습니다. (테스트용: 123456)');
  };

  const verifyCode = (type: 'email' | 'password') => {
    if (verificationCode === '123456') {
      if (type === 'email') {
        setEmailStep('success');
        setProfileData({ ...profileData, email: newEmail });
        setTimeout(() => {
          setIsEmailModalOpen(false);
          setEmailStep('input');
          setNewEmail('');
          setVerificationCode('');
          setIsCodeSent(false);
        }, 2000);
      } else {
        setPasswordStep('input');
        setVerificationCode('');
        setIsCodeSent(false);
      }
    } else {
      showToast('인증번호가 올바르지 않습니다.');
    }
  };

  const addCard = () => {
    const newCard = {
      id: `card_${Date.now()}`,
      bank: '새로운 은행',
      account: '000-000-000000',
      isDefault: false
    };
    setCards([...cards, newCard]);
  };

  const deleteCard = (id: string) => {
    setCardToDelete(id);
    setIsCardDeleteModalOpen(true);
  };

  const confirmDeleteCard = () => {
    if (cardToDelete) {
      const newCards = cards.filter(c => c.id !== cardToDelete);
      // If we deleted the default card, set the first remaining one as default
      if (newCards.length > 0 && cards.find(c => c.id === cardToDelete)?.isDefault) {
        newCards[0].isDefault = true;
      }
      setCards(newCards);
      setIsCardDeleteModalOpen(false);
      setCardToDelete(null);
    }
  };

  const toggleSetting = (key: keyof typeof settings) => {
    setSettings(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const unblockUser = (userId: string) => {
    // For unblock, we can just do it or add another modal, but for now let's just do it to avoid too many modals
    setBlockedUsers(prev => prev.filter(u => u.id !== userId));
  };

  const handleLogout = () => {
    setIsLogoutModalOpen(true);
  };

  const confirmLogout = () => {
    setIsLogoutModalOpen(false);
    navigate('/login');
  };

  const validateWithdrawal = () => {
    // (1) Points must be 0
    if (CURRENT_USER.points > 0) {
      return '보유 중인 포인트가 0원이어야 탈퇴할 수 있습니다.';
    }

    // (2) No items currently being sold
    const activeProducts = MOCK_PRODUCTS.filter(p => p.seller.id === CURRENT_USER.id && p.status === 'active');
    if (activeProducts.length > 0) {
      return '진행 중인 경매 물품이 있어 탈퇴할 수 없습니다.';
    }

    // (3) No items currently being traded (buying/selling in progress)
    // Trading means status is 'paid' or 'shipped' but not 'completed' or 'canceled'
    const tradingProducts = MOCK_PRODUCTS.filter(p => {
      const isSeller = p.seller.id === CURRENT_USER.id;
      const isBuyer = p.bids.some(b => b.bidderName === CURRENT_USER.nickname && p.winnerId === CURRENT_USER.id);
      const isTrading = ['paid', 'shipped'].includes(p.status);
      return (isSeller || isBuyer) && isTrading;
    });

    if (tradingProducts.length > 0) {
      return '거래 중인 물품이 있어 탈퇴할 수 없습니다.';
    }

    return null;
  };

  const openWithdrawModal = () => {
    setIsWithdrawModalOpen(true);
    setWithdrawStep('confirm');
  };

  const handleWithdraw = () => {
    if (withdrawInput !== '탈퇴하겠습니다') {
      setWithdrawError('문구를 정확히 입력해주세요.');
      return;
    }
    // In a real app, update DB (is_active = false)
    setWithdrawStep('success');
    setTimeout(() => {
      navigate('/');
    }, 3000);
  };

  return (
    <div className="max-w-5xl mx-auto px-6 py-8">
      <div className="flex items-center gap-4 mb-6">
        <h2 className="text-xl font-black text-gray-900 tracking-normal">계정 설정</h2>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Navigation/Categories */}
        <div className="lg:col-span-1 space-y-4">
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-50 bg-gray-50/50">
              <p className="text-xs font-bold text-gray-400 uppercase tracking-wider">계정 관리</p>
            </div>
            <div className="">
              <button
                onClick={() => setActiveTab('notification')}
                className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${activeTab === 'notification' ? 'bg-red-50 text-red-900' : 'text-gray-600 hover:bg-gray-50'}`}
              >
                <Bell className="w-5 h-5 mr-3" /> 알림 설정
              </button>
              <button
                onClick={() => setActiveTab('profile')}
                className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${activeTab === 'profile' ? 'bg-red-50 text-red-900' : 'text-gray-600 hover:bg-gray-50'}`}
              >
                <User className="w-5 h-5 mr-3" /> 프로필 수정
              </button>
              <button
                onClick={() => setActiveTab('block')}
                className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${activeTab === 'block' ? 'bg-red-50 text-red-900' : 'text-gray-600 hover:bg-gray-50'}`}
              >
                <Shield className="w-5 h-5 mr-3" /> 차단 사용자 관리
              </button>
              <button
                onClick={() => setActiveTab('card')}
                className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${activeTab === 'card' ? 'bg-red-50 text-red-900' : 'text-gray-600 hover:bg-gray-50'}`}
              >
                <CreditCard className="w-5 h-5 mr-3" /> 카드/계좌 관리
              </button>
              <button
                onClick={openWithdrawModal}
                className="w-full flex items-center px-6 py-4 font-bold text-sm text-red-500 hover:bg-red-50 transition-colors"
              >
                <UserMinus className="w-5 h-5 mr-3" /> 회원 탈퇴
              </button>
            </div>
          </div>
        </div>

        {/* Right Column: Content */}
        <div className="lg:col-span-2 space-y-8">
          {activeTab === 'profile' && (
            <section className="bg-white rounded-3xl border border-gray-100 shadow-sm p-8 animate-in fade-in duration-300">
              <div className="mb-8">
                <h3 className="text-xl font-bold text-gray-900">프로필 수정</h3>
              </div>

              <div className="space-y-8 max-w-2xl">
                {/* Nickname & Password */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
                  <div className="md:col-span-2">
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">닉네임</label>
                    <input
                      type="text"
                      className="block w-full px-5 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none h-[56px]"
                      value={profileData.nickname}
                      onChange={(e) => setProfileData({ ...profileData, nickname: e.target.value })}
                    />
                  </div>
                  <button
                    onClick={() => setIsPasswordModalOpen(true)}
                    className="h-[56px] px-6 bg-gray-50 text-gray-500 text-xs font-bold rounded-2xl hover:bg-gray-100 transition-all flex items-center justify-center gap-2 border border-gray-100"
                  >
                    <Shield className="w-4 h-4" />
                    비밀번호 변경
                  </button>
                </div>

                {/* Phone Number */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
                  <div className="md:col-span-2">
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">휴대폰 번호</label>
                    <input
                      type="tel"
                      className="block w-full px-5 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none h-[56px]"
                      value={profileData.phoneNum}
                      onChange={(e) => setProfileData({ ...profileData, phoneNum: e.target.value })}
                    />
                  </div>
                </div>

                {/* Email */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
                  <div className="md:col-span-2">
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">이메일</label>
                    <input
                      type="email"
                      className="block w-full px-5 py-4 bg-gray-100 border border-gray-100 rounded-2xl text-sm text-gray-500 cursor-not-allowed outline-none h-[56px]"
                      value={profileData.email}
                      readOnly
                    />
                  </div>
                  <button
                    onClick={() => setIsEmailModalOpen(true)}
                    className="h-[56px] px-6 bg-gray-900 text-white text-xs font-bold rounded-2xl hover:bg-black transition-all flex items-center justify-center"
                  >
                    이메일 변경
                  </button>
                </div>

                {/* Address */}
                <div className="space-y-4">
                  <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">주소</label>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
                    <div className="md:col-span-2">
                      <input
                        type="text"
                        readOnly
                        className="block w-full px-5 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm outline-none h-[56px]"
                        placeholder="주소 검색을 이용해주세요"
                        value={profileData.address}
                      />
                    </div>
                    <button
                      type="button"
                      className="h-[56px] px-6 bg-gray-900 text-white text-xs font-bold rounded-2xl hover:bg-black transition-all flex items-center justify-center"
                      onClick={() => setProfileData({ ...profileData, address: '서울 강남구 테헤란로 123' })}
                    >
                      주소 검색
                    </button>
                  </div>
                  <input
                    type="text"
                    className="block w-full px-5 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-[#FF5A5A]/20 focus:bg-white transition-all outline-none h-[56px]"
                    placeholder="상세주소"
                    value={profileData.addrDetail}
                    onChange={(e) => setProfileData({ ...profileData, addrDetail: e.target.value })}
                  />
                </div>

                <div className="pt-8">
                  <button
                    onClick={handleProfileSave}
                    disabled={isProfileSaving}
                    className="w-full py-5 bg-[#FF5A5A] text-white font-black rounded-2xl hover:bg-[#FF4545] transition-all shadow-xl shadow-red-100 active:scale-95 disabled:opacity-50"
                  >
                    {isProfileSaving ? '저장 중...' : '프로필 정보 저장'}
                  </button>
                </div>
              </div>
            </section>
          )}

          {/* Card/Account Management */}
          {activeTab === 'card' && (
            <section className="bg-white rounded-3xl border border-gray-100 shadow-sm p-8 animate-in fade-in duration-300">
              <div className="flex items-center justify-between mb-8">
                <h3 className="text-xl font-bold text-gray-900">카드/계좌 관리</h3>
                <button
                  onClick={() => alert('카드/계좌 추가 기능은 준비 중입니다.')}
                  className="px-4 py-2 bg-gray-900 text-white text-xs font-bold rounded-xl hover:bg-black transition-all"
                >
                  추가하기
                </button>
              </div>

              <div className="space-y-4">
                <DndContext
                  sensors={sensors}
                  collisionDetection={closestCenter}
                  onDragEnd={handleDragEnd}
                >
                  <SortableContext
                    items={cards.map(c => c.id)}
                    strategy={verticalListSortingStrategy}
                  >
                    {cards.map(card => (
                      <SortableCard key={card.id} card={card} onDelete={deleteCard} />
                    ))}
                  </SortableContext>
                </DndContext>
              </div>
            </section>
          )}

          {/* Notification Settings */}
          {activeTab === 'notification' && (
            <section className="bg-white rounded-3xl border border-gray-100 shadow-sm p-8 animate-in fade-in duration-300">
              <div className="mb-8">
                <h3 className="text-xl font-bold text-gray-900">알림 설정</h3>
              </div>

              <div className="space-y-6">
                <div className="flex items-center justify-between py-2">
                  <div>
                    <p className="font-bold text-gray-900">경매 종료 알림</p>
                    <p className="text-sm text-gray-500">관심 등록하거나 입찰한 경매가 종료될 때 알림을 받습니다.</p>
                  </div>
                  <button
                    onClick={() => toggleSetting('auctionEnd')}
                    className={`w-12 h-6 rounded-full transition-all relative ${settings.auctionEnd ? 'bg-emerald-500' : 'bg-gray-200'}`}
                  >
                    <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-all ${settings.auctionEnd ? 'left-7' : 'left-1'}`}></div>
                  </button>
                </div>

                <div className="flex items-center justify-between py-2">
                  <div>
                    <p className="font-bold text-gray-900">새로운 입찰 알림</p>
                    <p className="text-sm text-gray-500">내가 올린 상품에 새로운 입찰이 발생하면 알림을 받습니다.</p>
                  </div>
                  <button
                    onClick={() => toggleSetting('newBid')}
                    className={`w-12 h-6 rounded-full transition-all relative ${settings.newBid ? 'bg-emerald-500' : 'bg-gray-200'}`}
                  >
                    <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-all ${settings.newBid ? 'left-7' : 'left-1'}`}></div>
                  </button>
                </div>

                <div className="flex items-center justify-between py-2">
                  <div>
                    <p className="font-bold text-gray-900">채팅 알림</p>
                    <p className="text-sm text-gray-500">새로운 채팅 메시지가 도착하면 알림을 받습니다.</p>
                  </div>
                  <button
                    onClick={() => toggleSetting('chat')}
                    className={`w-12 h-6 rounded-full transition-all relative ${settings.chat ? 'bg-emerald-500' : 'bg-gray-200'}`}
                  >
                    <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-all ${settings.chat ? 'left-7' : 'left-1'}`}></div>
                  </button>
                </div>

                <div className="flex items-center justify-between py-2 border-t border-gray-50 pt-6">
                  <div>
                    <p className="font-bold text-gray-900">마케팅 정보 수신</p>
                    <p className="text-sm text-gray-500">이벤트, 혜택 등 다양한 마케팅 소식을 받습니다.</p>
                  </div>
                  <button
                    onClick={() => toggleSetting('marketing')}
                    className={`w-12 h-6 rounded-full transition-all relative ${settings.marketing ? 'bg-emerald-500' : 'bg-gray-200'}`}
                  >
                    <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-all ${settings.marketing ? 'left-7' : 'left-1'}`}></div>
                  </button>
                </div>
              </div>
            </section>
          )}

          {/* Blocked Users Management */}
          {activeTab === 'block' && (
            <section className="bg-white rounded-3xl border border-gray-100 shadow-sm p-8 animate-in fade-in duration-300">
              <div className="mb-8">
                <h3 className="text-xl font-bold text-gray-900">차단 사용자 관리</h3>
              </div>

              {blockedUsers.length > 0 ? (
                <div className="space-y-4">
                  {blockedUsers.map(user => (
                    <div key={user.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-2xl border border-gray-100">
                      <div className="flex items-center gap-3">
                        <img src={user.profileImage || undefined} alt={user.nickname} className="w-10 h-10 rounded-full object-cover" />
                        <div>
                          <p className="font-bold text-gray-900">{user.nickname}</p>
                          <p className="text-xs text-gray-400">매너온도 {user.mannerTemp}℃</p>
                        </div>
                      </div>
                      <button
                        onClick={() => unblockUser(user.id)}
                        className="px-4 py-2 text-xs font-bold text-gray-500 bg-white border border-gray-200 rounded-xl hover:bg-gray-100 transition-all"
                      >
                        차단 해제
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-12 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-200">
                  <p className="text-gray-400 font-medium">차단한 사용자가 없습니다.</p>
                </div>
              )}
            </section>
          )}
        </div>
      </div>

      {/* Card Delete Confirmation Modal */}
      {isCardDeleteModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setIsCardDeleteModalOpen(false)}></div>
          <div className="relative bg-white w-full max-w-sm rounded-[32px] shadow-2xl overflow-hidden animate-in zoom-in duration-200">
            <div className="p-8 text-center">
              <div className="w-16 h-16 bg-red-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
                <AlertCircle className="w-8 h-8 text-red-500" />
              </div>
              <h3 className="text-xl font-black text-gray-900 mb-2">카드/계좌 삭제</h3>
              <p className="text-sm text-gray-500 mb-8 font-medium">정말 삭제하시겠습니까?</p>

              <div className="flex gap-3">
                <button
                  onClick={() => setIsCardDeleteModalOpen(false)}
                  className="flex-1 py-3.5 bg-gray-100 text-gray-600 font-bold rounded-2xl hover:bg-gray-200 transition-all"
                >
                  취소
                </button>
                <button
                  onClick={confirmDeleteCard}
                  className="flex-1 py-3.5 bg-red-500 text-white font-bold rounded-2xl hover:bg-red-600 transition-all shadow-lg shadow-red-100"
                >
                  삭제
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Logout Confirmation Modal */}
      {isLogoutModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setIsLogoutModalOpen(false)}></div>
          <div className="relative bg-white w-full max-w-sm rounded-[32px] shadow-2xl overflow-hidden animate-in zoom-in duration-200">
            <div className="p-8 text-center">
              <div className="w-16 h-16 bg-red-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
                <LogOut className="w-8 h-8 text-red-500" />
              </div>
              <h3 className="text-xl font-black text-gray-900 mb-2">로그아웃 하시겠습니까?</h3>
              <p className="text-sm text-gray-500 mb-8 font-medium">다음에 또 만나요!</p>

              <div className="flex gap-3">
                <button
                  onClick={() => setIsLogoutModalOpen(false)}
                  className="flex-1 py-3.5 bg-gray-100 text-gray-600 font-bold rounded-2xl hover:bg-gray-200 transition-all"
                >
                  취소
                </button>
                <button
                  onClick={confirmLogout}
                  className="flex-1 py-3.5 bg-red-500 text-white font-bold rounded-2xl hover:bg-red-600 transition-all shadow-lg shadow-red-100"
                >
                  로그아웃
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Profile Save Success Modal */}
      {isProfileSaveModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setIsProfileSaveModalOpen(false)}></div>
          <div className="relative bg-white w-full max-w-sm rounded-[32px] shadow-2xl overflow-hidden animate-in zoom-in duration-200">
            <div className="p-8 text-center">
              <div className="w-16 h-16 bg-emerald-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="w-8 h-8 text-emerald-500" />
              </div>
              <h3 className="text-xl font-black text-gray-900 mb-2">수정 완료</h3>
              <p className="text-sm text-gray-500 mb-8 font-medium">프로필 정보가 성공적으로 수정되었습니다.</p>

              <button
                onClick={() => setIsProfileSaveModalOpen(false)}
                className="w-full py-3.5 bg-gray-900 text-white font-bold rounded-2xl hover:bg-black transition-all shadow-lg"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Password Change Modal */}
      {isPasswordModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setIsPasswordModalOpen(false)}></div>
          <div className="relative bg-white w-full max-w-md rounded-[32px] shadow-2xl overflow-hidden animate-in zoom-in duration-200">
            <div className="p-8">
              <div className="flex items-center justify-between mb-8">
                <h3 className="text-2xl font-black text-gray-900">비밀번호 변경</h3>
                <button onClick={() => setIsPasswordModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                  <X className="w-6 h-6" />
                </button>
              </div>

              {passwordStep === 'verify' && (
                <div className="space-y-6">
                  <div className="space-y-4">
                    <div>
                      <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">기존 비밀번호</label>
                      <input
                        type="password"
                        placeholder="기존 비밀번호 입력"
                        className="w-full px-5 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-gray-200 outline-none"
                        value={passwordData.current}
                        onChange={(e) => setPasswordData({ ...passwordData, current: e.target.value })}
                      />
                    </div>
                    <button
                      onClick={handlePasswordChange}
                      className="w-full py-4 bg-gray-900 text-white font-black rounded-2xl hover:bg-black transition-all shadow-xl"
                    >
                      확인
                    </button>
                  </div>
                </div>
              )}

              {passwordStep === 'input' && (
                <div className="space-y-6">
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">비밀번호</label>
                    <input
                      type="password"
                      placeholder="비밀번호 입력"
                      className="w-full px-5 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-gray-200 outline-none"
                      value={passwordData.new}
                      onChange={(e) => setPasswordData({ ...passwordData, new: e.target.value })}
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 ml-1">비밀번호 확인</label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <ShieldCheck
                          className={`h-5 w-5 ${passwordData.confirm === '' ? 'text-gray-400' : (passwordData.new === passwordData.confirm ? 'text-emerald-500' : 'text-red-500')}`}
                          title={passwordData.confirm !== '' && passwordData.new !== passwordData.confirm ? "비밀번호가 맞지 않습니다." : ""}
                        />
                      </div>
                      <input
                        type="password"
                        placeholder="비밀번호 다시 입력"
                        className="w-full pl-11 pr-4 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-gray-200 outline-none"
                        value={passwordData.confirm}
                        onChange={(e) => setPasswordData({ ...passwordData, confirm: e.target.value })}
                      />
                    </div>
                  </div>

                  <div className="pt-4">
                    <button
                      onClick={handlePasswordChange}
                      className="w-full py-4 bg-gray-900 text-white font-black rounded-2xl hover:bg-black transition-all shadow-xl"
                    >
                      비밀번호 변경 완료
                    </button>
                  </div>
                </div>
              )}

              {passwordStep === 'success' && (
                <div className="text-center py-8">
                  <div className="w-20 h-20 bg-emerald-50 rounded-3xl flex items-center justify-center mx-auto mb-6">
                    <CheckCircle2 className="w-10 h-10 text-emerald-500" />
                  </div>
                  <h3 className="text-2xl font-black text-gray-900 mb-2">변경 완료</h3>
                  <p className="text-sm text-gray-500">비밀번호가 성공적으로 변경되었습니다.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Email Change Modal */}
      {isEmailModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setIsEmailModalOpen(false)}></div>
          <div className="relative bg-white w-full max-w-md rounded-[32px] shadow-2xl overflow-hidden animate-in zoom-in duration-200">
            <div className="p-8">
              <div className="flex items-center justify-between mb-8">
                <h3 className="text-2xl font-black text-gray-900">이메일 변경</h3>
                <button onClick={() => setIsEmailModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                  <X className="w-6 h-6" />
                </button>
              </div>

              {emailStep === 'input' && (
                <div className="space-y-6">
                  <div className="space-y-4">
                    <input
                      type="email"
                      placeholder="새 이메일 주소 입력"
                      className="w-full px-5 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-gray-200 outline-none"
                      value={newEmail}
                      onChange={(e) => setNewEmail(e.target.value)}
                    />
                    {!isCodeSent && (
                      <button
                        onClick={() => sendVerificationCode('email')}
                        disabled={!newEmail}
                        className="w-full py-4 bg-gray-900 text-white text-sm font-bold rounded-2xl hover:bg-black transition-all disabled:opacity-50"
                      >
                        인증번호 발송
                      </button>
                    )}

                    {isCodeSent && (
                      <div className="space-y-4 animate-in slide-in-from-top-2 duration-300">
                        <div className="relative">
                          <input
                            type="text"
                            placeholder="인증번호 6자리 입력"
                            className="w-full px-5 py-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-gray-200 outline-none"
                            value={verificationCode}
                            onChange={(e) => setVerificationCode(e.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
                          />
                          {timer > 0 && (
                            <div className="absolute right-4 top-1/2 -translate-y-1/2 text-[10px] font-bold text-[#FF5A5A]">
                              {Math.floor(timer / 60)}:{(timer % 60).toString().padStart(2, '0')}
                            </div>
                          )}
                        </div>
                        <div className="flex justify-center px-1">
                          <button
                            type="button"
                            onClick={() => sendVerificationCode('email')}
                            className="text-[10px] font-bold text-gray-400 hover:text-gray-600 transition-colors underline underline-offset-2"
                          >
                            인증번호 재전송
                          </button>
                        </div>
                        <button
                          onClick={() => verifyCode('email')}
                          className="w-full py-4 bg-gray-900 text-white font-black rounded-2xl hover:bg-black transition-all shadow-xl"
                        >
                          인증 확인 및 변경
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {emailStep === 'success' && (
                <div className="text-center py-8">
                  <div className="w-20 h-20 bg-emerald-50 rounded-3xl flex items-center justify-center mx-auto mb-6">
                    <CheckCircle2 className="w-10 h-10 text-emerald-500" />
                  </div>
                  <h3 className="text-2xl font-black text-gray-900 mb-2">변경 완료</h3>
                  <p className="text-sm text-gray-500">이메일 주소가 성공적으로 변경되었습니다.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Withdrawal Modal */}
      {isWithdrawModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setIsWithdrawModalOpen(false)}></div>
          <div className="relative bg-white w-full max-w-md rounded-[32px] shadow-2xl overflow-hidden">
            <button
              onClick={() => setIsWithdrawModalOpen(false)}
              className="absolute top-6 right-6 p-2 text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X className="w-6 h-6" />
            </button>

            <div className="p-10">
              {withdrawStep === 'confirm' && (
                <div className="text-center">
                  <div className="w-20 h-20 bg-red-100 rounded-3xl flex items-center justify-center mx-auto mb-6">
                    <AlertCircle className="w-10 h-10 text-red-500" />
                  </div>
                  <h3 className="text-2xl font-black text-gray-900 mb-4">정말 탈퇴하시겠습니까?</h3>

                  <div className="bg-red-50 p-6 rounded-2xl text-left mb-8">
                    <p className="text-sm font-bold text-red-800 mb-4">탈퇴 조건 확인</p>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-red-700">보유 포인트 0원</span>
                        {CURRENT_USER.points === 0 ? (
                          <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                        ) : (
                          <span className="text-[10px] font-bold text-red-500 bg-white px-2 py-0.5 rounded-md shadow-sm">
                            {CURRENT_USER.points.toLocaleString()}P 보유 중
                          </span>
                        )}
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-red-700">판매 중인 물건 없음</span>
                        {MOCK_PRODUCTS.filter(p => p.seller.id === CURRENT_USER.id && p.status === 'active').length === 0 ? (
                          <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                        ) : (
                          <span className="text-[10px] font-bold text-red-500 bg-white px-2 py-0.5 rounded-md shadow-sm">
                            {MOCK_PRODUCTS.filter(p => p.seller.id === CURRENT_USER.id && p.status === 'active').length}건 진행 중
                          </span>
                        )}
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-red-700">거래 중인 물품 없음</span>
                        {MOCK_PRODUCTS.filter(p => {
                          const isSeller = p.seller.id === CURRENT_USER.id;
                          const isBuyer = p.bids.some(b => b.bidderName === CURRENT_USER.nickname && p.winnerId === CURRENT_USER.id);
                          const isTrading = ['paid', 'shipped'].includes(p.status);
                          return (isSeller || isBuyer) && isTrading;
                        }).length === 0 ? (
                          <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                        ) : (
                          <span className="text-[10px] font-bold text-red-500 bg-white px-2 py-0.5 rounded-md shadow-sm">
                            {MOCK_PRODUCTS.filter(p => {
                              const isSeller = p.seller.id === CURRENT_USER.id;
                              const isBuyer = p.bids.some(b => b.bidderName === CURRENT_USER.nickname && p.winnerId === CURRENT_USER.id);
                              const isTrading = ['paid', 'shipped'].includes(p.status);
                              return (isSeller || isBuyer) && isTrading;
                            }).length}건 거래 중
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="bg-gray-50 p-6 rounded-2xl text-left mb-8">
                    <p className="text-sm font-bold text-gray-800 mb-2">주의사항</p>
                    <ul className="text-xs text-gray-500 space-y-2 list-disc pl-4">
                      <li>탈퇴 시 모든 적립금과 거래 내역이 삭제되며 복구할 수 없습니다.</li>
                      <li>동일한 계정으로 재가입이 일정 기간 제한될 수 있습니다.</li>
                    </ul>
                  </div>

                  <button
                    onClick={() => {
                      const error = validateWithdrawal();
                      if (error) {
                        alert(error);
                      } else {
                        setWithdrawStep('reason');
                      }
                    }}
                    className="w-full py-4 bg-red-500 text-white font-bold rounded-2xl hover:bg-red-600 transition-all shadow-lg shadow-red-100"
                  >
                    다음 단계
                  </button>
                </div>
              )}

              {withdrawStep === 'reason' && (
                <div>
                  <h3 className="text-2xl font-black text-gray-900 mb-2">탈퇴 사유</h3>
                  <p className="text-sm text-gray-500 mb-8">탈퇴하시는 이유를 알려주시면 서비스 개선에 큰 도움이 됩니다.</p>

                  <div className="space-y-3 mb-8">
                    {[
                      '더 이상 서비스를 이용하지 않음',
                      '개인정보 보호를 위해',
                      '다른 계정으로 재가입',
                      '서비스 이용이 불편함',
                      '기타'
                    ].map(reason => (
                      <button
                        key={reason}
                        onClick={() => setWithdrawReason(reason)}
                        className={`w-full p-4 rounded-2xl border text-left text-sm font-bold transition-all ${withdrawReason === reason
                          ? 'border-indigo-600 bg-indigo-50 text-indigo-600'
                          : 'border-gray-100 bg-gray-50 text-gray-600 hover:bg-gray-100'
                          }`}
                      >
                        {reason}
                      </button>
                    ))}
                  </div>

                  {withdrawReason === '기타' && (
                    <textarea
                      value={withdrawReasonDetail}
                      onChange={(e) => setWithdrawReasonDetail(e.target.value)}
                      placeholder="사유를 입력해주세요."
                      className="w-full h-32 p-4 bg-gray-50 border border-gray-100 rounded-2xl text-sm mb-8 focus:ring-2 focus:ring-indigo-500/20 outline-none resize-none"
                    />
                  )}

                  <div className="flex gap-3">
                    <button
                      onClick={() => setWithdrawStep('confirm')}
                      className="flex-1 py-4 bg-gray-100 text-gray-600 font-bold rounded-2xl hover:bg-gray-200 transition-all"
                    >
                      이전
                    </button>
                    <button
                      onClick={() => setWithdrawStep('input')}
                      disabled={!withdrawReason || (withdrawReason === '기타' && !withdrawReasonDetail)}
                      className="flex-1 py-4 bg-gray-900 text-white font-bold rounded-2xl hover:bg-black transition-all shadow-xl disabled:opacity-50"
                    >
                      다음
                    </button>
                  </div>
                </div>
              )}

              {withdrawStep === 'input' && (
                <div>
                  <h3 className="text-2xl font-black text-gray-900 mb-2">확인 문구 입력</h3>
                  <p className="text-sm text-gray-500 mb-8">안전한 탈퇴를 위해 아래 문구를 정확히 입력해주세요.</p>

                  <div className="bg-gray-50 p-4 rounded-2xl text-center mb-6 border border-gray-100">
                    <p className="text-lg font-black text-emerald-600 tracking-wider">탈퇴하겠습니다</p>
                  </div>

                  <input
                    type="text"
                    value={withdrawInput}
                    onChange={(e) => {
                      setWithdrawInput(e.target.value);
                      setWithdrawError(null);
                    }}
                    placeholder="위 문구를 입력하세요"
                    className={`w-full p-4 bg-gray-50 border rounded-2xl mb-2 focus:outline-none transition-all ${withdrawError ? 'border-red-500' : 'border-gray-100 focus:border-emerald-500'}`}
                  />
                  {withdrawError && <p className="text-xs text-red-500 mb-6 ml-2">{withdrawError}</p>}

                  <button
                    onClick={handleWithdraw}
                    className="w-full py-4 bg-gray-900 text-white font-bold rounded-2xl hover:bg-black transition-all shadow-lg"
                  >
                    회원 탈퇴 완료
                  </button>
                </div>
              )}

              {withdrawStep === 'success' && (
                <div className="text-center py-10">
                  <div className="w-20 h-20 bg-emerald-100 rounded-3xl flex items-center justify-center mx-auto mb-6">
                    <CheckCircle2 className="w-10 h-10 text-emerald-500" />
                  </div>
                  <h3 className="text-2xl font-black text-gray-900 mb-2">탈퇴 처리가 완료되었습니다</h3>
                  <p className="text-sm text-gray-500">그동안 이용해주셔서 감사합니다.<br />잠시 후 메인 화면으로 이동합니다.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
