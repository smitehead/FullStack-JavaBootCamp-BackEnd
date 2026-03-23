import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { CURRENT_USER, MOCK_PRODUCTS } from '../services/mockData';
import { Product, Order } from '../types';
import { ChevronLeft, ChevronRight, MapPin, Truck, CreditCard, MessageSquare, CheckCircle2, XCircle, Package, Info, AlertCircle } from 'lucide-react';

export const WonProductDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [product, setProduct] = useState<Product | null>(null);
  const [order, setOrder] = useState<Order | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [showPaymentConfirm, setShowPaymentConfirm] = useState(false);
  const [showPurchaseConfirm, setShowPurchaseConfirm] = useState(false);
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);
  
  // Address state
  const [address, setAddress] = useState(CURRENT_USER.address || '');
  const [detailAddress, setDetailAddress] = useState('');
  const [isEditingAddress, setIsEditingAddress] = useState(false);

  useEffect(() => {
    const foundProduct = MOCK_PRODUCTS.find(p => p.id === id);
    if (foundProduct) {
      setProduct(foundProduct);
      // Simulate an order being created when the auction ends
      setOrder({
        id: `ORD-${id}`,
        productId: foundProduct.id,
        buyerId: CURRENT_USER.id,
        sellerId: foundProduct.seller.id,
        amount: foundProduct.currentPrice,
        status: 'pending',
        transactionMethod: foundProduct.transactionMethod === 'both' ? 'delivery' : foundProduct.transactionMethod as any,
        depositedAmount: 0,
        createdAt: new Date().toISOString()
      });
    }
  }, [id]);

  // Simulate 10-day auto-confirmation
  useEffect(() => {
    if (order?.status === 'paid') {
      const timer = setTimeout(() => {
        // In a real app, this would be checked on the server
        // For demo, we'll just show a message if they stay on the page for a long time (simulated)
        // or we can just assume it's been 10 days if we want to show the flow
      }, 1000000); // Long time
      return () => clearTimeout(timer);
    }
  }, [order?.status]);

  if (!product || !order) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  const handlePayment = () => {
    if (order.transactionMethod === 'delivery' && !address) {
      alert('배송지를 입력해주세요.');
      return;
    }

    setShowPaymentConfirm(true);
  };

  const executePayment = () => {
    setShowPaymentConfirm(false);
    setIsProcessing(true);
    // Simulate payment process
    setTimeout(() => {
      setOrder(prev => prev ? { ...prev, status: 'paid', depositedAmount: prev.amount, shippingAddress: address, shippingDetail: detailAddress } : null);
      setIsProcessing(false);
      alert('확정이 되었습니다.');
    }, 1500);
  };

  const handleConfirmPurchase = () => {
    setShowPurchaseConfirm(true);
  };

  const executeConfirmPurchase = () => {
    setShowPurchaseConfirm(false);
    setOrder(prev => prev ? { ...prev, status: 'completed' } : null);
    alert('구매 확정이 완료되었습니다. 후기를 남겨주세요!');
    navigate(`/review/${order.id}?productId=${product.id}`);
  };

  const handleCancel = () => {
    setShowCancelConfirm(true);
  };

  const executeCancel = () => {
    setShowCancelConfirm(false);
    setOrder(prev => prev ? { ...prev, status: 'canceled' } : null);
    alert('결제가 취소되었습니다.');
  };

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <button 
            onClick={() => navigate(-1)}
            className="flex items-center text-sm font-bold text-gray-500 hover:text-gray-900 transition-colors group"
          >
            <ChevronLeft className="w-5 h-5 mr-1 group-hover:-translate-x-1 transition-transform" />
            뒤로가기
          </button>
          <div className="flex items-center gap-2">
            <span className={`px-4 py-1.5 rounded-full text-xs font-black uppercase tracking-wider ${
              order.status === 'pending' ? 'bg-amber-100 text-amber-600' :
              order.status === 'paid' ? 'bg-emerald-100 text-emerald-600' :
              order.status === 'completed' ? 'bg-indigo-100 text-indigo-600' :
              'bg-gray-100 text-gray-500'
            }`}>
              {order.status === 'pending' && '결제 대기'}
              {order.status === 'paid' && '결제 완료 (배송 대기)'}
              {order.status === 'completed' && '거래 완료'}
              {order.status === 'canceled' && '거래 취소'}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column: Product & Payment Info */}
          <div className="lg:col-span-2 space-y-6">
            {/* Product Summary */}
            <section className="bg-white rounded-[32px] shadow-sm border border-gray-100 p-8">
              <h3 className="text-lg font-black text-gray-900 mb-6 flex items-center gap-2">
                <Package className="w-5 h-5 text-emerald-500" /> 낙찰 상품 정보
              </h3>
              <div className="flex gap-6">
                <div className="w-24 h-24 rounded-2xl overflow-hidden flex-shrink-0 border border-gray-100">
                  <img src={product.images[0] || undefined} alt={product.title} className="w-full h-full object-cover" />
                </div>
                <div className="flex-1">
                  <h4 className="text-xl font-bold text-gray-900 mb-1">{product.title}</h4>
                  <p className="text-sm text-gray-500 mb-4 line-clamp-1">{product.description}</p>
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">최종 낙찰가</span>
                    <span className="text-2xl font-black text-emerald-600">{product.currentPrice.toLocaleString()}원</span>
                  </div>
                </div>
              </div>
            </section>

            {/* Address Section */}
            <section className="bg-white rounded-[32px] shadow-sm border border-gray-100 p-8">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-lg font-black text-gray-900">
                  배송지 정보
                </h3>
                {order.status === 'pending' && (
                  <button 
                    onClick={() => setIsEditingAddress(!isEditingAddress)}
                    className="text-xs font-bold text-indigo-600 hover:text-indigo-800"
                  >
                    {isEditingAddress ? '저장' : '변경하기'}
                  </button>
                )}
              </div>

              {order.transactionMethod === 'face-to-face' ? (
                <div className="bg-gray-50 p-6 rounded-2xl border border-dashed border-gray-200 text-center">
                  <p className="text-sm font-bold text-gray-500">대면 거래 상품입니다. 판매자와 채팅으로 장소를 정해주세요.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="flex items-start gap-4">
                    <div className="flex-1">
                      {isEditingAddress ? (
                        <div className="space-y-3">
                          <div className="flex gap-2">
                            <input 
                              type="text" 
                              value={address}
                              onChange={(e) => setAddress(e.target.value)}
                              placeholder="주소를 입력해주세요"
                              className="flex-1 p-3 bg-gray-50 border border-gray-100 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 outline-none"
                            />
                            <button className="px-4 py-2 bg-gray-900 text-white text-xs font-bold rounded-xl">검색</button>
                          </div>
                          <input 
                            type="text" 
                            value={detailAddress}
                            onChange={(e) => setDetailAddress(e.target.value)}
                            placeholder="상세 주소를 입력해주세요"
                            className="w-full p-3 bg-gray-50 border border-gray-100 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 outline-none"
                          />
                        </div>
                      ) : (
                        <div>
                          <p className="font-bold text-gray-900">{address || '주소를 등록해주세요'}</p>
                          <p className="text-sm text-gray-500 mt-1">{detailAddress || '상세 주소 없음'}</p>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="bg-amber-50 p-4 rounded-2xl flex items-start gap-3 border border-amber-100">
                    <Info className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                    <p className="text-[11px] text-amber-800 leading-relaxed">
                      택배 거래 시 정확한 주소를 입력해주세요. 결제 완료 후에는 주소 변경이 어려울 수 있습니다.
                    </p>
                  </div>
                </div>
              )}
            </section>

            {/* Payment Summary */}
            <section className="bg-white rounded-[32px] shadow-sm border border-gray-100 p-8">
              <h3 className="text-lg font-black text-gray-900 mb-6">
                결제 정보
              </h3>
              <div className="space-y-4">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500 font-medium">낙찰 금액</span>
                  <span className="text-gray-900 font-bold">{order.amount.toLocaleString()}원</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500 font-medium">배송비</span>
                  <span className="text-gray-900 font-bold">0원 (무료배송)</span>
                </div>
                <div className="pt-4 border-t border-gray-50 flex justify-between items-center">
                  <span className="text-base font-black text-gray-900">총 결제 금액</span>
                  <span className="text-2xl font-black text-[#FF5A5A]">{order.amount.toLocaleString()}원</span>
                </div>
              </div>

              {/* Moved Action Buttons */}
              <div className="mt-8 space-y-3">
                {order.status === 'pending' && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <button 
                      onClick={handlePayment}
                      disabled={isProcessing}
                      className="w-full py-5 bg-indigo-600 hover:bg-indigo-500 text-white font-black rounded-2xl transition-all shadow-lg shadow-indigo-900/20 active:scale-95 disabled:opacity-50"
                    >
                      {isProcessing ? '처리 중...' : '입찰 구매 확정 (결제)'}
                    </button>
                    <button 
                      onClick={handleCancel}
                      className="w-full py-5 bg-gray-100 hover:bg-gray-200 text-gray-600 font-bold rounded-2xl transition-all border border-gray-200"
                    >
                      낙찰 취소하기
                    </button>
                  </div>
                )}

                {order.status === 'paid' && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <button 
                      onClick={handleConfirmPurchase}
                      className="w-full py-5 bg-emerald-500 hover:bg-emerald-400 text-white font-black rounded-2xl transition-all shadow-lg shadow-emerald-900/20 active:scale-95"
                    >
                      구매 확정하기
                    </button>
                    <button 
                      onClick={() => alert('판매자에게 환불 요청을 보냈습니다.')}
                      className="w-full py-5 bg-gray-100 hover:bg-gray-200 text-gray-600 font-bold rounded-2xl transition-all border border-gray-200"
                    >
                      환불 요청
                    </button>
                  </div>
                )}

                {order.status === 'completed' && (
                  <div className="bg-emerald-50 border border-emerald-100 p-6 rounded-2xl text-center">
                    <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-3" />
                    <p className="text-sm font-bold text-emerald-600">거래가 성공적으로 완료되었습니다.</p>
                  </div>
                )}

                {order.status === 'canceled' && (
                  <div className="bg-red-50 border border-red-100 p-6 rounded-2xl text-center">
                    <XCircle className="w-10 h-10 text-red-500 mx-auto mb-3" />
                    <p className="text-sm font-bold text-red-500">거래가 취소되었습니다.</p>
                  </div>
                )}
              </div>
            </section>
          </div>

          {/* Right Column: Actions & Seller Info */}
          <div className="space-y-6">
            <div className="sticky top-24 space-y-6">
              {/* Action Card */}
              <div className="bg-gray-900 rounded-[40px] p-8 text-white shadow-2xl shadow-indigo-100">
                <div className="flex items-center gap-4 mb-8">
                  <img src={product.seller.profileImage || undefined} alt={product.seller.nickname} className="w-12 h-12 rounded-2xl object-cover border-2 border-white/20" />
                  <div>
                    <p className="text-[10px] font-bold text-indigo-300 uppercase tracking-widest">판매자</p>
                    <p className="font-bold text-white">{product.seller.nickname}</p>
                  </div>
                  <Link to={`/seller/${product.seller.id}`} className="ml-auto p-2 bg-white/5 hover:bg-white/10 rounded-xl transition-colors">
                    <ChevronRight className="w-4 h-4" />
                  </Link>
                </div>

                <div className="pt-8 border-t border-white/10">
                  <div className="space-y-3">
                    <button 
                      onClick={() => alert('판매자와의 채팅방으로 이동합니다.')}
                      className="w-full py-5 bg-white text-gray-900 font-black rounded-2xl hover:bg-gray-50 transition-all flex items-center justify-center gap-2"
                    >
                      <MessageSquare className="w-5 h-5" />
                      판매자와 채팅하기
                    </button>
                  </div>
                </div>
              </div>

              {/* Safety Notice */}
              <div className="bg-white rounded-3xl p-6 border border-gray-100">
                <div className="flex gap-3">
                  <AlertCircle className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
                  <div>
                    <p className="text-xs font-bold text-gray-900 mb-1">안전 거래 안내</p>
                    <p className="text-[11px] text-gray-500 leading-relaxed">
                      결제된 포인트는 LiveBid에서 안전하게 보관하며, 구매자가 '구매 확정'을 누른 후에 판매자에게 전달됩니다. 
                      물건을 받지 못했거나 설명과 다를 경우 환불 요청을 해주세요.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation Modals */}
      {showPaymentConfirm && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-sm w-full p-8 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="w-16 h-16 bg-indigo-50 rounded-2xl flex items-center justify-center mb-6">
              <CreditCard className="w-8 h-8 text-indigo-600" />
            </div>
            <h3 className="text-xl font-black text-gray-900 mb-2">결제를 진행할까요?</h3>
            <p className="text-sm text-gray-500 font-medium leading-relaxed mb-8">
              낙찰된 금액만큼 포인트가 차감됩니다.<br/>
              결제 후에는 취소가 어려울 수 있습니다.
            </p>
            <div className="flex gap-3">
              <button 
                onClick={() => setShowPaymentConfirm(false)}
                className="flex-1 py-4 bg-gray-100 text-gray-600 font-bold rounded-2xl hover:bg-gray-200 transition-all"
              >
                취소
              </button>
              <button 
                onClick={executePayment}
                className="flex-1 py-4 bg-indigo-600 text-white font-black rounded-2xl hover:bg-indigo-500 transition-all shadow-lg shadow-indigo-200"
              >
                결제하기
              </button>
            </div>
          </div>
        </div>
      )}

      {showPurchaseConfirm && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-sm w-full p-8 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="w-16 h-16 bg-emerald-50 rounded-2xl flex items-center justify-center mb-6">
              <CheckCircle2 className="w-8 h-8 text-emerald-600" />
            </div>
            <h3 className="text-xl font-black text-gray-900 mb-2">구매를 확정하시겠습니까?</h3>
            <p className="text-sm text-gray-500 font-medium leading-relaxed mb-2">
              물건을 정상적으로 수령하셨나요?
            </p>
            <p className="text-xs text-red-500 font-bold mb-8 flex items-center gap-1">
              <AlertCircle className="w-3.5 h-3.5" /> 구매 확정 시 다시 되돌릴 수 없습니다.
            </p>
            <div className="flex gap-3">
              <button 
                onClick={() => setShowPurchaseConfirm(false)}
                className="flex-1 py-4 bg-gray-100 text-gray-600 font-bold rounded-2xl hover:bg-gray-200 transition-all"
              >
                취소
              </button>
              <button 
                onClick={executeConfirmPurchase}
                className="flex-1 py-4 bg-emerald-500 text-white font-black rounded-2xl hover:bg-emerald-400 transition-all shadow-lg shadow-emerald-200"
              >
                확정하기
              </button>
            </div>
          </div>
        </div>
      )}

      {showCancelConfirm && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-sm w-full p-8 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="w-16 h-16 bg-red-50 rounded-2xl flex items-center justify-center mb-6">
              <XCircle className="w-8 h-8 text-red-600" />
            </div>
            <h3 className="text-xl font-black text-gray-900 mb-2">낙찰을 취소하시겠습니까?</h3>
            <p className="text-sm text-gray-500 font-medium leading-relaxed mb-2">
              정말 취소하시겠습니까?
            </p>
            <p className="text-xs text-red-500 font-bold mb-8 flex items-center gap-1">
              <AlertCircle className="w-3.5 h-3.5" /> 취소 시 서비스 이용 패널티가 부과될 수 있습니다.
            </p>
            <div className="flex gap-3">
              <button 
                onClick={() => setShowCancelConfirm(false)}
                className="flex-1 py-4 bg-gray-100 text-gray-600 font-bold rounded-2xl hover:bg-gray-200 transition-all"
              >
                아니오
              </button>
              <button 
                onClick={executeCancel}
                className="flex-1 py-4 bg-red-500 text-white font-black rounded-2xl hover:bg-red-400 transition-all shadow-lg shadow-red-200"
              >
                취소하기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
