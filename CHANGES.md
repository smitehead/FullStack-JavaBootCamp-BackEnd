# 변경 내역

---

## 배너 관리 API 보완 + 프론트 연동 (2026-03-25)

### 백엔드 수정

**HeroBanner 엔티티** — `BANNER_TYPE` 컬럼 추가 (`VARCHAR2(10)`, 기본값 `"hero"`, hero/ad 구분)

**HeroBannerRequestDto** — `bannerType` 필드 추가

**HeroBannerResponseDto** — `bannerType` 필드 추가 + `from()` 변환 반영

**HeroBannerRepository** — `findAllByOrderBySortOrderAsc()` 추가 (관리자용 전체 배너 조회)

**HeroBannerServiceImpl**
- `getAllBanners()` 추가 — 활성/비활성 모두 포함, sortOrder 오름차순
- `create()` — `bannerType` 반영 (미입력 시 기본값 "hero")
- `update()` — `bannerType` 반영
- `toggleActive()` 추가 — isActive 0↔1 토글

**HeroBannerController**
- `GET /api/banners/all` — 관리자용 전체 배너 목록 (활성/비활성 모두)
- `PATCH /api/banners/{bannerNo}/toggle` — 배너 활성화/비활성화 토글

### 프론트엔드 수정

**types.ts** — `HeroBanner` 인터페이스를 DB 구조에 맞게 변경
- `id` → `bannerNo`, `type` → `bannerType`, `imageUrl` → `imgUrl`, `link` → `linkUrl`
- `isActive`: `boolean` → `number` (0/1)
- 미사용 필드 제거: `title`, `subtitle`, `label`, `buttons`, `isHtml`, `htmlContent`, `BannerButton`

**BannerManagement.tsx** — Mock 데이터 → API 연동
- 목록 조회: `MOCK_HERO_BANNERS` → `GET /api/banners/all`
- 등록: `POST /api/banners`
- 수정: `PUT /api/banners/{bannerNo}`
- 삭제: `DELETE /api/banners/{bannerNo}`
- 토글: `PATCH /api/banners/{bannerNo}/toggle`

---

## 신규 생성

### `GlobalExceptionHandler.java` (config 패키지)
- `@RestControllerAdvice`로 전역 예외 처리 통일
- `MethodArgumentNotValidException` → 400 (Bean Validation 실패, 어떤 필드가 왜 실패했는지 반환)
- `IllegalStateException` → 409 (중복 아이디/닉네임/이메일 충돌)
- `IllegalArgumentException` → 400 (만 14세 미만, 존재하지 않는 회원 등)
- **`JwtException` → 401** (토큰 만료/변조 시 "토큰이 유효하지 않습니다." 반환)
- **`Exception` → 500** (그 외 처리되지 않은 예외, "서버 오류가 발생했습니다." 반환)
  - ⚠️ 처리되지 않은 예외가 이제 응답에 스택 트레이스 없이 500만 반환됨 → 디버깅 시 콘솔 로그 확인 필요

### `JwtUtil.java` (config 패키지)
- JWT 토큰 생성/검증/파싱 유틸리티
- `generateToken(memberNo, userId)` - 토큰 발급
- `validateToken(token)` - 토큰 유효성 검증
- `getMemberNo(token)`, `getUserId(token)` - 클레임 추출
- `application.properties`의 `jwt.secret`, `jwt.expiration` 값 주입

### `JwtAuthenticationFilter.java` (config 패키지)
- `OncePerRequestFilter` 구현
- 요청 헤더 `Authorization: Bearer {token}` 에서 토큰 추출
- 토큰 유효 시 SecurityContext에 인증 정보 등록

### `AuthService.java` / `AuthServiceImpl.java` (service 패키지)
- 로그인 서비스 인터페이스 및 구현체
- 아이디 조회 → isActive 확인(탈퇴) → isSuspended 확인(정지) → BCrypt 비밀번호 비교 → JWT 발급

### `AuthController.java` (controller 패키지)
- `POST /api/auth/login` - 로그인, JWT 토큰 반환
- `POST /api/auth/logout` - 로그아웃 (클라이언트 토큰 삭제 방식, 서버는 200 OK 반환)

### `LoginRequestDto.java` / `LoginResponseDto.java` (dto 패키지)
- 로그인 요청/응답 DTO
- 응답: `{ token, memberNo, userId, nickname }`

---

## 수정 내역

