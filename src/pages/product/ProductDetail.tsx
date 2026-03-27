import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Product, BidHistory } from '@/types';
import { useAppContext } from '@/context/AppContext';
import { Heart, Share2, AlertTriangle, Clock, MapPin, Send, Flag, ShieldCheck, ChevronRight, TrendingUp, Info, X, Wallet, PlusCircle, ArrowLeft, Package, Users, MessageSquare, Edit2, Trash2, Reply, Sparkles } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import api from '@/services/api';
import { formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';
import { resolveImageUrls, BACKEND_URL } from '../../utils/imageUtils';
import { getMemberNo } from '@/utils/memberUtils';
import { showToast } from '@/components/toastService';
import { toast } from 'sonner';
import { motion } from 'motion/react';

export const ProductDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { products, user, updateCurrentUserPoints } = useAppContext();
  // updateCurrentUserPoints: AppContext의 user.points를 갱신해 헤더 포인트도 동시에 업데이트됨
  const [product, setProduct] = useState<Product | null | undefined>(null);
  const [bidAmount, setBidAmount] = useState<number>(0);
  const [activeTab, setActiveTab] = useState<'detail' | 'history' | 'shipping' | 'qna'>('detail');
  const [isWishlisted, setIsWishlisted] = useState(false);

  useEffect(() => {
    if (product) {
      setIsWishlisted(product.isWishlisted || false);
    }
  }, [product]);
  const [selectedImage, setSelectedImage] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      const sections = ['detail', 'history', 'shipping', 'qna'];
      const scrollPosition = window.scrollY + 300; // 탭 동기화 정확도를 위해 오프셋 증가

      for (const section of sections) {
        const element = document.getElementById(section);
        if (element) {
          const { offsetTop, offsetHeight } = element;
          if (scrollPosition >= offsetTop && scrollPosition < offsetTop + offsetHeight) {
            setActiveTab(section as any);
            break;
          }
        }
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // 입찰 관련 상태
  const [isBidModalOpen, setIsBidModalOpen] = useState(false);
  const [modalType, setModalType] = useState<'bid' | 'auto'>('bid');
  const [autoBidMaxAmount, setAutoBidMaxAmount] = useState<number>(0);
  const [showRechargePrompt, setShowRechargePrompt] = useState(false);

  const [visibleBidsCount, setVisibleBidsCount] = useState(5);
  const [timeLeft, setTimeLeft] = useState<string>('');

  // 입찰 성공 후 상품 데이터 재조회에 사용 (재호출 가능하도록 단돈으로 분리)
  const fetchProduct = React.useCallback(async () => {
    try {
      const memberNo = getMemberNo(user);
      let url = `/products/${id}`;
      if (memberNo) url += `?memberNo=${memberNo}`;

      const response = await api.get(url);
      const data = response.data;

      const isFinished = new Date(data.endTime).getTime() <= Date.now() || data.status === 'completed';

      const mappedProduct: Product = {
        id: String(data.productNo || id),
        title: data.title,
        description: data.description || '',
        category: data.category || '기타',
        seller: {
          id: String(data.seller?.sellerNo || 'seller_1'),
          nickname: data.seller?.nickname || '판매자',
          profileImage: '',
          points: 0,
          mannerTemp: data.seller?.mannerTemp || 36.5,
          joinedAt: ''
        },
        startPrice: data.startPrice || 0,
        currentPrice: data.currentPrice || 0,
        minBidIncrement: data.minBidUnit || 1000,
        startTime: new Date().toISOString(),
        endTime: data.endTime,
        images: resolveImageUrls(data.images || []),
        participantCount: data.participantCount || 0,
        bids: (data.bidHistory || []).map((b: any, idx: number) => ({
          id: `bid_${idx}`,
          bidderName: b.bidderNickname,
          amount: b.bidPrice,
          timestamp: b.bidTime
        })),
        status: isFinished ? 'completed' : 'active',
        location: data.location || '알 수 없음',
        transactionMethod: data.tradeType === '직거래' ? 'face-to-face' : 'delivery',
        isWishlisted: data.isWishlisted || false,
        wishlistCount: 0
      };

      setProduct(mappedProduct);
      setBidAmount((mappedProduct.currentPrice || 0) + (mappedProduct.minBidIncrement || 0));
      setAutoBidMaxAmount((mappedProduct.currentPrice || 0) + (mappedProduct.minBidIncrement || 0) * 5);
      setIsWishlisted(mappedProduct.isWishlisted || false);

    } catch (error) {
      console.error('Failed to fetch product details', error);
      setProduct(undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user?.id]);

  useEffect(() => {
    if (id) {
      fetchProduct();
    }
  }, [fetchProduct]);

  useEffect(() => {
    if (!product) return;
    const updateTime = () => {
      const end = new Date(product.endTime).getTime();
      const now = new Date().getTime();
      const diff = end - now;

      if (diff <= 0) {
        setTimeLeft('경매 종료');
      } else {
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);

        const h = String(hours).padStart(2, '0');
        const m = String(minutes).padStart(2, '0');
        const s = String(seconds).padStart(2, '0');

        setTimeLeft(`${h}:${m}:${s}`);
      }
    };

    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, [product]);

  // -----------------------------------------------------------------------------
  // 실시간 SSE 구독 (가격 업데이트 + 포인트 업데이트)
  // - priceUpdate: 다른 사람이 입찰하면 현재 가격 즉시 갱신
  // - pointUpdate: 내 포인트가 변경되면(입찰차감/환불) 즉시 팝업과 헤더에 반영
  // -----------------------------------------------------------------------------

  useEffect(() => {
    if (!product || !product.id) return;

    // 로그인한 사용자는 memberNo를 clientId로 사용 (AppContext SSE와 같은 채널)
    // 비로그인은 게스트 ID 사용 (priceUpdate만 수신)
    const clientId = getMemberNo(user)?.toString() ?? `guest_${id}`;
    const sseUrl = `${BACKEND_URL}/api/sse/subscribe?clientId=${clientId}`;
    const eventSource = new EventSource(sseUrl);

    // 1. 상품 가격 실시간 갱신
    eventSource.addEventListener('priceUpdate', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        if (String(data.productNo) === String(product.id)) {
          setProduct(prev => prev ? ({ ...prev, currentPrice: data.currentPrice }) : prev);
          const title = product?.title || '이 상품';
          const truncatedTitle = title.length > 10 ? title.substring(0, 10) + '..' : title;
          showToast(`'${truncatedTitle}' 상품에 새로운 입찰이 생겼습니다!`, 'bid');
        }
      } catch (e) {
        console.error('[SSE] priceUpdate 파싱 오류', e);
      }
    });

    // 2. 포인트 실시간 갱신 (입찰 차감 / 환불)
    // AppContext의 pointUpdate와 동일한 채널이지만,
    // 입찰 팝업이 열려 있는 경우 팝업 안의 포인트도 즉시 반영되게 updateCurrentUserPoints 호출
    eventSource.addEventListener('pointUpdate', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        if (data && typeof data.points === 'number') {
          // AppContext user.points 갱신 → 헤더 + 이 팝업의 {user?.points} 모두 자동 반영
          updateCurrentUserPoints(data.points);
        }
      } catch (e) {
        console.error('[SSE] pointUpdate 파싱 오류', e);
      }
    });

    // 오류 시 close() 미호출 → EventSource 스펙상 자동 재연결됨
    eventSource.onerror = (err) => {
      console.error('[SSE] 연결 오류', err);
    };

    return () => eventSource.close();
  }, [product?.id, user?.id]); // 상품 또는 로그인 사용자가 바뀔 때만 재연결

  const prevPriceRef = React.useRef<number>(0);

  useEffect(() => {
    if (product && prevPriceRef.current > 0 && product.currentPrice > prevPriceRef.current) {
      const oldPrice = prevPriceRef.current;
      const minInc = product.minBidIncrement || 0;

      // 모달에 세팅된 금액이 이전 기본 최소입찰가 그대로라면, 새로운 최소입찰가로 덮어써줍니다.
      setBidAmount(prev => prev === oldPrice + minInc ? product.currentPrice + minInc : prev);
      setAutoBidMaxAmount(prev => prev === oldPrice + minInc * 5 ? product.currentPrice + minInc * 5 : prev);
    }
    if (product) {
      prevPriceRef.current = product.currentPrice;
    }
  }, [product?.currentPrice]);

  if (product === null) return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-12 h-12 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  );

  if (product === undefined) return (
    <div className="max-w-[1200px] mx-auto px-10 py-32 text-center">
      <div className="bg-white rounded-[32px] border border-gray-100 p-20 shadow-sm">
        <Package className="w-20 h-20 text-gray-200 mx-auto mb-8" />
        <h2 className="text-2xl font-black text-gray-900 mb-4 tracking-tight">상품을 찾을 수 없거나 접근 권한이 없습니다.</h2>
        <p className="text-gray-400 font-medium mb-10 leading-relaxed">
          해당 상품이 삭제되었거나, 종료된 경매로 접근이 제한되었습니다.<br />
          판매자 또는 입찰 참여자만 종료된 상품을 확인할 수 있습니다.
        </p>
        <div className="flex flex-col items-center gap-4">
          <button
            onClick={() => navigate('/search')}
            className="px-10 py-4 bg-indigo-600 text-white font-bold rounded-2xl hover:bg-indigo-700 transition-all shadow-xl shadow-indigo-100 active:scale-95"
          >
            다른 상품 둘러보기
          </button>
          <button
            onClick={() => navigate('/search')}
            className="text-xs font-bold text-gray-400 hover:text-gray-600 underline underline-offset-4"
          >
            필터 초기화
          </button>
        </div>
      </div>
    </div>
  );

  const openBidModal = async (type: 'bid' | 'auto') => {
    if (!user) {
      alert('로그인이 필요한 서비스입니다. 로그인 페이지로 이동합니다.');
      navigate('/login');
      return;
    }
    if (user.isSuspended) {
      alert('계정이 정지된 상태에서는 입찰에 참여할 수 없습니다.');
      return;
    }

    // 백엔드에서 실시간 포인트 조회
    try {
      const memberNo = getMemberNo(user);
      if (!memberNo) return;
      const res = await api.get(`/members/${memberNo}`);
      updateCurrentUserPoints(res.data.points);
    } catch (e) {
      console.error("Failed to fetch user points", e);
    }

    setModalType(type);
    setBidAmount(product.currentPrice + product.minBidIncrement);
    setIsBidModalOpen(true);
  };

  const handleBidSubmit = async () => {
    if (user?.isSuspended) {
      alert('계정이 정지된 상태에서는 입찰에 참여할 수 없습니다.');
      setIsBidModalOpen(false);
      return;
    }
    const amountToValidate = modalType === 'bid' ? bidAmount : autoBidMaxAmount;

    if (modalType === 'bid' && bidAmount < product.currentPrice + product.minBidIncrement) {
      alert('최소 입찰 금액을 확인해주세요.');
      return;
    }

    if (modalType === 'auto' && autoBidMaxAmount <= product.currentPrice) {
      alert('자동 입찰 한도는 현재가보다 높아야 합니다.');
      return;
    }

    if (amountToValidate > (user?.points || 0)) {
      setShowRechargePrompt(true);
      return;
    }

    try {
      const memberNo = getMemberNo(user);
      if (!memberNo) return;
      await api.post('/bids', {
        productNo: product.id,
        memberNo: memberNo,
        bidPrice: amountToValidate
      });

      setIsBidModalOpen(false);
      alert(modalType === 'bid' ? '입찰이 완료되었습니다!' : '자동 입찰이 설정되었습니다!');

      // [수정] window.location.reload() 제거 - SSE가 실시간 가격 갱신을 처리함
      // 입찰 기록 등 추가 데이터를 위해 상품 정보만 재조회
      await fetchProduct();

    } catch (error: any) {
      const errorMsg = error.response?.data?.message || (typeof error.response?.data === 'string' ? error.response.data : '입찰에 실패했습니다.');
      alert(errorMsg);
    }
  };

  const chartData = [...product.bids]
    .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
    .map(bid => ({
      time: new Date(bid.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      amount: bid.amount
    }));

  // 입찰 내역이 없을 경우 시작가를 차트 기준점으로 추가
  if (chartData.length === 0) {
    chartData.push({
      time: new Date(product.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      amount: product.startPrice
    });
  }

  const toggleWishlist = async () => {
    if (!user) {
      alert('로그인이 필요한 서비스입니다.');
      navigate('/login');
      return;
    }
    try {
      const memberNo = getMemberNo(user);
      if (!memberNo) return;
      const response = await api.post(`/wishlists/toggle?memberNo=${memberNo}&productNo=${product?.id}`);
      const newState = response.data; // returns boolean

      setIsWishlisted(newState);
      setProduct(prev => prev ? ({
        ...prev,
        isWishlisted: newState,
        wishlistCount: Math.max(0, (prev.wishlistCount || 0) + (newState ? 1 : -1))
      }) : null);
    } catch (error) {
      console.error('Failed to toggle wishlist', error);
      alert('찜하기 처리 중 오류가 발생했습니다.');
    }
  };

  const isFinished = product ? (product.status === 'completed' || new Date(product.endTime).getTime() <= Date.now()) : false;

  return (
    <div className="max-w-[1200px] mx-auto px-10 py-4">
      {/* Header with Back Button */}
      <div className="flex items-center gap-4 mb-4">
        <button
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors"
        >
          <ArrowLeft className="w-6 h-6 text-gray-900" />
        </button>
      </div>

      {/* Top Section: Image & Info */}
      <div className="flex flex-col lg:flex-row gap-10 mb-12">

        {/* Left: Images */}
        <div className="lg:w-[55%] flex flex-col relative group">
          <div className="aspect-square bg-gray-100 rounded-2xl overflow-hidden shadow-sm relative flex items-center justify-center">
            {product.images && product.images.length > 0 ? (
              <img src={product.images[selectedImage]} alt={product.title} className="w-full h-full object-cover transition-transform duration-500" />
            ) : (
              <div className="flex flex-col items-center text-gray-300">
                <Package className="w-20 h-20 mb-2" />
                <span className="text-sm font-medium">등록된 이미지가 없습니다.</span>
              </div>
            )}

            {/* Navigation Arrows */}
            {product.images.length > 1 && (
              <>
                <button
                  onClick={() => setSelectedImage(prev => (prev === 0 ? product.images.length - 1 : prev - 1))}
                  className="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 bg-white/80 backdrop-blur-sm rounded-full flex items-center justify-center shadow-md opacity-0 group-hover:opacity-100 transition-opacity hover:bg-white"
                >
                  <ChevronRight className="w-6 h-6 rotate-180" />
                </button>
                <button
                  onClick={() => setSelectedImage(prev => (prev === product.images.length - 1 ? 0 : prev + 1))}
                  className="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 bg-white/80 backdrop-blur-sm rounded-full flex items-center justify-center shadow-md opacity-0 group-hover:opacity-100 transition-opacity hover:bg-white"
                >
                  <ChevronRight className="w-6 h-6" />
                </button>
              </>
            )}

            {/* Dots Indicator */}
            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
              {product.images.map((_, idx) => (
                <button
                  key={idx}
                  onClick={() => setSelectedImage(idx)}
                  className={`w-2 h-2 rounded-full transition-all ${selectedImage === idx ? 'bg-orange-500 w-4' : 'bg-white/50 hover:bg-white'}`}
                />
              ))}
            </div>
          </div>
        </div>

        {/* Right: Info & Bidding */}
        <div className="lg:w-[45%] flex flex-col">
          <div className="flex-1">
            {/* Breadcrumb */}
            <nav className="flex items-center text-xs text-gray-400 mb-3 space-x-1">
              <span>홈</span>
              <ChevronRight className="w-3 h-3" />
              <span>{product.category.split('>')[0] || product.category}</span>
              {product.category.includes('>') && (
                <>
                  <ChevronRight className="w-3 h-3" />
                  <span>{product.category.split('>')[1]}</span>
                </>
              )}
            </nav>

            {/* Title & Tags */}
            <div className="flex items-start justify-between mb-4">
              <div className="flex-1">
                <h1 className="text-3xl font-bold text-gray-900 mb-3">{product.title}</h1>
                <div className="flex flex-wrap gap-2 mb-3">
                  <span className="px-2 py-1 bg-gray-100 text-gray-600 text-[10px] font-bold rounded">직거래</span>
                  <span className="px-2 py-1 bg-gray-100 text-gray-600 text-[10px] font-bold rounded">택배거래</span>
                </div>
                <div className="flex items-center text-xs text-gray-400 space-x-3">
                  <span className="flex items-center"><MapPin className="w-3 h-3 mr-1" /> {product.location}</span>
                  <span className="flex items-center"><Clock className="w-3 h-3 mr-1" /> {formatDistanceToNow(new Date(product.startTime || Date.now()), { addSuffix: true, locale: ko })}</span>
                  <Link to={`/report?productId=${product.id}`} className="flex items-center text-gray-400 hover:text-red-500 transition-colors">
                    <Flag className="w-3 h-3 mr-1" /> 신고하기
                  </Link>
                </div>
              </div>
              <button className="p-2 text-gray-400 hover:text-gray-600 transition-colors">
                <Share2 className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Seller Profile - Redesigned to match image */}
          <div className="mb-4">
            <div className="flex justify-between items-start mb-1">
              <Link to={`/seller/${product.seller.id}`} className="font-bold text-lg text-gray-900 hover:text-orange-500 transition-colors">{product.seller.nickname}</Link>
              <Link to={`/seller/${product.seller.id}`}>
                {product.seller.profileImage ? (
                  <img src={product.seller.profileImage} alt="Seller" className="w-10 h-10 rounded-full object-cover border border-gray-100 hover:border-orange-500 transition-all" />
                ) : (
                  <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center border border-gray-100">
                    <Users className="w-6 h-6 text-gray-300" />
                  </div>
                )}
              </Link>
            </div>
            <div className="flex justify-between items-end mb-0.5">
              <div className="text-sm font-bold text-[#009678]">
                매너온도 <span className="text-lg">{product.seller.mannerTemp}</span>
              </div>
              <div className="text-xs text-gray-400">100</div>
            </div>
            <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-[#009678] rounded-full"
                style={{ width: `${product.seller.mannerTemp}%` }}
              ></div>
            </div>
          </div>

          {/* Auction Status Card - This will be at the bottom of the right column */}
          <div className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm mt-auto">
            <div className="flex justify-between items-start mb-6">
              <div>
                <p className="text-xs font-bold text-gray-400 mb-2">남은 시간</p>
                <div className="flex items-center text-2xl font-bold text-red-500">
                  <Clock className="w-6 h-6 mr-2" />
                  {timeLeft}
                </div>
              </div>
              <div className="text-right">
                <p className="text-xs font-bold text-gray-400 mb-2 flex items-center justify-end">
                  <Users className="w-3 h-3 mr-1" />
                  총 입찰 {product.bids.length}회 (참여 {product.participantCount}명)
                </p>
              </div>
            </div>

            <div className="space-y-4 mb-8">
              <div className="flex justify-between items-center text-sm">
                <span className="text-gray-500">시작 입찰가</span>
                <span className="font-bold text-gray-900">{(product.startPrice || 0).toLocaleString()} 원</span>
              </div>
              <div className="flex justify-between items-center text-sm">
                <span className="text-gray-500">최소 입찰 단위</span>
                <span className="font-bold text-gray-900">{(product.minBidIncrement || 0).toLocaleString()} 원</span>
              </div>
              <div className="flex justify-between items-center p-3 -mx-3 rounded-2xl bg-transparent border border-transparent">
                <span className="font-bold text-gray-500">현재 입찰가</span>
                <span className="text-3xl font-black text-orange-500">
                  {(product.currentPrice || 0).toLocaleString()} 원
                </span>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={toggleWishlist}
                className={`flex flex-col items-center justify-center transition-all min-w-[48px] ${isWishlisted ? 'text-red-500' : 'text-gray-300 hover:text-gray-400'}`}
              >
                <Heart className={`w-8 h-8 mb-1 ${isWishlisted ? 'fill-current' : ''}`} />
                <span className="text-xs font-bold text-gray-500">
                  {product.wishlistCount || 0}
                </span>
              </button>
              <button
                onClick={() => openBidModal('auto')}
                disabled={isFinished}
                className="flex-1 py-4 border border-orange-500 text-orange-500 font-bold rounded-xl hover:bg-orange-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                자동 입찰
              </button>
              <button
                onClick={() => openBidModal('bid')}
                disabled={isFinished}
                className="flex-1 py-4 bg-orange-500 text-white font-bold rounded-xl hover:bg-orange-600 transition-colors shadow-lg shadow-orange-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none"
              >
                입찰 참여하기
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Section: Content Sections */}
      <div className="border-t border-gray-100 pt-8">
        {/* Sticky Tabs for Navigation */}
        <div className="sticky top-[80px] md:top-[120px] bg-white z-40 border-b border-gray-100 mb-8 -mx-10 px-10 transition-all duration-300">
          <div className="flex">
            {[
              { id: 'detail', label: '상품 상세' },
              { id: 'history', label: `입찰 기록 (${product.participantCount})` },
              { id: 'shipping', label: '배송 정보' },
              { id: 'qna', label: `상품문의 (2)` }
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => {
                  setActiveTab(tab.id as any);
                  const element = document.getElementById(tab.id);
                  if (element) {
                    const headerOffset = 250; // 헤더 높이 고려한 스크롤 오프셋
                    const elementPosition = element.getBoundingClientRect().top;
                    const offsetPosition = elementPosition + window.pageYOffset - headerOffset;
                    window.scrollTo({
                      top: offsetPosition,
                      behavior: 'smooth'
                    });
                  }
                }}
                className={`pb-4 px-6 font-bold text-sm transition-all relative ${activeTab === tab.id ? 'text-gray-900' : 'text-gray-400 hover:text-gray-600'}`}
              >
                {tab.label}
                {activeTab === tab.id && <span className="absolute bottom-0 left-0 w-full h-0.5 bg-orange-500"></span>}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col lg:flex-row gap-12">
          {/* Left: Content */}
          <div className="lg:w-[70%] space-y-20">
            {/* Detail Section */}
            <div id="detail" className="scroll-mt-[300px] space-y-8">
              <div>
                <h3 className="text-lg font-bold text-gray-900 mb-4">상품 상세 설명</h3>
                <div className="text-gray-600 leading-relaxed whitespace-pre-line">
                  {product.description}
                </div>
              </div>

              {/* Chart Section */}
              <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="font-bold text-gray-800 flex items-center">
                    <TrendingUp className="w-5 h-5 mr-2 text-orange-500" /> 실시간 입찰 현황
                  </h3>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-gray-400">시작가 대비</span>
                    <span className="text-sm font-black text-orange-500">
                      +{(((product.currentPrice - product.startPrice) / product.startPrice) * 100).toFixed(1)}%
                    </span>
                  </div>
                </div>
                <div className="h-64 w-full">
                  <ResponsiveContainer width="100%" height="100%" minWidth={0}>
                    <LineChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                      <XAxis dataKey="time" tick={{ fontSize: 10 }} stroke="#9ca3af" />
                      <YAxis domain={['auto', 'auto']} tickFormatter={(value) => `${((value || 0) / 10000).toLocaleString()}만`} stroke="#9ca3af" tick={{ fontSize: 10 }} />
                      <Tooltip formatter={(value: number) => `${(value || 0).toLocaleString()}원`} contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }} />
                      <Line type="monotone" dataKey="amount" stroke="#f97316" strokeWidth={3} dot={{ r: 4, fill: '#f97316' }} activeDot={{ r: 6 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>

            {/* History Section */}
            <div id="history" className="scroll-mt-[300px] space-y-4">
              <h3 className="text-lg font-bold text-gray-900 mb-4">입찰 기록</h3>
              <div className="bg-white border border-gray-100 rounded-2xl overflow-hidden shadow-sm">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 text-gray-500">
                    <tr>
                      <th className="px-6 py-4 text-left font-bold">입찰자</th>
                      <th className="px-6 py-4 text-right font-bold">입찰 금액</th>
                      <th className="px-6 py-4 text-right font-bold">입찰 시간</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {product.bids.slice().reverse().slice(0, visibleBidsCount).map(bid => (
                      <tr key={bid.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-6 py-4 font-medium text-gray-900">{bid.bidderName}</td>
                        <td className="px-6 py-4 text-right font-bold text-orange-500">{(bid.amount || 0).toLocaleString()}원</td>
                        <td className="px-6 py-4 text-right text-gray-400">{new Date(bid.timestamp).toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {product.bids.length > visibleBidsCount && (
                  <button
                    onClick={() => setVisibleBidsCount(prev => Math.min(prev + 5, product.bids.length))}
                    className="w-full py-4 bg-gray-50 text-gray-500 font-bold text-sm hover:bg-gray-100 transition-colors border-t border-gray-100 flex items-center justify-center gap-2"
                  >
                    입찰 기록 5개 더보기 ({visibleBidsCount} / {product.bids.length})
                  </button>
                )}
              </div>
            </div>

            {/* Shipping Section */}
            <div id="shipping" className="scroll-mt-[300px] space-y-6">
              <h3 className="text-lg font-bold text-gray-900 mb-4">배송 정보</h3>
              <div className="bg-white border border-gray-100 rounded-2xl p-6 space-y-4">
                <div className="flex border-b border-gray-50 pb-4">
                  <span className="w-32 text-gray-500 font-medium">배송 방법</span>
                  <span className="text-gray-900">택배거래, 직거래 가능</span>
                </div>
                <div className="flex border-b border-gray-50 pb-4">
                  <span className="w-32 text-gray-500 font-medium">배송비</span>
                  <span className="text-gray-900">3,000원 (도서산간 지역 제외)</span>
                </div>
                <div className="flex">
                  <span className="w-32 text-gray-500 font-medium">거래 지역</span>
                  <span className="text-gray-900">{product.location}</span>
                </div>
              </div>
            </div>

            {/* QnA Section */}
            <div id="qna" className="scroll-mt-[300px] space-y-6">
              <h3 className="text-lg font-bold text-gray-900 mb-4">상품문의 (2)</h3>
              {!isFinished && (
                <div className="flex gap-3">
                  <input type="text" placeholder="상품에 대해 궁금한 점을 남겨주세요." className="flex-1 border border-gray-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none" />
                  <button className="bg-gray-900 text-white px-8 py-3 rounded-xl text-sm font-bold hover:bg-black transition-colors">등록</button>
                </div>
              )}
              <div className="space-y-6">
                <div className="border-b border-gray-100 pb-6">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-sm text-gray-900">구매희망자1</span>
                      <span className="text-[10px] px-1.5 py-0.5 bg-gray-100 text-gray-400 rounded">작성자</span>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-xs text-gray-400">2026.03.22</span>
                      <div className="flex items-center gap-2">
                        <button className="text-[10px] text-gray-400 hover:text-gray-600">수정</button>
                        <button className="text-[10px] text-gray-400 hover:text-red-400">삭제</button>
                      </div>
                    </div>
                  </div>
                  <p className="text-sm text-gray-600 mb-4">직거래 가능한가요?</p>

                  <div className="bg-gray-50 p-4 rounded-xl ml-6 border border-gray-100">
                    <div className="flex items-center justify-between mb-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-orange-600">판매자 답변</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-[10px] text-gray-400">2026.03.22</span>
                        <div className="flex items-center gap-2">
                          <button className="text-[10px] text-gray-400 hover:text-gray-600">수정</button>
                          <button className="text-[10px] text-gray-400 hover:text-red-400">삭제</button>
                        </div>
                      </div>
                    </div>
                    <p className="text-sm text-gray-600">네, 강남역 부근에서 가능합니다.</p>
                  </div>

                  {!isFinished && (
                    <button className="mt-3 ml-6 flex items-center gap-1 text-[10px] font-bold text-gray-400 hover:text-orange-500">
                      <Reply className="w-3 h-3" /> 답글 달기
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Right: Sidebar */}
          <div className="lg:w-[30%]">
            <div className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm sticky top-[160px] md:top-[200px] transition-all duration-300">
              <h4 className="font-bold text-gray-900 mb-6">안전 거래 팁</h4>
              <div className="space-y-6">
                <div className="flex gap-3">
                  <div className="w-8 h-8 bg-emerald-50 rounded-lg flex items-center justify-center flex-shrink-0">
                    <ShieldCheck className="w-5 h-5 text-emerald-500" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-gray-900 mb-1">안심 결제 사용</p>
                    <p className="text-xs text-gray-400 leading-relaxed">에스크로 시스템을 통해 안전하게 거래하세요.</p>
                  </div>
                </div>
                <div className="flex gap-3">
                  <div className="w-8 h-8 bg-orange-50 rounded-lg flex items-center justify-center flex-shrink-0">
                    <MapPin className="w-5 h-5 text-orange-500" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-gray-900 mb-1">대면 거래는 공공장소에서</p>
                    <p className="text-xs text-gray-400 leading-relaxed">밝고 안전한 장소에서 직접 상품을 확인하세요.</p>
                  </div>
                </div>
                <div className="flex gap-3">
                  <div className="w-8 h-8 bg-blue-50 rounded-lg flex items-center justify-center flex-shrink-0">
                    <Info className="w-5 h-5 text-blue-500" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-gray-900 mb-1">상품 꼼꼼히 확인</p>
                    <p className="text-xs text-gray-400 leading-relaxed">거래 완료 전에 반드시 상태를 체크하세요.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      {/* Bid Modal */}
      {isBidModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white w-full max-w-md rounded-3xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between">
              <h3 className="text-xl font-black text-gray-900">
                {modalType === 'bid' ? '입찰 참여하기' : '자동 입찰 설정'}
              </h3>
              <button onClick={() => setIsBidModalOpen(false)} className="p-2 hover:bg-gray-100 rounded-full transition-colors">
                <X className="w-6 h-6 text-gray-400" />
              </button>
            </div>

            <div className="p-8 space-y-8">
              {/* Point Info */}
              <div className="bg-gray-900 rounded-2xl p-5 text-white flex justify-between items-center shadow-lg">
                <div className="flex items-center gap-3">
                  <div className="bg-white/10 p-2 rounded-xl">
                    <Wallet className="w-5 h-5 text-orange-400" />
                  </div>
                  <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">보유 포인트</span>
                </div>
                <span className="text-xl font-black text-orange-400">{(user?.points || 0).toLocaleString()} P</span>
              </div>

              {/* Price Info */}
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-2xl border bg-gray-50 border-gray-100">
                  <p className="text-[10px] font-bold uppercase tracking-wider mb-1 text-gray-400">현재가</p>
                  <p className="text-lg font-black text-gray-900">{(product.currentPrice || 0).toLocaleString()}원</p>
                </div>
                <div className="bg-gray-50 p-4 rounded-2xl border border-gray-100">
                  <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">최소 입찰 단위</p>
                  <p className="text-lg font-black text-gray-900">{(product.minBidIncrement || 0).toLocaleString()}원</p>
                </div>
              </div>

              {/* Input Section */}
              <div className="space-y-4">
                {modalType === 'bid' ? (
                  <div>
                    <label className="block text-sm font-bold text-gray-700 mb-2">입찰 금액</label>
                    <div className="relative flex items-center">
                      <input
                        type="number"
                        value={bidAmount}
                        step={product.minBidIncrement}
                        onChange={(e) => setBidAmount(Number(e.target.value))}
                        className="w-full bg-gray-50 border-2 border-gray-100 rounded-2xl px-6 py-4 text-xl font-black focus:border-orange-500 focus:bg-white outline-none transition-all pr-12"
                      />
                      <span className="absolute right-6 font-bold text-gray-400 pointer-events-none">원</span>
                    </div>
                    <p className="text-xs text-gray-400 mt-2 flex items-center">
                      <Info className="w-3 h-3 mr-1" /> 최소 {((product.currentPrice || 0) + (product.minBidIncrement || 0)).toLocaleString()}원 이상 입찰 가능
                    </p>
                  </div>
                ) : (
                  <div>
                    <label className="block text-sm font-bold text-gray-700 mb-2">자동 입찰 한도</label>
                    <div className="relative flex items-center">
                      <input
                        type="number"
                        value={autoBidMaxAmount}
                        step={product.minBidIncrement}
                        onChange={(e) => setAutoBidMaxAmount(Number(e.target.value))}
                        className="w-full bg-gray-50 border-2 border-gray-100 rounded-2xl px-6 py-4 text-xl font-black focus:border-orange-500 focus:bg-white outline-none transition-all pr-12"
                      />
                      <span className="absolute right-6 font-bold text-gray-400 pointer-events-none">원</span>
                    </div>
                    <p className="text-xs text-gray-400 mt-2 flex items-center">
                      <Info className="w-3 h-3 mr-1" /> 설정한 금액까지 자동으로 상위 입찰을 진행합니다.
                    </p>
                  </div>
                )}
              </div>

              {/* Action Button */}
              <button
                onClick={handleBidSubmit}
                className="w-full py-5 bg-orange-500 text-white font-black rounded-2xl hover:bg-orange-600 transition-all shadow-xl shadow-orange-100 active:scale-[0.98]"
              >
                {modalType === 'bid' ? '입찰하기' : '자동 입찰 설정하기'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Recharge Prompt Modal */}
      {showRechargePrompt && (
        <div className="fixed inset-0 z-[110] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white w-full max-w-sm rounded-3xl shadow-2xl p-8 text-center animate-in zoom-in-95 duration-200">
            <div className="w-20 h-20 bg-orange-50 rounded-full flex items-center justify-center mx-auto mb-6">
              <AlertTriangle className="w-10 h-10 text-orange-500" />
            </div>
            <h3 className="text-2xl font-black text-gray-900 mb-3">포인트가 부족합니다</h3>
            <p className="text-gray-500 mb-8 leading-relaxed">
              입찰을 진행하기 위해 포인트 충전이 필요합니다.<br />
              현재 보유 포인트: <span className="text-indigo-600 font-bold">{(user?.points || 0).toLocaleString()}P</span><br />
              지금 충전하러 가시겠습니까?
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowRechargePrompt(false)}
                className="flex-1 py-4 bg-gray-100 text-gray-600 font-bold rounded-2xl hover:bg-gray-200 transition-colors"
              >
                나중에
              </button>
              <button
                onClick={() => navigate('/points/charge')}
                className="flex-1 py-4 bg-orange-500 text-white font-bold rounded-2xl hover:bg-orange-600 transition-colors shadow-lg shadow-orange-100 flex items-center justify-center gap-2"
              >
                충전하기
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
