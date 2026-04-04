# 변경 내역

---

## 공지사항 CRUD API 구현 (2026-04-04)

### 새로 생성된 파일
- `domain/community/repository/NoticeRepository.java` — 공지사항 JPA Repository (카테고리 필터 + 검색 + 페이징)
- `domain/community/dto/NoticeRequestDto.java` — 공지 등록/수정 요청 DTO
- `domain/community/dto/NoticeResponseDto.java` — 공지 응답 DTO (프론트 Notice 타입 맞춤)
- `domain/community/service/NoticeService.java` — 공지 Service 인터페이스
- `domain/community/service/NoticeServiceImpl.java` — 공지 Service 구현체
- `domain/community/controller/NoticeController.java` — 공지 API 6개 엔드포인트

### 수정된 파일
- `domain/community/entity/Notice.java` — category, description, isImportant 컬럼 추가

### API 엔드포인트
- `GET /api/notices` — 목록 조회 (category 필터 + keyword 검색 + 페이징, 중요 공지 우선 정렬)
- `GET /api/notices/{noticeNo}` — 상세 조회 (삭제되지 않은 것만)
- `GET /api/notices/all` — 관리자용 전체 목록 (삭제 포함)
- `POST /api/notices` — 관리자 공지 등록
- `PUT /api/notices/{noticeNo}` — 관리자 공지 수정
- `DELETE /api/notices/{noticeNo}` — 관리자 공지 삭제 (소프트 삭제, IS_DELETED=1)

### Notice 엔티티 추가 컬럼
- `CATEGORY` (VARCHAR 20) — 업데이트/이벤트/점검/정책
- `DESCRIPTION` (VARCHAR 300) — 짧은 설명
- `IS_IMPORTANT` (NUMBER, 기본 0) — 중요 공지 여부

---

## 리뷰 기능 구현 + 매너온도 자동 계산 (2026-04-04)

### 새로 생성된 파일
- `domain/community/dto/ReviewRequestDto.java` — 리뷰 작성 요청 DTO (resultNo, rating 1~5, content)
- `domain/community/dto/ReviewResponseDto.java` — 리뷰 응답 DTO (writerNickname 포함)
- `domain/community/service/ReviewService.java` — 리뷰 CRUD + 매너온도 자동 계산
- `domain/community/controller/ReviewController.java` — 리뷰 API 3개 엔드포인트

### 수정된 파일
- `domain/community/repository/ReviewRepository.java` — 평균 별점 쿼리 `findAverageRatingByTargetNo` 추가

### API 엔드포인트
- `POST /api/reviews` — 리뷰 작성 (구매확정 거래만, 중복 방지, 매너온도 자동 반영)
- `GET /api/reviews/target/{memberNo}` — 특정 회원이 받은 리뷰 목록
- `GET /api/reviews/my` — 내가 작성한 리뷰 목록

### 매너온도 자동 계산
#### 1. 리뷰 별점 기반 (ReviewService)
- 공식: `36.5 + (평균별점 - 3.0) * weight`
- weight: `min(리뷰수, 10) * 0.5` (최대 5.0)
- 범위: 0 ~ 100
- 별점 3점 = 변동 없음 / 5점 = 상승 / 1점 = 하락

#### 2. 구매확정 시 소폭 상승 (AuctionResultServiceImpl)
- `confirmPurchase()` 시 구매자 + 판매자 각각 +0.2
- 상한 100 제한

---

## 전체 흐름 정리 (2026-04-02 기준)

### 백엔드 핵심 흐름

#### 1. 로그인 → SSE 연결
```
POST /api/auth/login
  → JWT 발급 + memberNo 반환
  → 프론트 sessionStorage 저장

GET /api/sse/subscribe?clientId={memberNo}
  → SseService: emitter를 ConcurrentHashMap에 등록
  → 이후 백엔드 이벤트 → emitter.send() 로 실시간 전송
  → 이벤트 종류: notification / pointUpdate / priceUpdate / forceLogout
```

#### 2. 경매 입찰 흐름
```
POST /api/bids
  → BidServiceImpl: 비관적 락 (memberNo 오름차순 순서 고정, 데드락 방지)
  → 포인트 차감 / 이전 최고입찰자 환불
  → SseService.broadcastPriceUpdate() → 상품 구독자 전체 SSE
  → AutoBidService.triggerAutoBids() → 자동입찰 연쇄 처리
```

#### 3. 경매 종료 흐름 (AuctionScheduler — 30초 주기)
```
종료 시각 지난 ACTIVE 상품 조회
  → 최고 입찰 존재: AuctionResult 생성, 상품 status=완료
      → 낙찰자에게 알림 2개 (낙찰 축하 + 결제 요청)
      → 판매자에게 알림 1개 (낙찰 완료)
      → 유찰자 전원에게 알림 (다음 기회 안내)
  → 입찰 없음: 상품 status=취소
```

#### 4. 낙찰 이후 거래 흐름
```
결제: POST /api/auction-results/{no}/pay
  → 구매자 포인트 차감 + PointHistory(낙찰대금결제) 기록
  → SSE pointUpdate → 구매자 포인트 실시간 갱신
  → 판매자에게 알림 (결제 완료, 상품 준비 요청)

구매확정: POST /api/auction-results/{no}/confirm
  → 판매자 포인트 지급 + PointHistory(판매대금정산) 기록
  → SSE pointUpdate → 판매자 포인트 실시간 갱신
  → 판매자에게 알림 (정산 금액 포함)
```

#### 5. 알림 전달 흐름
```
NotificationService.sendAndSaveNotification(memberNo, type, content, linkUrl)
  → NOTIFICATION 테이블 저장 (isRead=0)
  → SseService.sendNotification(memberNo, notiDto)
      → 해당 회원 emitter에 'notification' 이벤트 전송
      → emitter 없으면 조용히 무시 (오프라인 회원)
  → 프론트: setNotifications(prev => [newNoti, ...prev]) 즉시 반영
```

