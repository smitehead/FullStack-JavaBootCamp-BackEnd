import { CategoryItem } from './types';

export const CATEGORY_DATA: CategoryItem[] = [
  {
    id: 'cat_1',
    name: '수입명품',
    subCategories: [
      { id: 'cat_1_1', name: '여성신발', subCategories: [{ id: 'cat_1_1_1', name: '구두/로퍼' }, { id: 'cat_1_1_2', name: '운동화/스니커즈' }, { id: 'cat_1_1_3', name: '샌들/슬리퍼' }, { id: 'cat_1_1_4', name: '워커/부츠' }] },
      { id: 'cat_1_2', name: '남성신발', subCategories: [{ id: 'cat_1_2_1', name: '구두/로퍼' }, { id: 'cat_1_2_2', name: '운동화/스니커즈' }, { id: 'cat_1_2_3', name: '샌들/슬리퍼' }, { id: 'cat_1_2_4', name: '워커/부츠' }] },
      { id: 'cat_1_3', name: '가방/핸드백', subCategories: [{ id: 'cat_1_3_1', name: '숄더백' }, { id: 'cat_1_3_2', name: '크로스백' }, { id: 'cat_1_3_3', name: '토트백' }, { id: 'cat_1_3_4', name: '백팩' }, { id: 'cat_1_3_5', name: '힙색/메신저백' }, { id: 'cat_1_3_6', name: '파우치/클러치백' }, { id: 'cat_1_3_7', name: '서류가방' }, { id: 'cat_1_3_8', name: '여행가방' }] },
      { id: 'cat_1_4', name: '지갑/벨트', subCategories: [{ id: 'cat_1_4_1', name: '여성용지갑' }, { id: 'cat_1_4_2', name: '남성용지갑' }, { id: 'cat_1_4_3', name: '머니클립/명함/키지갑' }, { id: 'cat_1_4_4', name: '벨트/멜빵' }] },
      { id: 'cat_1_5', name: '여성의류', subCategories: [{ id: 'cat_1_5_1', name: '자켓/코트' }, { id: 'cat_1_5_2', name: '패딩/야상/점퍼' }, { id: 'cat_1_5_3', name: '티셔츠/민소매/탑' }, { id: 'cat_1_5_4', name: '니트/스웨터/가디건' }, { id: 'cat_1_5_5', name: '블라우스/남방' }, { id: 'cat_1_5_6', name: '바지/데님' }, { id: 'cat_1_5_7', name: '스커트' }, { id: 'cat_1_5_8', name: '원피스' }] },
      { id: 'cat_1_6', name: '남성의류', subCategories: [{ id: 'cat_1_6_1', name: '자켓/코트' }, { id: 'cat_1_6_2', name: '패딩/야상/점퍼' }, { id: 'cat_1_6_3', name: '티셔츠/민소매/탑' }, { id: 'cat_1_6_4', name: '니트/스웨터/가디건' }, { id: 'cat_1_6_5', name: '셔츠/남방' }, { id: 'cat_1_6_6', name: '바지/데님' }] },
      { id: 'cat_1_7', name: '시계', subCategories: [{ id: 'cat_1_7_1', name: '여성용시계' }, { id: 'cat_1_7_2', name: '남성용시계' }] },
      { id: 'cat_1_8', name: '쥬얼리', subCategories: [{ id: 'cat_1_8_1', name: '반지' }, { id: 'cat_1_8_2', name: '목걸이' }, { id: 'cat_1_8_3', name: '귀걸이' }, { id: 'cat_1_8_4', name: '팔찌' }, { id: 'cat_1_8_5', name: '기타 쥬얼리' }] },
      { id: 'cat_1_9', name: '기타' }
    ]
  },
  {
    id: 'cat_2',
    name: '패션의류',
    subCategories: [
      { id: 'cat_2_1', name: '여성의류', subCategories: [{ id: 'cat_2_1_1', name: '자켓/코트' }, { id: 'cat_2_1_2', name: '패딩/야상/점퍼' }, { id: 'cat_2_1_3', name: '티셔츠/민소매/탑' }, { id: 'cat_2_1_4', name: '니트/스웨터/가디건' }, { id: 'cat_2_1_5', name: '블라우스/남방' }, { id: 'cat_2_1_6', name: '바지/데님' }, { id: 'cat_2_1_7', name: '스커트' }, { id: 'cat_2_1_8', name: '원피스' }] },
      { id: 'cat_2_2', name: '남성의류', subCategories: [{ id: 'cat_2_2_1', name: '자켓/코트' }, { id: 'cat_2_2_2', name: '패딩/야상/점퍼' }, { id: 'cat_2_2_3', name: '티셔츠/민소매/탑' }, { id: 'cat_2_2_4', name: '니트/스웨터/가디건' }, { id: 'cat_2_2_5', name: '셔츠/남방' }, { id: 'cat_2_2_6', name: '바지/데님' }] }
    ]
  },
  {
    id: 'cat_3',
    name: '패션잡화',
    subCategories: [
      { id: 'cat_3_1', name: '여성신발', subCategories: [{ id: 'cat_3_1_1', name: '구두/로퍼' }, { id: 'cat_3_1_2', name: '운동화/스니커즈' }, { id: 'cat_3_1_3', name: '샌들/슬리퍼' }, { id: 'cat_3_1_4', name: '워커/부츠' }] },
      { id: 'cat_3_2', name: '남성신발', subCategories: [{ id: 'cat_3_2_1', name: '구두/로퍼' }, { id: 'cat_3_2_2', name: '운동화/스니커즈' }, { id: 'cat_3_2_3', name: '샌들/슬리퍼' }, { id: 'cat_3_2_4', name: '워커/부츠' }] },
      { id: 'cat_3_3', name: '가방/핸드백', subCategories: [{ id: 'cat_3_3_1', name: '숄더백' }, { id: 'cat_3_3_2', name: '크로스백' }, { id: 'cat_3_3_3', name: '토트백' }, { id: 'cat_3_3_4', name: '백팩' }, { id: 'cat_3_3_5', name: '힙색/메신저백' }, { id: 'cat_3_3_6', name: '파우치/클러치백' }, { id: 'cat_3_3_7', name: '서류가방' }, { id: 'cat_3_3_8', name: '여행가방' }] },
      { id: 'cat_3_4', name: '지갑/벨트', subCategories: [{ id: 'cat_3_4_1', name: '여성용지갑' }, { id: 'cat_3_4_2', name: '남성용지갑' }, { id: 'cat_3_4_3', name: '머니클립/명함/키지갑' }, { id: 'cat_3_4_4', name: '벨트/멜빵' }] },
      { id: 'cat_3_5', name: '시계', subCategories: [{ id: 'cat_3_5_1', name: '여성용시계' }, { id: 'cat_3_5_2', name: '남성용시계' }] },
      { id: 'cat_3_6', name: '쥬얼리', subCategories: [{ id: 'cat_3_6_1', name: '반지' }, { id: 'cat_3_6_2', name: '목걸이' }, { id: 'cat_3_6_3', name: '귀걸이' }, { id: 'cat_3_6_4', name: '팔찌' }, { id: 'cat_3_6_5', name: '기타 쥬얼리' }] },
      { id: 'cat_3_7', name: '패션소품', subCategories: [{ id: 'cat_3_7_1', name: '모자' }, { id: 'cat_3_7_2', name: '선글라스/안경테' }, { id: 'cat_3_7_3', name: '스카프/머플러' }, { id: 'cat_3_7_4', name: '양말/스타킹' }, { id: 'cat_3_7_5', name: '장갑' }, { id: 'cat_3_7_6', name: '기타 소품' }] }
    ]
  },
  {
    id: 'cat_4',
    name: '뷰티',
    subCategories: [
      { id: 'cat_4_1', name: '스킨케어' },
      { id: 'cat_4_2', name: '메이크업' },
      { id: 'cat_4_3', name: '향수' },
      { id: 'cat_4_4', name: '바디/헤어' },
      { id: 'cat_4_5', name: '네일아트' },
      { id: 'cat_4_6', name: '미용기기' }
    ]
  },
  {
    id: 'cat_5',
    name: '출산/유아동',
    subCategories: [
      { id: 'cat_5_1', name: '베이비의류' },
      { id: 'cat_5_2', name: '여아의류' },
      { id: 'cat_5_3', name: '남아의류' },
      { id: 'cat_5_4', name: '유아동신발/잡화' },
      { id: 'cat_5_5', name: '기저귀/물티슈' },
      { id: 'cat_5_6', name: '분유/이유식' },
      { id: 'cat_5_7', name: '유아동가구' },
      { id: 'cat_5_8', name: '유모차/카시트' },
      { id: 'cat_5_9', name: '완구/교구' },
      { id: 'cat_5_10', name: '수유/임부용품' }
    ]
  },
  {
    id: 'cat_6',
    name: '모바일/태블릿',
    subCategories: [
      { id: 'cat_6_1', name: '스마트폰' },
      { id: 'cat_6_2', name: '태블릿' },
      { id: 'cat_6_3', name: '케이스/액세서리' },
      { id: 'cat_6_4', name: '웨어러블' },
      { id: 'cat_6_5', name: '일반폰' }
    ]
  },
  {
    id: 'cat_7',
    name: '가전제품',
    subCategories: [
      { id: 'cat_7_1', name: 'TV' },
      { id: 'cat_7_2', name: '냉장고' },
      { id: 'cat_7_3', name: '세탁기/건조기' },
      { id: 'cat_7_4', name: '에어컨' },
      { id: 'cat_7_5', name: '생활가전' },
      { id: 'cat_7_6', name: '주방가전' },
      { id: 'cat_7_7', name: '미용/건강가전' },
      { id: 'cat_7_8', name: '음향기기' },
      { id: 'cat_7_9', name: '기타가전' }
    ]
  },
  {
    id: 'cat_8',
    name: '노트북/PC',
    subCategories: [
      { id: 'cat_8_1', name: '노트북' },
      { id: 'cat_8_2', name: '데스크탑' },
      { id: 'cat_8_3', name: '모니터' },
      { id: 'cat_8_4', name: 'PC주변기기' },
      { id: 'cat_8_5', name: 'PC부품' },
      { id: 'cat_8_6', name: '저장장치' },
      { id: 'cat_8_7', name: '네트워크장비' },
      { id: 'cat_8_8', name: '소모품' }
    ]
  },
  {
    id: 'cat_9',
    name: '카메라/캠코더',
    subCategories: [
      { id: 'cat_9_1', name: 'DSLR/미러리스' },
      { id: 'cat_9_2', name: '컴팩트카메라' },
      { id: 'cat_9_3', name: '필름카메라' },
      { id: 'cat_9_4', name: '캠코더' },
      { id: 'cat_9_5', name: '렌즈/필터' },
      { id: 'cat_9_6', name: '삼각대/플래시' },
      { id: 'cat_9_7', name: '가방/액세서리' }
    ]
  },
  {
    id: 'cat_10',
    name: '가구/인테리어',
    subCategories: [
      { id: 'cat_10_1', name: '침실가구' },
      { id: 'cat_10_2', name: '거실가구' },
      { id: 'cat_10_3', name: '주방가구' },
      { id: 'cat_10_4', name: '아이방가구' },
      { id: 'cat_10_5', name: '서재/사무용가구' },
      { id: 'cat_10_6', name: 'DIY자재/공구' },
      { id: 'cat_10_7', name: '침구' },
      { id: 'cat_10_8', name: '커튼/블라인드' },
      { id: 'cat_10_9', name: '조명' },
      { id: 'cat_10_10', name: '인테리어소품' }
    ]
  },
  {
    id: 'cat_11',
    name: '리빙/생활',
    subCategories: [
      { id: 'cat_11_1', name: '주방용품' },
      { id: 'cat_11_2', name: '욕실용품' },
      { id: 'cat_11_3', name: '생활용품' },
      { id: 'cat_11_4', name: '세제/유연제' },
      { id: 'cat_11_5', name: '화장지/물티슈' },
      { id: 'cat_11_6', name: '수납/정리' },
      { id: 'cat_11_7', name: '공구/산업용품' }
    ]
  },
  {
    id: 'cat_12',
    name: '게임',
    subCategories: [
      { id: 'cat_12_1', name: '콘솔게임기' },
      { id: 'cat_12_2', name: 'PC게임' },
      { id: 'cat_12_3', name: '모바일게임' },
      { id: 'cat_12_4', name: '게임주변기기' }
    ]
  },
  {
    id: 'cat_13',
    name: '반려동물/취미',
    subCategories: [
      { id: 'cat_13_1', name: '강아지용품' },
      { id: 'cat_13_2', name: '고양이용품' },
      { id: 'cat_13_3', name: '기타반려동물' },
      { id: 'cat_13_4', name: '악기' },
      { id: 'cat_13_5', name: '피규어/인형' },
      { id: 'cat_13_6', name: '프라모델/RC' },
      { id: 'cat_13_7', name: '희귀/수집품' },
      { id: 'cat_13_8', name: '예술/골동품' },
      { id: 'cat_13_9', name: '헬스/요가' },
      { id: 'cat_13_10', name: '기타취미' }
    ]
  },
  {
    id: 'cat_14',
    name: '도서/음반/문구',
    subCategories: [
      { id: 'cat_14_1', name: '도서' },
      { id: 'cat_14_2', name: '음반/DVD' },
      { id: 'cat_14_3', name: '문구/사무용품' }
    ]
  },
  {
    id: 'cat_15',
    name: '티켓/쿠폰',
    subCategories: [
      { id: 'cat_15_1', name: '기프티콘' },
      { id: 'cat_15_2', name: '상품권' },
      { id: 'cat_15_3', name: '공연/티켓' },
      { id: 'cat_15_4', name: '여행/숙박' },
      { id: 'cat_15_5', name: '기타' }
    ]
  },
  {
    id: 'cat_16',
    name: '스포츠',
    subCategories: [
      { id: 'cat_16_1', name: '축구' },
      { id: 'cat_16_2', name: '야구' },
      { id: 'cat_16_3', name: '농구' },
      { id: 'cat_16_4', name: '배구' },
      { id: 'cat_16_5', name: '골프' },
      { id: 'cat_16_6', name: '테니스' },
      { id: 'cat_16_7', name: '배드민턴' },
      { id: 'cat_16_8', name: '탁구' },
      { id: 'cat_16_9', name: '수영' },
      { id: 'cat_16_10', name: '기타스포츠' }
    ]
  },
  {
    id: 'cat_17',
    name: '레저/여행',
    subCategories: [
      { id: 'cat_17_1', name: '캠핑' },
      { id: 'cat_17_2', name: '낚시' },
      { id: 'cat_17_3', name: '등산' },
      { id: 'cat_17_4', name: '자전거' },
      { id: 'cat_17_5', name: '스키/보드' },
      { id: 'cat_17_6', name: '인라인/스케이트보드' },
      { id: 'cat_17_7', name: '여행용품' },
      { id: 'cat_17_8', name: '기타레저' }
    ]
  },
  {
    id: 'cat_18',
    name: '오토바이',
    subCategories: [
      { id: 'cat_18_1', name: '오토바이/스쿠터' },
      { id: 'cat_18_2', name: '오토바이용품/부품' }
    ]
  },
  {
    id: 'cat_19',
    name: '공구/산업용품',
    subCategories: [
      { id: 'cat_19_1', name: '전동공구' },
      { id: 'cat_19_2', name: '수공구' },
      { id: 'cat_19_3', name: '측정공구' },
      { id: 'cat_19_4', name: '산업용품/자재' }
    ]
  },
  {
    id: 'cat_20',
    name: '중고차',
    subCategories: [
      { id: 'cat_20_1', name: '국산차' },
      { id: 'cat_20_2', name: '수입차' }
    ]
  }
];

export const LOCATION_DATA = [
  {
    name: '서울특별시',
    sub: [
      {
        name: '강남구',
        sub: ['역삼동', '삼성동', '청담동']
      },
      {
        name: '마포구',
        sub: ['서교동', '합정동', '망원동']
      }
    ]
  },
  {
    name: '경기도',
    sub: [
      {
        name: '수원시',
        sub: ['팔달구', '영통구', '장안구']
      },
      {
        name: '성남시',
        sub: ['분당구', '수정구', '중원구']
      }
    ]
  },
  {
    name: '부산광역시',
    sub: [
      {
        name: '해운대구',
        sub: ['우동', '중동', '좌동']
      }
    ]
  }
];
