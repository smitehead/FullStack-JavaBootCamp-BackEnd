# 변경 내역

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

processPayment()에 sse호출을 통해 결재완료가 되면 판매자의 포인트가 즉시 반영되도록 수정