#### 6. 관리자 기능 흐름
```
관리자 로그인 → 프론트 fetchAdminData() (5개 API 병렬)
  /admin/members, /admin/reports, /admin/members/manner-history
  /admin/activity-logs, /admin/products

관리자 액션 (정지/매너온도/포인트/권한/신고처리)
  → PUT API 호출
  → AdminServiceImpl/ReportServiceImpl: DB 업데이트
  → ActivityLog 자동 기록 (admin, targetId, action, details)
  → 대상 회원에게 알림 자동 발송 (SSE 실시간)
  → 프론트 fetchAdminData() + refreshActivityLogs() 재호출
```

#### 7. 배너 관리 흐름
```
BannerScheduler (1분 주기)
  → endAt이 지난 isActive=1 배너 → isActive=0 자동 처리

프론트 배너 관리
  → GET /api/banners/all (관리자, 활성/비활성 전체)
  → GET /api/banners?type=hero (메인, 활성만)
  → POST/PUT/DELETE/PATCH 토글
```

---

## 날짜: 2026-04-02

### [백엔드]

---

#### 31. 경매 관련 알림 자동 발송 구현 (오수환)

##### 배경
낙찰/유찰/결제완료/구매확정/경매취소 등 주요 경매 이벤트 발생 시 관련 회원에게 알림이 전송되지 않았음.

##### 수정

**`BidHistoryRepository.java`**
- `findDistinctBiddersByProductNo()` — 특정 상품 전체 입찰자 목록 (취소 제외)
- `findDistinctBiddersExcluding()` — 특정 상품 입찰자 목록 (낙찰자 제외, 유찰 알림용)

**`AuctionScheduler.java`**
- `NotificationService` 의존성 주입 추가
- 낙찰자 → `"축하합니다! [상품명] 경매에 최종 낙찰되었습니다."` (type: bid)
- 낙찰자 → `"[상품명]의 결제를 진행해 주세요. (24시간 내 미결제 시 취소 가능)"` (type: bid)
- 판매자 → `"[상품명]이 최종 낙찰되었습니다."` (type: bid)
- 유찰자 전원 → `"[상품명] 경매가 종료되었습니다. 다음 기회를 노려보세요!"` (type: bid)

**`AuctionResultServiceImpl.java`**
- `@Slf4j`, `NotificationService` 의존성 주입 추가
- `processPayment()` → 판매자에게 결제완료 알림 (type: activity)
- `confirmPurchase()` → 판매자에게 구매확정 + 포인트 정산액 알림 (type: activity)

**`ProductServiceImpl.java`**
- `@Slf4j`, `NotificationService` 의존성 주입 추가
- `cancelAuctionByAdmin()` → 입찰자 전원에게 경매 취소 알림 (type: bid)

##### 공통 패턴
- 모든 알림 전송을 try-catch로 감싸 알림 실패가 트랜잭션 롤백을 유발하지 않도록 격리

---

#### 32. 영어 주석 한글화 (오수환)

##### 수정
- **`WishlistServiceImpl.java`**
  - `// Removed from wishlist` → `// 위시리스트에서 제거됨`
  - `// Added to wishlist` → `// 위시리스트에 추가됨`

---

## 날짜: 2026-03-31

### [백엔드]

---

#### 30. SSE 안정화 — 좀비 emitter 및 재연결 루프 수정 (오수환)

##### 문제
- 프론트엔드 React StrictMode(개발 환경)가 useEffect를 두 번 실행하면서 같은 `clientId`로 SSE를 두 번 연결
- 첫 번째 연결의 `onCompletion`이 두 번째 emitter까지 `remove(clientId)`로 삭제 → 알림 전송 시 emitter 없음
- `subscribe` 시 기존 emitter에 `complete()` 호출 → 브라우저 자동 재연결 루프 발생

##### 수정
- **`SseService.java`**
  - `onCompletion` / `onTimeout` / `onError` 콜백을 `remove(clientId)` → `remove(clientId, emitter)`로 변경
    - 새 emitter가 등록된 후 기존 emitter의 cleanup이 새 것을 삭제하는 문제 방지
  - `subscribe` 시 기존 emitter `complete()` 호출 제거 → 맵에서만 교체 (재연결 루프 방지)
  - `sendPointUpdate`, `sendForceLogout`도 동일하게 `remove(clientId, emitter)` 적용
  - 디버그용 `log.info` 전체 제거 및 Logger 의존성 제거

---

## 날짜: 2026-03-30

### [백엔드]

---

#### 28. 자동입찰 기능 구현

##### 신규 생성
- **`AutoBid.java`** — 자동입찰 엔티티 (`AUTO_BID` 테이블, AUTO_BID_SEQ 시퀀스, isActive 플래그)
- **`AutoBidRequestDto.java`** — 자동입찰 등록 요청 DTO (`productNo`, `maxPrice`)
- **`AutoBidResponseDto.java`** — 자동입찰 응답 DTO
- **`AutoBidRepository.java`** — 자동입찰 레포지터리
  - `findActiveByProductNo()` — 상품의 활성 자동입찰 목록 (최대가 높은 순)
  - `findByMemberNoAndProductNoAndIsActive()` — 특정 회원의 특정 상품 자동입찰 조회
  - `existsByMemberNoAndProductNoAndIsActive()` — 자동입찰 존재 여부 확인
- **`AutoBidService.java`** — 자동입찰 서비스 인터페이스
  - `registerAutoBid()` — 자동입찰 등록 (이미 있으면 maxPrice 갱신)
  - `cancelAutoBid()` — 자동입찰 취소
  - `triggerAutoBids()` — 입찰 발생 시 자동입찰 트리거
  - `getActiveAutoBid()` — 특정 회원의 특정 상품 활성 자동입찰 조회

##### 수정
- **`AuctionResultServiceImpl.java`** — `processPayment()` 구매자 포인트 차감 로직 추가
  - 결제 시 구매자 포인트 잔액 검증 후 낙찰 금액 차감
  - `PointHistory` 타입 `"낙찰대금결제"` 로 이력 기록
  - 구매자 포인트 SSE 실시간 반영 (`sseService.sendPointUpdate()`)
