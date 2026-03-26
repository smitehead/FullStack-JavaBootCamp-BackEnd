import { Product, User, Category, BidHistory, Notification, Review, ReviewTag, ChatRoom, Notice, Inquiry, HeroBanner, Report } from '@/types';

export const CURRENT_USER: User = {
  id: 'user_1',
  nickname: '경매왕',
  profileImage: 'https://picsum.photos/seed/user1/200/200',
  points: 150000,
  mannerTemp: 37.5,
  joinedAt: '2023-01-15',
  email: 'user1@example.com',
  phoneNum: '010-1234-5678',
  address: '서울 강남구 테헤란로 123',
  isActive: true,
  isAdmin: false,
  settings: {
    auctionEnd: true,
    newBid: true,
    marketing: false,
    chat: true
  },
  blockedUserIds: ['user_99', 'user_100']
};

export const ADMIN_USER: User = {
  id: 'admin_1',
  nickname: '관리자',
  profileImage: 'https://picsum.photos/seed/admin/200/200',
  points: 9999999,
  mannerTemp: 99.9,
  joinedAt: '2020-01-01',
  email: 'admin1@example.com',
  phoneNum: '010-0000-0000',
  address: '본사',
  isActive: true,
  isAdmin: true,
};

export const MOCK_USERS: User[] = [
  { ...CURRENT_USER, auctionCount: 15, postCount: 24 },
  { ...ADMIN_USER, auctionCount: 0, postCount: 0 },
  {
    id: 'user_2',
    nickname: '테크매니아',
    profileImage: 'https://picsum.photos/seed/user2/200/200',
    points: 50000,
    mannerTemp: 42.5,
    joinedAt: '2022-05-20',
    email: 'tech@example.com',
    isActive: true,
    auctionCount: 42,
    postCount: 56,
  },
  {
    id: 'user_3',
    nickname: '홈스타일링',
    profileImage: 'https://picsum.photos/seed/user3/200/200',
    points: 120000,
    mannerTemp: 38.2,
    joinedAt: '2023-03-12',
    email: 'home@example.com',
    isActive: true,
    auctionCount: 12,
    postCount: 18,
  },
  {
    id: 'user_4',
    nickname: '패션피플',
    profileImage: 'https://picsum.photos/seed/user4/200/200',
    points: 85000,
    mannerTemp: 41.2,
    joinedAt: '2023-08-05',
    email: 'fashion@example.com',
    isActive: true,
    auctionCount: 28,
    postCount: 35,
  },
  {
    id: 'user_5',
    nickname: '북웜',
    profileImage: 'https://picsum.photos/seed/user5/200/200',
    points: 32000,
    mannerTemp: 36.8,
    joinedAt: '2023-11-20',
    email: 'book@example.com',
    isActive: true,
    auctionCount: 5,
    postCount: 12,
  }
];

export const MOCK_POINT_HISTORY = [
  { id: 'ph_1', type: 'charge', amount: 50000, balance: 150000, description: '포인트 충전 (계좌)', createdAt: '2024-03-19T14:30:00' },
  { id: 'ph_2', type: 'withdraw', amount: 20000, balance: 100000, description: '포인트 출금 (신한은행)', createdAt: '2024-03-18T10:20:00' },
  { id: 'ph_3', type: 'charge', amount: 100000, balance: 120000, description: '포인트 충전 (카드)', createdAt: '2024-03-15T16:45:00' },
  { id: 'ph_4', type: 'use', amount: 30000, balance: 20000, description: '경매 입찰 (아이패드 프로)', createdAt: '2024-03-10T09:15:00' },
];

export const BLOCKED_USERS: User[] = [
  {
    id: 'user_99',
    nickname: '매너꽝',
    profileImage: 'https://picsum.photos/seed/blocked1/100/100',
    points: 0,
    mannerTemp: 12.5,
    joinedAt: '2022-11-05'
  },
  {
    id: 'user_100',
    nickname: '사기꾼조심',
    profileImage: 'https://picsum.photos/seed/blocked2/100/100',
    points: 0,
    mannerTemp: 5.0,
    joinedAt: '2023-02-10'
  }
];