### `SecurityConfig.java`
- `PasswordEncoder` (BCryptPasswordEncoder) 빈 등록 추가
- `JwtAuthenticationFilter` 주입 및 `UsernamePasswordAuthenticationFilter` 앞에 등록
- `SessionCreationPolicy.STATELESS` 설정 (JWT 방식이므로 세션 미사용)
- **CORS 설정 추가** (`corsConfigurationSource()` 빈 등록)
  - 허용 출처: 전체 (`*`) — 개발 단계, 운영 시 특정 도메인으로 제한 필요
  - 허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS
  - 허용 헤더: 전체 (Authorization 포함)
  - allowCredentials: true
- **불필요한 코드 제거**
  - `ignoringRequestMatchers("/h2-console/**")` 제거 (Oracle DB 사용, H2 미사용 / `.disable()`과 중복)
  - `.requestMatchers("/h2-console/**").permitAll()` 제거 (동일 사유)
- **주석 보강** — CORS/CSRF/STATELESS/JWT 필터/권한설정 각 항목에 상세 설명 추가

### `MemberRequestDto.java`
- Bean Validation 어노테이션 추가 (DB insert 전 Java 레벨 사전 검증)

| 필드 | 추가된 검증 |
|------|------------|
| userId | @NotBlank, @Size(min=4, max=20), @Pattern(영문+숫자) |
| password | @NotBlank, @Size(min=8, max=20) |
| nickname | @NotBlank, @Size(min=2, max=15) |
| email | @NotBlank, @Email, @Size(max=50) |
| phoneNum | @NotBlank, @Pattern(010-xxxx-xxxx 형식) |
| emdNo | @NotNull |
| addrDetail | @NotBlank, @Size(max=255) |
| birthDate | @NotNull |

### `MemberService.java` (인터페이스)
- 중복 확인 메서드 3개 추가
  - `isUserIdDuplicate(String userId)`
  - `isNicknameDuplicate(String nickname)`
  - `isEmailDuplicate(String email)`

### `MemberServiceImpl.java`
- `PasswordEncoder` 의존성 주입 추가
- 비밀번호 BCrypt 암호화 저장 (`passwordEncoder.encode()`)
- 만 14세 미만 가입 제한 로직 추가 (`IllegalArgumentException`)
- `validateDuplicate()`: 아이디·닉네임·이메일 중복 검증 추가 (`IllegalStateException`)
- `isUserIdDuplicate()`, `isNicknameDuplicate()`, `isEmailDuplicate()` 구현

### `MemberController.java`
- `join()` 메서드에 `@Valid` 추가 (Bean Validation 활성화)
- 중복 확인 엔드포인트 3개 추가

| 엔드포인트 | 설명 |
|-----------|------|
| GET `/api/members/check-userid?userId=xxx` | 아이디 중복 확인 |
| GET `/api/members/check-nickname?nickname=xxx` | 닉네임 중복 확인 |
| GET `/api/members/check-email?email=xxx` | 이메일 중복 확인 |

- 응답: `{ "duplicate": true/false }`

### `MemberRepository.java`
- `existsBy*` 메서드 → `@Query("SELECT COUNT(m) > 0 ...")` 로 교체
- Oracle 11g가 `FETCH FIRST n ROWS ONLY` 문법 미지원하여 ORA-00933 오류 발생 → COUNT 기반 쿼리로 해결

### `HeroBanner.java`
- `sortOrder`, `isActive`, `createdAt` 필드에 `@Builder.Default` 추가
- 누락 시 Builder 패턴으로 객체 생성 시 기본값이 적용되지 않는 문제 수정

### `application.properties`
- JWT 설정 추가
  - `jwt.secret` - HS256 서명 키 (32자 이상)
  - `jwt.expiration` - 토큰 만료 시간 (86400000ms = 24시간)
- Oracle Dialect 변경 이력
  - 초기: `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect` (FETCH 사용, 11g 비호환)
  - 1차: `org.hibernate.community.dialect.OracleDialect` → ClassNotFoundException 발생
  - 2차: `org.hibernate.community.dialect.Oracle10gDialect` → 시도
  - **최종: `spring.jpa.database-platform=org.hibernate.community.dialect.OracleLegacyDialect`** (ROWNUM 사용, 11g 완전 호환)