- **`ProductServiceImpl.java`**
  - `getMyBiddingProducts()` — 결제완료/구매확정된 낙찰 상품은 입찰내역에서 제외 (구매내역으로 이동)
  - `getMyPurchasedProducts()` — 구매확정뿐만 아니라 결제완료 상태도 포함하도록 조건 확장
- **`SecurityConfig.java`** — CORS 허용 Origin에 EC2 서버 IP 추가 (`http://54.164.62.214`, `:3000`, `:5173`)

---

#### 29. ProductController 병합 충돌 해결

##### 수정
- **`ProductController.java`** — 이전 브랜치 병합 과정에서 발생한 충돌 해결

---

## 날짜: 2026-03-27

### [백엔드]

---

#### 0. 환경변수 분리 (.env)

##### 신규 생성
- **`.env`** — 민감 정보 저장 (git 제외): DB 접속 정보, JWT secret, Gmail SMTP 계정
- **`ENV_SETUP.md`** — 팀원용 환경변수 설정 가이드

##### 수정
- **`application.properties`** — DB/JWT/SMTP 하드코딩 값 → `${변수명}` 환경변수 참조로 교체
- **`ProjectApplication.java`** — 서버 시작 시 `.env` 파일을 Java 기본 기능으로 직접 파싱하여 로드 (외부 라이브러리 미사용)
- **`.gitignore`** — `.env` 추가 (깃 업로드 차단)

---

#### 1. 이메일 인증 SMTP 구현

##### 신규 생성
- **`EmailService.java`** — 인증번호 생성(6자리)/발송/검증, ConcurrentHashMap 메모리 저장, 3분 만료

##### 수정
- **`pom.xml`** — `spring-boot-starter-mail` 의존성 추가
- **`application.properties`** — Gmail SMTP 설정 추가
- **`AuthController.java`** — 엔드포인트 2개 추가

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/send-email-code` | 인증번호 이메일 발송 |
| POST | `/api/auth/verify-email-code` | 인증번호 검증 |

---

#### 2. 관리자 활동 로그 조회 API

##### 신규 생성
- **`ActivityLogResponseDto.java`** — 활동 로그 응답 DTO (`from(ActivityLog)` + `setAdminNickname`)
- **`AdminActivityLogController.java`** — 활동 로그 조회 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/admin/activity-logs` | 전체 활동 로그 (최신순) |
| GET | `/api/admin/activity-logs?targetType={type}` | 대상 유형별 필터 |

##### 수정
- **`AdminService.java`** — `getAllActivityLogs()`, `getActivityLogsByTargetType()` 인터페이스 추가
- **`AdminServiceImpl.java`** — 구현 추가 (관리자 닉네임 조회 포함)

---

#### 3. 관리자 알림 관리 API

##### 신규 생성
- **`AdminNotificationController.java`** — 관리자 알림 관리 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/admin/notifications/broadcast` | 전체 활성 회원에게 알림 발송 + 활동 로그 기록 |
| GET | `/api/admin/notifications/recent` | 최근 발송 알림 50건 조회 |

##### 수정
- **`NotificationService.java`** — `getAllRecentNotifications(int limit)` 메서드 추가

---

#### 4. 경매 관리 API

##### 신규 생성
- **`AdminProductResponseDto.java`** — 관리자용 상품 응답 DTO (productNo, title, sellerNickname, 가격, 참여자수, status)
- **`AdminProductController.java`** — 경매 관리 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/admin/products` | 전체 상품 목록 (삭제 제외, 최신순) |
| PUT | `/api/admin/products/{productNo}/cancel` | 경매 강제 종료 + 활동 로그 기록 |

##### 수정
- **`ProductService.java`** — `getAllProductsForAdmin()`, `cancelAuctionByAdmin()` 인터페이스 추가
- **`ProductServiceImpl.java`** — 구현 추가 (판매자 닉네임/이미지/참여자수 배치 쿼리, 진행중 경매만 취소 가능)
- **`ProductRepository.java`** — `findByIsDeletedOrderByCreatedAtDesc()` 추가

---

#### 5. 관리자 JWT 권한 체크 구현

##### 수정
- **`JwtUtil.java`** — `generateToken(Long memberNo, String userId, Integer isAdmin)` isAdmin 클레임 추가, `getIsAdmin(String token)` 메서드 추가
- **`AuthServiceImpl.java`** — `generateToken()` 호출 시 `member.getIsAdmin()` 전달
- **`LoginResponseDto.java`** — `isAdmin` 필드 추가
- **`JwtAuthenticationFilter.java`** — isAdmin 클레임 추출 후 `ROLE_ADMIN` 또는 `ROLE_USER` 권한 등록
- **`SecurityConfig.java`** — `/api/admin/**` 경로에 `.hasRole("ADMIN")` 권한 체크 추가

---

#### 5-1. 배너 타입 쿼리 파라미터 필터링

##### 수정
- **`HeroBannerRepository.java`** — `findActiveBannersByType(String type)` 쿼리 추가 (활성 배너를 타입별로 조회)
- **`HeroBannerServiceImpl.java`** — `type` 파라미터로 히어로배너/광고배너 분기 처리
- **`HeroBannerController.java`** — `@RequestParam(required = false) String type` 추가 (`?type=hero`, `?type=ad`)

---

#### 5-2. 만료 배너 자동 비활성화 스케줄러

##### 신규 생성
- **`BannerScheduler.java`** — 매 1분마다 실행, `endAt`이 지난 활성 배너를 자동으로 `isActive=0` 처리

---

#### 6. 히어로배너 활동 로그 기록

##### 수정
- **`HeroBannerController.java`**
  - `ActivityLogRepository` 의존성 추가
  - 모든 CRUD 엔드포인트에 `Authentication` 파라미터 추가
  - `logActivity()` private 메서드 추가
  - `create`, `update`, `delete`, `toggleActive` 실행 시 활동 로그 자동 기록 (`targetType: "banner"`)

---

### [프론트엔드]

- 없음

---

## 날짜: 2026-03-26

### [백엔드]

---

#### 5. 배너 이미지 파일 업로드 지원