const generateBids = (basePrice: number): BidHistory[] => {
  return [
    { id: 'bid_1', bidderId: 'user_10', bidderName: '입찰자A', amount: basePrice, timestamp: '2023-10-25T10:00:00' },
    { id: 'bid_2', bidderId: 'user_11', bidderName: '입찰자B', amount: basePrice + 5000, timestamp: '2023-10-25T10:30:00' },
    { id: 'bid_3', bidderId: 'user_12', bidderName: '입찰자C', amount: basePrice + 12000, timestamp: '2023-10-25T11:15:00' },
    { id: 'bid_4', bidderId: 'user_10', bidderName: '입찰자A', amount: basePrice + 15000, timestamp: '2023-10-25T12:00:00' },
  ];
};

export const MOCK_PRODUCTS: Product[] = [
  {
    id: 'prod_1',
    title: '아이패드 프로 12.9 5세대 M1',
    description: '거의 새것입니다. 박스 풀구성입니다. 기스 하나도 없어요.',
    category: Category.DIGITAL,
    seller: { ...CURRENT_USER, nickname: '테크매니아' },
    startPrice: 800000,
    currentPrice: 850000,
    minBidIncrement: 10000,
    instantPrice: 1200000,
    startTime: '2023-10-25T09:00:00',
    endTime: new Date(Date.now() + 86400000).toISOString(), // 24 hours later
    images: ['https://picsum.photos/seed/ipad/600/400', 'https://picsum.photos/seed/ipad2/600/400'],
    participantCount: 12,
    bids: generateBids(800000),
    status: 'active',
    location: '서울 강남구 역삼동',
    transactionMethod: 'delivery',
    isWishlisted: false
  },
  {
    id: 'prod_2',
    title: '빈티지 가죽 자켓',
    description: '10년 된 리얼 가죽 자켓입니다. 에이징이 예쁘게 되어있습니다.',
    category: Category.CLOTHING,
    seller: { ...CURRENT_USER, nickname: '패션피플', mannerTemp: 41.2 },
    startPrice: 50000,
    currentPrice: 72000,
    minBidIncrement: 1000,
    startTime: '2023-10-24T09:00:00',
    endTime: new Date(Date.now() + 3600000).toISOString(), // 1 hour later
    images: ['https://picsum.photos/seed/jacket/600/400'],
    participantCount: 5,
    bids: generateBids(50000),
    status: 'active',
    location: '부산 해운대구',
    transactionMethod: 'face-to-face',
    isWishlisted: true
  },
  {
    id: 'prod_3',
    title: '고급 원목 식탁 세트',
    description: '이사를 가게 되어 급하게 처분합니다. 직접 가져가셔야 합니다.',
    category: Category.FURNITURE,
    seller: { ...CURRENT_USER, nickname: '홈스타일링' },
    startPrice: 100000,
    currentPrice: 100000,
    minBidIncrement: 5000,
    startTime: '2023-10-20T09:00:00',
    endTime: new Date(Date.now() - 3600000).toISOString(), // 종료된 경매
    images: ['https://picsum.photos/seed/table/600/400'],
    participantCount: 0,
    bids: [],
    status: 'completed',
    location: '경기도 성남시',
    transactionMethod: 'face-to-face',
    isWishlisted: false,
    winnerId: CURRENT_USER.id
  },
  {
    id: 'prod_4',
    title: '해리포터 원서 전권',
    description: '영어 공부하려고 샀다가 한 번도 안 읽었습니다.',
    category: Category.BOOKS,
    seller: { ...CURRENT_USER, nickname: '북웜' },
    startPrice: 30000,
    currentPrice: 45000,
    minBidIncrement: 1000,
    startTime: '2023-10-25T15:00:00',
    endTime: new Date(Date.now() + 172800000).toISOString(), // 2 days later
    images: ['https://picsum.photos/seed/books/600/400'],
    participantCount: 3,
    bids: generateBids(30000),
    status: 'active',
    location: '서울 마포구',
    transactionMethod: 'delivery',
    isWishlisted: false
  },
  {
    id: 'prod_won_buy',
    title: '애플워치 울트라 2',
    description: '미개봉 새상품입니다. 선물 받았는데 필요 없어서 판매합니다.',
    category: Category.DIGITAL,
    seller: { 
      id: 'user_2',
      nickname: '테크매니아',
      profileImage: 'https://picsum.photos/seed/user2/100/100',
      points: 50000,
      mannerTemp: 42.5,
      joinedAt: '2022-05-20'
    },
    startPrice: 700000,
    currentPrice: 850000,
    minBidIncrement: 10000,
    startTime: '2023-10-20T09:00:00',
    endTime: '2023-10-24T18:00:00',
    images: ['https://picsum.photos/seed/watch/600/400'],
    participantCount: 8,
    bids: [
      { id: 'bid_w1', bidderId: 'user_20', bidderName: '입찰자X', amount: 700000, timestamp: '2023-10-20T10:00:00' },
      { id: 'bid_w2', bidderId: 'user_1', bidderName: '경매왕', amount: 850000, timestamp: '2023-10-24T17:30:00' },
    ],
    status: 'completed',
    location: '서울 마포구',
    transactionMethod: 'face-to-face',
    isWishlisted: false,
    winnerId: 'user_1'
  },
  {
    id: 'prod_won_sell',
    title: '닌텐도 스위치 OLED 화이트',
    description: '실사용 10회 미만입니다. 상태 아주 좋아요.',
    category: Category.DIGITAL,
    seller: CURRENT_USER,
    startPrice: 250000,
    currentPrice: 320000,
    minBidIncrement: 5000,
    startTime: '2023-10-21T09:00:00',
    endTime: '2023-10-24T20:00:00',
    images: ['https://picsum.photos/seed/switch/600/400'],
    participantCount: 15,
    bids: [
      { id: 'bid_s1', bidderId: 'user_30', bidderName: '홈스타일링', amount: 320000, timestamp: '2023-10-24T19:45:00' },
    ],
    status: 'completed',
    location: '서울 강남구',
    transactionMethod: 'delivery',
    isWishlisted: false,
    winnerId: 'user_3'
  }
];

