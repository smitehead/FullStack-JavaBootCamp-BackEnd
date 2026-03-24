import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Category, CategoryItem, TransactionMethod, Product } from '../types';
import { CATEGORY_DATA } from '../constants';
import { useAppContext } from '../context/AppContext';
import { Camera, Calendar, DollarSign, MapPin, Truck, Info, AlignLeft, Package, ArrowLeft } from 'lucide-react';
import api from '../services/api';
import { ProductRequestDto } from '../types';

export const ProductRegister: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const editProduct = location.state?.product as Product | undefined;

  const [images, setImages] = useState<string[]>([]);
  const [imageFiles, setImageFiles] = useState<File[]>([]);
  const [title, setTitle] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  
  // Category State
  const [largeCat, setLargeCat] = useState<string>('');
  const [mediumCat, setMediumCat] = useState<string>('');
  const [smallCat, setSmallCat] = useState<string>('');

  // Price State
  const [startPrice, setStartPrice] = useState<number>(0);
  const [instantPrice, setInstantPrice] = useState<number>(0);
  const [isInstantPriceEnabled, setIsInstantPriceEnabled] = useState<boolean>(true);
  const [minBidIncrement, setMinBidIncrement] = useState<number>(1000);

  // Time State
  const [duration, setDuration] = useState<string>('3');
  const [endTime, setEndTime] = useState<string>('');
  const [isManualTime, setIsManualTime] = useState(false);
  const [manualDate, setManualDate] = useState<string>('');
  const [manualTime, setManualTime] = useState<string>('');

  const maxDate = new Date();
  maxDate.setMonth(maxDate.getMonth() + 3);
  const maxDateString = maxDate.toISOString().split('T')[0];

  // Transaction State
  const [methods, setMethods] = useState<{ face: boolean; delivery: boolean }>({ face: true, delivery: false });
  const [address, setAddress] = useState('');
  const [detailedAddress, setDetailedAddress] = useState('');
  const [shippingFee, setShippingFee] = useState<number>(0);
  const [isFreeShipping, setIsFreeShipping] = useState(false);

  useEffect(() => {
    if (editProduct) {
      setImages(editProduct.images);
      setTitle(editProduct.title);
      setDescription(editProduct.description);
      setStartPrice(editProduct.startPrice);
      setInstantPrice(editProduct.instantPrice || 0);
      setIsInstantPriceEnabled(!!editProduct.instantPrice);
      setMinBidIncrement(editProduct.minBidIncrement);
      setAddress(editProduct.location);
      setMethods({
        face: editProduct.transactionMethod === 'face-to-face' || editProduct.transactionMethod === 'both',
        delivery: editProduct.transactionMethod === 'delivery' || editProduct.transactionMethod === 'both'
      });
    }
  }, [editProduct]);

  const toggleMethod = (type: 'face' | 'delivery') => {
    setMethods(prev => {
      const next = { ...prev, [type]: !prev[type] };
      // Prevent unselecting both
      if (!next.face && !next.delivery) return prev;
      return next;
    });
  };

  useEffect(() => {
    if (isFreeShipping) {
      setShippingFee(0);
    }
  }, [isFreeShipping]);

  useEffect(() => {
    if (isManualTime) {
      if (manualDate && manualTime) {
        const date = new Date(`${manualDate}T${manualTime}`);
        setEndTime(date.toLocaleString('ko-KR', { 
          year: 'numeric', 
          month: 'long', 
          day: 'numeric', 
          hour: '2-digit', 
          minute: '2-digit',
          weekday: 'short'
        }));
      } else {
        setEndTime('');
      }
    } else if (duration) {
      const now = new Date();
      const days = parseInt(duration);
      const end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
      setEndTime(end.toLocaleString('ko-KR', { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric', 
        hour: '2-digit', 
        minute: '2-digit',
        weekday: 'short'
      }));
    }
  }, [duration, isManualTime, manualDate, manualTime]);

  const { user, addProduct } = useAppContext();

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (user?.isSuspended) {
      alert('계정이 정지된 상태에서는 상품을 등록할 수 없습니다.');
      return;
    }
    if (e.target.files && e.target.files[0]) {
      if (images.length >= 5) {
        alert('이미지는 최대 5장까지 등록 가능합니다.');
        return;
      }
      const file = e.target.files[0];
      const url = URL.createObjectURL(file);
      setImages([...images, url]);
      setImageFiles([...imageFiles, file]);
    }
  };

  const removeImage = (idx: number) => {
    setImages(images.filter((_, i) => i !== idx));
    setImageFiles(imageFiles.filter((_, i) => i !== idx));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (user?.isSuspended) {
      alert('계정이 정지된 상태에서는 상품을 등록할 수 없습니다.');
      return;
    }
    // Basic Price Validation
    if (startPrice < 0 || minBidIncrement < 0 || (isInstantPriceEnabled && instantPrice < 0)) {
      alert('금액은 0원 이상이어야 합니다.');
      return;
    }

    // Instant Price Validation
    if (isInstantPriceEnabled) {
      if (instantPrice <= startPrice) {
        alert('즉시 구매가는 경매 시작가보다 커야 합니다.');
        return;
      }
      if (instantPrice < startPrice + minBidIncrement) {
        alert(`즉시 구매가는 최소한 시작가(${startPrice.toLocaleString()}원) + 최소 입찰 단위(${minBidIncrement.toLocaleString()}원)인 ${(startPrice + minBidIncrement).toLocaleString()}원 이상이어야 합니다.`);
        return;
      }
    }

    // Bid Increment Validation
    if (startPrice < 10000) {
      if (minBidIncrement % 100 !== 0) {
        alert('1만원 미만 상품은 100원 단위로 입찰 단위를 설정해주세요.');
        return;
      }
    } else {
      if (minBidIncrement < 1000 || minBidIncrement % 100 !== 0) {
        alert('1만원 이상 상품은 최소 1,000원 이상이며 100원 단위로 입찰 단위를 설정해주세요.');
        return;
      }
    }

    if (!user) {
      alert('로그인이 필요합니다.');
      return;
    }

    try {
      const sellerNo = parseInt(user.id.replace(/[^0-9]/g, '') || '1', 10);
      const categoryNoStr = smallCat || mediumCat || largeCat || 'cat_1';
      const categoryNo = parseInt(categoryNoStr.replace(/[^0-9]/g, '') || '1', 10);

      const computedEndTime = isManualTime && manualDate && manualTime 
        ? new Date(`${manualDate}T${manualTime}`).toISOString().slice(0, 19)
        : new Date(Date.now() + parseInt(duration) * 24 * 60 * 60 * 1000).toISOString().slice(0, 19);

      const productDto: ProductRequestDto = {
        sellerNo,
        categoryNo,
        title,
        description,
        tradeType: methods.face ? '직거래' : '택배거래',
        tradeEmdNo: 1, // Default town code
        tradeAddrDetail: address,
        startPrice,
        buyoutPrice: isInstantPriceEnabled ? instantPrice : null,
        minBidUnit: minBidIncrement,
        endTime: computedEndTime
      };

      const formData = new FormData();
      formData.append(
        'product',
        new Blob([JSON.stringify(productDto)], { type: 'application/json' }),
        'product.json'
      );

      imageFiles.forEach(file => {
        formData.append('images', file);
      });

      await api.post('/products', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      alert('상품이 성공적으로 등록되었습니다.');
      navigate('/');
    } catch (error) {
      console.error('Failed to register product', error);
      alert('상품 등록 중 오류가 발생했습니다.');
    }
  };

  const selectedLarge = CATEGORY_DATA.find(c => c.id === largeCat);
  const selectedMedium = selectedLarge?.subCategories?.find(c => c.id === mediumCat);

  return (
    <div className="max-w-3xl mx-auto px-6 py-12">
      <div className="flex items-center gap-4 mb-10">
        <button 
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors"
        >
          <ArrowLeft className="w-6 h-6 text-gray-900" />
        </button>
        <div className="bg-emerald-100 p-3 rounded-2xl">
          <Package className="w-8 h-8 text-emerald-600" />
        </div>
        <div>
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">경매 상품 등록</h1>
          <p className="text-sm text-gray-500 font-medium">새로운 경매를 시작해보세요!</p>
        </div>
      </div>
      
      <form onSubmit={handleSubmit} className="space-y-12">
        
        {/* Image Upload */}
        <section>
          <div className="flex items-center justify-between mb-4">
            <label className="text-sm font-bold text-gray-700">상품 이미지 <span className="text-red-500">*</span></label>
            <span className="text-xs text-gray-400">{images.length} / 5</span>
          </div>
          <div className="flex flex-wrap gap-4">
            <label className="w-24 h-24 flex flex-col items-center justify-center border-2 border-dashed border-gray-300 rounded-xl cursor-pointer hover:bg-gray-50 hover:border-brand transition-all text-gray-400 hover:text-brand group">
               <Camera className="w-6 h-6 mb-1 group-hover:scale-110 transition-transform" />
               <span className="text-[10px] font-bold uppercase tracking-wider">Add Photo</span>
               <input type="file" className="hidden" accept="image/*" onChange={handleImageUpload} />
            </label>
            {images.map((img, idx) => (
              <div key={idx} className="w-24 h-24 relative rounded-xl overflow-hidden border border-gray-100 shadow-sm group">
                 <img src={img || undefined} alt="preview" className="w-full h-full object-cover" />
                 <button 
                  type="button" 
                  onClick={() => removeImage(idx)}
                  className="absolute top-1 right-1 bg-black/50 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-500"
                 >
                   <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                 </button>
              </div>
            ))}
          </div>
          <p className="text-[11px] text-gray-400 mt-3 flex items-center">
            <Info className="w-3 h-3 mr-1" /> 상품 이미지는 최대 5장까지 등록 가능합니다.
          </p>
        </section>

        {/* Basic Info & Description */}
        <section className="space-y-6">
           <div>
              <label className="block text-sm font-bold text-gray-700 mb-2">제목 <span className="text-red-500">*</span></label>
              <input 
                type="text" 
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="상품 제목을 입력해주세요" 
                className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-4 border text-sm" 
                required 
              />
            </div>

            <div>
               <label className="block text-sm font-bold text-gray-700 mb-3 flex items-center">
                 <AlignLeft className="w-4 h-4 mr-1.5 text-gray-400" /> 상품 설명 <span className="text-red-500 ml-1">*</span>
               </label>
               <textarea 
                 rows={8} 
                 value={description}
                 onChange={(e) => setDescription(e.target.value)}
                 placeholder="상품의 상태, 구매 시기, 하자 여부 등을 자세히 적어주세요. 상세한 설명은 빠른 판매와 분쟁 예방에 도움이 됩니다." 
                 className="w-full border-gray-200 rounded-2xl shadow-sm focus:ring-brand focus:border-brand p-5 border text-sm leading-relaxed"
                 required
               ></textarea>
            </div>
        </section>

        {/* Category Selection */}
        <section className="space-y-4">
          <label className="block text-sm font-bold text-gray-700">카테고리 <span className="text-red-500">*</span></label>
          <div className="grid grid-cols-3 gap-3">
            <select 
              value={largeCat} 
              onChange={(e) => { setLargeCat(e.target.value); setMediumCat(''); setSmallCat(''); }}
              className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-3 text-sm border bg-white"
            >
              <option value="">대분류</option>
              {CATEGORY_DATA.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <select 
              value={mediumCat} 
              onChange={(e) => { setMediumCat(e.target.value); setSmallCat(''); }}
              disabled={!largeCat}
              className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-3 text-sm border bg-white disabled:bg-gray-50 disabled:text-gray-400"
            >
              <option value="">중분류</option>
              {selectedLarge?.subCategories?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <select 
              value={smallCat} 
              onChange={(e) => setSmallCat(e.target.value)}
              disabled={!mediumCat}
              className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-3 text-sm border bg-white disabled:bg-gray-50 disabled:text-gray-400"
            >
              <option value="">소분류</option>
              {selectedMedium?.subCategories?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
        </section>

        {/* Auction Settings */}
        <section className="bg-gray-50 p-8 rounded-3xl space-y-8 border border-gray-100">
           <h3 className="font-bold text-lg text-gray-900 flex items-center">
              <DollarSign className="w-5 h-5 mr-2 text-brand-dark" /> 경매 설정
           </h3>
           
           <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div>
                <label className="block text-sm font-bold text-gray-700 mb-2">경매 시작가 <span className="text-red-500">*</span></label>
                <div className="relative">
                  <input 
                    type="number" 
                    value={startPrice || ''}
                    onChange={(e) => setStartPrice(Math.max(0, Number(e.target.value)))}
                    placeholder="0" 
                    step="100"
                    min="0"
                    className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-4 pr-10 border bg-white font-medium" 
                    required 
                  />
                  <span className="absolute right-4 top-0 h-full flex items-center text-gray-400 font-bold">원</span>
                </div>
              </div>
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-sm font-bold text-gray-700">즉시 구매가 (선택)</label>
                  <label className="flex items-center gap-2 cursor-pointer group">
                    <input 
                      type="checkbox" 
                      checked={!isInstantPriceEnabled}
                      onChange={(e) => setIsInstantPriceEnabled(!e.target.checked)}
                      className="w-4 h-4 rounded border-gray-300 text-brand focus:ring-brand"
                    />
                    <span className={`text-xs font-bold transition-colors ${!isInstantPriceEnabled ? 'text-brand' : 'text-gray-400 group-hover:text-gray-600'}`}>선택 안함</span>
                  </label>
                </div>
                <div className="relative">
                  <input 
                    type="number" 
                    value={isInstantPriceEnabled ? (instantPrice || '') : ''}
                    onChange={(e) => setInstantPrice(Math.max(0, Number(e.target.value)))}
                    disabled={!isInstantPriceEnabled}
                    placeholder={isInstantPriceEnabled ? "0" : "비활성화됨"} 
                    step="100"
                    min="0"
                    className={`w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-4 pr-10 border font-medium transition-all ${!isInstantPriceEnabled ? 'bg-gray-100 text-gray-400' : 'text-brand-dark bg-white'}`} 
                  />
                  <span className={`absolute right-4 top-0 h-full flex items-center font-bold transition-colors ${!isInstantPriceEnabled ? 'text-gray-300' : 'text-gray-400'}`}>원</span>
                </div>
              </div>
           </div>

           <div>
              <label className="block text-sm font-bold text-gray-700 mb-3">최소 입찰 단위 <span className="text-red-500">*</span></label>
              <div className="flex flex-col gap-3">
                <div className="relative">
                  <input 
                    type="number" 
                    value={minBidIncrement || ''}
                    onChange={(e) => setMinBidIncrement(Math.max(0, Number(e.target.value)))}
                    placeholder="예: 100" 
                    step="100"
                    min="0"
                    className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-4 pr-10 border bg-white font-medium"
                    required
                  />
                  <span className="absolute right-4 top-0 h-full flex items-center text-gray-400 font-bold">원</span>
                </div>
                <div className="bg-white/50 border border-gray-200 p-3 rounded-xl flex items-start gap-2">
                  <Info className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
                  <div className="text-[11px] text-gray-500 leading-relaxed">
                    <p className="font-bold text-gray-700 mb-0.5">입찰 단위 안내</p>
                    <p>· 1만원 미만 : 100원 단위 입찰 가능 (예: 300원, 700원)</p>
                    <p>· 1만원 이상 : 1,000원 이상 설정 가능 (100원 단위 포함 가능, 예: 1,100원)</p>
                  </div>
                </div>
              </div>
           </div>

           <div>
              <label className="block text-sm font-bold text-gray-700 mb-4">경매 종료 일시 <span className="text-red-500">*</span></label>
              <div className="flex flex-wrap gap-2 mb-4">
                {['1', '3', '7', '14'].map(d => (
                  <button
                    key={d}
                    type="button"
                    onClick={() => { setDuration(d); setIsManualTime(false); }}
                    className={`px-5 py-2.5 rounded-full text-sm font-bold transition-all border ${duration === d && !isManualTime ? 'bg-brand border-brand text-black shadow-md shadow-brand/20' : 'bg-white border-gray-200 text-gray-500 hover:border-brand hover:text-brand'}`}
                  >
                    {d === '14' ? '2주' : `${d}일`}
                  </button>
                ))}
                <button
                  type="button"
                  onClick={() => setIsManualTime(true)}
                  className={`px-5 py-2.5 rounded-full text-sm font-bold transition-all border ${isManualTime ? 'bg-brand border-brand text-black shadow-md shadow-brand/20' : 'bg-white border-gray-200 text-gray-500 hover:border-brand hover:text-brand'}`}
                >
                  직접 입력
                </button>
              </div>

              {isManualTime && (
                <div className="mb-4 animate-in fade-in slide-in-from-top-2 duration-300 grid grid-cols-2 gap-3">
                   <input 
                    type="date" 
                    value={manualDate}
                    onChange={(e) => setManualDate(e.target.value)}
                    max={maxDateString}
                    min={new Date().toISOString().split('T')[0]}
                    className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-4 border bg-white" 
                   />
                   <input 
                    type="time" 
                    value={manualTime}
                    onChange={(e) => setManualTime(e.target.value)}
                    className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-4 border bg-white" 
                   />
                </div>
              )}

              <div className="bg-gray-900 text-white p-5 rounded-2xl flex items-center justify-between shadow-lg">
                <div className="flex items-center gap-3">
                  <div className="bg-white/10 p-2.5 rounded-xl">
                    <Calendar className="w-5 h-5 text-brand" />
                  </div>
                  <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">경매 마감 예정</span>
                </div>
                <span className="text-lg font-black text-brand">{endTime || '일시를 선택해주세요'}</span>
              </div>
           </div>
        </section>

        {/* Transaction Method */}
        <section className="space-y-6">
           <label className="block text-sm font-bold text-gray-700">거래 방식 <span className="text-red-500">*</span> <span className="text-xs font-normal text-gray-400 ml-2">(중복 선택 가능)</span></label>
           <div className="grid grid-cols-2 gap-4">
              <button
                type="button"
                onClick={() => toggleMethod('face')}
                className={`flex-1 p-6 rounded-3xl border transition-all text-left ${methods.face ? 'border-brand bg-brand/5 shadow-sm' : 'border-gray-100 bg-white hover:border-gray-200'}`}
              >
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center mb-4 ${methods.face ? 'bg-brand text-black' : 'bg-gray-100 text-gray-400'}`}>
                  <MapPin className="w-5 h-5" />
                </div>
                <p className={`font-black ${methods.face ? 'text-brand-dark' : 'text-gray-900'}`}>대면 거래</p>
                <p className="text-xs text-gray-400 font-medium mt-1">직접 만나서 거래</p>
              </button>
              <button
                type="button"
                onClick={() => toggleMethod('delivery')}
                className={`flex-1 p-6 rounded-3xl border transition-all text-left ${methods.delivery ? 'border-brand bg-brand/5 shadow-sm' : 'border-gray-100 bg-white hover:border-gray-200'}`}
              >
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center mb-4 ${methods.delivery ? 'bg-brand text-black' : 'bg-gray-100 text-gray-400'}`}>
                  <Truck className="w-5 h-5" />
                </div>
                <p className={`font-black ${methods.delivery ? 'text-brand-dark' : 'text-gray-900'}`}>택배 거래</p>
                <p className="text-xs text-gray-400 font-medium mt-1">택배를 통한 비대면 거래</p>
              </button>
           </div>

           {methods.delivery && (
             <div className="space-y-3 animate-in fade-in slide-in-from-top-2 duration-300">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-bold text-gray-500">배송비 설정</label>
                  <label className="flex items-center gap-2 cursor-pointer group">
                    <input 
                      type="checkbox" 
                      checked={isFreeShipping}
                      onChange={(e) => setIsFreeShipping(e.target.checked)}
                      className="w-4 h-4 rounded border-gray-300 text-brand focus:ring-brand"
                    />
                    <span className={`text-xs font-bold transition-colors ${isFreeShipping ? 'text-brand' : 'text-gray-400 group-hover:text-gray-600'}`}>택배비 무료</span>
                  </label>
                </div>
                <div className="relative">
                  <input 
                    type="number" 
                    value={shippingFee || ''}
                    onChange={(e) => {
                      const val = Math.max(0, Number(e.target.value));
                      setShippingFee(val);
                      if (val > 0) setIsFreeShipping(false);
                    }}
                    disabled={isFreeShipping}
                    min="0"
                    placeholder={isFreeShipping ? "무료배송" : "배송비를 입력해주세요"} 
                    className={`w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-4 pr-10 border text-sm transition-all ${isFreeShipping ? 'bg-gray-50 text-gray-400' : 'bg-white'}`} 
                  />
                  <span className={`absolute right-4 top-0 h-full flex items-center font-bold transition-colors ${isFreeShipping ? 'text-gray-300' : 'text-gray-400'}`}>원</span>
                </div>
             </div>
           )}

           {methods.face && (
             <div className="space-y-3 animate-in fade-in slide-in-from-top-2 duration-300">
                <label className="block text-xs font-bold text-gray-500">거래 희망 장소</label>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <input 
                      type="text" 
                      value={address}
                      readOnly
                      placeholder="주소를 검색해주세요" 
                      className="w-full px-4 py-4 border-gray-200 rounded-xl shadow-sm bg-gray-50 text-sm border cursor-not-allowed" 
                    />
                  </div>
                  <button 
                    type="button"
                    onClick={() => setAddress('서울 강남구 테헤란로 123')}
                    className="px-6 py-4 bg-gray-900 text-white rounded-xl text-sm font-bold hover:bg-gray-800 transition-colors"
                  >
                    주소 검색
                  </button>
                </div>
                <input 
                  type="text" 
                  value={detailedAddress}
                  onChange={(e) => setDetailedAddress(e.target.value)}
                  placeholder="상세 주소 (예: 건물명, 동/호수)" 
                  className="w-full border-gray-200 rounded-xl shadow-sm focus:ring-brand focus:border-brand p-4 border text-sm bg-white" 
                />
             </div>
           )}
        </section>


        {/* Buttons */}
        <div className="flex justify-end space-x-4 pt-8 border-t border-gray-100">
           <button 
             type="button" 
             onClick={() => navigate(-1)}
             className="px-8 py-4 rounded-2xl border border-gray-200 text-gray-500 font-bold hover:bg-gray-50 transition-all active:scale-95"
           >
             취소
           </button>
           <button 
             type="submit" 
             className="px-12 py-4 rounded-2xl bg-brand text-black font-black hover:bg-brand-dark transition-all shadow-xl shadow-brand/20 active:scale-95"
           >
             등록하기
           </button>
        </div>

      </form>
    </div>
  );
};