##### 수정
- **`FileStore.java`** — `storeGenericFile()` 메서드 추가 (배너/프로필 등 범용 이미지 저장, UUID 파일명 반환)
- **`ImageController.java`** — `POST /api/images/upload` 엔드포인트 추가 (`MultipartFile → { "url": "/api/images/uuid.jpg" }`)

---

#### 6. 관리자 페이지 4대 기능 일괄 구현

##### 신규 생성

**DTO (7개)**

| 파일 | 용도 |
|------|------|
| `AdminMemberResponseDto.java` | 관리자용 회원 응답 (password 제외, 정지/권한 정보 포함) |
| `SuspendRequestDto.java` | 회원 정지 요청 (`suspendDays`, `suspendReason`) |
| `MannerTempRequestDto.java` | 매너온도 변경 요청 (`newTemp`, `reason`) |
| `PointsRequestDto.java` | 포인트 증감 요청 (`pointAmount`) |
| `ReportResponseDto.java` | 신고 응답 (신고자/피신고자 닉네임 포함) |
| `ReportResolveRequestDto.java` | 신고 처리 요청 (`status`, `penaltyMsg`) |
| `MannerHistoryResponseDto.java` | 매너온도 변동 이력 응답 (닉네임 포함) |

**Repository**
- `ReportRepository.java` — 전체/상태별/대상별 신고 조회

**Service (4개)**
- `AdminService.java` (인터페이스) — 회원 관리 기능 정의
- `AdminServiceImpl.java` — 회원 목록/검색, 정지/해제, 매너온도, 포인트, 권한, 매너온도 이력
- `ReportService.java` (인터페이스) — 신고 관리 기능 정의
- `ReportServiceImpl.java` — 신고 목록/상태별 조회, 신고 처리 (상태변경 + 제재 + 알림)

**Controller (2개)**
- `AdminMemberController.java` (`/api/admin/members`) — 회원 관리 API
- `AdminReportController.java` (`/api/admin/reports`) — 신고 관리 API

##### API 엔드포인트

**회원 관리 (`/api/admin/members`)**

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/admin/members?keyword=xxx` | 회원 목록 (닉네임/이메일 검색) |
| PUT | `/api/admin/members/{memberNo}/suspend` | 정지 (일수 + 사유, 999=영구) |
| PUT | `/api/admin/members/{memberNo}/unsuspend` | 정지 해제 |
| PUT | `/api/admin/members/{memberNo}/manner-temp` | 매너온도 변경 (사유 기록) |
| PUT | `/api/admin/members/{memberNo}/points` | 포인트 증감 |
| PUT | `/api/admin/members/{memberNo}/role` | 권한 변경 (`{ "isAdmin": 1 }`) |
| GET | `/api/admin/members/manner-history?memberNo=xxx` | 매너온도 변동 이력 |

**신고 관리 (`/api/admin/reports`)**

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/admin/reports?status=접수` | 신고 목록 (상태 필터) |
| PUT | `/api/admin/reports/{reportNo}/resolve` | 신고 처리 (상태변경 + 제재 + 알림) |

##### 기존 파일 수정
- **`MemberRepository.java`** — `findAllByOrderByJoinedAtDesc()`, `searchByKeyword()`, `findByNickname()` 추가
- **`MannerHistoryRepository.java`** — `findAllByOrderByCreatedAtDesc()` 추가

##### 부가 기능
- **활동 로그 자동 기록**: 모든 관리자 액션 시 `ActivityLog` 테이블에 자동 기록
- **알림 자동 발송**: 정지/해제/신고처리 시 대상 회원에게 SSE 실시간 알림
- **매너온도 이력**: 변경 시 `MannerHistory` 테이블에 변동 전/후 온도 + 사유 기록

---

#### 7. 배너 관리 API 보완

##### 수정
- **`HeroBanner.java`** — `BANNER_TYPE` 컬럼 추가 (`VARCHAR2(10)`, 기본값 `"hero"`)
- **`HeroBannerRequestDto.java`** — `bannerType` 필드 추가
- **`HeroBannerResponseDto.java`** — `bannerType` 필드 추가
- **`HeroBannerRepository.java`** — `findAllByOrderBySortOrderAsc()` 추가
- **`HeroBannerServiceImpl.java`** — `getAllBanners()`, `toggleActive()` 추가, `create`/`update`에 `bannerType` 반영

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/banners/all` | 관리자용 전체 배너 (활성/비활성 모두) |
| PATCH | `/api/banners/{bannerNo}/toggle` | 배너 활성화/비활성화 토글 |

---

#### 8. 김태우 커밋 반영 (ba24831)

> SSE 최적화, 데드락 방지, 이미지 처리 개선, 무한스크롤/정렬 추가

- **`BidServiceImpl.java`** — 데드락 방지(비관적 락 순서 고정), 본인 재입찰 차단, SSE 발송 격리, 입찰 취소 환불
- **`SseService.java`** — `sendPointUpdate()` 추가, `broadcastPriceUpdate()` ConcurrentModification 방지
- **`ProductServiceImpl.java`** — 무한스크롤/정렬 기능 추가
- **`ImageController.java`** — 이미지 확장자 검증 추가
- **`FileStore.java`** — 이미지 처리 개선
- **`NotificationService.java`** — SSE 전송 데이터를 구조화된 Map 객체로 변경
- **`BidServiceImpl.java`** — 알림 type `"입찰"` → `"bid"` 로 통일
- **`SseController.java`** — `@CrossOrigin(origins = "*")` 추가

---

### [프론트엔드]

- 없음

---

## 날짜: 2026-03-25

### [백엔드]

- CORS 설정 수정 (와일드카드 → 실제 도메인)
- memberNo 보안 취약점 수정 (클라이언트 전달 → Authentication 서버 추출)
- 테스트 엔드포인트 제거 (NotificationController)
- AuctionScheduler 실행 주기 수정 (1초 → 30초)
- 예외 무시 패턴 개선 (BidServiceImpl)
- 상품 목록 N+1 쿼리 제거
- 동시 회원가입 중복 방지 핸들러 추가

### [프론트엔드]

- 없음

---

## 날짜: 2026-03-24

### [백엔드]

---

#### 9. 다기기 동시 로그인 방지 구현

##### 수정
- **`Member.java`** — `currentToken` 필드 추가 (`CURRENT_TOKEN VARCHAR2(500)`, nullable)
- **`AuthService.java`** — `logout(Long memberNo)` 메서드 추가
- **`AuthServiceImpl.java`** — 로그인 시 `currentToken` 저장, 로그아웃 시 null 초기화, `SseService.sendForceLogout()` 호출
- **`JwtAuthenticationFilter.java`** — DB `currentToken`과 요청 토큰 비교, 불일치 시 401
- **`AuthController.java`** — `logout()`에서 `authService.logout(memberNo)` 호출
- **`SseService.java`** — `sendForceLogout(Long memberNo)` 메서드 추가

---

#### 10. 알림함 API 구현

##### 신규 생성
- **`NotificationResponseDto.java`** — 알림 응답 DTO
- **`NotificationController.java`** — 알림 CRUD 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/notifications` | 로그인 회원의 알림 목록 (최신순) |
| GET | `/api/notifications/unread-count` | 미읽음 알림 개수 |
| PATCH | `/api/notifications/{notiNo}/read` | 단일 알림 읽음 처리 |
| PATCH | `/api/notifications/read-all` | 전체 알림 읽음 처리 |