export const NOTIFICATIONS: Notification[] = [
  { id: 'n_won_buy', message: "축하합니다! '애플워치 울트라 2' 상품을 낙찰받으셨습니다. 판매자와 대화를 시작해보세요.", read: false, link: '/inbox?tab=chat', createdAt: '2023-10-24T18:05:00', type: 'bid' },
  { id: 'n_won_sell', message: "회원님의 '닌텐도 스위치 OLED' 상품이 낙찰되었습니다. 구매자와 거래를 진행해주세요.", read: false, link: '/inbox?tab=chat', createdAt: '2023-10-24T20:05:00', type: 'bid' },
  { id: 'n1', message: "'아이패드 프로' 상품 입찰 경쟁이 치열합니다!", read: false, link: '/product/prod_1', createdAt: '2023-10-25T12:00:00', type: 'bid' },
  { id: 'n2', message: "관심 등록한 '빈티지 자켓' 경매가 곧 종료됩니다.", read: true, link: '/product/prod_2', createdAt: '2023-10-25T09:00:00', type: 'bid' },
  { id: 'n3', message: "새로운 팔로워가 생겼습니다!", read: false, link: '/mypage', createdAt: '2023-10-24T18:30:00', type: 'activity' },
  { id: 'n4', message: "시스템 점검 안내: 내일 새벽 2시부터 4시까지 점검이 예정되어 있습니다.", read: true, link: '/', createdAt: '2023-10-23T10:00:00', type: 'system' },
];

