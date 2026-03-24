import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { CURRENT_USER, MOCK_PRODUCTS, MOCK_REVIEW_TAGS } from '../../services/mockData';
import { Product } from '../../types';
import { ChevronLeft, Star, Camera, CheckCircle2, Package, User } from 'lucide-react';

export const ReviewCreate: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const productId = queryParams.get('productId');
  
  const [product, setProduct] = useState<Product | null>(null);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [content, setContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  useEffect(() => {
    if (productId) {
      const found = MOCK_PRODUCTS.find(p => p.id === productId);
      if (found) setProduct(found);
    }
  }, [productId]);

  if (!product) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  const toggleTag = (tagContent: string) => {
    setSelectedTags(prev => 
      prev.includes(tagContent) 
        ? prev.filter(t => t !== tagContent) 
        : [...prev, tagContent]
    );
  };

  const handleSubmit = () => {
    if (selectedTags.length === 0 && !content.trim()) {
      alert('후기 태그를 선택하거나 내용을 입력해주세요.');
      return;
    }

    setIsSubmitting(true);
    // Simulate API call
    setTimeout(() => {
      setIsSubmitting(false);
      setShowSuccess(true);
      setTimeout(() => {
        navigate('/mypage');
      }, 2000);
    }, 1500);
  };

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4">
      <div className="max-w-2xl mx-auto">
        {/* Header */}
        <div className="relative flex items-center justify-center mb-12">
          <button 
            onClick={() => navigate(-1)}
            className="absolute left-0 flex items-center text-sm font-bold text-gray-500 hover:text-gray-900 transition-colors group"
          >
            <ChevronLeft className="w-5 h-5 mr-1 group-hover:-translate-x-1 transition-transform" />
            뒤로가기
          </button>
          <h2 className="text-2xl font-black text-gray-900">거래 후기 작성</h2>
        </div>

        {/* Review Content */}
        <section className="bg-white rounded-[40px] shadow-sm border border-gray-100 p-10 space-y-10">
          <div className="flex items-center gap-5 pb-8 border-b border-gray-50">
            <img 
              src={product.seller.profileImage || undefined} 
              alt={product.seller.nickname} 
              className="w-16 h-16 rounded-2xl object-cover border border-gray-100 shadow-sm" 
            />
            <h3 className="text-xl font-black text-gray-900 leading-tight">
              <span className="text-indigo-600">{product.seller.nickname}</span>님과의 거래<br/>
              어떤 점이 좋았나요?
            </h3>
          </div>

          <div>
            <p className="text-sm font-bold text-gray-400 uppercase tracking-widest mb-6">거래하며 느낀 점을 태그로 선택해주세요. (중복 선택 가능)</p>
            <div className="flex flex-wrap gap-2.5">
              {MOCK_REVIEW_TAGS.map(tag => (
                <button
                  key={tag.id}
                  onClick={() => toggleTag(tag.content)}
                  className={`px-5 py-2.5 rounded-2xl text-sm font-bold transition-all border ${
                    selectedTags.includes(tag.content)
                      ? 'bg-indigo-600 border-indigo-600 text-white shadow-lg shadow-indigo-100'
                      : 'bg-gray-50 border-gray-100 text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  {tag.content}
                </button>
              ))}
            </div>
          </div>

          <div>
            <h3 className="text-lg font-black text-gray-900 mb-4">직접 후기 남기기</h3>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="판매자에게 따뜻한 후기를 남겨주세요."
              className="w-full h-40 p-6 bg-gray-50 border border-gray-100 rounded-3xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:bg-white transition-all outline-none resize-none"
            ></textarea>
          </div>

          <button
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="w-full py-5 bg-gray-900 text-white font-black rounded-2xl hover:bg-black transition-all shadow-xl active:scale-95 disabled:opacity-50"
          >
            {isSubmitting ? '등록 중...' : '후기 등록하기'}
          </button>
        </section>
      </div>

      {/* Success Modal */}
      {showSuccess && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-[40px] max-w-sm w-full p-10 text-center shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="w-20 h-20 bg-emerald-50 rounded-3xl flex items-center justify-center mx-auto mb-6">
              <CheckCircle2 className="w-10 h-10 text-emerald-500" />
            </div>
            <h3 className="text-2xl font-black text-gray-900 mb-2">후기 등록 완료!</h3>
            <p className="text-sm text-gray-500 font-medium leading-relaxed">
              소중한 후기가 등록되었습니다.<br/>
              마이페이지로 이동합니다.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
