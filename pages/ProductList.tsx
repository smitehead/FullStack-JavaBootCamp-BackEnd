import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CURRENT_USER } from '../services/mockData';
import { useAppContext } from '../context/AppContext';
import { ProductCard } from '../components/ProductCard';
import { CATEGORY_DATA, LOCATION_DATA } from '../constants';
import { ChevronRight, Search, ChevronLeft, ChevronRight as ChevronRightIcon, RotateCcw, X, Plus, Minus } from 'lucide-react';

type SortOption = 'all' | 'popular' | 'ending' | 'latest';

export const ProductList: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { products } = useAppContext();
  
  // Filter States (Sync with URL)
  const [largeCat, setLargeCat] = useState(searchParams.get('large') || '');
  const [mediumCat, setMediumCat] = useState(searchParams.get('medium') || '');
  const [smallCat, setSmallCat] = useState(searchParams.get('small') || '');
  
  // Expansion States
  const [expandedLarge, setExpandedLarge] = useState<string | null>(searchParams.get('large'));
  const [expandedMedium, setExpandedMedium] = useState<string | null>(searchParams.get('medium'));
  
  const [minPrice, setMinPrice] = useState(searchParams.get('minPrice') || '');
  const [maxPrice, setMaxPrice] = useState(searchParams.get('maxPrice') || '');
  
  const [city, setCity] = useState(searchParams.get('city') || '');
  const [district, setDistrict] = useState(searchParams.get('district') || '');
  const [neighborhood, setNeighborhood] = useState(searchParams.get('neighborhood') || '');
  
  const [delivery, setDelivery] = useState(searchParams.get('delivery') === 'true');
  const [faceToFace, setFaceToFace] = useState(searchParams.get('face') === 'true');
  
  const [sort, setSort] = useState<SortOption>((searchParams.get('sort') as SortOption) || 'all');
  const [page, setPage] = useState(Number(searchParams.get('page')) || 1);
  const itemsPerPage = 3;

  // UI States
  const [isCategoryExpanded, setIsCategoryExpanded] = useState(true);

  // Sync state with URL when URL changes (e.g. back button)
  useEffect(() => {
    const l = searchParams.get('large') || '';
    const m = searchParams.get('medium') || '';
    const s = searchParams.get('small') || '';
    setLargeCat(l);
    setMediumCat(m);
    setSmallCat(s);
    if (l) setExpandedLarge(l);
    if (m) setExpandedMedium(m);

    setMinPrice(searchParams.get('minPrice') || '');
    setMaxPrice(searchParams.get('maxPrice') || '');
    setCity(searchParams.get('city') || '');
    setDistrict(searchParams.get('district') || '');
    setNeighborhood(searchParams.get('neighborhood') || '');
    setDelivery(searchParams.get('delivery') === 'true');
    setFaceToFace(searchParams.get('face') === 'true');
    setSort((searchParams.get('sort') as SortOption) || 'all');
    setPage(Number(searchParams.get('page')) || 1);
  }, [searchParams]);

  // Derived Data
  const selectedLarge = CATEGORY_DATA.find(c => c.id === largeCat);
  const selectedMedium = selectedLarge?.subCategories?.find(c => c.id === mediumCat);
  const selectedSmall = selectedMedium?.subCategories?.find(c => c.id === smallCat);
  
  const expandedLargeData = CATEGORY_DATA.find(c => c.id === expandedLarge);
  const expandedMediumData = expandedLargeData?.subCategories?.find(c => c.id === expandedMedium);

  const selectedCity = LOCATION_DATA.find(l => l.name === city);
  const selectedDistrict = selectedCity?.sub?.find(d => d.name === district);

  // Filter Logic
  const filteredProducts = useMemo(() => {
    let result = [...products];
    
    // Filter out products from blocked users (mutual)
    result = result.filter(p => 
      !CURRENT_USER.blockedUserIds?.includes(p.seller.id) && 
      !p.seller.blockedUserIds?.includes(CURRENT_USER.id)
    );

    // Filter out completed or canceled products
    const now = new Date().getTime();
    result = result.filter(p => p.status === 'active' && new Date(p.endTime).getTime() > now);

    // Category Filter
    if (largeCat) {
      // In real app: result = result.filter(p => p.largeCategoryId === largeCat);
    }
    if (mediumCat) {
      // In real app: result = result.filter(p => p.mediumCategoryId === mediumCat);
    }
    if (smallCat) {
      // In real app: result = result.filter(p => p.smallCategoryId === smallCat);
    }

    // Price Filter
    if (minPrice) result = result.filter(p => p.currentPrice >= Number(minPrice));
    if (maxPrice) result = result.filter(p => p.currentPrice <= Number(maxPrice));

    // Location Filter
    if (city) {
      // In real app: result = result.filter(p => p.location.includes(city));
    }

    // Delivery Filter
    if (delivery || faceToFace) {
      result = result.filter(p => {
        if (delivery && p.transactionMethod === 'delivery') return true;
        if (faceToFace && p.transactionMethod === 'face-to-face') return true;
        return false;
      });
    }

    // Sorting
    if (sort === 'popular') result.sort((a, b) => b.participantCount - a.participantCount);
    else if (sort === 'ending') result.sort((a, b) => new Date(a.endTime).getTime() - new Date(b.endTime).getTime());
    else if (sort === 'latest') result.sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
    
    return result;
  }, [largeCat, mediumCat, smallCat, minPrice, maxPrice, city, district, neighborhood, delivery, faceToFace, sort]);

  // Pagination
  const totalPages = Math.ceil(filteredProducts.length / itemsPerPage);
  const paginatedProducts = filteredProducts.slice((page - 1) * itemsPerPage, page * itemsPerPage);

  const updateParams = (newParams: any) => {
    const params = Object.fromEntries(searchParams.entries());
    const merged = { ...params, ...newParams };
    Object.keys(merged).forEach(key => {
      if (merged[key] === '' || merged[key] === undefined || merged[key] === null || merged[key] === false) {
        delete merged[key];
      }
    });
    if (!newParams.page) {
      merged.page = '1';
      setPage(1);
    }
    setSearchParams(merged);
  };

  const handleLargeClick = (id: string) => {
    if (id === '') {
      updateParams({ large: '', medium: '', small: '' });
      setExpandedLarge(null);
      setExpandedMedium(null);
    } else {
      updateParams({ large: id, medium: '', small: '' });
      setExpandedLarge(id);
      setExpandedMedium(null);
    }
  };

  const handleMediumClick = (id: string) => {
    updateParams({ medium: id, small: '' });
    setExpandedMedium(id);
  };

  const handleSmallClick = (id: string) => {
    updateParams({ small: id });
  };

  const toggleExpandLarge = (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    setExpandedLarge(expandedLarge === id ? null : id);
  };

  const toggleExpandMedium = (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    setExpandedMedium(expandedMedium === id ? null : id);
  };

  const handleSortChange = (newSort: SortOption) => {
    updateParams({ sort: newSort });
  };

  const handlePageChange = (newPage: number) => {
    updateParams({ page: String(newPage) });
    window.scrollTo(0, 0);
  };

  const resetAll = () => {
    setSearchParams({});
    setPage(1);
    setExpandedLarge(null);
    setExpandedMedium(null);
  };

  const removeFilter = (key: string) => {
    if (key === 'price') {
      updateParams({ minPrice: '', maxPrice: '' });
    } else if (key === 'location') {
      updateParams({ city: '', district: '', neighborhood: '' });
    } else if (key === 'delivery') {
      updateParams({ delivery: '' });
    } else if (key === 'face') {
      updateParams({ face: '' });
    } else if (key === 'large') {
      updateParams({ large: '', medium: '', small: '' });
      setExpandedLarge(null);
      setExpandedMedium(null);
    } else if (key === 'medium') {
      updateParams({ medium: '', small: '' });
      setExpandedMedium(null);
    } else if (key === 'small') {
      updateParams({ small: '' });
    }
  };

  return (
    <div className="max-w-[1200px] mx-auto px-10 py-8 space-y-8">
      {/* Breadcrumb */}
      <nav className="flex items-center text-sm text-gray-500 space-x-2">
        <span className="cursor-pointer hover:text-gray-900" onClick={() => navigate('/')}>홈</span>
        <ChevronRightIcon className="w-4 h-4" />
        <span className={`cursor-pointer ${!largeCat ? 'font-bold text-gray-900' : 'hover:text-gray-900'}`} onClick={() => handleLargeClick('')}>전체</span>
        {selectedLarge && (
          <>
            <ChevronRightIcon className="w-4 h-4" />
            <span className={`cursor-pointer ${!mediumCat ? 'font-bold text-gray-900' : 'hover:text-gray-900'}`} onClick={() => handleLargeClick(largeCat)}>{selectedLarge.name}</span>
          </>
        )}
        {selectedMedium && (
          <>
            <ChevronRightIcon className="w-4 h-4" />
            <span className={`cursor-pointer ${!smallCat ? 'font-bold text-gray-900' : 'hover:text-gray-900'}`} onClick={() => handleMediumClick(mediumCat)}>{selectedMedium.name}</span>
          </>
        )}
        {selectedSmall && (
          <>
            <ChevronRightIcon className="w-4 h-4" />
            <span className="font-bold text-gray-900">{selectedSmall.name}</span>
          </>
        )}
      </nav>

      {/* Filter Container */}
      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden shadow-sm">
        {/* Category Section */}
        <div className="flex border-b border-gray-100">
          <div className="w-32 bg-gray-50 p-4 flex-shrink-0 flex flex-col border-r border-gray-100">
            <div className="flex items-center cursor-pointer group" onClick={() => setIsCategoryExpanded(!isCategoryExpanded)}>
              <span className="text-sm font-bold text-gray-700 group-hover:text-brand transition-colors">카테고리</span>
              {isCategoryExpanded ? (
                <Minus className="w-3 h-3 ml-auto text-gray-400 group-hover:text-brand transition-colors" />
              ) : (
                <Plus className="w-3 h-3 ml-auto text-gray-400 group-hover:text-brand transition-colors" />
              )}
            </div>
          </div>
          <div className="flex-grow flex flex-col">
            {/* Category Breadcrumb/Header - Always Visible */}
            <div className="px-6 pt-4 pb-2 flex items-center space-x-2 text-sm">
              <button 
                onClick={() => handleLargeClick('')}
                className={`font-bold ${!largeCat ? 'text-brand' : 'text-gray-900 hover:underline'}`}
              >
                전체
              </button>
              {selectedLarge && (
                <>
                  <ChevronRightIcon className="w-3 h-3 text-gray-300" />
                  <button 
                    onClick={() => { setMediumCat(''); setSmallCat(''); updateParams({ medium: '', small: '' }); }}
                    className={`font-bold ${!mediumCat ? 'text-brand' : 'text-gray-900 hover:underline'}`}
                  >
                    {selectedLarge.name}
                  </button>
                </>
              )}
              {selectedMedium && (
                <>
                  <ChevronRightIcon className="w-3 h-3 text-gray-300" />
                  <button 
                    onClick={() => { setSmallCat(''); updateParams({ small: '' }); }}
                    className={`font-bold ${!smallCat ? 'text-brand' : 'text-gray-900 hover:underline'}`}
                  >
                    {selectedMedium.name}
                  </button>
                </>
              )}
              {selectedSmall && (
                <>
                  <ChevronRightIcon className="w-3 h-3 text-gray-300" />
                  <span className="font-bold text-brand">{selectedSmall.name}</span>
                </>
              )}
            </div>

            {/* Category Grid - Collapsible */}
            <div className={`px-6 pb-6 flex-grow transition-all duration-300 ${isCategoryExpanded ? 'opacity-100' : 'h-0 opacity-0 overflow-hidden p-0'}`}>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-y-3 gap-x-8 mt-2">
                {!largeCat ? (
                  CATEGORY_DATA.map(cat => (
                    <button
                      key={cat.id}
                      onClick={() => handleLargeClick(cat.id)}
                      className="text-sm text-left text-gray-600 hover:text-brand hover:font-bold transition-all"
                    >
                      {cat.name}
                    </button>
                  ))
                ) : !mediumCat ? (
                  selectedLarge?.subCategories?.map(sub => (
                    <button
                      key={sub.id}
                      onClick={() => handleMediumClick(sub.id)}
                      className="text-sm text-left text-gray-600 hover:text-brand hover:font-bold transition-all"
                    >
                      {sub.name}
                    </button>
                  ))
                ) : (
                  selectedMedium?.subCategories?.map(small => (
                    <button
                      key={small.id}
                      onClick={() => handleSmallClick(small.id)}
                      className={`text-sm text-left transition-all ${smallCat === small.id ? 'text-brand font-bold' : 'text-gray-600 hover:text-brand hover:font-bold'}`}
                    >
                      {small.name}
                    </button>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Price Row */}
        <div className="flex border-b border-gray-100">
          <div className="w-32 bg-gray-50 p-4 flex-shrink-0 flex items-center border-r border-gray-100">
            <span className="text-sm font-bold text-gray-700">가격대</span>
          </div>
          <div className="p-4 flex items-center space-x-2">
            <div className="relative">
              <input 
                type="number" 
                placeholder="최소 금액" 
                value={minPrice}
                onChange={(e) => setMinPrice(e.target.value)}
                className="w-40 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-brand"
              />
              <span className="absolute right-3 top-2 text-gray-400 text-sm">원</span>
            </div>
            <span className="text-gray-400">~</span>
            <div className="relative">
              <input 
                type="number" 
                placeholder="최대 금액" 
                value={maxPrice}
                onChange={(e) => setMaxPrice(e.target.value)}
                className="w-40 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-brand"
              />
              <span className="absolute right-3 top-2 text-gray-400 text-sm">원</span>
            </div>
            <button 
              onClick={() => updateParams({ minPrice, maxPrice })}
              className="bg-blue-500 text-white px-6 py-2 rounded text-sm font-bold hover:bg-brand-dark transition-colors shadow-sm"
            >
              적용
            </button>
          </div>
        </div>

        {/* Location Row */}
        <div className="flex border-b border-gray-100">
          <div className="w-32 bg-gray-50 p-4 flex-shrink-0 flex items-center border-r border-gray-100">
            <span className="text-sm font-bold text-gray-700">지역</span>
          </div>
          <div className="p-4 flex items-center space-x-2">
            <select 
              value={city} 
              onChange={(e) => { setCity(e.target.value); setDistrict(''); setNeighborhood(''); }}
              className="w-40 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-brand"
            >
              <option value="">시/도 선택</option>
              {LOCATION_DATA.map(l => <option key={l.name} value={l.name}>{l.name}</option>)}
            </select>
            <select 
              value={district} 
              onChange={(e) => { setDistrict(e.target.value); setNeighborhood(''); }}
              disabled={!city}
              className="w-40 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-brand disabled:bg-gray-50 disabled:text-gray-400"
            >
              <option value="">시/군/구 선택</option>
              {selectedCity?.sub?.map(d => <option key={d.name} value={d.name}>{d.name}</option>)}
            </select>
            <select 
              value={neighborhood} 
              onChange={(e) => setNeighborhood(e.target.value)}
              disabled={!district}
              className="w-40 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-brand disabled:bg-gray-50 disabled:text-gray-400"
            >
              <option value="">읍/면/동 선택</option>
              {selectedDistrict?.sub?.map(n => <option key={n} value={n}>{n}</option>)}
            </select>
            <button 
              onClick={() => updateParams({ city, district, neighborhood })}
              className="bg-blue-500 text-white px-6 py-2 rounded text-sm font-bold hover:bg-brand-dark transition-colors shadow-sm"
            >
              적용
            </button>
          </div>
        </div>

        {/* Delivery Row */}
        <div className="flex">
          <div className="w-32 bg-gray-50 p-4 flex-shrink-0 flex items-center border-r border-gray-100">
            <span className="text-sm font-bold text-gray-700">배송형태</span>
          </div>
          <div className="p-4 flex items-center space-x-6">
            <label className="flex items-center space-x-2 cursor-pointer group">
              <input 
                type="checkbox" 
                checked={faceToFace}
                onChange={(e) => { setFaceToFace(e.target.checked); updateParams({ face: e.target.checked ? 'true' : '' }); }}
                className="w-4 h-4 rounded border-gray-300 text-brand focus:ring-brand" 
              />
              <span className="text-sm text-gray-600 group-hover:text-gray-900">대면거래</span>
            </label>
            <label className="flex items-center space-x-2 cursor-pointer group">
              <input 
                type="checkbox" 
                checked={delivery}
                onChange={(e) => { setDelivery(e.target.checked); updateParams({ delivery: e.target.checked ? 'true' : '' }); }}
                className="w-4 h-4 rounded border-gray-300 text-brand focus:ring-brand" 
              />
              <span className="text-sm text-gray-600 group-hover:text-gray-900">택배거래</span>
            </label>
          </div>
        </div>

        {/* Active Filter Chips */}
        <div className="bg-gray-50/50 border-t border-gray-100 p-4 flex items-center justify-between">
          <div className="flex flex-wrap gap-2">
            {largeCat && (
              <div className="flex items-center bg-white border border-brand/30 text-brand px-3 py-1 rounded-full text-xs font-medium">
                <span>{CATEGORY_DATA.find(c => c.id === largeCat)?.name}</span>
                <button onClick={() => removeFilter('large')} className="ml-2 hover:text-brand-dark"><X className="w-3 h-3" /></button>
              </div>
            )}
            {mediumCat && (
              <div className="flex items-center bg-white border border-brand/30 text-brand px-3 py-1 rounded-full text-xs font-medium">
                <span>{selectedMedium?.name}</span>
                <button onClick={() => removeFilter('medium')} className="ml-2 hover:text-brand-dark"><X className="w-3 h-3" /></button>
              </div>
            )}
            {smallCat && (
              <div className="flex items-center bg-white border border-brand/30 text-brand px-3 py-1 rounded-full text-xs font-medium">
                <span>{selectedSmall?.name}</span>
                <button onClick={() => removeFilter('small')} className="ml-2 hover:text-brand-dark"><X className="w-3 h-3" /></button>
              </div>
            )}
            {(searchParams.get('minPrice') || searchParams.get('maxPrice')) && (
              <div className="flex items-center bg-white border border-brand/30 text-brand px-3 py-1 rounded-full text-xs font-medium">
                <span>
                  {searchParams.get('minPrice') ? `${Number(searchParams.get('minPrice')).toLocaleString()}원` : '0원'} ~ 
                  {searchParams.get('maxPrice') ? `${Number(searchParams.get('maxPrice')).toLocaleString()}원` : '무제한'}
                </span>
                <button onClick={() => removeFilter('price')} className="ml-2 hover:text-brand-dark"><X className="w-3 h-3" /></button>
              </div>
            )}
            {searchParams.get('city') && (
              <div className="flex items-center bg-white border border-brand/30 text-brand px-3 py-1 rounded-full text-xs font-medium">
                <span>{searchParams.get('city')} {searchParams.get('district')} {searchParams.get('neighborhood')}</span>
                <button onClick={() => removeFilter('location')} className="ml-2 hover:text-brand-dark"><X className="w-3 h-3" /></button>
              </div>
            )}
            {delivery && (
              <div className="flex items-center bg-white border border-brand/30 text-brand px-3 py-1 rounded-full text-xs font-medium">
                <span>택배거래</span>
                <button onClick={() => removeFilter('delivery')} className="ml-2 hover:text-brand-dark"><X className="w-3 h-3" /></button>
              </div>
            )}
            {faceToFace && (
              <div className="flex items-center bg-white border border-brand/30 text-brand px-3 py-1 rounded-full text-xs font-medium">
                <span>대면거래</span>
                <button onClick={() => removeFilter('face')} className="ml-2 hover:text-brand-dark"><X className="w-3 h-3" /></button>
              </div>
            )}
          </div>
          <button 
            onClick={resetAll}
            className="flex items-center text-xs text-gray-400 hover:text-gray-600 font-medium transition-colors"
          >
            <RotateCcw className="w-3 h-3 mr-1" /> 초기화
          </button>
        </div>
      </div>

      {/* List Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 py-4">
        <div className="flex items-center space-x-2">
          <span className="text-sm font-bold text-gray-900">총 <span className="text-gray-900">{filteredProducts.length}</span>개 상품</span>
        </div>
        
        <div className="flex items-center space-x-1">
          {[
            { id: 'all', label: '전체' },
            { id: 'popular', label: '인기순' },
            { id: 'ending', label: '종료임박순' },
            { id: 'latest', label: '최신순' }
          ].map(opt => (
            <button
              key={opt.id}
              onClick={() => handleSortChange(opt.id as SortOption)}
              className={`px-4 py-2 text-sm font-bold transition-all border-b-2 ${sort === opt.id ? 'border-brand text-brand' : 'border-transparent text-gray-400 hover:text-gray-600'}`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* Product Grid */}
      <div className="grid grid-cols-4 gap-6">
        {paginatedProducts.map(product => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      {/* No Results */}
      {paginatedProducts.length === 0 && (
        <div className="py-32 text-center bg-white rounded-3xl border border-gray-100 shadow-sm">
          <Search className="w-16 h-16 text-gray-200 mx-auto mb-6" />
          <p className="text-gray-500 text-xl font-bold mb-2">조건에 맞는 상품이 없습니다.</p>
          <p className="text-gray-400 text-sm mb-8">다른 검색어나 필터를 사용해보세요.</p>
          <button 
            onClick={resetAll}
            className="text-xs font-bold text-gray-400 hover:text-gray-600 underline underline-offset-4"
          >
            필터 초기화
          </button>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center space-x-2 pt-12">
          <button 
            onClick={() => handlePageChange(page - 1)}
            disabled={page === 1}
            className="w-10 h-10 flex items-center justify-center rounded border border-gray-200 hover:bg-gray-50 disabled:opacity-30 transition-all"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          
          {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
            <button
              key={p}
              onClick={() => handlePageChange(p)}
              className={`w-10 h-10 flex items-center justify-center rounded font-bold transition-all ${page === p ? 'bg-blue-500 text-white' : 'border border-gray-200 text-gray-500 hover:border-brand hover:text-brand'}`}
            >
              {p}
            </button>
          ))}

          <button 
            onClick={() => handlePageChange(page + 1)}
            disabled={page === totalPages}
            className="w-10 h-10 flex items-center justify-center rounded border border-gray-200 hover:bg-gray-50 disabled:opacity-30 transition-all"
          >
            <ChevronRightIcon className="w-5 h-5" />
          </button>
        </div>
      )}
    </div>
  );
};
