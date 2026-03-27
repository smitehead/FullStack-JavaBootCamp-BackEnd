import React, { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AlertTriangle, ChevronLeft, Send, Info, CheckCircle2, Camera, X, Plus } from 'lucide-react';
import { showToast } from '@/components/toastService';

export const Report: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const productId = searchParams.get('productId');
  const sellerId = searchParams.get('sellerId');
  const sellerNickname = searchParams.get('sellerNickname');

  const [reason, setReason] = useState('');
  const [details, setDetails] = useState('');
  const [images, setImages] = useState<File[]>([]);
  const [isSubmitted, setIsSubmitted] = useState(false);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const newFiles = Array.from(e.target.files);
      if (images.length + newFiles.length > 5) {
        showToast('최대 5장까지 업로드 가능합니다.', 'error');
        return;
      }
      setImages(prev => [...prev, ...newFiles]);
    }
  };

  const removeImage = (index: number) => {
    setImages(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!reason) {
      showToast('신고 사유를 선택해주세요.', 'error');
      return;
    }
    // Simulate API call
    setIsSubmitted(true);
    setTimeout(() => {
      navigate(-1);
    }, 3000);
  };

  const productReasons = [
    '부적절한 상품 (가품, 판매금지 물품 등)',
    '허위 매물 / 사기 의심',
    '전문 판매업자 의심',
    '기타 사유'
  ];

  const sellerReasons = [
    '비매너 채팅 / 욕설',
    '거래 직전 취소 / 노쇼',
    '다른 연락처 유도',
    '기타 사유'
  ];

  const reportReasons = sellerId ? sellerReasons : productReasons;

  if (isSubmitted) {
    return (
      <div className="min-h-[calc(100vh-64px)] bg-gray-50 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white p-12 rounded-[40px] shadow-2xl text-center border border-gray-100">
          <div className="w-24 h-24 bg-emerald-100 rounded-3xl flex items-center justify-center mx-auto mb-8">
            <CheckCircle2 className="w-12 h-12 text-emerald-600" />
          </div>
          <h2 className="text-3xl font-black text-gray-900 mb-4 tracking-tight">신고 접수 완료</h2>
          <p className="text-gray-500 font-medium leading-relaxed">
            신고가 정상적으로 접수되었습니다.<br />
            운영팀에서 검토 후 조치하겠습니다.<br />
            <span className="text-xs text-gray-400 mt-4 block">잠시 후 이전 페이지로 이동합니다.</span>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[calc(100vh-64px)] bg-gray-50 py-12 px-4">
      <div className="max-w-xl mx-auto">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center text-sm font-bold text-gray-400 hover:text-gray-900 transition-colors mb-6 group"
        >
          <ChevronLeft className="w-5 h-5 mr-1 group-hover:-translate-x-1 transition-transform" />
          뒤로가기
        </button>

        <div className="bg-white rounded-[32px] shadow-xl border border-gray-100 overflow-hidden">
          <div className="p-8 md:p-12">
            <div className="flex items-center gap-4 mb-8">
              <div className="bg-red-100 p-3 rounded-2xl">
                <AlertTriangle className="w-8 h-8 text-red-500" />
              </div>
              <div>
                <h1 className="text-2xl font-black text-gray-900 tracking-tight">
                  {sellerId ? `${sellerNickname}님 신고하기` : '상품 신고하기'}
                </h1>
                <p className="text-sm text-gray-500 font-medium">
                  {sellerId ? '부적절한 판매자를 신고하여 안전한 거래 환경을 만들어주세요.' : '부적절한 상품을 신고해주세요.'}
                </p>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-8">
              <div>
                <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-4 ml-1">신고 사유 선택 <span className="text-red-500">*</span></label>
                <div className="grid grid-cols-1 gap-3">
                  {reportReasons.map((r, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => setReason(r)}
                      className={`w-full p-4 text-left rounded-2xl border-2 transition-all font-bold text-sm ${reason === r ? 'border-red-500 bg-red-50 text-red-600' : 'border-gray-50 bg-gray-50 text-gray-600 hover:border-gray-200'}`}
                    >
                      {r}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-4 ml-1">상세 내용 (선택사항)</label>
                <textarea
                  rows={5}
                  className="w-full p-5 bg-gray-50 border border-gray-100 rounded-2xl text-sm focus:ring-2 focus:ring-red-500/20 focus:bg-white transition-all resize-none placeholder:text-gray-300"
                  placeholder="신고 사유에 대한 구체적인 내용을 입력해주세요."
                  value={details}
                  onChange={(e) => setDetails(e.target.value)}
                ></textarea>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-4 ml-1">
                  사진 첨부 ({images.length}/5)
                </label>
                <div className="flex flex-wrap gap-3">
                  {images.map((file, idx) => (
                    <div key={idx} className="relative w-20 h-20 rounded-xl overflow-hidden border border-gray-100 group">
                      <img
                        src={URL.createObjectURL(file) || undefined}
                        alt={`upload-${idx}`}
                        className="w-full h-full object-cover"
                      />
                      <button
                        type="button"
                        onClick={() => removeImage(idx)}
                        className="absolute top-1 right-1 bg-black/50 text-white p-1 rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </div>
                  ))}
                  {images.length < 5 && (
                    <label className="w-20 h-20 rounded-xl border-2 border-dashed border-gray-200 flex flex-col items-center justify-center gap-1 cursor-pointer hover:border-red-300 hover:bg-red-50 transition-all group">
                      <Camera className="w-6 h-6 text-gray-400 group-hover:text-red-500 transition-colors" />
                      <span className="text-[10px] font-bold text-gray-400 group-hover:text-red-500 transition-colors">사진 추가</span>
                      <input
                        type="file"
                        accept="image/*"
                        multiple
                        className="hidden"
                        onChange={handleImageChange}
                      />
                    </label>
                  )}
                </div>
              </div>

              <div className="bg-amber-50 p-6 rounded-2xl border border-amber-100">
                <div className="flex gap-3">
                  <Info className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                  <p className="text-xs text-amber-800 leading-relaxed font-medium">
                    허위 신고로 판명될 경우 서비스 이용에 제한이 있을 수 있습니다.
                    신고 내용은 운영팀에서 신중하게 검토하겠습니다.
                  </p>
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-5 bg-gray-900 text-white font-black rounded-2xl hover:bg-black transition-all shadow-xl shadow-gray-200 active:scale-95 flex items-center justify-center gap-2"
              >
                <Send className="w-5 h-5" />
                신고 접수하기
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