export const MOCK_CHATS: ChatRoom[] = [
  {
    id: 'chat_won_buy',
    productId: 'prod_won_buy',
    productTitle: '애플워치 울트라 2',
    productImage: 'https://picsum.photos/seed/watch/200/200',
    otherUser: {
      id: 'user_2',
      nickname: '테크매니아',
      profileImage: 'https://picsum.photos/seed/user2/100/100',
      role: 'seller'
    },
    lastMessage: '안녕하세요, 낙찰 축하드립니다! 직거래 장소 어디가 편하신가요?',
    lastMessageAt: '2023-10-24T18:10:00',
    unreadCount: 1,
    messages: []
  },
  {
    id: 'chat_won_sell',
    productId: 'prod_won_sell',
    productTitle: '닌텐도 스위치 OLED 화이트',
    productImage: 'https://picsum.photos/seed/switch/200/200',
    otherUser: {
      id: 'user_3',
      nickname: '홈스타일링',
      profileImage: 'https://picsum.photos/seed/user3/100/100',
      role: 'buyer'
    },
    lastMessage: '안녕하세요! 낙찰받은 사람입니다. 택배로 받고 싶은데 주소 보내드리면 될까요?',
    lastMessageAt: '2023-10-24T20:10:00',
    unreadCount: 1,
    messages: []
  },
  {
    id: 'chat_1',
    productId: 'prod_1',
    productTitle: '아이패드 프로 12.9 5세대 M1',
    productImage: 'https://picsum.photos/seed/ipad/200/200',
    otherUser: {
      id: 'user_2',
      nickname: '테크매니아',
      profileImage: 'https://picsum.photos/seed/user2/100/100',
      role: 'seller'
    },
    lastMessage: '네, 직거래 가능합니다.',
    lastMessageAt: '2023-10-25T14:20:00',
    unreadCount: 2,
    messages: []
  }
];

export const MOCK_REVIEW_TAGS: ReviewTag[] = [
  { id: 'tag_1', content: '응답이 빨라요', count: 12 },
  { id: 'tag_2', content: '친절하고 매너가 좋아요', count: 8 },
  { id: 'tag_3', content: '시간 약속을 잘 지켜요', count: 5 },
  { id: 'tag_4', content: '상품 상태가 설명과 같아요', count: 15 },
];

export const MOCK_REVIEWS: Review[] = [
  {
    id: 'rev_1',
    authorId: 'user_2',
    authorNickname: '쿨거래장인',
    authorProfileImage: 'https://picsum.photos/seed/user2/100/100',
    targetUserId: CURRENT_USER.id,
    productId: 'prod_1',
    productTitle: '아이패드 프로 12.9 5세대 M1',
    content: '배송도 빠르고 상품 상태도 너무 좋네요! 잘 쓰겠습니다.',
    isHidden: false,
    createdAt: '2023-10-20T14:30:00'
  },
  {
    id: 'rev_2',
    authorId: 'user_3',
    authorNickname: '미니멀리스트',
    authorProfileImage: 'https://picsum.photos/seed/user3/100/100',
    targetUserId: CURRENT_USER.id,
    productId: 'prod_5',
    productTitle: '맥북 에어 M2',
    content: '좋은 가격에 잘 샀습니다. 매너가 좋으시네요.',
    isHidden: false,
    createdAt: '2023-10-15T11:20:00'
  }
];

