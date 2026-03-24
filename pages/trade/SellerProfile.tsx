import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { MOCK_PRODUCTS, MOCK_REVIEWS, MOCK_REVIEW_TAGS } from '../../services/mockData';
import { ProductCard } from '../../components/ProductCard';
import { Package, MessageSquare, Thermometer, Calendar, User, ChevronRight, AlertCircle, Shield } from 'lucide-react';
import { Review, User as UserType, Product } from '../../types';

export const SellerProfile: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'selling' | 'reviews'>('selling');
  const [sellingFilter, setSellingFilter] = useState<'all' | 'active' | 'completed'>('all');
  const [seller, setSeller] = useState<UserType | null>(null);
  const [sellerProducts, setSellerProducts] = useState<Product[]>([]);
  const [sellerReviews, setSellerReviews] = useState<Review[]>([]);
  const [isBlocked, setIsBlocked] = useState(false);

  useEffect(() => {
    // In a real app, we would fetch the user by ID
    // For now, we find the seller from MOCK_PRODUCTS or use a default if not found
    const foundProduct = MOCK_PRODUCTS.find(p => p.seller.id === id);
    if (foundProduct) {
      setSeller(foundProduct.seller);
    } else {
      // Fallback for demo if ID doesn't match any seller in MOCK_PRODUCTS
      setSeller({
        id: id || 'unknown',
        nickname: '판매자',
        profileImage: `https://picsum.photos/seed/${id}/200/200`,
        mannerTemp: 36.5,
        joinedAt: '2023-05-20',
        points: 0
      });
    }

    // Filter products by this seller
    const products = MOCK_PRODUCTS.filter(p => p.seller.id === id);
    setSellerProducts(products);

    // Filter reviews for this seller
    const reviews = MOCK_REVIEWS.filter(r => r.targetUserId === id);
    setSellerReviews(reviews);
  }, [id]);

  if (!seller) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  const filteredProducts = sellerProducts.filter(p => {
    const now = new Date().getTime();
    const isFinished = p.status === 'completed' || new Date(p.endTime).getTime() <= now;
    
    // In other's profile, only show active products
    if (isFinished) return false;

    if (sellingFilter === 'all') return true;
    return p.status === sellingFilter;
  });

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
        {/* Background Accent */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-50 rounded-full -mr-32 -mt-32 opacity-50 z-0"></div>
        
        <div className="relative z-10 flex flex-col md:flex-row items-center md:items-start gap-8">
          {/* Profile Image */}
          <div className="w-32 h-32 rounded-3xl overflow-hidden border-4 border-white shadow-xl flex-shrink-0">
            <img 
              src={seller.profileImage || undefined} 
              alt="Profile" 
              className="w-full h-full object-cover" 
              referrerPolicy="no-referrer"
            />
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
                        alert(`${seller.nickname}님이 ${!isBlocked ? '차단' : '차단 해제'}되었습니다.`);
                      }}
                      className={`text-xs font-bold transition-colors flex items-center gap-1 ${isBlocked ? 'text-blue-500 hover:text-blue-600' : 'text-gray-400 hover:text-gray-600'}`}
                    >
                      <Shield className={`w-3 h-3 ${isBlocked ? 'fill-blue-500' : ''}`} /> {isBlocked ? '차단풀기' : '차단하기'}
                    </button>
                    <button 
                      onClick={() => navigate(`/report?sellerId=${seller.id}&sellerNickname=${encodeURIComponent(seller.nickname)}`)}
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
                  <div 
                    className="h-full bg-emerald-500 rounded-full"
                    style={{ width: `${(seller.mannerTemp / 100) * 100}%` }}
                  ></div>
                </div>
              </div>
            </div>

            {/* Quick Stats Grid */}
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
                  <p className="text-xl font-black text-gray-900">{sellerReviews.length}<span className="text-sm font-medium ml-1">건</span></p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Content Area */}
      <div className="flex flex-col md:flex-row gap-8">
        {/* Sidebar Navigation */}
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

        {/* Main List */}
        <div className="flex-1">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
            <h3 className="text-xl font-bold text-gray-900">
              {activeTab === 'selling' && '판매 상품'}
              {activeTab === 'reviews' && '거래 후기'}
            </h3>

            {activeTab === 'selling' && (
              <div className="flex bg-gray-100 p-1 rounded-xl">
                <button 
                  onClick={() => setSellingFilter('all')}
                  className={`px-4 py-1.5 text-xs font-bold rounded-lg transition-all ${sellingFilter === 'all' ? 'bg-white text-emerald-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                >
                  전체
                </button>
                <button 
                  onClick={() => setSellingFilter('active')}
                  className={`px-4 py-1.5 text-xs font-bold rounded-lg transition-all ${sellingFilter === 'active' ? 'bg-white text-emerald-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                >
                  판매중
                </button>
                <button 
                  onClick={() => setSellingFilter('completed')}
                  className={`px-4 py-1.5 text-xs font-bold rounded-lg transition-all ${sellingFilter === 'completed' ? 'bg-white text-emerald-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                >
                  판매완료
                </button>
              </div>
            )}
          </div>
          
          {activeTab === 'selling' && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredProducts.length > 0 ? (
                filteredProducts.map(p => <ProductCard key={p.id} product={p} />)
              ) : (
                <div className="col-span-full py-20 text-center bg-white rounded-3xl border border-dashed border-gray-200">
                  <Package className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                  <p className="text-gray-500 font-medium">등록된 상품이 없습니다.</p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'reviews' && (
            <div className="flex flex-col gap-8">
              {/* Review Tags Section */}
              <div className="bg-white rounded-2xl border border-gray-100 p-6">
                <h4 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4">이 판매자에 대한 태그 후기</h4>
                <div className="flex flex-wrap gap-3">
                  {MOCK_REVIEW_TAGS.map(tag => (
                    <div key={tag.id} className="bg-gray-50 px-4 py-2 rounded-xl flex items-center gap-2 border border-gray-100">
                      <span className="text-sm font-medium text-gray-700">{tag.content}</span>
                      <span className="bg-emerald-100 text-emerald-600 text-xs font-black px-2 py-0.5 rounded-full">{tag.count}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Direct Reviews List */}
              <div className="flex flex-col gap-4">
                <h4 className="text-sm font-bold text-gray-400 uppercase tracking-wider">받은 거래 후기</h4>
                {sellerReviews.length > 0 ? (
                  sellerReviews.map(review => (
                    <div key={review.id} className="bg-white rounded-2xl border border-gray-100 p-6 shadow-sm">
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex items-center gap-3">
                          <img src={review.authorProfileImage || undefined} alt={review.authorNickname} className="w-10 h-10 rounded-full object-cover" referrerPolicy="no-referrer" />
                          <div>
                            <p className="font-bold text-gray-900">{review.authorNickname}</p>
                            <p className="text-xs text-gray-400">{new Date(review.createdAt).toLocaleDateString()}</p>
                          </div>
                        </div>
                      </div>
                      <div className="bg-gray-50 rounded-xl p-4 mb-3">
                        <p className="text-sm text-gray-700 leading-relaxed">
                          {review.content}
                        </p>
                      </div>
                      <div className="flex items-center gap-2 text-xs font-medium text-gray-400">
                        <Package className="w-3.5 h-3.5" />
                        <span>{review.productTitle}</span>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="py-20 text-center bg-white rounded-3xl border border-dashed border-gray-200">
                    <MessageSquare className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                    <p className="text-gray-500 font-medium">받은 후기가 없습니다.</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
