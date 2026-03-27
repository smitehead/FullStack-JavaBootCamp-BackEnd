export enum Category {
  DIGITAL = "디지털기기",
  FURNITURE = "가구/인테리어",
  CLOTHING = "의류",
  BOOKS = "도서",
  ETC = "기타",
}

export interface CategoryItem {
  id: string;
  name: string;
  subCategories?: CategoryItem[];
}

export type TransactionMethod = "face-to-face" | "delivery" | "both";

export interface UserSettings {
  auctionEnd: boolean;
  newBid: boolean;
  marketing: boolean;
  chat: boolean;
}

export interface User {
  id: string;
  nickname: string;
  profileImage: string;
  points: number;
  mannerTemp: number; // 매너온도
  joinedAt: string;
  email?: string;
  phoneNum?: string;
  address?: string;
  isActive?: boolean;
  isAdmin?: boolean;
  isSuspended?: boolean;
  suspensionEndDate?: string;
  suspensionReason?: string;
  isPermanentlySuspended?: boolean;
  isWithdrawn?: boolean;
  settings?: UserSettings;
  blockedUserIds?: string[];
  auctionCount?: number; // Added for admin management
  postCount?: number; // Added for admin management
}

export interface Report {
  id: string;
  reporterId: string;
  targetId: string; // User ID or Product ID
  targetType: "user" | "product";
  reason: string;
  details: string;
  status: "pending" | "resolved";
  createdAt: string;
}

export interface MannerHistory {
  id: string;
  userId: string;
  userNickname: string;
  previousTemp: number;
  newTemp: number;
  reason: string;
  adminId: string;
  createdAt: string;
}

export interface ActivityLog {
  id: string;
  adminId: string;
  adminNickname: string;
  action: string;
  targetId?: string;
  targetType?: "user" | "product" | "notice" | "inquiry" | "report";
  details: string;
  createdAt: string;
}

export interface BidHistory {
  id: string;
  bidderId: string;
  bidderName: string;
  amount: number;
  timestamp: string;
}

export interface Product {
  id: string;
  title: string;
  description: string;
  category: Category;
  seller: User;
  startPrice: number;
  currentPrice: number;
  minBidIncrement: number;
  instantPrice?: number;
  startTime: string;
  endTime: string;
  images: string[];
  participantCount: number;
  bids: BidHistory[];
  status: "active" | "completed" | "canceled";
  location: string;
  detailedAddress?: string;
  transactionMethod: TransactionMethod;
  isWishlisted?: boolean;
  wishlistCount?: number;
  winnerId?: string;
}

export type NotificationType = "bid" | "activity";

export interface Notification {
  id: string;
  message: string;
  read: boolean;
  link: string;
  createdAt: string;
  type: NotificationType;
}

export interface ChatMessage {
  id: string;
  senderId: string;
  content: string;
  createdAt: string;
}

export interface ChatRoom {
  id: string;
  productId: string;
  productTitle: string;
  productImage: string;
  otherUser: {
    id: string;
    nickname: string;
    profileImage: string;
    role: "seller" | "buyer";
  };
  lastMessage: string;
  lastMessageAt: string;
  unreadCount: number;
  messages: ChatMessage[];
}

export interface ReviewTag {
  id: string;
  content: string;
  count: number;
}

export interface Review {
  id: string;
  authorId: string;
  authorNickname: string;
  authorProfileImage: string;
  targetUserId: string;
  productId: string;
  productTitle: string;
  content: string;
  isHidden: boolean;
  createdAt: string;
}

export interface Order {
  id: string;
  productId: string;
  buyerId: string;
  sellerId: string;
  amount: number;
  status: "pending" | "paid" | "delivered" | "completed" | "canceled";
  shippingAddress?: string;
  shippingDetail?: string;
  transactionMethod: "face-to-face" | "delivery";
  depositedAmount: number;
  createdAt: string;
}

export type NoticeCategory = "업데이트" | "이벤트" | "점검" | "정책";

export interface Notice {
  id: string;
  category: NoticeCategory;
  title: string;
  content: string;
  description: string;
  isImportant: boolean;
  createdAt: string;
}

export type InquiryCategory = "버그 신고" | "환불 문의" | "계정 문의" | "기타";
export type BugType =
  | "기능 작동 오류"
  | "화면/UI 오류"
  | "데이터/정보 오류"
  | "로그인/계정 문제"
  | "속도/접속 저하"
  | "기타";
export type InquiryStatus = "답변 대기중" | "답변 완료";

export interface Inquiry {
  id: string;
  userId: string;
  category: InquiryCategory;
  bugType?: BugType;
  title: string;
  content: string;
  images?: string[];
  status: InquiryStatus;
  answer?: string;
  answeredAt?: string;
  createdAt: string;
}

export type BannerType = "hero" | "ad";

export interface HeroBanner {
  bannerNo: number;
  bannerType: BannerType;
  imgUrl: string;
  linkUrl: string;
  sortOrder: number;
  isActive: number;       // 1=활성, 0=비활성
  createdAt: string;
  endAt: string | null;
}

export interface WithdrawnUser {
  id: string;
  nickname: string;
  email: string;
  reason: string;
  details?: string;
  withdrawnAt: string;
}

export interface ProductRequestDto {
  sellerNo: number;
  categoryNo: number;
  title: string;
  description: string;
  tradeType: string;
  tradeEmdNo?: number;
  tradeAddrDetail?: string;
  startPrice: number;
  buyoutPrice?: number | null;
  minBidUnit: number;
  endTime: string;
}