export const MOCK_NOTICES: Notice[] = [
  {
    id: 'notice_1',
    category: '점검',
    title: '시스템 정기 점검 안내 (2024년 5월 25일)',
    description: '안정적인 서비스 제공을 위해 시스템 정기 점검이 진행될 예정입니다. 점검 시간에는 서비스 이용이 일시적으로 중단되오니...',
    content: `안녕하세요, 옥션 마켓플레이스 운영팀입니다.
더욱 안정적이고 쾌적한 경매 서비스 환경을 제공해 드리기 위해 시스템 정기 점검을 실시할 예정입니다.

[점검 상세 안내]
- 점검 일시: 2024년 5월 22일(수) 오전 02:00 ~ 06:00 (약 4시간)
- 점검 대상: 웹사이트 전체 및 모바일 애플리케이션 전 서비스
- 점검 내용: 서버 안정화 작업, 데이터베이스 최적화 및 보안 업데이트

이용 제한 사항:
점검이 진행되는 동안에는 아래와 같은 모든 서비스 이용이 일시 중단됩니다.
- 실시간 경매 입찰 및 물품 등록
- 결제 서비스 및 마이페이지 정보 조회
- 회원 가입 및 정보 수정

경매 종료 시간이 점검 시간과 겹치는 물품의 경우, 점검 시간만큼 경매 종료 시간이 자동으로 연장될 예정입니다. 입찰 참여 시 이 점 유의하시기 바랍니다.

작업 상황에 따라 점검 시간이 단축되거나 다소 지연될 수 있으며, 변동 사항 발생 시 추가 공지를 통해 안내해 드리겠습니다.
회원 여러분의 너그러운 양해 부탁드리며, 보다 신뢰받는 서비스를 제공하기 위해 최선을 다하겠습니다.

감사합니다.`,
    isImportant: true,
    viewCount: 1240,
    createdAt: '2024-05-20T10:00:00'
  },
  {
    id: 'notice_2',
    category: '업데이트',
    title: "신규 경매 카테고리 '디지털 아트' 추가 안내",
    description: '많은 회원분들이 기다려 주셨던 디지털 아트 카테고리가 새롭게 오픈되었습니다. 지금 바로 혁신적인 예술 작품들을 만나보세요.',
    content: '디지털 아트 카테고리가 추가되었습니다...',
    isImportant: false,
    viewCount: 850,
    createdAt: '2024-05-18T14:00:00'
  },
  {
    id: 'notice_3',
    category: '이벤트',
    title: '여름맞이 수수료 제로 이벤트!',
    description: '본격적인 여름을 맞아 한 달간 모든 낙찰 수수료를 면제해 드립니다. 파격적인 혜택을 놓치지 마세요.',
    content: '여름맞이 수수료 제로 이벤트 안내...',
    isImportant: false,
    viewCount: 2100,
    createdAt: '2024-05-15T09:00:00'
  },
  {
    id: 'notice_4',
    category: '정책',
    title: '이용약관 및 개인정보 처리방침 개정 안내',
    description: '보다 나은 서비스 제공을 위해 이용약관 및 개인정보 처리방침이 일부 개정될 예정입니다. 변경 내용을 확인하시어 이용에 참고하시기 바랍니다.',
    content: '이용약관 개정 안내...',
    isImportant: false,
    viewCount: 560,
    createdAt: '2024-05-10T11:00:00'
  },
  {
    id: 'notice_5',
    category: '업데이트',
    title: '경매 마켓플레이스 모바일 앱 2.0 버전 출시',
    description: '더 빠르고 편리해진 새로운 앱을 경험해보세요. UI 전면 개편 및 알림 기능이 강화되었습니다.',
    content: '모바일 앱 2.0 출시 안내...',
    isImportant: false,
    viewCount: 3200,
    createdAt: '2024-05-05T15:00:00'
  }
];