##### 수정
- **`NotificationRepository.java`** — `markAllAsRead()` JPQL UPDATE 추가
- **`NotificationService.java`** — `getNotifications()`, `getUnreadCount()`, `markAllAsRead()` 추가

---

#### 11. JWT 인증 + 보안 구현

##### 신규 생성
- **`JwtUtil.java`** — JWT 토큰 생성/검증/파싱
- **`JwtAuthenticationFilter.java`** — Bearer 토큰 추출 + SecurityContext 등록
- **`GlobalExceptionHandler.java`** — 전역 예외 처리 (400/401/409/500)
- **`AuthService.java`** / **`AuthServiceImpl.java`** — 로그인 서비스
- **`AuthController.java`** — `POST /api/auth/login`, `POST /api/auth/logout`
- **`LoginRequestDto.java`** / **`LoginResponseDto.java`** — 로그인 DTO

##### 수정
- **`SecurityConfig.java`** — BCrypt 빈, JWT 필터, CORS 설정, STATELESS 세션
- **`MemberRequestDto.java`** — Bean Validation 어노테이션 추가
- **`MemberServiceImpl.java`** — BCrypt 암호화, 14세 미만 제한, 중복 검증
- **`MemberController.java`** — `@Valid` 추가, 중복 확인 엔드포인트 3개 추가
- **`MemberRepository.java`** — `existsBy*` → COUNT 기반 쿼리 (Oracle 11g 호환)
- **`HeroBanner.java`** — `@Builder.Default` 추가
- **`application.properties`** — JWT 설정, Oracle Dialect → `OracleLegacyDialect`
- **`pom.xml`** — jjwt 의존성 추가 (0.12.6)

---

### [프론트엔드]

- 없음

---

## API 테스트 가이드 (Thunder Client 기준)

### 1. 회원가입
- **POST** `http://localhost:8080/api/members`
- Body: `{ userId, password, nickname, email, phoneNum, emdNo, addrDetail, birthDate }`

### 2. 로그인
- **POST** `http://localhost:8080/api/auth/login`
- Body: `{ userId, password }`
- 응답: `{ token, memberNo, userId, nickname }`

### 3. 로그아웃
- **POST** `http://localhost:8080/api/auth/logout`
- Header: `Authorization: Bearer {token}`

### 4. 중복 확인
- **GET** `/api/members/check-userid?userId=xxx`
- **GET** `/api/members/check-nickname?nickname=xxx`
- **GET** `/api/members/check-email?email=xxx`

### 5. JWT 인증 필요 API
- 로그인 후 받은 token을 Header에 추가: `Authorization: Bearer {token}`

## 날짜: 2026-03-25

CORS 설정 수정 (와일드카드 → 실제 도메인)
memberNo 보안 취약점 수정 (클라이언트 전달 → Authentication 서버 추출)
테스트 엔드포인트 제거 (NotificationController)
AuctionScheduler 실행 주기 수정 (1초 → 30초)
예외 무시 패턴 개선 (BidServiceImpl)
상품 목록 N+1 쿼리 제거
동시 회원가입 중복 방지 핸들러 추가

---

## 날짜: 2026-03-27 (낙찰 페이지 구현 및 버그 수정)

---

### 12. @EnableScheduling 누락 수정

#### 수정
- **`ProjectApplication.java`** — `@EnableScheduling` 추가
  - 누락으로 인해 `AuctionScheduler`의 `@Scheduled` 메서드가 아예 실행되지 않던 문제 수정
  - 추가 후 30초마다 경매 종료 자동 낙찰 처리 정상 동작

---

### 13. AuctionResult 전체 스택 신규 구현

