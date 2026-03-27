import React, { useRef, useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, TrendingUp, Grid, ChevronLeft, ChevronRight } from 'lucide-react';
import api from '@/services/api';
import { ProductCard } from '@/components/ProductCard';
import { CATEGORY_DATA } from '@/constants';
import { BACKEND_URL } from '@/utils/imageUtils';
import { motion, AnimatePresence } from 'motion/react';
import { resolveImageUrl, resolveImageUrls } from '../utils/imageUtils';

export const Home: React.FC = () => {
  const navigate = useNavigate();
  const scrollRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [startX, setStartX] = useState(0);
  const [scrollLeft, setScrollLeft] = useState(0);
  const [dragDistance, setDragDistance] = useState(0);
  const [currentBanner, setCurrentBanner] = useState(0);
  const [direction, setDirection] = useState(0);
  const [popularProducts, setPopularProducts] = useState<any[]>([]);
  const [heroBanners, setHeroBanners] = useState<{ id: number; image: string; link: string }[]>([]);
  const [adBanners, setAdBanners] = useState<{ id: number; image: string; link: string }[]>([]);
  const [currentAdBanner, setCurrentAdBanner] = useState(0);

  // 배너 API 로드 (히어로 + 광고)
  useEffect(() => {
    const fetchBanners = async () => {
      try {
        const res = await api.get('/banners');
        const all = res.data || [];
        const toItem = (b: any) => ({
          id: b.bannerNo,
          image: resolveImageUrl(b.imgUrl) || b.imgUrl,
          link: b.linkUrl || '/search',
        });
        const hero = all.filter((b: any) => b.bannerType === 'hero').map(toItem);
        const ad = all.filter((b: any) => b.bannerType === 'ad').map(toItem);
        if (hero.length > 0) setHeroBanners(hero);
        if (ad.length > 0) setAdBanners(ad);
      } catch (error) {
        console.error('배너 조회 실패', error);
      }
    };
    fetchBanners();
  }, []);

  useEffect(() => {
    const fetchPopularProducts = async () => {
      try {
        // 넉넉하게 가져와서 진행중인 경매만 필터링합니다.
        const response = await api.get('/products?page=1&size=20&sort=popular');
        const content = response.data.content || response.data;
        const mapped = content.map((item: any) => ({
          ...item,
          id: String(item.id),
          seller: item.seller || { id: String(item.sellerId || 'unknown'), nickname: item.sellerNickname || '판매자' },
          images: resolveImageUrls(item.images || []),
          category: item.category || '기타',
          participantCount: item.participantCount || 0,
          currentPrice: item.currentPrice || 0,
          endTime: item.endTime || new Date().toISOString()
        }));

        const now = Date.now();
        const activeProducts = mapped.filter((p: any) => p.status === 'active' && new Date(p.endTime).getTime() > now);
        setPopularProducts(activeProducts.slice(0, 8));
      } catch (error) {
        console.error('상품 조회 실패', error);
      }
    };
    fetchPopularProducts();
  }, []);

  // 홈 페이지 실시간 가격 업데이트 SSE 연결
  useEffect(() => {
    const clientId = 'guest_home_' + Math.random().toString(36).substring(7);
    const eventSource = new EventSource(`${BACKEND_URL}/api/sse/subscribe?clientId=${clientId}`);

    eventSource.addEventListener('priceUpdate', (event: any) => {
      try {
        const data = JSON.parse(event.data);
        setPopularProducts(prev => prev.map(p =>
          String(p.id) === String(data.productNo) ? { ...p, currentPrice: data.currentPrice } : p
        ));
      } catch (e) {
        console.error("Home SSE parsing error", e);
      }
    });

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => eventSource.close();
  }, []);

  useEffect(() => {
    if (heroBanners.length === 0) return;
    const timer = setInterval(() => {
      setDirection(1);
      setCurrentBanner((prev) => (prev + 1) % heroBanners.length);
    }, 6000);
    return () => clearInterval(timer);
  }, [heroBanners.length]);

  useEffect(() => {
    if (adBanners.length <= 1) return;
    const timer = setInterval(() => {
      setCurrentAdBanner((prev) => (prev + 1) % adBanners.length);
    }, 7000);
    return () => clearInterval(timer);
  }, [adBanners.length]);

  const nextBanner = () => {
    if (heroBanners.length === 0) return;
    setDirection(1);
    setCurrentBanner((prev) => (prev + 1) % heroBanners.length);
  };

  const prevBanner = () => {
    if (heroBanners.length === 0) return;
    setDirection(-1);
    setCurrentBanner((prev) => (prev - 1 + heroBanners.length) % heroBanners.length);
  };

  // popularProducts는 위에서 API를 통해 가져와서 State로 관리합니다.

  const handleMouseDown = (e: React.MouseEvent) => {
    if (!scrollRef.current) return;
    setIsDragging(true);
    setStartX(e.pageX - scrollRef.current.offsetLeft);
    setScrollLeft(scrollRef.current.scrollLeft);
    setDragDistance(0);
  };

  const handleMouseLeave = () => {
    setIsDragging(false);
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging || !scrollRef.current) return;
    e.preventDefault();
    const x = e.pageX - scrollRef.current.offsetLeft;
    const walk = (x - startX) * 1.2; // 스크롤 속도 조정값 (기존 2에서 변경)
    scrollRef.current.scrollLeft = scrollLeft - walk;
    setDragDistance(Math.abs(x - startX));
  };

  const handleCategoryClick = (catId: string) => {
    // 드래그가 아닌 클릭일 때만 이동
    if (dragDistance < 5) {
      navigate(`/search?large=${encodeURIComponent(catId)}`);
    }
  };

  return (
    <div className="pb-20">
      {/* Hero Banner */}
      <section className="relative h-[360px] text-white overflow-hidden w-full bg-black">
        {heroBanners.length > 0 && (
          <AnimatePresence initial={false} custom={direction}>
            <motion.div
              key={currentBanner}
              custom={direction}
              variants={{
                enter: (direction: number) => ({
                  x: direction > 0 ? '100%' : '-100%',
                  opacity: 1
                }),
                center: {
                  zIndex: 1,
                  x: 0,
                  opacity: 1
                },
                exit: (direction: number) => ({
                  zIndex: 0,
                  x: direction < 0 ? '100%' : '-100%',
                  opacity: 1
                })
              }}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{
                x: { type: "tween", duration: 0.5, ease: "easeInOut" },
                opacity: { duration: 0.5 }
              }}
              className="absolute inset-0"
            >
              <Link to={heroBanners[currentBanner].link} className="block w-full h-full">
                <div
                  className="absolute inset-0 bg-cover bg-center"
                  style={{ backgroundImage: `url('${heroBanners[currentBanner].image}')` }}
                >
                </div>
              </Link>
            </motion.div>
          </AnimatePresence>
        )}

        {/* Banner Controls */}
        <div className="absolute bottom-10 right-10 z-20 flex items-center gap-4">
          <div className="flex gap-2">
            {heroBanners.map((_, idx) => (
              <button
                key={idx}
                onClick={() => setCurrentBanner(idx)}
                className={`h-1.5 rounded-full transition-all duration-300 ${currentBanner === idx ? 'w-8 bg-[#FF5A5A]' : 'w-2 bg-white/30'}`}
              />
            ))}
          </div>
          <div className="flex gap-2 ml-4">
            <button onClick={prevBanner} className="p-2 rounded-full bg-black/20 hover:bg-black/40 backdrop-blur-sm text-white transition-colors">
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button onClick={nextBanner} className="p-2 rounded-full bg-black/20 hover:bg-black/40 backdrop-blur-sm text-white transition-colors">
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      </section>

      <div className="max-w-[1200px] mx-auto px-10 space-y-12 mt-8">
        {/* Category Section */}
        <section>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-2xl font-bold text-gray-900">카테고리별 탐색</h2>
            <div className="flex items-center gap-4">
              <Link to="/search" className="text-sm font-bold text-gray-500 hover:text-gray-900 transition-colors flex items-center group">
                전체보기 <ArrowRight className="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
              </Link>
            </div>
          </div>

          <div className="relative">
            <div
              ref={scrollRef}
              onMouseDown={handleMouseDown}
              onMouseLeave={handleMouseLeave}
              onMouseUp={handleMouseUp}
              onMouseMove={handleMouseMove}
              className={`flex overflow-x-auto pb-4 gap-4 scrollbar-hide cursor-grab active:cursor-grabbing select-none`}
            >
              {CATEGORY_DATA.map(cat => (
                <button
                  key={cat.id}
                  onClick={() => handleCategoryClick(cat.id)}
                  className="flex-none w-[calc(100%/2-12px)] sm:w-[calc(100%/3-12px)] md:w-[calc(100%/4-12px)] lg:w-[calc(100%/6-14px)] flex flex-col items-center p-6 bg-white border border-gray-100 rounded-2xl shadow-sm hover:shadow-md hover:border-gray-200 transition-all group pointer-events-auto"
                >
                  <div className="w-12 h-12 bg-gray-50 rounded-xl flex items-center justify-center mb-3 group-hover:bg-gray-100 transition-colors">
                    <span className="text-2xl">📦</span>
                  </div>
                  <span className="font-bold text-gray-700 group-hover:text-gray-900 transition-colors whitespace-nowrap">{cat.name}</span>
                </button>
              ))}
            </div>
          </div>
        </section>

        {/* Popular Items */}
        <section>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-2xl font-bold text-gray-900">실시간 인기 경매</h2>
            <Link to="/search?sort=popular" className="text-sm font-bold text-gray-500 hover:text-gray-900 transition-colors flex items-center group">
              더보기 <ArrowRight className="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {popularProducts.map(product => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        </section>

        {/* Banner Ad */}
        {adBanners.length > 0 && (
          <section className="pb-8">
            <Link to={adBanners[currentAdBanner].link} className="block w-full h-[200px] md:h-[250px] relative overflow-hidden rounded-[32px] group shadow-2xl">
              <div
                className="absolute inset-0 bg-cover bg-center transition-transform duration-700 group-hover:scale-105"
                style={{ backgroundImage: `url('${adBanners[currentAdBanner].image}')` }}
              >
                <div className="absolute inset-0 bg-black/10 group-hover:bg-black/5 transition-colors"></div>
              </div>
            </Link>
          </section>
        )}
      </div>
    </div>
  );
};