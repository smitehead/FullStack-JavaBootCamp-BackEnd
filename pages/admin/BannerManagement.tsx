import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Edit2, Image, Link as LinkIcon, CheckCircle2, XCircle, ChevronLeft, ChevronRight, Upload } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { HeroBanner, BannerType } from '@/types';
import api from '@/services/api';
import { resolveImageUrl } from '@/utils/imageUtils';

export const BannerManagement: React.FC = () => {
  const [banners, setBanners] = useState<HeroBanner[]>([]);
  const [activeTab, setActiveTab] = useState<BannerType>('hero');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [editingBanner, setEditingBanner] = useState<HeroBanner | null>(null);
  const [bannerToDelete, setBannerToDelete] = useState<number | null>(null);

  // Form States
  const [imgUrl, setImgUrl] = useState('');
  const [linkUrl, setLinkUrl] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [isActive, setIsActive] = useState(1);
  const [bannerType, setBannerType] = useState<BannerType>('hero');
  const [sortOrder, setSortOrder] = useState(0);
  const [endAt, setEndAt] = useState('');

  // Preview States
  const [previewIndex, setPreviewIndex] = useState(0);
  const [direction, setDirection] = useState(0);

  // API: 전체 배너 목록 조회
  const fetchBanners = async () => {
    try {
      const res = await api.get('/banners/all');
      setBanners(res.data);
    } catch (e) {
      console.error('배너 목록 조회 실패', e);
    }
  };

  useEffect(() => {
    fetchBanners();
  }, []);

  const filteredBanners = banners.filter(b => b.bannerType === activeTab);
  const activeBannersForPreview = banners.filter(b => b.isActive === 1 && b.bannerType === activeTab);

  useEffect(() => {
    if (activeBannersForPreview.length <= 1) {
      setPreviewIndex(0);
      return;
    }
    const timer = setInterval(() => {
      setDirection(1);
      setPreviewIndex((prev) => (prev + 1) % activeBannersForPreview.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [activeBannersForPreview.length]);

  const handleOpenModal = (banner?: HeroBanner) => {
    if (banner) {
      setEditingBanner(banner);
      setImgUrl(banner.imgUrl);
      setLinkUrl(banner.linkUrl || '');
      setIsActive(banner.isActive);
      setBannerType(banner.bannerType);
      setSortOrder(banner.sortOrder ?? 0);
      setEndAt(banner.endAt ? banner.endAt.slice(0, 16) : '');
    } else {
      setEditingBanner(null);
      setImgUrl('');
      setLinkUrl('');
      setIsActive(1);
      setBannerType(activeTab);
      setSortOrder(0);
      setEndAt('');
    }
    setIsModalOpen(true);
  };

  // API: 배너 등록/수정
  const handleSave = async () => {
    try {
      const body = { bannerType, imgUrl, linkUrl, isActive, sortOrder, endAt: endAt || null };
      if (editingBanner) {
        await api.put(`/banners/${editingBanner.bannerNo}`, body);
      } else {
        await api.post('/banners', body);
      }
      setIsModalOpen(false);
      fetchBanners();
    } catch (e) {
      console.error('배너 저장 실패', e);
    }
  };

  const handleDelete = (bannerNo: number) => {
    setBannerToDelete(bannerNo);
    setIsDeleteModalOpen(true);
  };

  // API: 배너 삭제
  const confirmDelete = async () => {
    if (bannerToDelete) {
      try {
        await api.delete(`/banners/${bannerToDelete}`);
        setBannerToDelete(null);
        setIsDeleteModalOpen(false);
        fetchBanners();
      } catch (e) {
        console.error('배너 삭제 실패', e);
      }
    }
  };

  // API: 배너 활성화/비활성화 토글
  const toggleActive = async (bannerNo: number) => {
    try {
      await api.patch(`/banners/${bannerNo}/toggle`);
      fetchBanners();
    } catch (e) {
      console.error('배너 토글 실패', e);
    }
  };

  const variants = {
    enter: (direction: number) => ({
      x: direction > 0 ? 500 : -500,
      opacity: 0
    }),
    center: {
      zIndex: 1,
      x: 0,
      opacity: 1
    },
    exit: (direction: number) => ({
      zIndex: 0,
      x: direction < 0 ? 500 : -500,
      opacity: 0
    })
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-gray-900 tracking-tight">배너 관리</h1>
          <p className="text-gray-500 mt-1 text-[11px] font-medium">메인 화면의 히어로 배너와 하단 광고 배너를 관리합니다.</p>
        </div>
        <button
          onClick={() => handleOpenModal()}
          className="bg-[#FF5A5A] text-white px-6 py-2 rounded-none font-black hover:bg-[#E04848] transition-all flex items-center justify-center shadow-lg shadow-red-900/10 active:scale-95 shrink-0 text-sm"
        >
          <Plus className="w-4 h-4 mr-2" /> 새 배너 등록
        </button>
      </header>

      <div className="flex items-center space-x-6 border-b border-gray-200 w-full px-1">
        {(['hero', 'ad'] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`pb-3 text-xs font-black transition-all relative ${
              activeTab === tab
                ? 'text-[#FF5A5A]'
                : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            {tab === 'hero' ? '히어로 배너' : '광고 배너'}
            {activeTab === tab && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#FF5A5A]" />
            )}
          </button>
        ))}
      </div>

      {/* Carousel Preview */}
      <section className="overflow-hidden relative">
        <div className="flex items-center justify-between mb-4 relative z-10 px-1">
          <h2 className="text-lg font-black text-gray-900 tracking-tight">
            {activeTab === 'hero' ? '메인 히어로 미리보기' : '하단 광고 미리보기'}
          </h2>
          <div className="flex items-center space-x-2 bg-gray-100 px-3 py-1 rounded-none border border-gray-200">
            <span className="w-1.5 h-1.5 bg-green-500 rounded-none animate-pulse"></span>
            <span className="text-[10px] font-black text-gray-600 uppercase tracking-widest">Live Preview</span>
          </div>
        </div>

        <div className={`relative ${activeTab === 'hero' ? 'h-[400px] md:h-[500px]' : 'h-[250px] md:h-[300px]'} overflow-hidden bg-gray-100 rounded-none group`}>
          <AnimatePresence initial={false} custom={direction}>
            {activeBannersForPreview.length > 0 ? (
              <motion.div
                key={activeBannersForPreview[previewIndex]?.bannerNo}
                custom={direction}
                variants={variants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{
                  x: { type: "spring", stiffness: 300, damping: 30 },
                  opacity: { duration: 0.2 }
                }}
                className="absolute inset-0"
              >
                {activeTab === 'hero' ? (
                  <div className="absolute inset-0 bg-black">
                    <div
                      className="absolute inset-0 bg-cover bg-center"
                      style={{ backgroundImage: `url('${resolveImageUrl(activeBannersForPreview[previewIndex]?.imgUrl) || activeBannersForPreview[previewIndex]?.imgUrl || ''}')` }}
                    ></div>
                  </div>
                ) : (
                  <div className="absolute inset-0 flex items-center justify-center px-10">
                    <div className="w-full h-[200px] rounded-[32px] overflow-hidden shadow-2xl border border-white/5 relative group/ad">
                      <div
                        className="absolute inset-0 bg-cover bg-center"
                        style={{ backgroundImage: `url('${resolveImageUrl(activeBannersForPreview[previewIndex]?.imgUrl) || activeBannersForPreview[previewIndex]?.imgUrl || ''}')` }}
                      ></div>
                    </div>
                  </div>
                )}
              </motion.div>
            ) : (
              <div className="h-full flex items-center justify-center text-gray-400 font-black text-sm">
                활성화된 배너가 없습니다.
              </div>
            )}
          </AnimatePresence>

          {/* Controls */}
          {activeBannersForPreview.length > 1 && (
            <>
              <button
                onClick={() => {
                  setDirection(-1);
                  setPreviewIndex((prev) => (prev - 1 + activeBannersForPreview.length) % activeBannersForPreview.length);
                }}
                className="absolute left-6 top-1/2 -translate-y-1/2 z-20 p-4 bg-white/20 backdrop-blur-md text-white hover:bg-white/40 rounded-none transition-all opacity-0 group-hover:opacity-100"
              >
                <ChevronLeft className="w-6 h-6" />
              </button>
              <button
                onClick={() => {
                  setDirection(1);
                  setPreviewIndex((prev) => (prev + 1) % activeBannersForPreview.length);
                }}
                className="absolute right-6 top-1/2 -translate-y-1/2 z-20 p-4 bg-white/20 backdrop-blur-md text-white hover:bg-white/40 rounded-none transition-all opacity-0 group-hover:opacity-100"
              >
                <ChevronRight className="w-6 h-6" />
              </button>
              <div className="absolute bottom-8 left-1/2 -translate-x-1/2 z-20 flex space-x-2">
                {activeBannersForPreview.map((_, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      setDirection(idx > previewIndex ? 1 : -1);
                      setPreviewIndex(idx);
                    }}
                    className={`h-1.5 rounded-none transition-all ${idx === previewIndex ? 'bg-[#FF5A5A] w-8' : 'bg-white/40 w-2 hover:bg-white/60'}`}
                  ></button>
                ))}
              </div>
            </>
          )}
        </div>
      </section>

      {/* Banner Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {filteredBanners.map((banner) => (
          <div key={banner.bannerNo} className="bg-white rounded-none shadow-sm border border-gray-100 overflow-hidden group hover:shadow-md transition-all">
            <div className="relative h-48 overflow-hidden">
              <img src={resolveImageUrl(banner.imgUrl) || banner.imgUrl || undefined} alt="Banner" className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" />
              <div className="absolute inset-0 bg-black/20"></div>
              <div className="absolute top-4 right-4 flex space-x-2">
                <button
                  onClick={() => handleOpenModal(banner)}
                  className="p-2 bg-white/20 backdrop-blur-md text-white hover:bg-white/40 rounded-none transition-colors"
                >
                  <Edit2 className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDelete(banner.bannerNo)}
                  className="p-2 bg-red-500/80 backdrop-blur-md text-white hover:bg-red-600 rounded-none transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
            <div className="p-4 flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="flex items-center space-x-1.5 text-[10px] text-gray-500 font-medium">
                  <LinkIcon className="w-3 h-3" />
                  <span className="max-w-[100px] truncate">{banner.linkUrl}</span>
                </div>
                <div className="flex items-center space-x-1.5">
                  {banner.isActive === 1 ? (
                    <span className="flex items-center text-[9px] font-black text-green-600 bg-green-50 px-2 py-0.5 rounded-none">
                      <CheckCircle2 className="w-2.5 h-2.5 mr-1" /> 활성
                    </span>
                  ) : (
                    <span className="flex items-center text-[9px] font-black text-gray-400 bg-gray-50 px-2 py-0.5 rounded-none">
                      <XCircle className="w-2.5 h-2.5 mr-1" /> 비활성
                    </span>
                  )}
                </div>
              </div>
              <button
                onClick={() => toggleActive(banner.bannerNo)}
                className={`text-[10px] font-black px-4 py-1.5 rounded-none transition-all ${
                  banner.isActive === 1
                    ? 'text-red-600 bg-red-50 hover:bg-red-100'
                    : 'text-green-600 bg-green-50 hover:bg-green-100'
                }`}
              >
                {banner.isActive === 1 ? '비활성화' : '활성화'}
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Editor Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-6 bg-black/60 backdrop-blur-sm">
          <div className="bg-white w-full max-w-lg rounded-none p-10 shadow-2xl animate-in fade-in zoom-in duration-300 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-8">
              <h2 className="text-2xl font-black text-gray-900 tracking-tight">
                {editingBanner ? '배너 수정' : '새 배너 등록'}
              </h2>
            </div>

            <div className="space-y-8 mb-10">
              <div>
                <label className="block text-xs font-black text-gray-700 mb-3 uppercase tracking-widest">배너 타입</label>
                <div className="flex gap-3">
                  <button
                    onClick={() => setBannerType('hero')}
                    className={`flex-1 py-3 rounded-none font-black text-sm transition-all ${
                      bannerType === 'hero'
                        ? 'bg-[#FF5A5A] text-white shadow-lg shadow-red-500/20'
                        : 'bg-gray-50 text-gray-400 border border-gray-100 hover:bg-gray-100'
                    }`}
                  >
                    히어로 배너
                  </button>
                  <button
                    onClick={() => setBannerType('ad')}
                    className={`flex-1 py-3 rounded-none font-black text-sm transition-all ${
                      bannerType === 'ad'
                        ? 'bg-[#FF5A5A] text-white shadow-lg shadow-red-500/20'
                        : 'bg-gray-50 text-gray-400 border border-gray-100 hover:bg-gray-100'
                    }`}
                  >
                    광고 배너
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-black text-gray-700 mb-2">이미지</label>
                {/* 파일 업로드 */}
                <div className="mb-2">
                  <label
                    className={`flex items-center justify-center gap-2 w-full py-2.5 border-2 border-dashed border-gray-300 bg-gray-50 hover:border-[#FF5A5A] hover:bg-red-50/30 transition-all cursor-pointer text-sm font-bold text-gray-500 ${isUploading ? 'opacity-50 pointer-events-none' : ''}`}
                  >
                    <Upload className="w-4 h-4" />
                    {isUploading ? '업로드 중...' : '파일 선택하여 업로드'}
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={async (e) => {
                        const file = e.target.files?.[0];
                        if (!file) return;
                        setIsUploading(true);
                        try {
                          const formData = new FormData();
                          formData.append('file', file);
                          const res = await api.post('/images/upload', formData);
                          setImgUrl(res.data.url);
                        } catch (err) {
                          console.error('이미지 업로드 실패', err);
                          alert('이미지 업로드에 실패했습니다.');
                        } finally {
                          setIsUploading(false);
                          e.target.value = '';
                        }
                      }}
                    />
                  </label>
                </div>
                {/* 또는 URL 직접 입력 */}
                <div className="flex space-x-3">
                  <div className="flex-1 relative flex items-center">
                    <Image className="absolute left-3 text-gray-400 w-4 h-4" />
                    <input
                      type="text"
                      placeholder="또는 URL 직접 입력 (https://...)"
                      className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm"
                      value={imgUrl}
                      onChange={(e) => setImgUrl(e.target.value)}
                    />
                  </div>
                  {imgUrl && (
                    <div className="w-10 h-10 rounded-none overflow-hidden border border-gray-200 shrink-0">
                      <img src={resolveImageUrl(imgUrl) || imgUrl} alt="미리보기" className="w-full h-full object-cover" />
                    </div>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-xs font-black text-gray-700 mb-2">연결 링크 (URL)</label>
                <div className="relative flex items-center">
                  <LinkIcon className="absolute left-3 text-gray-400 w-4 h-4" />
                  <input
                    type="text"
                    placeholder="/search"
                    className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm"
                    value={linkUrl}
                    onChange={(e) => setLinkUrl(e.target.value)}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-black text-gray-700 mb-2">노출 순서</label>
                  <input
                    type="number"
                    min={0}
                    placeholder="0"
                    className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm"
                    value={sortOrder}
                    onChange={(e) => setSortOrder(Number(e.target.value))}
                  />
                  <p className="text-[10px] text-gray-400 mt-1">숫자가 작을수록 먼저 노출</p>
                </div>
                <div>
                  <label className="block text-xs font-black text-gray-700 mb-2">종료 일시</label>
                  <input
                    type="datetime-local"
                    className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-none focus:outline-none focus:ring-2 focus:ring-[#FF5A5A] font-medium text-sm"
                    value={endAt}
                    onChange={(e) => setEndAt(e.target.value)}
                  />
                  <p className="text-[10px] text-gray-400 mt-1">미설정 시 계속 노출</p>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <label className="flex items-center space-x-2 cursor-pointer group">
                  <input
                    type="checkbox"
                    className="w-5 h-5 rounded-none border-gray-300 text-[#FF5A5A] focus:ring-[#FF5A5A] cursor-pointer"
                    checked={isActive === 1}
                    onChange={(e) => setIsActive(e.target.checked ? 1 : 0)}
                  />
                  <span className="text-xs font-black text-gray-700 group-hover:text-[#FF5A5A] transition-colors">배너 활성화</span>
                </label>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setIsModalOpen(false)}
                className="flex-1 py-3 rounded-none font-black text-gray-500 bg-gray-100 hover:bg-gray-200 transition-all active:scale-95 text-sm"
              >
                취소
              </button>
              <button
                onClick={handleSave}
                className="flex-1 py-3 rounded-none font-black text-white bg-[#FF5A5A] hover:bg-[#E04848] transition-all active:scale-95 shadow-lg shadow-red-500/10 text-sm"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
      {/* Delete Confirmation Modal */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-6 bg-black/60 backdrop-blur-sm">
          <div className="bg-white w-full max-w-sm rounded-none p-8 shadow-2xl animate-in fade-in zoom-in duration-300">
            <div className="w-16 h-16 bg-red-50 rounded-none flex items-center justify-center mb-4 mx-auto">
              <Trash2 className="w-8 h-8 text-red-500" />
            </div>
            <h2 className="text-xl font-black text-gray-900 mb-2 text-center tracking-tight">
              정말 삭제하시겠습니까?
            </h2>
            <p className="text-gray-500 font-medium text-center mb-8 text-sm">
              삭제된 배너는 복구할 수 없습니다.<br />신중하게 결정해주세요.
            </p>

            <div className="flex gap-3">
              <button
                onClick={() => setIsDeleteModalOpen(false)}
                className="flex-1 py-3 rounded-none font-black text-gray-500 bg-gray-100 hover:bg-gray-200 transition-all active:scale-95 text-sm"
              >
                취소
              </button>
              <button
                onClick={confirmDelete}
                className="flex-1 py-3 rounded-none font-black text-white bg-[#FF5A5A] hover:bg-[#E04848] transition-all active:scale-95 shadow-lg shadow-red-500/20 text-sm"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