export const MOCK_INQUIRIES: Inquiry[] = [
  {
    id: 'inq_1',
    userId: 'user_1',
    category: '버그 신고',
    bugType: '기능 작동 오류',
    title: '입찰 버튼이 클릭되지 않아요',
    content: '아이패드 프로 상품 상세 페이지에서 입찰하기 버튼을 눌러도 아무런 반응이 없습니다. 크롬 브라우저 사용 중입니다.',
    status: '답변 완료',
    answer: '안녕하세요, 경매왕님. 이용에 불편을 드려 죄송합니다. 해당 문제는 현재 수정 완료되었으며, 브라우저 캐시 삭제 후 다시 시도해 주시기 바랍니다.',
    answeredAt: '2024-05-21T10:00:00',
    createdAt: '2024-05-20T15:00:00'
  },
  {
    id: 'inq_2',
    userId: 'user_1',
    category: '환불 문의',
    title: '낙찰 취소 및 환불 요청합니다',
    content: '상품 설명과 실제 상품 상태가 너무 다릅니다. 환불 절차 안내 부탁드립니다.',
    status: '답변 대기중',
    createdAt: '2024-05-22T09:00:00'
  },
  {
    id: 'inq_3',
    userId: 'user_1',
    category: '기타',
    title: '매너 온도 기준이 궁금해요',
    content: '매너 온도가 어떻게 계산되는지, 그리고 어떻게 하면 올릴 수 있는지 궁금합니다.',
    status: '답변 완료',
    answer: '매너 온도는 거래 후 받은 후기와 매너 평가를 바탕으로 계산됩니다. 긍정적인 평가를 많이 받을수록 온도가 올라가며, 불친절하거나 약속을 어길 경우 내려갈 수 있습니다.',
    answeredAt: '2024-05-19T14:00:00',
    createdAt: '2024-05-18T11:00:00'
  }
];

export const MOCK_REPORTS: Report[] = [
  {
    id: 'rep_1',
    reporterId: 'user_2',
    targetId: 'prod_1',
    targetType: 'product',
    reason: '허위 매물',
    details: '실제 상품과 사진이 다릅니다.',
    status: 'pending',
    createdAt: '2024-05-22T10:00:00'
  },
  {
    id: 'rep_2',
    reporterId: 'user_3',
    targetId: 'user_4',
    targetType: 'user',
    reason: '비매너 채팅',
    details: '욕설을 사용합니다.',
    status: 'pending',
    createdAt: '2024-05-22T11:30:00'
  }
];

export const MOCK_HERO_BANNERS: HeroBanner[] = [
  {
    id: 'banner_1',
    type: 'hero',
    imageUrl: 'https://images.unsplash.com/photo-1449034446853-66c86144b0ad?auto=format&fit=crop&w=1920&q=80',
    title: '',
    subtitle: '',
    label: '',
    buttons: [],
    link: '/search',
    isActive: true,
    createdAt: '2024-01-01T00:00:00'
  },
  {
    id: 'banner_2',
    type: 'hero',
    imageUrl: 'https://images.unsplash.com/photo-1556742049-02e53f40d990?auto=format&fit=crop&w=1920&q=80',
    title: '',
    subtitle: '',
    label: '',
    buttons: [],
    link: '/signup',
    isActive: true,
    createdAt: '2024-01-02T00:00:00'
  },
  {
    id: 'banner_3',
    type: 'hero',
    imageUrl: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1920&q=80',
    title: '',
    subtitle: '',
    label: '',
    buttons: [],
    link: '/search',
    isActive: true,
    createdAt: '2024-01-03T00:00:00'
  },
  {
    id: 'ad_1',
    type: 'ad',
    imageUrl: 'https://images.unsplash.com/photo-1556742049-02e53f40d990?auto=format&fit=crop&w=1920&q=80',
    title: '',
    subtitle: '',
    buttons: [],
    link: '/signup',
    isActive: true,
    createdAt: '2024-01-04T00:00:00'
  },
  {
    id: 'ad_2',
    type: 'ad',
    imageUrl: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1920&q=80',
    title: '',
    subtitle: '',
    buttons: [],
    link: '/mypage/invite',
    isActive: true,
    createdAt: '2024-01-05T00:00:00'
  },
  {
    id: 'ad_3',
    type: 'ad',
    imageUrl: 'https://images.unsplash.com/photo-1556742049-02e53f40d990?auto=format&fit=crop&w=1920&q=80',
    title: '',
    subtitle: '',
    buttons: [],
    link: '/download',
    isActive: true,
    createdAt: '2024-01-06T00:00:00'
  }
];
