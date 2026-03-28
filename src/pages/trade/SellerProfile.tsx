import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ProductCard } from '@/components/ProductCard';
import { Package, MessageSquare, ChevronRight, AlertCircle, Shield } from 'lucide-react';
import { Product } from '@/types';
import { showToast } from '@/components/toastService';
import api from '@/services/api';
import { resolveImageUrl, resolveImageUrls } from '@/utils/imageUtils';

interface SellerInfo {
  sellerNo: number;
  nickname: string;
  profileImgUrl: string | null;
  mannerTemp: number;
  joinedAt: string;
}

function mapToProduct(item: any): Product {
  return {
    ...item,
    id: String(item.id),
    title: item.title || '',
    description: '',
    category: item.category || '기타',
    seller: { id: 'unknown', nickname: '', profileImage: '', points: 0, mannerTemp: 36.5, joinedAt: '' },
    startPrice: item.currentPrice || 0,
    currentPrice: item.currentPrice || 0,
    minBidIncrement: item.minBidUnit || 1000,
    startTime: new Date().toISOString(),
    endTime: item.endTime || new Date().toISOString(),
    images: resolveImageUrls(item.images || []),
    participantCount: item.participantCount || 0,
    bids: [],
    status: item.status || 'active',
    location: item.location || '',
    transactionMethod: 'both',
    isWishlisted: false,
  };
}