#### 신규 생성
- **`AuctionResultResponseDto.java`** — 낙찰 결과 응답 DTO (resultNo, status, finalPrice, tradeType, images, seller, deliveryAddrDetail 등)
- **`AuctionResultService.java`** — 서비스 인터페이스
- **`AuctionResultServiceImpl.java`** — 서비스 구현체 (낙찰자 본인 확인, 상태 전이 검증)
- **`AuctionResultController.java`** — 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/auction-results/product/{productNo}` | 낙찰 결과 상세 조회 (낙찰자 본인만) |
| POST | `/api/auction-results/{resultNo}/pay` | 결제 처리 (배송대기 → 결제완료) |
| POST | `/api/auction-results/{resultNo}/confirm` | 구매 확정 (결제완료 → 구매확정) |
| POST | `/api/auction-results/{resultNo}/cancel` | 거래 취소 |

#### 수정
- **`AuctionResultServiceImpl.java`** — `processPayment()` address 미저장 버그 수정 (`address + " " + addressDetail` 합쳐서 저장)

---

### 14. 판매자 포인트 지급 구현

#### 수정
- **`AuctionResultServiceImpl.java`** — `processPayment()`에 판매자 포인트 지급 로직 추가
  - 결제 시 낙찰 금액을 판매자 포인트에 지급
  - `PointHistory` 타입 `"낙찰대금수령"` 으로 이력 기록
- **`PointHistoryRepository`** 의존성 추가

---

### 15. 마이페이지 입찰·구매 탭 분리 백엔드 지원

#### 신규 생성
- **`BidHistoryRepository.java`** — 낙찰 여부 배치 조회 쿼리 3개 추가
  - `findWonProductNosByMemberNo()` — 내가 낙찰받은 상품 번호 전체
  - `findWonProductNosInList()` — 특정 상품 목록에서 낙찰받은 것만 (N+1 방지 배치)
  - `findWinnerByProductNo()` — 특정 상품의 낙찰 입찰 기록

#### 수정
- **`ProductListResponseDto.java`** — `bidStatus` 필드 추가 (`"bidding"` / `"won"` / `"lost"`)
- **`ProductServiceImpl.java`**
  - `getMyBiddingProducts()` — 입찰 내역 + bidStatus 계산 (`toProductListDtosWithBidStatus()`)
  - `getMyPurchasedProducts()` — 구매확정 완료 상품만 반환 신규 추가
- **`ProductService.java`** — `getMyPurchasedProducts()` 인터페이스 추가
- **`ProductController.java`** — `GET /api/products/my-purchased` 엔드포인트 추가

---

### 16. GlobalExceptionHandler SSE 충돌 수정

#### 수정
- **`GlobalExceptionHandler.java`** — `AsyncRequestTimeoutException` 전용 핸들러 추가
  - SSE 연결 타임아웃 시 JSON 응답 시도로 발생하던 `HttpMessageNotWritableException` 방지

---

## 김태우 날짜: 2026-03-28 (스케줄러 최적화 및 실시간 낙찰 처리 구현)

---

### 17. PRODUCT 테이블 복합 인덱스 추가

#### SQL
- **`PRODUCT (STATUS, END_TIME)` 복합 인덱스 추가 예정**
  - 스케줄러가 30초마다 실행하는 `WHERE STATUS = 0 AND END_TIME < :now` 쿼리 최적화
  - Full Table Scan → Index Range Scan 개선 (하루 2,880회 반복 쿼리)
  - 컬럼 순서: 등치 조건(STATUS) 먼저, 범위 조건(END_TIME) 나중

---

### 18. AuctionExpiryWatchdog 신규 구현 (경매 종료 정각 처리)

#### 신규 생성
- **`scheduler/AuctionExpiryWatchdog.java`** — 상품 등록 시 endTime 정각에 낙찰 처리 1회 예약
  - `TaskScheduler.schedule(task, Instant)`로 endTime 정각에 `processOne()` 실행
  - `@PostConstruct`로 서버 재시작 시 DB에서 미종료 경매 자동 재예약
  - `ConcurrentHashMap<Long, ScheduledFuture<?>>` 으로 스레드 안전 예약 관리
  - 경매 삭제/강제종료 시 `cancel()` 호출로 예약 취소
- **`config/SchedulerConfig.java`** — `TaskScheduler` 빈 등록 (poolSize=5, prefix="scheduler-")

#### 수정
- **`ProductRepository.java`** — `findByStatusAndEndTimeAfter()` 쿼리 추가 (재시작 복구용)
- **`ProductServiceImpl.java`**
  - `save()` — 상품 등록 시 `auctionExpiryWatchdog.scheduleClose()` 호출
  - `deleteProduct()` — 삭제 시 `auctionExpiryWatchdog.cancel()` 호출
  - `cancelAuctionByAdmin()` — 강제종료 시 `auctionExpiryWatchdog.cancel()` 호출

#### 역할 분담
| | 역할 |
|---|---|
| `AuctionExpiryWatchdog` | endTime 정각에 즉시 처리 (주) |
| `AuctionScheduler` | 30초 폴링, Watchdog 누락 상품 보완 (백업) |
| `AuctionClosingService.processOne()` | `status != 0` 체크로 중복 처리 방지 |

---

### 19. 낙찰 알림 @TransactionalEventListener 적용 (커밋 후 발송)

#### 신규 생성
- **`scheduler/AuctionClosedEvent.java`** — 낙찰 완료 이벤트 record (엔티티 대신 primitive 값만 보유)
- **`scheduler/AuctionNotificationListener.java`** — `@TransactionalEventListener(AFTER_COMMIT)`으로 커밋 완료 후 알림 발송
  - `@Transactional(REQUIRES_NEW)` — 낙찰 실패자 DB 조회를 위한 새 트랜잭션

#### 수정
- **`AuctionClosingService.java`** — `sendNotifications()` 제거 → `eventPublisher.publishEvent()` 로 교체
  - 기존: 트랜잭션 내에서 알림 발송 → 커밋 실패 시 알림 오발송 가능
  - 변경: 트랜잭션 커밋 완료 후에만 알림 발송 → DB 확정 후 발송 보장

---

## 날짜: 2026-03-28 (버그 수정 — SSE, 스케줄러 중복 처리)

---

### 20. SSE 초기 더미 메시지 파싱 오류 수정

#### 문제
`SseService.subscribe()`에서 첫 더미 메시지를 `sendToClient()`로 전송 시 이벤트명이 `"notification"`으로 발송됨.
프론트 `AppContext.tsx`가 `notification` 이벤트 수신 시 `JSON.parse()` 시도 → `"SSE Connected"`는 JSON 아님 → `SyntaxError`.

#### 수정
- **`SseService.java`** — 초기 더미 메시지를 `"notification"` 이벤트 대신 `"connect"` 이벤트명으로 전송
  - 프론트에서 `"connect"` 이벤트는 별도 리스너 없으므로 자동 무시됨

---

### 21. SSE 클라이언트 정상 연결 해제 시 zombie emitter 수정

#### 문제
SSE 클라이언트가 연결을 먼저 끊은 상태에서 서버가 알림 전송 시 `IOException(WSAECONNABORTED)` 발생.
catch 블록에서 emitter를 map에서 제거했으나 `completeWithError()` 미호출 → Spring 내부에서 emitter를 zombie 상태로 추가 처리하며 stack trace 출력.

#### 수정
- **`SseService.java`** — `sendToClient()`, `sendPointUpdate()`, `sendForceLogout()` 세 메서드의 catch 블록에 `emitter.completeWithError(e)` 추가
- **`application.properties`** — `ResponseBodyEmitter` 로그 레벨 WARN으로 설정하여 정상 disconnect 노이즈 제거

---

### 22. GlobalExceptionHandler 미처리 예외 로깅 추가

#### 수정
- **`GlobalExceptionHandler.java`** — `@Slf4j` 추가, catch-all `handleException()` 에 `log.error("[500] ...")` 추가
  - 기존: 500 반환 시 콘솔에 아무 로그도 남지 않아 원인 파악 불가
  - 변경: 예외 메시지와 스택 트레이스 기록

---

### 23. 스케줄러 중복 처리 버그 수정 (Watchdog + AuctionScheduler race condition)

#### 문제
`AuctionExpiryWatchdog`(정각 처리)과 `AuctionScheduler`(30초 폴링)가 동일 상품에 대해 동시에 `processOne()`을 실행하는 경우 발생.
두 트랜잭션이 모두 `product.status=0`을 읽은 후 각자 커밋 → `AUCTION_RESULT` 레코드 2개 생성.
이후 `findByBidNo()`가 `Optional<AuctionResult>` 반환인데 결과 2개 → Hibernate `NonUniqueResultException` → 500.

#### 수정
- **`AuctionResultRepository.java`** — `findByBidNo()` → `findFirstByBidNo()` 변경
  - 중복 레코드가 존재해도 `NonUniqueResultException` 없이 첫 번째 결과 반환
- **`AuctionResultServiceImpl.java`** — `findByBidNo()` → `findFirstByBidNo()` 호출 변경
- **`AuctionClosingService.java`** — `processOne()` 멱등성 보장 로직 추가
  - AuctionResult 저장 전 `findFirstByBidNo()`로 이미 존재하는지 확인
  - 이미 존재하면 AuctionResult 저장 및 이벤트 발행 모두 건너뜀 (중복 알림 방지)
- **`BidHistoryRepository.java`** — `findWinnerByProductNo()` → `findFirstByProductNoAndIsWinnerOrderByBidPriceDesc()` 변경
  - 동일 이유: 중복 처리 시 `isWinner=1` 레코드가 2개일 때 `NonUniqueResultException` 방지

#### 근본 해결 (별도 SQL 실행 필요)
```sql
ALTER TABLE AUCTION_RESULT ADD CONSTRAINT UQ_AUCTION_RESULT_BID_NO UNIQUE (BID_NO);
```
DB 레벨 unique 제약으로 두 트랜잭션이 동시에 같은 BID_NO 저장 시 하나 자동 롤백.

---

## 날짜: 2026-03-28

### [백엔드 + 프론트엔드]

---

#### 24. processPayment() SSE 포인트 실시간 반영

##### 배경
구매자가 결제하기를 누르면 판매자 포인트가 DB에 즉시 반영되지만 헤더의 포인트 숫자는 새로고침 전까지 변경되지 않는 문제.

##### 수정
- **`AuctionResultServiceImpl.java`** — `processPayment()` 내 판매자 포인트 업데이트 직후 `sseService.sendPointUpdate()` 호출 추가
- **`AppContext.tsx`** (프론트) — SSE `pointUpdate` 이벤트 수신 시 헤더 포인트 즉시 갱신 (기존 구현 활용)

---

#### 25. 프로필 이미지 변경 기능 구현

##### 배경
마이페이지에 프로필 이미지 변경 UI는 있었으나 로컬 미리보기만 동작하고 서버 저장은 미구현 상태.

##### 신규
- **`PUT /api/members/{memberNo}/profile-image-url`** (MemberController) — JSON `{ "url": "/api/images/uuid.jpg" }` 수신 → Member.profileImgUrl 업데이트
  - 파일 업로드는 기존 `POST /api/images/upload` 재활용 (2단계 분리)

##### 수정
- **`Member.java`** — `PROFILE_IMG_URL VARCHAR(500)` 컬럼 추가 (ddl-auto=update로 자동 생성)
- **`MemberService.java`** / **`MemberServiceImpl.java`** — `updateProfileImage(memberNo, url)` 메서드 추가
- **`MemberController.java`** — `PUT /{memberNo}/profile-image-url` 엔드포인트 추가
- **`AppContext.tsx`** (프론트)
  - `mapMemberToUser()` — `profileImgUrl` → `profileImage` URL 변환 매핑 추가
  - 로그인 / 세션 복원 시 `profileImgUrl` DB 값 반영
  - `updateCurrentUserProfileImage()` 함수 추가 → 업로드 후 컨텍스트 + sessionStorage 동기화
- **`MyPage.tsx`** (프론트)
  - 이미지 선택 시 로컬 미리보기 즉시 반영
  - 1단계: `POST /images/upload`로 파일 업로드 → URL 수신
  - 2단계: `PUT /members/{no}/profile-image-url`로 URL 저장
  - 업로드 중 Settings 버튼 스피너로 교체 + 완료/실패 토스트 표시

---

#### 27. 판매자 프로필 페이지 DB 연동

##### 배경
상품 상세 페이지에서 판매자 클릭 시 이동하는 `/seller/:id` 페이지가 mock 데이터로만 구성되어 있었음.

##### 신규
- **`SellerProfileResponseDto.java`** — 판매자 공개 프로필 응답 DTO (sellerNo, nickname, profileImgUrl, mannerTemp, joinedAt, 판매상품 목록)
- **`GET /api/members/{memberNo}/seller-profile`** (MemberController) — 판매자 기본 정보 + 판매 상품 목록 반환

##### 수정
- **`MemberService.java`** / **`MemberServiceImpl.java`** — `getSellerProfile()` 구현
  - MemberServiceImpl에 ProductRepository, ProductImageRepository, BidHistoryRepository 추가 주입
  - 삭제되지 않은 판매 상품 조회 + 메인 이미지 / 참여자 수 배치 조회
- **`ProductDetailResponseDto.SellerInfoDto`** — `profileImgUrl` 필드 추가
- **`ProductServiceImpl.java`** — `getProductDetail()` 내 SellerInfoDto 빌드 시 `profileImgUrl` 포함
- **`SellerProfile.tsx`** (프론트) — mock 데이터 제거 → `GET /members/{id}/seller-profile` API 연동
  - 판매자 프로필 이미지 없을 시 닉네임 첫 글자 폴백
  - 판매 상품 필터(전체/판매중/판매완료) 동작
  - 로딩 스피너 추가
  - 거래 후기 탭은 후기 기능 미구현으로 빈 상태 표시
- **`ProductDetail.tsx`** (프론트) — 판매자 정보 매핑 시 `profileImgUrl` → `profileImage` 반영

---

#### 26. Spring Boot 3.x @PathVariable 파라미터명 누락 수정

##### 배경
Spring Boot 3.5.x (Spring Framework 6.x)는 `-parameters` 컴파일러 플래그 없이 리플렉션으로 파라미터명을 읽지 못함.
`pom.xml`에서 `maven-compiler-plugin`에 Lombok `annotationProcessorPaths`만 추가하고 `<parameters>true</parameters>`를 누락 → Spring Boot 부모 POM의 기본값이 덮어씌워짐.
`@PathVariable Long memberNo` 형태로 이름을 명시하지 않으면 400 Bad Request 발생.

##### 수정
- **`pom.xml`** — `maven-compiler-plugin` 설정에 `<parameters>true</parameters>` 추가
- 아래 컨트롤러 전체 `@PathVariable` 어노테이션에 명시적 이름 추가 (이름 없는 형태 → `@PathVariable("xxx")` 형태로 일괄 수정)
  - `MemberController.java` — `memberNo`
  - `AdminMemberController.java` — `memberNo` (5개)
  - `AdminReportController.java` — `reportNo`
  - `AdminProductController.java` — `productNo`
  - `HeroBannerController.java` — `bannerNo` (3개)
  - `NotificationController.java` — `notiNo`

---
커밋용 메세지

---

## 날짜: 2026-03-31 (자동입찰 구현 및 경매 결과 크리티컬 버그 수정)

---

### 24. 자동입찰 전체 스택 신규 구현

#### 신규 생성

- **`entity/AutoBid.java`** — 자동입찰 엔티티 (`AUTO_BID` 테이블, `AUTO_BID_SEQ` 시퀀스)
- **`repository/AutoBidRepository.java`** — 자동입찰 조회 리포지토리
  - `findActiveByProductNo()` — `@Query`로 `maxPrice DESC` 정렬 보장
  - `findByMemberNoAndProductNoAndIsActive()` — 내 활성 자동입찰 조회
- **`dto/AutoBidRequestDto.java`** — 자동입찰 등록 요청 DTO (`productNo`, `maxPrice`)
- **`dto/AutoBidResponseDto.java`** — 자동입찰 응답 DTO
- **`service/AutoBidService.java`** — 서비스 인터페이스
- **`service/AutoBidServiceImpl.java`** — 서비스 구현체
- **`controller/AutoBidController.java`** — REST API 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auto-bid` | 자동입찰 등록/수정 |
| DELETE | `/api/auto-bid/{productNo}` | 자동입찰 취소 |
| GET | `/api/auto-bid/active?productNo=` | 내 활성 자동입찰 조회 (없으면 204) |

