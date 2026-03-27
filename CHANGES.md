# 변경 내역

---

## 날짜: 2026-03-27

---

### 1. 이메일 인증 SMTP 구현

#### 신규 생성
- **`EmailService.java`** — 인증번호 생성(6자리)/발송/검증, ConcurrentHashMap 메모리 저장, 3분 만료

#### 수정
- **`pom.xml`** — `spring-boot-starter-mail` 의존성 추가
- **`application.properties`** — Gmail SMTP 설정 추가
- **`AuthController.java`** — 엔드포인트 2개 추가

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/send-email-code` | 인증번호 이메일 발송 |
| POST | `/api/auth/verify-email-code` | 인증번호 검증 |

---

### 2. 관리자 활동 로그 조회 API

#### 신규 생성
- **`ActivityLogResponseDto.java`** — 활동 로그 응답 DTO (`from(ActivityLog)` + `setAdminNickname`)
- **`AdminActivityLogController.java`** — 활동 로그 조회 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/admin/activity-logs` | 전체 활동 로그 (최신순) |
| GET | `/api/admin/activity-logs?targetType={type}` | 대상 유형별 필터 |

#### 수정
- **`AdminService.java`** — `getAllActivityLogs()`, `getActivityLogsByTargetType()` 인터페이스 추가
- **`AdminServiceImpl.java`** — 구현 추가 (관리자 닉네임 조회 포함)

---

### 3. 관리자 알림 관리 API

#### 신규 생성
- **`AdminNotificationController.java`** — 관리자 알림 관리 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/admin/notifications/broadcast` | 전체 활성 회원에게 알림 발송 + 활동 로그 기록 |
| GET | `/api/admin/notifications/recent` | 최근 발송 알림 50건 조회 |

#### 수정
- **`NotificationService.java`** — `getAllRecentNotifications(int limit)` 메서드 추가

---

### 4. 경매 관리 API

#### 신규 생성
- **`AdminProductResponseDto.java`** — 관리자용 상품 응답 DTO (productNo, title, sellerNickname, 가격, 참여자수, status)
- **`AdminProductController.java`** — 경매 관리 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/admin/products` | 전체 상품 목록 (삭제 제외, 최신순) |
| PUT | `/api/admin/products/{productNo}/cancel` | 경매 강제 종료 + 활동 로그 기록 |

#### 수정
- **`ProductService.java`** — `getAllProductsForAdmin()`, `cancelAuctionByAdmin()` 인터페이스 추가
- **`ProductServiceImpl.java`** — 구현 추가 (판매자 닉네임/이미지/참여자수 배치 쿼리, 진행중 경매만 취소 가능)
- **`ProductRepository.java`** — `findByIsDeletedOrderByCreatedAtDesc()` 추가

---

### 5. 히어로배너 활동 로그 기록

#### 수정
- **`HeroBannerController.java`**
  - `ActivityLogRepository` 의존성 추가
  - 모든 CRUD 엔드포인트에 `Authentication` 파라미터 추가
  - `logActivity()` private 메서드 추가
  - `create`, `update`, `delete`, `toggleActive` 실행 시 활동 로그 자동 기록 (`targetType: "banner"`)

---

## 날짜: 2026-03-26

---

### 5. 배너 이미지 파일 업로드 지원

#### 수정
- **`FileStore.java`** — `storeGenericFile()` 메서드 추가 (배너/프로필 등 범용 이미지 저장, UUID 파일명 반환)
- **`ImageController.java`** — `POST /api/images/upload` 엔드포인트 추가 (`MultipartFile → { "url": "/api/images/uuid.jpg" }`)

---

### 6. 관리자 페이지 4대 기능 일괄 구현

#### 신규 생성

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

#### API 엔드포인트

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

#### 기존 파일 수정

- **`MemberRepository.java`** — `findAllByOrderByJoinedAtDesc()`, `searchByKeyword()`, `findByNickname()` 추가
- **`MannerHistoryRepository.java`** — `findAllByOrderByCreatedAtDesc()` 추가

#### 부가 기능
- **활동 로그 자동 기록**: 모든 관리자 액션 시 `ActivityLog` 테이블에 자동 기록
- **알림 자동 발송**: 정지/해제/신고처리 시 대상 회원에게 SSE 실시간 알림
- **매너온도 이력**: 변경 시 `MannerHistory` 테이블에 변동 전/후 온도 + 사유 기록

---

### 7. 배너 관리 API 보완

#### 수정
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

### 8. 김태우 커밋 반영 (ba24831)

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

## 날짜: 2026-03-24

---

### 9. 다기기 동시 로그인 방지 구현

#### 수정
- **`Member.java`** — `currentToken` 필드 추가 (`CURRENT_TOKEN VARCHAR2(500)`, nullable)
- **`AuthService.java`** — `logout(Long memberNo)` 메서드 추가
- **`AuthServiceImpl.java`** — 로그인 시 `currentToken` 저장, 로그아웃 시 null 초기화, `SseService.sendForceLogout()` 호출
- **`JwtAuthenticationFilter.java`** — DB `currentToken`과 요청 토큰 비교, 불일치 시 401
- **`AuthController.java`** — `logout()`에서 `authService.logout(memberNo)` 호출
- **`SseService.java`** — `sendForceLogout(Long memberNo)` 메서드 추가

---

### 10. 알림함 API 구현

#### 신규 생성
- **`NotificationResponseDto.java`** — 알림 응답 DTO
- **`NotificationController.java`** — 알림 CRUD 컨트롤러

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/notifications` | 로그인 회원의 알림 목록 (최신순) |
| GET | `/api/notifications/unread-count` | 미읽음 알림 개수 |
| PATCH | `/api/notifications/{notiNo}/read` | 단일 알림 읽음 처리 |
| PATCH | `/api/notifications/read-all` | 전체 알림 읽음 처리 |

#### 수정
- **`NotificationRepository.java`** — `markAllAsRead()` JPQL UPDATE 추가
- **`NotificationService.java`** — `getNotifications()`, `getUnreadCount()`, `markAllAsRead()` 추가

---

### 11. JWT 인증 + 보안 구현

#### 신규 생성
- **`JwtUtil.java`** — JWT 토큰 생성/검증/파싱
- **`JwtAuthenticationFilter.java`** — Bearer 토큰 추출 + SecurityContext 등록
- **`GlobalExceptionHandler.java`** — 전역 예외 처리 (400/401/409/500)
- **`AuthService.java`** / **`AuthServiceImpl.java`** — 로그인 서비스
- **`AuthController.java`** — `POST /api/auth/login`, `POST /api/auth/logout`
- **`LoginRequestDto.java`** / **`LoginResponseDto.java`** — 로그인 DTO

#### 수정
- **`SecurityConfig.java`** — BCrypt 빈, JWT 필터, CORS 설정, STATELESS 세션
- **`MemberRequestDto.java`** — Bean Validation 어노테이션 추가
- **`MemberServiceImpl.java`** — BCrypt 암호화, 14세 미만 제한, 중복 검증
- **`MemberController.java`** — `@Valid` 추가, 중복 확인 엔드포인트 3개 추가
- **`MemberRepository.java`** — `existsBy*` → COUNT 기반 쿼리 (Oracle 11g 호환)
- **`HeroBanner.java`** — `@Builder.Default` 추가
- **`application.properties`** — JWT 설정, Oracle Dialect → `OracleLegacyDialect`
- **`pom.xml`** — jjwt 의존성 추가 (0.12.6)

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