export const SellerProfile: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'selling' | 'reviews'>('selling');
  const [sellingFilter, setSellingFilter] = useState<'all' | 'active' | 'completed'>('all');
  const [seller, setSeller] = useState<SellerInfo | null>(null);
  const [sellerProducts, setSellerProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [isBlocked, setIsBlocked] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    api.get(`/members/${id}/seller-profile`)
      .then(res => {
        const data = res.data;
        setSeller({
          sellerNo: data.sellerNo,
          nickname: data.nickname,
          profileImgUrl: resolveImageUrl(data.profileImgUrl),
          mannerTemp: data.mannerTemp ?? 36.5,
          joinedAt: data.joinedAt ? new Date(data.joinedAt).toLocaleDateString() : '-',
        });
        setSellerProducts((data.products || []).map(mapToProduct));
      })
      .catch(() => showToast('판매자 정보를 불러오지 못했습니다.', 'error'))
      .finally(() => setLoading(false));
  }, [id]);

  const filteredProducts = sellerProducts.filter(p => {
    if (sellingFilter === 'all') return true;
    return p.status === sellingFilter;
  });

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!seller) return null;

  return (
    <div className="max-w-[1200px] mx-auto px-10 py-8">
      {/* Breadcrumb */}
      <nav className="flex items-center text-sm text-gray-500 space-x-2 mb-8">
        <Link to="/" className="hover:text-gray-900 transition-colors">홈</Link>
        <ChevronRight className="w-4 h-4" />
        <span className="font-bold text-gray-900">판매자 프로필</span>
      </nav>

      {/* Profile Header */}
      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 md:p-10 mb-8 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-50 rounded-full -mr-32 -mt-32 opacity-50 z-0"></div>

        <div className="relative z-10 flex flex-col md:flex-row items-center md:items-start gap-8">
          {/* Profile Image */}
          <div className="w-32 h-32 rounded-3xl overflow-hidden border-4 border-white shadow-xl flex-shrink-0 bg-gray-100 flex items-center justify-center">
            {seller.profileImgUrl ? (
              <img src={seller.profileImgUrl} alt="Profile" className="w-full h-full object-cover" referrerPolicy="no-referrer" />
            ) : (
              <span className="text-4xl text-gray-400">{seller.nickname?.charAt(0) || '?'}</span>
            )}
          </div>

          {/* User Info */}
          <div className="flex-1 w-full">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-8">
              <div className="text-center md:text-left">
                <h2 className="text-xl font-bold text-gray-900 tracking-tight mb-1">{seller.nickname}</h2>
                <div className="flex items-center justify-center md:justify-start gap-3 text-sm text-gray-400 font-medium">
                  <span>가입일: {seller.joinedAt}</span>
                  <span className="w-1 h-1 bg-gray-200 rounded-full"></span>
                  <span>판매상품 {sellerProducts.length}개</span>
                  <div className="flex items-center gap-2 ml-2">
                    <button
                      onClick={() => {
                        setIsBlocked(!isBlocked);
                        showToast(`${seller.nickname}님이 ${!isBlocked ? '차단' : '차단 해제'}되었습니다.`, 'success');
                      }}
                      className={`text-xs font-bold transition-colors flex items-center gap-1 ${isBlocked ? 'text-blue-500 hover:text-blue-600' : 'text-gray-400 hover:text-gray-600'}`}
                    >
                      <Shield className={`w-3 h-3 ${isBlocked ? 'fill-blue-500' : ''}`} /> {isBlocked ? '차단풀기' : '차단하기'}
                    </button>
                    <button
                      onClick={() => navigate(`/report?sellerId=${seller.sellerNo}&sellerNickname=${encodeURIComponent(seller.nickname)}`)}
                      className="text-xs font-bold text-red-400 hover:text-red-600 transition-colors flex items-center gap-1"
                    >
                      <AlertCircle className="w-3 h-3" /> 신고하기
                    </button>
                  </div>
                </div>
              </div>

              <div className="flex-1 max-w-md w-full">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-baseline gap-1.5">
                    <span className="text-sm font-bold text-emerald-600">매너온도</span>
                    <span className="text-xl font-black text-emerald-600">{seller.mannerTemp}</span>
                  </div>
                  <span className="text-xs font-medium text-gray-300">100</span>
                </div>
                <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${(seller.mannerTemp / 100) * 100}%` }}></div>
                </div>
              </div>
            </div>

            {/* Quick Stats */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="bg-gray-50 p-4 rounded-2xl border border-gray-100 flex items-center gap-4">
                <div className="bg-emerald-100 p-3 rounded-xl">
                  <Package className="w-6 h-6 text-emerald-600" />
                </div>
                <div>
                  <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-0.5">전체 판매</p>
                  <p className="text-xl font-black text-gray-900">{sellerProducts.length}<span className="text-sm font-medium ml-1">건</span></p>
                </div>
              </div>
              <div className="bg-gray-50 p-4 rounded-2xl border border-gray-100 flex items-center gap-4">
                <div className="bg-indigo-100 p-3 rounded-xl">
                  <MessageSquare className="w-6 h-6 text-indigo-600" />
                </div>
                <div>
                  <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-0.5">받은 후기</p>
                  <p className="text-xl font-black text-gray-900">0<span className="text-sm font-medium ml-1">건</span></p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Content Area */}
      <div className="flex flex-col md:flex-row gap-8">
        {/* Sidebar */}
        <div className="w-full md:w-64 flex-shrink-0">
          <nav className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden sticky top-24">
            <button
              onClick={() => setActiveTab('selling')}
              className={`w-full flex items-center px-6 py-4 font-bold text-sm border-l-4 transition-colors ${activeTab === 'selling' ? 'border-emerald-600 bg-emerald-50 text-emerald-900' : 'border-transparent text-gray-600 hover:bg-gray-50'}`}
            >
              <Package className="w-5 h-5 mr-3" /> 판매 상품
            </button>
            <button
              onClick={() => setActiveTab('reviews')}
              className={`w-full flex items-center px-6 py-4 font-bold text-sm border-l-4 transition-colors ${activeTab === 'reviews' ? 'border-emerald-600 bg-emerald-50 text-emerald-900' : 'border-transparent text-gray-600 hover:bg-gray-50'}`}
            >
              <MessageSquare className="w-5 h-5 mr-3" /> 거래 후기
            </button>
          </nav>
        </div>

        {/* Main */}
        <div className="flex-1">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
            <h3 className="text-xl font-bold text-gray-900">
              {activeTab === 'selling' ? '판매 상품' : '거래 후기'}
            </h3>

            {activeTab === 'selling' && (
              <div className="flex bg-gray-100 p-1 rounded-xl">
                {(['all', 'active', 'completed'] as const).map(f => (
                  <button key={f} onClick={() => setSellingFilter(f)}
                    className={`px-4 py-1.5 text-xs font-bold rounded-lg transition-all ${sellingFilter === f ? 'bg-white text-emerald-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>
                    {f === 'all' ? '전체' : f === 'active' ? '판매중' : '판매완료'}
                  </button>
                ))}
              </div>
            )}
          </div>

          {activeTab === 'selling' && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredProducts.length > 0 ? (
                filteredProducts.map(p => <ProductCard key={p.id} product={p} isSold={p.status === 'completed'} />)
              ) : (
                <div className="col-span-full py-20 text-center bg-white rounded-3xl border border-dashed border-gray-200">
                  <Package className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                  <p className="text-gray-500 font-medium">등록된 상품이 없습니다.</p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'reviews' && (
            <div className="py-20 text-center bg-white rounded-3xl border border-dashed border-gray-200">
              <MessageSquare className="w-12 h-12 mx-auto mb-4 text-gray-300" />
              <p className="text-gray-500 font-medium">받은 후기가 없습니다.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
