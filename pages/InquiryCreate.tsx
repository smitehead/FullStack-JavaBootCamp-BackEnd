import React, { useState, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ChevronLeft, Camera, X, AlertCircle, CheckCircle2, Send, Info } from 'lucide-react';
import { InquiryCategory, BugType } from '../types';

export const InquiryCreate: React.FC = () => {
  const navigate = useNavigate();
  const [category, setCategory] = useState<InquiryCategory | ''>('');
  const [bugType, setBugType] = useState<BugType | ''>('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [images, setImages] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const categories: InquiryCategory[] = ['버그 신고', '환불 문의', '계정 문의', '기타'];
  const bugTypes: BugType[] = ['기능 작동 오류', '화면/UI 오류', '데이터/정보 오류', '로그인/계정 문제', '속도/접속 저하', '기타'];

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      // In a real app, we would upload to a server. Here we just create object URLs.
      const newImages = Array.from(files).map(file => URL.createObjectURL(file as File));
      setImages(prev => [...prev, ...newImages].slice(0, 5)); // Limit to 5 images
    }
  };

  const removeImage = (index: number) => {
    setImages(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!category || !title || !content) return;
    
    setIsSubmitting(true);
    
    // Simulate API call
    setTimeout(() => {
      setIsSubmitting(false);
      alert('문의가 접수되었습니다. 최대한 빨리 답변해 드리겠습니다.');
      navigate('/inquiry');
    }, 1500);
  };

  return (
    <div className="max-w-[800px] mx-auto px-6 py-12">
      {/* Back Button */}
      <Link to="/inquiry" className="inline-flex items-center gap-2 text-sm font-bold text-red-500 hover:text-red-600 transition-colors mb-8">
        <ChevronLeft className="w-4 h-4" />
        문의 목록으로
      </Link>

      <div className="mb-10">
        <h1 className="text-3xl font-black text-gray-900 mb-2 tracking-tight">새 문의 작성</h1>
        <p className="text-gray-500 font-medium">불편하신 사항을 상세히 남겨주시면 신속히 확인하겠습니다.</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Category Selection */}
        <div className="space-y-4">
          <label className="block text-sm font-black text-gray-900 uppercase tracking-wider">문의 유형</label>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {categories.map(cat => (
              <button
                key={cat}
                type="button"
                onClick={() => setCategory(cat)}
                className={`px-4 py-3 rounded-2xl text-sm font-bold transition-all border ${
                  category === cat
                    ? 'bg-red-50 border-red-200 text-red-500 shadow-sm'
                    : 'bg-white border-gray-100 text-gray-400 hover:border-gray-200 hover:text-gray-600'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Bug Type Selection (Conditional) */}
        {category === '버그 신고' && (
          <div className="space-y-4 animate-in fade-in slide-in-from-top-4 duration-300">
            <label className="block text-sm font-black text-gray-900 uppercase tracking-wider">버그 유형</label>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {bugTypes.map(type => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setBugType(type)}
                  className={`px-4 py-3 rounded-2xl text-sm font-bold transition-all border ${
                    bugType === type
                      ? 'bg-blue-50 border-blue-200 text-blue-500 shadow-sm'
                      : 'bg-white border-gray-100 text-gray-400 hover:border-gray-200 hover:text-gray-600'
                  }`}
                >
                  {type}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Title and Content */}
        <div className="space-y-6">
          <div className="space-y-2">
            <label className="block text-sm font-black text-gray-900 uppercase tracking-wider">제목</label>
            <input
              type="text"
              placeholder="문의 제목을 입력해주세요"
              className="w-full px-6 py-4 bg-white border border-gray-100 rounded-2xl shadow-sm focus:ring-2 focus:ring-red-500/20 focus:border-red-500 transition-all outline-none font-medium"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <label className="block text-sm font-black text-gray-900 uppercase tracking-wider">내용</label>
            <textarea
              placeholder="문의 내용을 상세히 입력해주세요. (예: 발생 일시, 발생 경로, 오류 메시지 등)"
              className="w-full px-6 py-4 bg-white border border-gray-100 rounded-2xl shadow-sm focus:ring-2 focus:ring-red-500/20 focus:border-red-500 transition-all outline-none font-medium min-h-[200px] resize-none"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              required
            />
          </div>
        </div>

        {/* Image Attachment */}
        <div className="space-y-4">
          <label className="block text-sm font-black text-gray-900 uppercase tracking-wider">사진 첨부 (최대 5장)</label>
          <div className="flex flex-wrap gap-4">
            {images.map((img, index) => (
              <div key={index} className="relative w-24 h-24 rounded-2xl overflow-hidden border border-gray-100 group">
                <img src={img || undefined} alt={`upload-${index}`} className="w-full h-full object-cover" />
                <button
                  type="button"
                  onClick={() => removeImage(index)}
                  className="absolute top-1 right-1 w-6 h-6 bg-black/50 text-white rounded-full flex items-center justify-center hover:bg-black/70 transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ))}
            
            {images.length < 5 && (
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="w-24 h-24 rounded-2xl border-2 border-dashed border-gray-200 flex flex-col items-center justify-center gap-1 text-gray-400 hover:border-red-200 hover:text-red-500 hover:bg-red-50/30 transition-all"
              >
                <Camera className="w-6 h-6" />
                <span className="text-[10px] font-bold">사진 추가</span>
              </button>
            )}
          </div>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleImageUpload}
            className="hidden"
            accept="image/*"
            multiple
          />
          <p className="text-xs text-gray-400 font-medium flex items-center gap-1.5">
            <Info className="w-3.5 h-3.5" />
            JPG, PNG 형식의 이미지만 업로드 가능합니다.
          </p>
        </div>

        {/* Submit Button */}
        <div className="pt-6">
          <button
            type="submit"
            disabled={isSubmitting || !category || !title || !content}
            className={`w-full py-5 rounded-2xl font-black text-lg shadow-xl transition-all flex items-center justify-center gap-3 ${
              isSubmitting || !category || !title || !content
                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                : 'bg-red-500 text-white hover:bg-red-600 shadow-red-500/20 active:scale-[0.98]'
            }`}
          >
            {isSubmitting ? (
              <>
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                접수 중...
              </>
            ) : (
              <>
                <Send className="w-5 h-5" />
                문의 접수하기
              </>
            )}
          </button>
        </div>
      </form>

      {/* Guide Box */}
      <div className="mt-12 p-6 bg-gray-50 rounded-3xl border border-gray-100">
        <div className="flex items-center gap-2 mb-4 text-gray-900">
          <AlertCircle className="w-5 h-5" />
          <span className="font-bold">문의 전 확인해주세요</span>
        </div>
        <ul className="space-y-3 text-sm text-gray-500 font-medium">
          <li className="flex gap-2">
            <span className="text-red-500">•</span>
            환불 문의 시 낙찰된 상품명과 낙찰 일시를 정확히 기재해 주시면 빠른 처리가 가능합니다.
          </li>
          <li className="flex gap-2">
            <span className="text-red-500">•</span>
            버그 신고 시 오류가 발생한 화면의 스크린샷을 첨부해 주시면 원인 파악에 큰 도움이 됩니다.
          </li>
          <li className="flex gap-2">
            <span className="text-red-500">•</span>
            문의 답변은 영업일 기준 최대 48시간 이내에 완료됩니다.
          </li>
        </ul>
      </div>
    </div>
  );
};
