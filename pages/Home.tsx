import React, { useRef, useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, TrendingUp, Grid, ChevronLeft, ChevronRight } from 'lucide-react';
import { CURRENT_USER } from '../services/mockData';
import { useAppContext } from '../context/AppContext';
import { ProductCard } from '../components/ProductCard';
import { CATEGORY_DATA } from '../constants';
import { motion, AnimatePresence } from 'motion/react';

const HERO_BANNERS = [
  {
    id: 1,
    image: 'https://images.unsplash.com/photo-1449034446853-66c86144b0ad?auto=format&fit=crop&w=1920&q=80',
    link: '/search'
  },
  {
    id: 2,
    image: 'https://images.unsplash.com/photo-1563013544-824ae1b704d3?auto=format&fit=crop&w=1920&q=80',
    link: '/cs'
  },
  {
    id: 3,
    image: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1920&q=80',
    link: '/search'
  }
];

export const Home: React.FC = () => {
  const navigate = useNavigate();
  const { products } = useAppContext();
  const scrollRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [startX, setStartX] = useState(0);
  const [scrollLeft, setScrollLeft] = useState(0);
  const [dragDistance, setDragDistance] = useState(0);
  const [currentBanner, setCurrentBanner] = useState(0);
  const [direction, setDirection] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setDirection(1);
      setCurrentBanner((prev) => (prev + 1) % HERO_BANNERS.length);
    }, 6000);
    return () => clearInterval(timer);
  }, []);

  const nextBanner = () => {
    setDirection(1);
    setCurrentBanner((prev) => (prev + 1) % HERO_BANNERS.length);
  };

  const prevBanner = () => {
    setDirection(-1);
    setCurrentBanner((prev) => (prev - 1 + HERO_BANNERS.length) % HERO_BANNERS.length);
  };

  const now = new Date().getTime();
  const popularProducts = [...products]
    .filter(p => 
      !CURRENT_USER.blockedUserIds?.includes(p.seller.id) && 
      !p.seller.blockedUserIds?.includes(CURRENT_USER.id) &&
      p.status === 'active' && 
      new Date(p.endTime).getTime() > now
    )
    .sort((a, b) => b.participantCount - a.participantCount)
    .slice(0, 8);

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
    const walk = (x - startX) * 1.2; // Adjusted scroll speed (was 2)
    scrollRef.current.scrollLeft = scrollLeft - walk;
    setDragDistance(Math.abs(x - startX));
  };

  const handleCategoryClick = (catId: string) => {
    // Only navigate if it wasn't a significant drag
    if (dragDistance < 5) {
      navigate(`/search?large=${encodeURIComponent(catId)}`);
    }
  };

  return (
    <div className="pb-20">
      {/* Hero Banner */}
      <section className="relative h-[360px] text-white overflow-hidden w-full bg-black">
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
            <Link to={HERO_BANNERS[currentBanner].link} className="block w-full h-full">
              <div 
                className="absolute inset-0 bg-cover bg-center" 
                style={{ backgroundImage: `url('${HERO_BANNERS[currentBanner].image}')` }}
              >
              </div>
            </Link>
          </motion.div>
        </AnimatePresence>

        {/* Banner Controls */}
        <div className="absolute bottom-10 right-10 z-20 flex items-center gap-4">
          <div className="flex gap-2">
            {HERO_BANNERS.map((_, idx) => (
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
        <section className="pb-8">
          <Link to="/signup" className="block w-full h-[200px] md:h-[250px] relative overflow-hidden rounded-[32px] group shadow-2xl">
            <div 
              className="absolute inset-0 bg-cover bg-center transition-transform duration-700 group-hover:scale-105"
              style={{ backgroundImage: `url('https://images.unsplash.com/photo-1557683316-973673baf926?auto=format&fit=crop&w=1200&q=80')` }}
            >
              <div className="absolute inset-0 bg-black/10 group-hover:bg-black/5 transition-colors"></div>
            </div>
          </Link>
        </section>
      </div>
    </div>
  );
};