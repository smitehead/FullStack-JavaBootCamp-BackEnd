import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Clock, User, Heart } from 'lucide-react';
import { Product } from '../types';
import { MOCK_PRODUCTS } from '../services/mockData';
import { formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

interface ProductCardProps {
  product: Product;
  isWon?: boolean;
  isSold?: boolean;
  isConfirmed?: boolean;
}

export const ProductCard: React.FC<ProductCardProps> = ({ 
  product, 
  isWon = false, 
  isSold = false, 
  isConfirmed = false 
}) => {
  const [timeLeft, setTimeLeft] = useState<string>('');
  const [isFinished, setIsFinished] = useState<boolean>(false);
  const [isWishlisted, setIsWishlisted] = useState<boolean>(product.isWishlisted);

  useEffect(() => {
    const updateTime = () => {
      const end = new Date(product.endTime).getTime();
      const now = new Date().getTime();
      const diff = end - now;

      if (diff <= 0) {
        setTimeLeft('종료');
        setIsFinished(true);
      } else {
        setIsFinished(false);
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
  }, [product.endTime]);

  const toggleWishlist = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const newState = !isWishlisted;
    setIsWishlisted(newState);
    
    // Update mock data
    const p = MOCK_PRODUCTS.find(item => item.id === product.id);
    if (p) {
      p.isWishlisted = newState;
      p.wishlistCount = (p.wishlistCount || 0) + (newState ? 1 : -1);
    }
  };

  const showBadge = isWon || isSold;
  const linkTo = isWon ? `/won/${product.id}` : `/products/${product.id}`;

  return (
    <Link 
      to={linkTo} 
      className={`group block bg-white rounded-[24px] shadow-sm hover:shadow-md transition-all duration-300 overflow-hidden border ${
        showBadge ? 'border-indigo-200 ring-2 ring-indigo-50' : 'border-gray-50'
      }`}
    >
      <div className="relative aspect-square m-0.5 rounded-[22px] overflow-hidden bg-gray-100 isolate [transform:translateZ(0)]">
        <img 
          src={product.images[0] || undefined} 
          alt={product.title} 
          className={`w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ${
            product.status === 'completed' ? 'grayscale-[0.3]' : ''
          }`}
        />
        
        {/* Participant Count */}
        <div className="absolute top-3 left-3 bg-black/40 text-white text-[10px] px-2 py-1 rounded-full flex items-center backdrop-blur-sm">
          <User className="w-3 h-3 mr-1" />
          {product.participantCount}명 참여
        </div>

        {/* Heart Icon - Only shows when wishlisted or on hover, and NOT won/sold */}
        {!showBadge && (
          <button 
            onClick={toggleWishlist}
            className={`absolute top-3 right-3 w-8 h-8 bg-white/80 backdrop-blur-sm rounded-full flex items-center justify-center shadow-sm hover:scale-110 transition-all z-10 ${isWishlisted ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}
          >
            <Heart className={`w-4 h-4 transition-colors ${isWishlisted ? 'text-red-500 fill-red-500' : 'text-gray-400'}`} />
          </button>
        )}

        {/* Redesigned Auction Ended Overlay */}
        {product.status === 'completed' && !showBadge && (
          <div className="absolute inset-0 bg-gray-900/70 flex flex-col items-center justify-center backdrop-blur-[2px] p-4 text-center">
            <div className="bg-white/20 backdrop-blur-md border border-white/30 px-4 py-1.5 rounded-full mb-3">
              <span className="text-white font-black text-xs tracking-tight">경매 종료</span>
            </div>
            <div className="flex flex-col items-center gap-1.5">
              <div className="flex items-center gap-1.5 text-white/90 text-[11px] font-bold">
                <User className="w-3 h-3" />
                <span>{product.seller.nickname}</span>
              </div>
              <div className="flex items-center gap-2 text-white/70 text-[10px] font-medium">
                <span>관심 {product.participantCount + 2}</span>
                <span className="w-0.5 h-0.5 bg-white/30 rounded-full"></span>
                <span>입찰 {product.participantCount}명</span>
              </div>
            </div>
          </div>
        )}

        {/* Won Success Badge */}
        {isWon && (
          <div className="absolute inset-0 bg-emerald-600/80 flex flex-col items-center justify-center backdrop-blur-sm animate-in fade-in duration-500 group-hover:bg-emerald-500/90 transition-colors cursor-pointer rounded-[inherit]">
            <div className="bg-white text-emerald-600 px-5 py-2 rounded-full font-black text-sm mb-2 shadow-xl">
              낙찰 성공!
            </div>
            <div className="text-white text-[10px] font-bold uppercase tracking-widest px-3 py-1 rounded-full border border-white/30 group-hover:bg-white group-hover:text-emerald-600 transition-all">
              판매자와 대화하기
            </div>
          </div>
        )}

        {/* Sold Badge */}
        {isSold && (
          <div className={`absolute inset-0 flex flex-col items-center justify-center backdrop-blur-sm animate-in fade-in duration-500 transition-colors cursor-pointer rounded-[inherit] ${
            isConfirmed ? 'bg-indigo-600/80 group-hover:bg-indigo-500/90' : 'bg-gray-800/80 group-hover:bg-gray-700/90'
          }`}>
            <div className={`px-5 py-2 rounded-full font-black text-sm mb-2 shadow-xl ${
              isConfirmed ? 'bg-white text-indigo-600' : 'bg-white text-gray-800'
            }`}>
              {isConfirmed ? '판매 완료!' : '낙찰 발생'}
            </div>
            <div className={`text-white text-[10px] font-bold uppercase tracking-widest px-3 py-1 rounded-full border border-white/30 group-hover:bg-white transition-all ${
              isConfirmed ? 'group-hover:text-indigo-600' : 'group-hover:text-gray-800'
            }`}>
              {isConfirmed ? '구매자와 대화하기' : '구매자 구매 확정 대기'}
            </div>
          </div>
        )}
      </div>
      
      <div className="p-4 pt-2">
        <h3 className="font-bold text-gray-900 text-base mb-1 truncate text-left">{product.title}</h3>
        
        {/* Address - Aligned Left */}
        <div className="text-[11px] text-gray-400 mb-3 text-left">
          {product.location}
        </div>
        
        <div className="flex justify-between items-end">
          <div className="text-left">
            <p className="text-[10px] text-gray-400 mb-0.5">
              {product.status === 'completed' ? '최종 낙찰가' : '현재 입찰가'}
            </p>
            <p className={`text-lg font-black ${showBadge ? 'text-indigo-600' : 'text-gray-900'}`}>
              {product.currentPrice.toLocaleString()}원
            </p>
          </div>

          {/* Timer pill */}
          <div className={`flex items-center px-2 py-1 rounded-lg text-[11px] font-bold ${isFinished ? 'bg-gray-100 text-gray-500' : 'bg-red-50 text-red-500'}`}>
            {isFinished ? (
              <div className="flex items-center gap-1">
                <span>종료</span>
              </div>
            ) : (
              <div className="flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" />
                <span>{timeLeft}</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </Link>
  );
};