### `pom.xml`
- jjwt 의존성 추가 (버전 0.12.6)
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson`

### `.gitignore`
- `CLAUDE.md`, `CHANGES.md`, `FLOW.md` 제외 추가 → CHANGES.md는 이후 커밋에 포함하기로 변경

---

## 알림함 API 구현

### `NotificationResponseDto.java` (dto 패키지) - 신규
- 알림 응답 DTO
- `from(Notification)` 팩토리 메서드로 엔티티 → DTO 변환
- 필드: `notiNo`, `type`, `content`, `linkUrl`, `isRead`, `createdAt`

### `NotificationController.java` (controller 패키지) - 신규
- `GET /api/notifications` — 로그인 회원의 알림 목록 (최신순, 인증 필요)
- `GET /api/notifications/unread-count` — 미읽음 알림 개수 `{ "count": N }` (헤더 뱃지용)
- `PATCH /api/notifications/{notiNo}/read` — 단일 알림 읽음 처리
- `PATCH /api/notifications/read-all` — 전체 알림 읽음 처리
- `SecurityContext`에서 `memberNo` 추출 (JwtAuthenticationFilter에서 principal에 저장된 값)

### `NotificationRepository.java` - 수정
- `markAllAsRead(Long memberNo)` JPQL UPDATE 쿼리 추가
  - `UPDATE Notification SET isRead = 1 WHERE memberNo = :memberNo AND isRead = 0`

### `NotificationService.java` - 수정
- `getNotifications(Long memberNo)` — 알림 목록 조회 후 DTO 변환
- `getUnreadCount(Long memberNo)` — 미읽음 개수 반환
- `markAllAsRead(Long memberNo)` — 전체 읽음 처리 위임

---

## 프론트엔드 알림 SSE 실시간 연동 `2026-03-21`

### 신규 생성 (프론트엔드)
- **`hooks/useNotifications.ts`**
  - 로그인 시 `GET /api/notifications` 호출 → 초기 알림 목록 로드
  - `GET /api/sse/subscribe?clientId={memberNo}` 로 SSE 연결
  - `notification` 이벤트 수신 시 알림 목록 상단에 실시간 추가
  - `markAsRead(id)` — `PATCH /api/notifications/{notiNo}/read` 호출
  - `markAllAsRead()` — `PATCH /api/notifications/read-all` 호출
  - 컴포넌트 언마운트 시 SSE 연결 자동 종료

### 수정 (프론트엔드)
- **`components/Layout.tsx`** (헤더 벨 아이콘)
  - `NOTIFICATIONS` mockData → `useNotifications` 훅으로 교체
  - 알림 클릭 시 `markAsRead()` 호출하여 읽음 처리
- **`pages/Inbox.tsx`** (알림함 페이지)
  - `NOTIFICATIONS` mockData → `GET /api/notifications` 실제 API로 교체
  - 알림 탭에 "전체 읽음" 버튼 추가 (`markAllAsRead()` 호출)

### SSE clientId 규칙
- 로그인한 사용자: `clientId = memberNo` (localStorage에서 추출)
- 비로그인 사용자: 서버가 UUID 자동 발급 (알림 기능 사용 불가)

---

## API 테스트 가이드 (Thunder Client 기준)

> 아래 `{중괄호}` 항목은 테스터가 임의로 입력하는 값입니다.

### 1. 회원가입
- **POST** `http://localhost:8080/api/members`
- Body (JSON):
```json
{
  "userId": "{영문+숫자 4~20자}",
  "password": "{영문+숫자+특수문자 8~20자}",
  "nickname": "{2~15자}",
  "email": "{이메일 형식}",
  "phoneNum": "{010-xxxx-xxxx}",
  "emdNo": {DB에 존재하는 읍면동 번호},
  "addrDetail": "{상세주소}",
  "birthDate": "{yyyy-MM-dd}"
}
```
- 성공 응답: `200 OK` / 본문: 생성된 `memberNo` (숫자)
- 실패 케이스: 중복 아이디/닉네임/이메일 → `409`, 유효성 오류 → `400`

### 2. 로그인
- **POST** `http://localhost:8080/api/auth/login`
- Body (JSON):
```json
{
  "userId": "{가입한 아이디}",
  "password": "{가입한 비밀번호}"
}
```
- 성공 응답:
```json
{
  "token": "{JWT 토큰}",
  "memberNo": {회원번호},
  "userId": "{아이디}",
  "nickname": "{닉네임}"
}
```
- 실패 케이스: 잘못된 비밀번호 → `400`, 탈퇴 계정 → `400`, 정지 계정 → `400`

### 3. 로그아웃
- **POST** `http://localhost:8080/api/auth/logout`
- Header: `Authorization: Bearer {로그인 시 받은 token}`
- 성공 응답: `200 OK` (본문 없음)

