import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Bell, Search, Menu, X, User as UserIcon, LogOut, ChevronDown, ChevronUp, Sparkles, Plus, MapPin, Share2, Instagram, Youtube, Info, Headphones, Megaphone, Settings as SettingsIcon, Clock, TrendingUp, ShieldAlert } from 'lucide-react';
import { CURRENT_USER } from '@/services/mockData';
import { useAppContext } from '@/context/AppContext';
import { Category } from '@/types';
import { CATEGORY_DATA } from '@/constants';

interface HeaderProps {
  onLogout: () => void;
}

const Header: React.FC<HeaderProps> = ({ onLogout }) => {
  const { user, notifications, markNotificationAsRead, markAllNotificationsAsRead, unreadNotificationsCount } = useAppContext();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isCategoryOpen, setIsCategoryOpen] = useState(false);
  const [isNotiOpen, setIsNotiOpen] = useState(false);
  const [notiTab, setNotiTab] = useState<'noti' | 'chat'>('noti');
  const [searchTerm, setSearchTerm] = useState('');
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [showTopBar, setShowTopBar] = useState(true);
  const [activeCategory, setActiveCategory] = useState<string | null>(CATEGORY_DATA[0].id);
  const [recentSearches, setRecentSearches] = useState<string[]>(['아이폰 15', '맥북 프로', '에어팟']);
  const navigate = useNavigate();
  const location = useLocation();

  const RELATED_SEARCHES = ['아이폰 15', '맥북 프로', '에어팟', '닌텐도 스위치', '캠핑의자', '가죽 자켓', '원목 식탁', '해리포터'];
  const filteredRelated = searchTerm.trim()
    ? RELATED_SEARCHES.filter(s => s.toLowerCase().includes(searchTerm.toLowerCase()))
    : [];

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 40) {
        setShowTopBar(false);
      } else {
        setShowTopBar(true);
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchTerm.trim()) {
      if (!recentSearches.includes(searchTerm.trim())) {
        setRecentSearches(prev => [searchTerm.trim(), ...prev].slice(0, 5));
      }
      navigate(`/search?q=${encodeURIComponent(searchTerm)}`);
      setIsSearchFocused(false);
    }
  };

  const handleCategoryClick = (cat: Category) => {
    navigate(`/search?category=${encodeURIComponent(cat)}`);
    setIsCategoryOpen(false);
    setIsMenuOpen(false);
  };

  return (
    <header className="sticky top-0 z-50 bg-white border-b border-gray-100">
      {/* Top Utility Bar - Disappears on scroll */}
      <div className={`hidden md:block bg-gray-50 border-b border-gray-100 transition-all duration-300 overflow-hidden ${showTopBar ? 'h-10 opacity-100' : 'h-0 opacity-0'}`}>
        <div className="max-w-[1200px] mx-auto px-6 h-10 flex items-center justify-between text-[11px] font-bold text-gray-500">
          <div className="flex items-center gap-2">
            <Link to="/notice" className="px-3 py-1 hover:bg-gray-200 rounded-lg transition-colors">공지사항</Link>
            <Link to="/faq" className="px-3 py-1 hover:bg-gray-200 rounded-lg transition-colors">자주 묻는 질문</Link>
            <Link to="/inquiry" className="px-3 py-1 hover:bg-gray-200 rounded-lg transition-colors">문의하기</Link>
          </div>
          <div className="flex items-center gap-2">
            {!user ? (
              <div className="flex items-center gap-2">
                <Link to="/login" className="px-3 py-1 hover:bg-gray-200 rounded-lg transition-colors">로그인/회원가입</Link>
              </div>
            ) : (
              <span className="text-gray-400 mr-2">{user.nickname}님 환영합니다</span>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-[1200px] mx-auto px-6">
        {/* Main Header Row */}
        <div className="flex items-center justify-between h-20">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-1 shrink-0 group">
            <div className="bg-[#FF5A5A] p-2 rounded-xl shadow-sm group-hover:scale-110 transition-transform">
              <Sparkles className="w-6 h-6 text-white" />
            </div>
            <span className="text-2xl font-black text-gray-800 tracking-tighter italic">JAVAJAVA</span>
          </Link>

          {/* Search Bar */}
          <div className="flex-1 max-w-2xl mx-12 hidden md:block relative">
            <form onSubmit={handleSearch} className="relative group flex items-center">
              <input
                type="text"
                placeholder="찾으시는 상품이 있나요?"
                className="w-full pl-6 pr-12 py-3.5 bg-[#F8F9FA] border-none rounded-2xl text-sm focus:ring-2 focus:ring-gray-100 focus:bg-white transition-all outline-none placeholder:text-gray-400"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onFocus={() => setIsSearchFocused(true)}
                onBlur={() => setTimeout(() => setIsSearchFocused(false), 200)}
              />
              <button type="submit" className="absolute right-4 h-full flex items-center justify-center text-gray-400 group-focus-within:text-gray-600 transition-colors">
                <Search className="w-5 h-5" />
              </button>
            </form>

            {isSearchFocused && (
              <div className="absolute top-full left-0 w-full bg-white border border-gray-100 rounded-2xl shadow-2xl mt-2 py-4 z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                {searchTerm.trim() === '' ? (
                  <>
                    <div className="px-6 flex items-center justify-between mb-3">
                      <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">최근 검색어</p>
                      <button
                        onClick={() => setRecentSearches([])}
                        className="text-[10px] font-bold text-gray-300 hover:text-gray-500"
                      >
                        전체 삭제
                      </button>
                    </div>
                    <div className="space-y-1">
                      {recentSearches.length > 0 ? (
                        recentSearches.map((term, idx) => (
                          <button
                            key={idx}
                            onClick={() => {
                              setSearchTerm(term);
                              navigate(`/search?q=${encodeURIComponent(term)}`);
                              setIsSearchFocused(false);
                            }}
                            className="w-full text-left px-6 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors flex items-center justify-between group"
                          >
                            <div className="flex items-center gap-3">
                              <Clock className="w-3.5 h-3.5 text-gray-300" />
                              {term}
                            </div>
                            <X
                              className="w-3 h-3 text-gray-300 opacity-0 group-hover:opacity-100 hover:text-red-500 transition-all"
                              onClick={(e) => {
                                e.stopPropagation();
                                setRecentSearches(prev => prev.filter((_, i) => i !== idx));
                              }}
                            />
                          </button>
                        ))
                      ) : (
                        <p className="px-6 py-4 text-xs text-gray-400 text-center italic">최근 검색어가 없습니다.</p>
                      )}
                    </div>
                  </>
                ) : (
                  <>
                    <p className="px-6 text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-3">연관 검색어</p>
                    <div className="space-y-1">
                      {filteredRelated.length > 0 ? (
                        filteredRelated.map((term, idx) => (
                          <button
                            key={idx}
                            onClick={() => {
                              setSearchTerm(term);
                              navigate(`/search?q=${encodeURIComponent(term)}`);
                              setIsSearchFocused(false);
                            }}
                            className="w-full text-left px-6 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 hover:text-[#FF5A5A] transition-colors flex items-center gap-3"
                          >
                            <TrendingUp className="w-3.5 h-3.5 text-gray-300" />
                            {term}
                          </button>
                        ))
                      ) : (
                        <p className="px-6 py-4 text-xs text-gray-400 text-center italic">연관 검색어가 없습니다.</p>
                      )}
                    </div>
                  </>
                )}
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="flex items-center space-x-5">
            <div className="hidden xl:flex items-center space-x-4 mr-2">
              <Link to="/notice" className="text-sm font-bold text-gray-500 hover:text-gray-900 transition-colors">고객센터</Link>
            </div>

            {/* 포인트 영역 (비회원은 0으로 보이게 고정) */}
            <div className="flex flex-col items-end">
              <Link to="/points" className="bg-[#F8F9FA] px-4 py-2.5 rounded-2xl flex items-center space-x-2 border border-gray-50 hover:bg-gray-100 transition-colors">
                <span className="text-[#FF5A5A] font-black text-sm">P</span>
                <span className="text-sm font-black text-gray-700">
                  {user ? user.points.toLocaleString() : '0'}
                </span>
              </Link>
            </div>

            {user ? (
              <>
                <Link
                  to="/register"
                  className="hidden lg:flex items-center space-x-1.5 bg-[#FF5A5A] text-white px-5 py-3 rounded-2xl text-sm font-bold hover:bg-[#FF4545] transition-all shadow-lg shadow-red-100 active:scale-95"
                >
                  <span>경매 등록</span>
                </Link>

                <div className="relative">
                  <button
                    onClick={() => {
                      const opening = !isNotiOpen;
                      setIsNotiOpen(opening);
                      if (opening) markAllNotificationsAsRead();
                    }}
                    className="p-2.5 text-gray-400 hover:text-gray-600 hover:bg-gray-50 rounded-xl transition-all relative"
                  >
                    <Bell className="w-6 h-6" />
                    {unreadNotificationsCount > 0 && (
                      <span className="absolute top-2.5 right-2.5 w-2 h-2 bg-[#FF5A5A] rounded-full border-2 border-white"></span>
                    )}
                  </button>
                  {isNotiOpen && (
                    <div className="absolute right-0 mt-3 w-80 bg-white border border-gray-100 rounded-2xl shadow-2xl overflow-hidden z-50">
                      <div className="flex border-b border-gray-50">
                        <button
                          onClick={() => setNotiTab('noti')}
                          className={`flex-1 px-5 py-3 text-sm transition-all ${notiTab === 'noti' ? 'font-black text-gray-900 border-b-2 border-gray-900' : 'font-bold text-gray-400 hover:text-gray-600'}`}
                        >
                          알림
                        </button>
                        <button
                          onClick={() => setNotiTab('chat')}
                          className={`flex-1 px-5 py-3 text-sm transition-all ${notiTab === 'chat' ? 'font-black text-gray-900 border-b-2 border-gray-900' : 'font-bold text-gray-400 hover:text-gray-600'}`}
                        >
                          대화
                        </button>
                      </div>
                      <div className="max-h-80 overflow-y-auto">
                        {notiTab === 'noti' ? (
                          notifications.length > 0 ? notifications.map(noti => (
                            <Link
                              key={noti.id}
                              to="/inbox?tab=noti"
                              className={`block px-5 py-4 text-sm hover:bg-gray-50 transition-colors ${!noti.read ? 'bg-red-50/30' : ''}`}
                              onClick={() => {
                                markNotificationAsRead(noti.id);
                                setIsNotiOpen(false);
                              }}
                            >
                              <p className="text-gray-700 leading-snug font-medium line-clamp-2">{noti.message}</p>
                              <span className="text-[10px] font-bold text-gray-300 mt-2 block uppercase tracking-wider">{new Date(noti.createdAt).toLocaleDateString()}</span>
                            </Link>
                          )) : (
                            <div className="py-10 text-center text-sm text-gray-400 font-medium">알림이 없습니다.</div>
                          )
                        ) : (
                          <div className="py-10 text-center text-sm text-gray-400 font-medium">대화 내역이 없습니다.</div>
                        )}
                      </div>
                      <Link
                        to={`/inbox?tab=${notiTab}`}
                        className="block text-center py-3 text-xs font-bold text-gray-400 hover:text-gray-600 border-t border-gray-50"
                        onClick={() => setIsNotiOpen(false)}
                      >
                        전체보기
                      </Link>
                    </div>
                  )}
                </div>

                <div className="flex items-center space-x-4">
                  <div className="relative group">
                    <button className="flex items-center gap-2 shrink-0">
                      <img
                        src={user.profileImage || undefined}
                        alt="Profile"
                        className="w-10 h-10 rounded-full border-2 border-white shadow-md hover:ring-2 hover:ring-[#FF5A5A]/30 transition-all object-cover"
                      />
                    </button>

                    <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-100 rounded-2xl shadow-2xl py-2 z-50 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 transform origin-top-right scale-95 group-hover:scale-100">
                      <Link
                        to="/mypage"
                        className="flex items-center px-4 py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-50 hover:text-[#FF5A5A] transition-colors"
                      >
                        <UserIcon className="w-4 h-4 mr-2.5" /> 프로필 보기
                      </Link>
                      <Link
                        to="/settings"
                        className="flex items-center px-4 py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-50 hover:text-[#FF5A5A] transition-colors"
                      >
                        <SettingsIcon className="w-4 h-4 mr-2.5" /> 계정 설정
                      </Link>
                      <div className="border-t border-gray-50 mt-1 pt-1 flex justify-end px-4 pb-2">
                        <button
                          onClick={onLogout}
                          className="text-[11px] font-medium text-gray-400 hover:text-gray-600 transition-colors"
                        >
                          로그아웃
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-2">
                <Link
                  to="/login"
                  className="px-5 py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-50 rounded-xl transition-all border border-gray-100"
                >
                  로그인
                </Link>
                <Link
                  to="/signup"
                  className="px-5 py-2.5 text-sm font-bold text-white bg-[#FF5A5A] hover:bg-[#FF4545] rounded-xl transition-all shadow-lg shadow-red-100 active:scale-95"
                >
                  회원가입
                </Link>
              </div>
            )}
          </div>
        </div>

        {/* Sub Header Row */}
        <div className="flex items-center justify-between h-12 pb-2">
          <div className="relative group">
            <button
              className="flex items-center text-sm font-bold text-gray-700 hover:text-[#FF5A5A] transition-colors"
              onClick={() => setIsCategoryOpen(!isCategoryOpen)}
            >
              카테고리 {isCategoryOpen ? <ChevronUp className="w-4 h-4 ml-1.5 opacity-50" /> : <ChevronDown className="w-4 h-4 ml-1.5 opacity-50" />}
            </button>

            {isCategoryOpen && (
              <div className="absolute top-full left-0 w-[900px] h-[600px] bg-white border border-gray-100 rounded-2xl shadow-2xl mt-2 z-50 animate-in fade-in slide-in-from-top-2 duration-200 flex overflow-hidden">
                {/* Sidebar */}
                <div className="w-56 bg-gray-50 border-r border-gray-100 overflow-y-auto custom-scrollbar">
                  {CATEGORY_DATA.map((cat) => (
                    <button
                      key={cat.id}
                      onMouseEnter={() => setActiveCategory(cat.id)}
                      onClick={() => {
                        navigate(`/search?category=${encodeURIComponent(cat.name)}`);
                        setIsCategoryOpen(false);
                      }}
                      className={`w-full text-left px-6 py-4 text-sm font-bold transition-all flex items-center justify-between group ${activeCategory === cat.id
                          ? 'bg-white text-[#FF5A5A]'
                          : 'text-gray-600 hover:bg-white hover:text-[#FF5A5A]'
                        }`}
                    >
                      {cat.name}
                    </button>
                  ))}
                </div>

                {/* Content */}
                <div className="flex-1 p-6 overflow-y-auto custom-scrollbar bg-white">
                  {activeCategory && (
                    <div className="grid grid-cols-3 gap-x-10 gap-y-10">
                      {CATEGORY_DATA.find(c => c.id === activeCategory)?.subCategories?.map((sub) => (
                        <div key={sub.id} className="space-y-4">
                          <button
                            onClick={() => {
                              navigate(`/search?category=${encodeURIComponent(sub.name)}`);
                              setIsCategoryOpen(false);
                            }}
                            className="text-base font-black text-gray-900 hover:text-[#FF5A5A] transition-colors border-b-2 border-transparent hover:border-[#FF5A5A] pb-1 inline-block"
                          >
                            {sub.name}
                          </button>
                          {sub.subCategories && (
                            <div className="flex flex-col space-y-2">
                              {sub.subCategories.map(item => (
                                <button
                                  key={item.id}
                                  onClick={() => {
                                    navigate(`/search?category=${encodeURIComponent(item.name)}`);
                                    setIsCategoryOpen(false);
                                  }}
                                  className="text-sm font-bold text-gray-400 hover:text-[#FF5A5A] transition-colors text-left"
                                >
                                  {item.name}
                                </button>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="flex items-center space-x-3">
            {user?.isAdmin && (
              <Link
                to="/admin"
                className="flex items-center text-xs font-black text-white bg-black px-4 py-1.5 rounded-full shadow-lg hover:bg-gray-800 transition-all active:scale-95"
              >
                관리자모드
              </Link>
            )}
            <button
              onClick={() => navigate('/settings?tab=profile')}
              className="flex items-center text-xs font-bold text-gray-400 bg-white border border-gray-100 px-3 py-1.5 rounded-full shadow-sm hover:border-[#FF5A5A] hover:text-[#FF5A5A] transition-all group overflow-hidden"
            >
              <MapPin className="w-3.5 h-3.5 mr-1.5 text-[#FF5A5A] shrink-0" />
              <span className="whitespace-nowrap">{user?.address ? user.address.split(' ').slice(0, 2).join(' ') : '위치를 확인할 수 없습니다.'}</span>
              <span className="text-[10px] max-w-0 opacity-0 group-hover:max-w-[100px] group-hover:opacity-100 transition-all duration-300 overflow-hidden whitespace-nowrap ml-0 group-hover:ml-2 border-l border-transparent group-hover:border-gray-200 pl-0 group-hover:pl-2">
                위치 변경하기
              </span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};

const Footer: React.FC = () => {
  return (
    <footer className="bg-[#F8F9FA] pt-16 pb-12 border-t border-gray-100">
      <div className="max-w-[1200px] mx-auto px-6">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-12 mb-16">
          {/* Company Info */}
          <div className="md:col-span-5">
            <div className="flex items-center space-x-2 mb-6">
              <div className="bg-gray-400 p-1.5 rounded-lg">
                <Sparkles className="w-5 h-5 text-white" />
              </div>
              <span className="text-xl font-black text-gray-700 tracking-tight">JAVAJAVA</span>
            </div>
            <div className="space-y-2 text-sm text-gray-500 font-medium leading-relaxed">
              <p>대표이사: 홍길동 | 사업자등록번호: 123-45-67890</p>
              <p>통신판매업신고: 2024-서울강남-00000</p>
              <p>주소: 서울특별시 강남구 테헤란로 123, 옥션타워 15층</p>
              <p>이메일: help@auctionhouse.co.kr | 고객센터: 1588-0000</p>
              <p className="pt-4 text-xs text-gray-400">© 2024 AuctionHouse Inc. All rights reserved.</p>
            </div>
          </div>

          {/* Links */}
          <div className="md:col-span-2">
            <h4 className="text-sm font-black text-gray-800 mb-6 uppercase tracking-wider">서비스 및 정책</h4>
            <ul className="space-y-4 text-sm text-gray-500 font-bold">
              <li><Link to="/about?tab=intro" className="hover:text-[#FF5A5A] transition-colors">서비스 소개</Link></li>
              <li><Link to="/about?tab=privacy" className="hover:text-[#FF5A5A] transition-colors">개인정보처리방침</Link></li>
              <li><Link to="/about?tab=terms" className="hover:text-[#FF5A5A] transition-colors">이용약관</Link></li>
              <li><Link to="/about?tab=policy" className="hover:text-[#FF5A5A] transition-colors">운영정책</Link></li>
            </ul>
          </div>

          <div className="md:col-span-2">
            <h4 className="text-sm font-black text-gray-800 mb-6 uppercase tracking-wider">고객지원</h4>
            <ul className="space-y-4 text-sm text-gray-500 font-medium">
              <li className="flex items-center gap-2"><Sparkles className="w-4 h-4 opacity-50" /><Link to="/notice" className="hover:text-[#FF5A5A] transition-colors">공지사항</Link></li>
              <li className="flex items-center gap-2"><Info className="w-4 h-4 opacity-50" /><Link to="/faq" className="hover:text-[#FF5A5A] transition-colors">자주 묻는 질문 (FAQ)</Link></li>
              <li className="flex items-center gap-2"><ChevronDown className="w-4 h-4 opacity-50" /><Link to="/inquiry" className="hover:text-[#FF5A5A] transition-colors">1:1 문의하기</Link></li>
            </ul>
          </div>

          {/* Social */}
          <div className="md:col-span-3">
            <h4 className="text-sm font-black text-gray-800 mb-6 uppercase tracking-wider">FOLLOW US</h4>
            <div className="flex items-center gap-3">
              <a href="#" className="w-11 h-11 bg-white rounded-full flex items-center justify-center text-gray-400 hover:bg-[#FF5A5A] hover:text-white transition-all shadow-sm border border-gray-100">
                <Share2 className="w-5 h-5" />
              </a>
              <a href="#" className="w-11 h-11 bg-white rounded-full flex items-center justify-center text-gray-400 hover:bg-[#FF5A5A] hover:text-white transition-all shadow-sm border border-gray-100">
                <Instagram className="w-5 h-5" />
              </a>
              <a href="#" className="w-11 h-11 bg-white rounded-full flex items-center justify-center text-gray-400 hover:bg-[#FF5A5A] hover:text-white transition-all shadow-sm border border-gray-100">
                <Youtube className="w-5 h-5" />
              </a>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="pt-8 border-t border-gray-200 flex flex-col md:flex-row justify-between items-center gap-4">
          <div className="flex items-center space-x-6 text-[11px] font-bold text-gray-400">
            <a href="#" className="hover:text-gray-600">구매안전서비스 가입 사실 확인</a>
            <span className="opacity-30">|</span>
            <a href="#" className="hover:text-gray-600">서울보증보험 가입</a>
          </div>
          <p className="text-[11px] font-medium text-gray-400">
            본 사이트의 상품/판매자 정보는 거래 당사자의 책임하에 제공됩니다.
          </p>
        </div>
      </div>
    </footer>
  );
};

export const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const navigate = useNavigate();
  const { logout, forceLogoutModalOpen, closeForceLogoutModal } = useAppContext();
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);

  // 로그아웃 확인 모달 오픈
  const handleLogout = () => {
    setIsLogoutModalOpen(true);
  };

  const confirmLogout = () => {
    logout();
    setIsLogoutModalOpen(false);
    navigate('/login');
  };

  return (
    <div className="min-h-screen flex flex-col font-sans bg-white">
      <Header onLogout={handleLogout} />
      <main className="flex-grow">
        {children}
      </main>
      <Footer />

      {/* 강제 로그아웃 알림 모달 (다른 기기에서 로그인 감지) */}
      {forceLogoutModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div className="relative bg-white w-full max-w-sm rounded-[32px] shadow-2xl overflow-hidden animate-in zoom-in duration-200">
            <div className="p-8 text-center">
              <div className="w-16 h-16 bg-orange-50 rounded-2xl flex items-center justify-center mx-auto mb-6">
                <ShieldAlert className="w-8 h-8 text-orange-500" />
              </div>
              <h3 className="text-xl font-black text-gray-900 mb-2">다른 기기에서 로그인됨</h3>
              <p className="text-sm text-gray-500 mb-8 font-medium leading-relaxed">
                동일한 계정으로 다른 기기에서 로그인되어<br />자동 로그아웃 처리되었습니다.
              </p>
              <button
                onClick={() => {
                  closeForceLogoutModal();
                  navigate('/login');
                }}
                className="w-full py-3.5 bg-[#FF5A5A] text-white font-bold rounded-2xl hover:bg-[#FF4545] transition-all shadow-lg shadow-red-100"
              >
                로그인 페이지로 이동
              </button>
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
    </div>
  );
};
