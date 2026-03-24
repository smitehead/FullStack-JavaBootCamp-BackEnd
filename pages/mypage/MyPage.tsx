import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { CURRENT_USER, MOCK_PRODUCTS, MOCK_REVIEWS, MOCK_REVIEW_TAGS } from '../../services/mockData';
import { ProductCard } from '../../components/ProductCard';
import { Settings, Package, ShoppingBag, Heart, User, Wallet, PlusCircle, MinusCircle, Thermometer, MessageSquare, EyeOff, Eye, ArrowLeft, Trash2, RefreshCw, AlertTriangle, X } from 'lucide-react';
import { Review, ReviewTag, Product } from '../../types';

export const MyPage: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'selling' | 'buying' | 'wishlist' | 'reviews'>('selling');
  const [sellingFilter, setSellingFilter] = useState<'all' | 'active' | 'completed'>('all');
  const [reviews, setReviews] = useState<Review[]>(MOCK_REVIEWS);
  const [profileImage, setProfileImage] = useState(CURRENT_USER.profileImage);
  const [mySellingProducts, setMySellingProducts] = useState<Product[]>(MOCK_PRODUCTS.filter(p => p.seller.id === CURRENT_USER.id));
  const [deleteProduct, setDeleteProduct] = useState<Product | null>(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  const handleProfileImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const reader = new FileReader();
      reader.onloadend = () => {
        setProfileImage(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const triggerFileInput = () => {
    fileInputRef.current?.click();
  };

  const toggleReviewVisibility = (id: string) => {
    setReviews(prev => prev.map(r => r.id === id ? { ...r, isHidden: !r.isHidden } : r));
  };

  const handleDeleteClick = (product: Product) => {
    setDeleteProduct(product);
    setShowDeleteModal(true);
  };

  const confirmDelete = () => {
    if (!deleteProduct) return;
    setMySellingProducts(prev => prev.filter(p => p.id !== deleteProduct.id));
    setShowDeleteModal(false);
    setDeleteProduct(null);
    alert('게시글이 삭제되었습니다.');
  };

  const handleRepost = (product: Product) => {
    navigate('/register', { state: { product } });
  };

  // Filter mock products for demo
  const sellingProducts = mySellingProducts.filter(p => {
    if (sellingFilter === 'all') return true;
    return p.status === sellingFilter;
  });
  
  const buyingProducts = MOCK_PRODUCTS.filter(p => p.bids.some(b => b.bidderName === CURRENT_USER.nickname));
  const wishlistProducts = MOCK_PRODUCTS.filter(p => p.isWishlisted);

  return (
    <div className="max-w-[1200px] mx-auto px-10 py-8">
      {/* Profile Header */}
      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-6 md:p-10 mb-8 relative overflow-hidden">
        {/* Background Accent */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-50 rounded-full -mr-32 -mt-32 opacity-50 z-0"></div>
        
        <div className="relative z-10 flex flex-col md:flex-row items-center md:items-start gap-8">
          {/* Profile Image & Settings */}
          <div className="relative group">
            <div className="w-32 h-32 rounded-3xl overflow-hidden border-4 border-white shadow-xl">
              <img 
                src={profileImage || undefined} 
                alt="Profile" 
                className="w-full h-full object-cover transform group-hover:scale-110 transition-transform duration-500" 
                referrerPolicy="no-referrer"
              />
            </div>
            <button 
              onClick={triggerFileInput}
              className="absolute -bottom-2 -right-2 bg-white text-gray-700 p-2.5 rounded-2xl shadow-lg hover:bg-indigo-600 hover:text-white transition-all duration-300 border border-gray-100"
            >
              <Settings className="w-5 h-5" />
            </button>
            <input 
              type="file" 
              ref={fileInputRef} 
              onChange={handleProfileImageChange} 
              accept="image/*" 
              className="hidden" 
            />
          </div>

          {/* User Info */}
          <div className="flex-1 flex flex-col md:flex-row gap-8 w-full">
            {/* Left Column: Name, Manner, Quick Stats */}
            <div className="flex-1 flex flex-col gap-6">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-8">
                <div className="text-center md:text-left flex-shrink-0">
                  <h2 className="text-xl font-bold text-gray-900 tracking-tight mb-1">{CURRENT_USER.nickname}</h2>
                  <p className="text-sm text-gray-400 font-medium">가입일: {CURRENT_USER.joinedAt}</p>
                </div>
                
                <div className="flex-1 max-w-md">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-baseline gap-1.5">
                      <span className="text-sm font-bold text-emerald-600">매너온도</span>
                      <span className="text-xl font-black text-emerald-600">{CURRENT_USER.mannerTemp}</span>
                    </div>
                    <span className="text-xs font-medium text-gray-300">100</span>
                  </div>
                  <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-emerald-500 rounded-full"
                      style={{ width: `${(CURRENT_USER.mannerTemp / 100) * 100}%` }}
                    ></div>
                  </div>
                </div>
              </div>

              {/* Quick Stats / Buttons */}
              <div className="grid grid-cols-2 gap-4">
                <button 
                  onClick={() => setActiveTab('wishlist')}
                  className="bg-white border border-gray-100 p-4 rounded-2xl shadow-sm hover:shadow-md transition-all flex items-center gap-4 group text-left"
                >
                  <div className="bg-pink-50 p-3 rounded-xl group-hover:bg-pink-100 transition-colors">
                    <Heart className="w-6 h-6 text-pink-500 fill-pink-500" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-0.5">찜목록</p>
                    <p className="text-xl font-black text-gray-900">{wishlistProducts.length}<span className="text-sm font-medium ml-1">개</span></p>
                  </div>
                </button>
                <button 
                  onClick={() => setActiveTab('buying')}
                  className="bg-white border border-gray-100 p-4 rounded-2xl shadow-sm hover:shadow-md transition-all flex items-center gap-4 group text-left"
                >
                  <div className="bg-indigo-50 p-3 rounded-xl group-hover:bg-indigo-100 transition-colors">
                    <ShoppingBag className="w-6 h-6 text-indigo-500" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-0.5">구매/입찰 내역</p>
                    <p className="text-xl font-black text-gray-900">{buyingProducts.length}<span className="text-sm font-medium ml-1">건</span></p>
                  </div>
                </button>
              </div>
            </div>

            {/* Right Column: Points Section (Vertical) */}
            <div className="w-full md:w-72">
              <div className="bg-gray-900 rounded-3xl p-5 text-white flex flex-col justify-between h-full shadow-xl shadow-indigo-100">
                <div className="flex flex-col gap-2">
                  <Link to="/points" className="flex items-center gap-3 group cursor-pointer">
                    <div className="bg-white/10 p-2 rounded-xl group-hover:bg-white/20 transition-colors">
                      <Wallet className="w-5 h-5 text-indigo-400" />
                    </div>
                    <p className="text-[10px] font-bold text-indigo-300 uppercase tracking-widest group-hover:text-white transition-colors">보유 포인트</p>
                  </Link>
                  <h3 className="text-3xl font-black tracking-tight">{CURRENT_USER.points.toLocaleString()}<span className="text-lg ml-1 text-indigo-400">P</span></h3>
                </div>
                
                <div className="flex gap-2 mt-4">
                  <Link 
                    to="/points/charge"
                    className="flex-1 flex items-center justify-center bg-indigo-600 hover:bg-indigo-500 text-white py-3 rounded-2xl text-xs font-bold transition-all active:scale-95"
                  >
                    <span>충전</span>
                  </Link>
                  <Link 
                    to="/points/withdraw"
                    className="flex-1 flex items-center justify-center bg-white/10 hover:bg-white/20 text-white py-3 rounded-2xl text-xs font-bold transition-all active:scale-95 border border-white/10"
                  >
                    <span>출금</span>
                  </Link>
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
          <nav className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
             <button 
               onClick={() => setActiveTab('selling')}
               className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${activeTab === 'selling' ? 'bg-indigo-50 text-indigo-900' : 'text-gray-600 hover:bg-gray-50'}`}
             >
               <Package className="w-5 h-5 mr-3" /> 판매 내역
             </button>
             <button 
               onClick={() => setActiveTab('buying')}
               className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${activeTab === 'buying' ? 'bg-indigo-50 text-indigo-900' : 'text-gray-600 hover:bg-gray-50'}`}
             >
               <ShoppingBag className="w-5 h-5 mr-3" /> 구매/입찰 내역
             </button>
             <button 
               onClick={() => setActiveTab('wishlist')}
               className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${activeTab === 'wishlist' ? 'bg-indigo-50 text-indigo-900' : 'text-gray-600 hover:bg-gray-50'}`}
             >
               <Heart className="w-5 h-5 mr-3" /> 찜 목록
             </button>
             <button 
               onClick={() => setActiveTab('reviews')}
               className={`w-full flex items-center px-6 py-4 font-bold text-sm transition-colors ${activeTab === 'reviews' ? 'bg-indigo-50 text-indigo-900' : 'text-gray-600 hover:bg-gray-50'}`}
             >
               <MessageSquare className="w-5 h-5 mr-3" /> 리뷰 관리
             </button>
          </nav>
        </div>

        {/* Main List */}
        <div className="flex-1">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
            <h3 className="text-xl font-bold text-gray-900">
              {activeTab === 'selling' && '판매 내역'}
              {activeTab === 'buying' && '구매 및 입찰 내역'}
              {activeTab === 'wishlist' && '찜한 목록'}
              {activeTab === 'reviews' && '리뷰 관리'}
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
                  경매중
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
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {activeTab === 'selling' && sellingProducts.map(p => (
              <div key={p.id} className="flex flex-col gap-2">
                <ProductCard 
                  product={p} 
                  isSold={p.status === 'completed'} 
                  isConfirmed={p.id === 'prod_won_sell'} // Simulate one confirmed sale
                />
                <div className="flex gap-2">
                  <button 
                    onClick={() => handleDeleteClick(p)}
                    className="flex-1 py-2 bg-gray-100 text-gray-600 rounded-xl text-xs font-bold hover:bg-red-50 hover:text-red-500 transition-all flex items-center justify-center gap-1.5"
                  >
                    <Trash2 className="w-3.5 h-3.5" /> 삭제하기
                  </button>
                  {p.status === 'completed' && (
                    <button 
                      onClick={() => handleRepost(p)}
                      className="flex-1 py-2 bg-emerald-50 text-emerald-600 rounded-xl text-xs font-bold hover:bg-emerald-100 transition-all flex items-center justify-center gap-1.5"
                    >
                      <RefreshCw className="w-3.5 h-3.5" /> 재게시
                    </button>
                  )}
                </div>
              </div>
            ))}
            {activeTab === 'buying' && buyingProducts.map(p => (
              <ProductCard 
                key={p.id} 
                product={p} 
                isWon={p.winnerId === CURRENT_USER.id}
              />
            ))}
            {activeTab === 'wishlist' && wishlistProducts.map(p => <ProductCard key={p.id} product={p} />)}
          </div>

          {activeTab === 'reviews' && (
            <div className="flex flex-col gap-8">
              {/* Review Tags Section */}
              <div className="bg-white rounded-2xl border border-gray-100 p-6">
                <h4 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4">나에 대한 템플릿 태그</h4>
                <div className="flex flex-wrap gap-3">
                  {MOCK_REVIEW_TAGS.map(tag => (
                    <div key={tag.id} className="bg-gray-50 px-4 py-2 rounded-xl flex items-center gap-2 border border-gray-100">
                      <span className="text-sm font-medium text-gray-700">{tag.content}</span>
                      <span className="bg-indigo-100 text-indigo-600 text-xs font-black px-2 py-0.5 rounded-full">{tag.count}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Direct Reviews List */}
              <div className="flex flex-col gap-4">
                <h4 className="text-sm font-bold text-gray-400 uppercase tracking-wider">받은 거래 후기</h4>
                {reviews.length > 0 ? (
                  reviews.map(review => (
                    <div key={review.id} className={`bg-white rounded-2xl border p-6 transition-all ${review.isHidden ? 'opacity-50 border-gray-200 bg-gray-50' : 'border-gray-100 shadow-sm'}`}>
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex items-center gap-3">
                          <img src={review.authorProfileImage || undefined} alt={review.authorNickname} className="w-10 h-10 rounded-full object-cover" referrerPolicy="no-referrer" />
                          <div>
                            <p className="font-bold text-gray-900">{review.authorNickname}</p>
                            <p className="text-xs text-gray-400">{new Date(review.createdAt).toLocaleDateString()}</p>
                          </div>
                        </div>
                        <div className="flex flex-col items-end gap-2">
                          {/* Hide Button: Visible to seller (current user) or author (mocked as visible for demo) */}
                          {(review.targetUserId === CURRENT_USER.id || review.authorId === CURRENT_USER.id) && (
                            <button 
                              onClick={() => toggleReviewVisibility(review.id)}
                              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${review.isHidden ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
                            >
                              {review.isHidden ? (
                                <><Eye className="w-3.5 h-3.5" /> 보이기</>
                              ) : (
                                <><EyeOff className="w-3.5 h-3.5" /> 가리기</>
                              )}
                            </button>
                          )}
                        </div>
                      </div>
                      <div className="bg-gray-50 rounded-xl p-4 mb-3">
                        <p className="text-sm text-gray-700 leading-relaxed">
                          {review.isHidden ? '가려진 리뷰입니다.' : review.content}
                        </p>
                      </div>
                      <div className="flex items-center gap-2 text-xs font-medium text-gray-400">
                        <Package className="w-3.5 h-3.5" />
                        <span>{review.productTitle}</span>
                      </div>
                    </div>
                  ))
                ) : (
                  <EmptyState message="받은 후기가 없습니다." />
                )}
              </div>
            </div>
          )}
          
          {(activeTab === 'selling' && sellingProducts.length === 0) && <EmptyState message="해당하는 판매 상품이 없습니다." />}
          {(activeTab === 'buying' && buyingProducts.length === 0) && <EmptyState message="입찰 참여 내역이 없습니다." />}
          {(activeTab === 'wishlist' && wishlistProducts.length === 0) && <EmptyState message="찜한 상품이 없습니다." />}
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && deleteProduct && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center px-6">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowDeleteModal(false)}></div>
          <div className="bg-white rounded-[32px] w-full max-w-md relative z-10 overflow-hidden shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="p-8">
              <div className="flex items-center justify-between mb-6">
                <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${deleteProduct.participantCount > 0 ? 'bg-red-50 text-red-500' : 'bg-gray-50 text-gray-500'}`}>
                  {deleteProduct.participantCount > 0 ? <AlertTriangle className="w-6 h-6" /> : <Trash2 className="w-6 h-6" />}
                </div>
                <button onClick={() => setShowDeleteModal(false)} className="text-gray-400 hover:text-gray-600">
                  <X className="w-6 h-6" />
                </button>
              </div>

              <h3 className="text-xl font-black text-gray-900 mb-2">게시글을 삭제하시겠습니까?</h3>
              
              {deleteProduct.participantCount > 0 && deleteProduct.status !== 'completed' ? (
                <div className="bg-red-50 p-4 rounded-2xl mb-6">
                  <p className="text-sm text-red-600 font-bold leading-relaxed">
                    현재 입찰자가 {deleteProduct.participantCount}명 있습니다. <br/>
                    경매 도중 삭제 시 매너온도가 차감될 수 있습니다.
                  </p>
                </div>
              ) : (
                <p className="text-gray-500 text-sm font-medium mb-6 leading-relaxed">
                  삭제된 게시글은 복구할 수 없습니다. <br/>
                  정말로 삭제하시겠습니까?
                </p>
              )}

              <div className="flex gap-3">
                <button 
                  onClick={() => setShowDeleteModal(false)}
                  className="flex-1 py-4 bg-gray-100 text-gray-600 rounded-2xl font-bold hover:bg-gray-200 transition-all"
                >
                  취소
                </button>
                <button 
                  onClick={confirmDelete}
                  className="flex-1 py-4 bg-red-500 text-white rounded-2xl font-bold hover:bg-red-600 transition-all shadow-lg shadow-red-200"
                >
                  삭제하기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const EmptyState = ({ message }: { message: string }) => (
  <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500">
     <Package className="w-12 h-12 mx-auto mb-4 text-gray-300" />
     <p>{message}</p>
  </div>
);