---

### 25. 자동입찰 경쟁 해소 알고리즘 구현 (triggerAutoBids)

#### 핵심 로직
활성 자동입찰을 `maxPrice` 내림차순으로 조회 후 한 트랜잭션에서 승자 결정 및 단 1회 입찰 처리.

- **승자**: `maxPrice` 1위
- **낙찰가**: `min(2위.maxPrice + minUnit, 1위.maxPrice)`, 단 `minNextBid` 이상 보장
- **2위 이하**: 즉시 비활성화 + 패배 알림
- **승자 포인트 부족**: 승자도 비활성화

#### 수정
- **`service/BidServiceImpl.java`** — `processBid()` 완료 후 `autoBidService.triggerAutoBids()` 호출 추가 (수동 입찰자 제외)

---

### 26. [크리티컬 버그 수정] processPayment 구매자 이중 차감

#### 문제
`AuctionResultServiceImpl.processPayment()`에서 구매자 포인트를 재차감하는 버그.
입찰 시 에스크로로 이미 차감된 금액을 결제 시에도 다시 차감 → 낙찰가를 두 번 납부.

#### 수정
- **`AuctionResultServiceImpl.java`** — 구매자 차감 블록 전체 제거
  - 변경 전: 구매자 차감 + PointHistory + SSE + 판매자 지급
  - 변경 후: 판매자 지급만 (에스크로 이전 역할만 수행)

---

### 27. [크리티컬 버그 수정] cancelTransaction 포인트 환불 없음

#### 문제
`cancelTransaction()`이 상태를 "거래취소"로만 변경하고 포인트 환불/회수를 하지 않음 → 구매자 에스크로 영구 소멸.

#### 수정
- **`AuctionResultServiceImpl.java`** — 상태별 분기 환불 로직 추가

| 취소 시점 | 처리 내용 |
|-----------|-----------|
| 배송대기 | 구매자 에스크로 환불 (`buyer.points += bidPrice`) + PointHistory + SSE |
| 결제완료 | 판매자 대금 회수 (`seller.points -= bidPrice`) + PointHistory + SSE + 알림 → 구매자 에스크로 환불 + PointHistory + SSE |

- 비관적 락 순서(memberNo 오름차순) 적용으로 데드락 방지
- 이미 취소된 거래 재시도 시 `IllegalStateException` 반환


### 28. ec2로 변경 주소 변경

export const api = axios.create({
  baseURL: '/api', // 백엔드 URL을 상대경로로 변경!
  withCredentials: true,
});

이렇게 상대 주소로 변경하였으며 localhost:8080으로 절대 경로로 등록되 있는걸 backend:8080 , frontend:8080으로 상대경로로 변경