### 4. 중복 확인
- **GET** `http://localhost:8080/api/members/check-userid?userId={확인할 아이디}`
- **GET** `http://localhost:8080/api/members/check-nickname?nickname={확인할 닉네임}`
- **GET** `http://localhost:8080/api/members/check-email?email={확인할 이메일}`
- 응답: `{ "duplicate": true }` 또는 `{ "duplicate": false }`

### 5. JWT 인증이 필요한 API 테스트 방법
- 로그인 후 받은 `token` 값을 복사
- Thunder Client 요청 Headers 탭에 추가:
  - Key: `Authorization`
  - Value: `Bearer {token}`

---

## 프론트엔드 연동 수정사항 반영 요청 `2026-03-24`

> 프론트엔드에서 로그인/회원가입을 백엔드에 실제 연동하는 작업을 완료함.
> 아래는 백엔드에서 추가 처리가 필요한 항목들.

---

### 1. 중복 로그인 방지 구현 요청

#### 현재 문제
- JWT Stateless 방식이므로 동일 계정으로 여러 기기/브라우저에서 동시 로그인 가능.
- 프론트엔드에서는 같은 브라우저 내 중복 로그인은 막았으나, **다른 기기·브라우저 간 중복 세션은 백엔드에서만 처리 가능**.
- `POST /api/auth/logout` 이 현재 서버에서 아무 처리 없이 200만 반환 → 토큰 무효화 안 됨.

#### 요청 구현 방안 (선택)

**방안 A — DB 기반 토큰 관리 (간단)**
1. `member_token` 테이블 생성
```sql
CREATE TABLE member_token (
  member_no  NUMBER PRIMARY KEY REFERENCES member(member_no),
  token      VARCHAR2(512) NOT NULL,
  created_at TIMESTAMP DEFAULT SYSDATE
);
```
2. 로그인 시 → 기존 토큰 삭제 후 신규 토큰 저장 (한 계정 = 토큰 1개)
3. `JwtAuthenticationFilter`에서 DB 토큰과 요청 토큰 비교 → 불일치 시 401
4. 로그아웃 시 → DB에서 해당 토큰 삭제

**방안 B — Redis 기반 (권장, 성능 우수)**
1. Redis에 `token:{memberNo}` 키로 유효 토큰 저장 (TTL = JWT 만료시간 24h)
2. 로그인 시 → 기존 키 덮어쓰기 → 이전 토큰 자동 무효화
3. `JwtAuthenticationFilter`에서 Redis 토큰 비교
4. 로그아웃 시 → Redis 키 삭제

---

### 2. emdNo (읍면동 코드) 처리 요청

#### 현재 상태
- 프론트엔드 회원가입 주소 입력이 자유 텍스트 형식이며 주소 검색 API 미연동.
- 백엔드 `MemberRequestDto`의 `emdNo`가 필수 FK → 프론트에서 현재 `emdNo: 1` 임시 하드코딩 중.
- **DB에 `emdNo = 1` 데이터가 없으면 회원가입 실패**.

#### 요청 처리 방안 (선택)

| 방안 | 내용 |
|------|------|
| A (정식) | 주소 검색 API (`GET /api/address?keyword=xxx`) 구현 후 프론트 연동 |
| B (임시) | `emdNo` 컬럼을 nullable 허용 또는 기본값 설정하여 주소 없이도 가입 가능하게 처리 |

---

---

## 다기기 동시 로그인 방지 구현 `2026-03-24`

> 위에서 요청한 방안 A (DB 기반 토큰 관리)를 Member 엔티티 컬럼 방식으로 구현 완료.
> 별도 테이블 생성 없이 Member 테이블에 `CURRENT_TOKEN` 컬럼만 추가.

### 변경된 파일

#### `Member.java`
- `currentToken` 필드 추가 (`CURRENT_TOKEN VARCHAR2(500)`, nullable)
- `ddl-auto=update`로 서버 재시작 시 자동으로 DB 컬럼 추가됨

#### `AuthService.java`
- `logout(Long memberNo)` 메서드 인터페이스 추가

#### `AuthServiceImpl.java`
- `login()` → 로그인 성공 시 신규 토큰을 `member.currentToken`에 저장 (기존 토큰 자동 덮어쓰기)
- `logout()` → 해당 회원의 `currentToken`을 null로 초기화
- `@Transactional` 어노테이션 추가

#### `JwtAuthenticationFilter.java`
- `MemberRepository` 의존성 추가
- 토큰 유효성 검증 후 DB의 `currentToken`과 비교하는 로직 추가
- 불일치 시 `401 Unauthorized` 응답 반환 (`{ "error": "다른 기기에서 로그인되어 자동 로그아웃 처리되었습니다." }`)
- `currentToken == null`인 경우(기존 로그인 세션)는 허용 → 재로그인 후부터 적용됨

#### `AuthController.java`
- `logout()` → `SecurityContextHolder`에서 `memberNo` 추출 후 `authService.logout(memberNo)` 호출

#### `SseService.java` (추가)
- `sendForceLogout(Long memberNo)` 메서드 추가
- 해당 회원의 SSE 연결에 `forceLogout` 이벤트 전송 (연결 없으면 건너뜀)

#### `AuthServiceImpl.java` (추가)
- `SseService` 의존성 주입
- `login()` → currentToken 교체 전에 `sseService.sendForceLogout()` 호출하여 기존 기기에 즉시 알림

### 동작 흐름

```
1. A기기 로그인  → DB: currentToken = "tokenA", SSE 구독 중
2. B기기 로그인  → sendForceLogout() 호출 → A기기 SSE에 forceLogout 이벤트 즉시 전송
                → DB: currentToken = "tokenB" (tokenA 덮어씀)
3. A기기 프론트  → SSE 이벤트 수신 → 즉시 로그아웃 (새로고침 불필요)

(A기기 SSE 미연결 시 백업)
3. A기기 API 요청 → Filter에서 "tokenA" ≠ DB "tokenB" 감지 → 401 반환
4. A기기 프론트  → 401 interceptor 수신 → 로그아웃
```

---

---

## 김태우 커밋 반영 `2026-03-25` (ba24831)

> SSE 최적화, 데드락 방지, 이미지 처리 개선, 무한스크롤/정렬 추가

### `BidServiceImpl.java` - 수정
- **데드락 방지**: 이전/현재 입찰자 비관적 락 순서 고정 (memberNo 오름차순)
- **본인 재입찰 차단**: 현재 최고 입찰자가 본인이면 추가 입찰 불가
- **SSE 발송 격리**: `try-catch`로 SSE/알림 실패가 트랜잭션 롤백을 유발하지 않도록 처리
- **입찰 취소 환불 추가**: `cancelBid()` 시 포인트 환불 + PointHistory 기록 + SSE 포인트 갱신

### `SseService.java` - 수정
- **`sendPointUpdate(Long memberNo, Long currentPoints)`** 메서드 추가 — 특정 사용자에게 포인트 갱신 이벤트 전송
- **`broadcastPriceUpdate()`** 개선 — forEach 루프 중 직접 remove 대신 deadClients 리스트 수집 후 일괄 제거 (ConcurrentModification 방지)

### `ProductServiceImpl.java` - 수정
- 무한스크롤 기능 추가
- 정렬 기능 추가

### `ImageController.java` - 수정
- 이미지 확장자 검증 추가

### `FileStore.java` (util 패키지) - 수정
- 이미지 처리 개선

### `ProductImageRepository.java` - 수정
- 쿼리 최적화

### `WishlistRepository.java` - 수정
- 쿼리 추가

### `application.properties` - 수정
- 설정값 조정

### `NotificationService.java` - 수정
- `sendAndSaveNotification()` — SSE 전송 데이터를 문자열 → 구조화된 Map 객체로 변경
  - `{ notiNo, type, content, linkUrl, isRead, createdAt }` 형태로 전송
  - 프론트에서 `type`으로 알림 종류 구분, `linkUrl`로 클릭 시 이동 처리 가능

### `BidServiceImpl.java` - 알림 type 통일
- `"입찰"` → `"bid"` 로 변경 (프론트 Inbox.tsx 필터와 일치)

### `.gitignore` - 수정
- `hs_err_pid*.log`, `replay_pid*.log` 제외 추가 (JVM 크래시 로그)

### `SseController.java` - 수정
- `@CrossOrigin(origins = "*")` CORS 허용 추가

---

### 3. 이메일 인증 API 구현 요청

현재 프론트엔드 이메일 인증이 Mock 상태 (랜덤 숫자를 `alert`로 노출).

| 엔드포인트 | 설명 | 요청 Body |
|-----------|------|----------|
| `POST /api/auth/send-email-code` | 인증번호 이메일 발송 | `{ "email": "user@example.com" }` |
| `POST /api/auth/verify-email-code` | 인증번호 검증 | `{ "email": "user@example.com", "code": "123456" }` |

검증 응답 예시:
```json
{ "verified": true }
// 또는
{ "verified": false }
```
