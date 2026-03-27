# 프론트엔드 수정사항

---

## 날짜: 2026-03-27

---

### 1. 이메일 인증 백엔드 연동 (`pages/auth/Signup.tsx`)

#### 문제
이메일 인증이 프론트에서 랜덤 숫자를 생성해 `alert`로 노출하는 Mock 방식이었음.

#### 수정 내용
- Mock 인증 → 실제 백엔드 API 호출로 교체
- `POST /api/auth/send-email-code` — 인증번호 이메일 발송
- `POST /api/auth/verify-email-code` — 인증번호 검증

---

### 2. 관리자 활동 로그 백엔드 연동 (`context/AppContext.tsx`)

#### 문제
`addActivityLog` 함수가 프론트에서만 로컬 state에 추가하는 방식이었고, 백엔드에서 조회하는 API 엔드포인트가 없어 페이지에 로그가 표시되지 않았음.

#### 수정 내용
- `fetchAdminData()`에 `GET /api/admin/activity-logs` 호출 추가
- `addActivityLog` 함수 제거 (백엔드가 관리자 액션 시 자동 기록)
- `refreshActivityLogs()` 함수 신규 추가 — 관리자 액션 후 로그 새로고침
- 모든 관리자 액션(`suspendUser`, `unsuspendUser`, `resolveReport`, `updateUserRole`, `updateUserManner`, `updateUserPoints`) 실행 후 `await refreshActivityLogs()` 호출 추가
- `mapActivityLogToFrontend()` 헬퍼 함수 추가 (백엔드 DTO → 프론트 타입 변환)

---

### 3. 알림 관리 페이지 백엔드 연동 (`pages/admin/NotificationManagement.tsx`)

#### 문제
`useAppContext()`의 `INITIAL_NOTIFICATIONS` Mock 데이터를 사용 중이었음.

#### 수정 내용
- 전면 재작성: Mock 의존 제거, 직접 API 호출 방식으로 교체
- `api.get('/admin/notifications/recent')` — 최근 발송 알림 50건 조회
- `api.post('/admin/notifications/broadcast', { type, content, linkUrl })` — 전체 회원 알림 발송
- 알림 유형을 `NotificationType` enum에서 한글 문자열(`'시스템'`, `'활동'`, `'입찰'`)로 변경
- 기존 `TS2367` TypeScript 에러 해소 (enum과 문자열 비교 문제)

---

### 4. 경매 관리 페이지 백엔드 연동

#### `context/AppContext.tsx`
- `fetchAdminData()`에 `GET /api/admin/products` 호출 추가 — Mock 상품 데이터 대신 실제 DB 상품 로딩
- `cancelAuction()` → `PUT /api/admin/products/{productNo}/cancel` API 호출로 교체 (기존: 로컬 state만 변경)
- `mapAdminProductToFrontend()` 매핑 함수 추가 (백엔드 status 정수 0/1/2 → 프론트 문자열 변환)

#### `pages/admin/AuctionManagement.tsx`
- `resolveImageUrl()` 적용 (백엔드 이미지 경로 → 전체 URL)
- `handleCancelAuction` async 처리
- 남은 시간을 실제 `endTime` 기반으로 계산 표시 (일/시간/분)

---

### 5. 관리자 페이지 버그 수정

#### `types.ts`
- `User` 타입에 `isWithdrawn?: boolean` 필드 추가

#### `context/AppContext.tsx`
- `sendAdminMessage`, `toggleMaintenanceMode` 스텁 구현 (console.warn만 출력, 백엔드 미구현)

#### `pages/admin/UserManagement.tsx`
- 포인트/매너온도 표시 시 `NaN` 방지 처리

#### `pages/admin/AdminDashboard.tsx`
- 미처리 신고 수를 하드코딩에서 실제 `reports` 데이터 기반으로 교체

#### `pages/admin/ReportManagement.tsx`
- 제재 사유를 셀렉트박스 고정값에서 직접 입력 가능하도록 수정

---

## 날짜: 2026-03-26

---

### 5. 히어로배너 Home.tsx API 연동

#### 문제
`HERO_BANNERS` 상수 배열(Mock 하드코딩)로 배너를 표시하고 있었음.

#### 수정 내용
- `HERO_BANNERS` 상수 제거
- `heroBanners` state + `useEffect`로 `GET /api/banners` API 호출
- `resolveImageUrl()`로 이미지 경로 변환 (상대경로 → 절대 URL)
- 배너 0개일 때 슬라이드/컨트롤 안전 처리

---

### 6. 배너 관리 페이지 이미지 업로드 (`pages/admin/BannerManagement.tsx`)

#### 수정 내용
- 배너 등록/수정 모달에 파일 업로드 버튼 추가
- 파일 선택 → `POST /api/images/upload` → imgUrl 자동 세팅
- 기존 URL 직접 입력도 유지
- 배너 이미지 표시에 `resolveImageUrl()` 적용

---

### 7. 배너 관리 Mock → API 전환 (`pages/admin/BannerManagement.tsx`)

#### 수정 내용
- `MOCK_HERO_BANNERS` → `GET /api/banners/all`
- 등록: `POST /api/banners`
- 수정: `PUT /api/banners/{bannerNo}`
- 삭제: `DELETE /api/banners/{bannerNo}`
- 토글: `PATCH /api/banners/{bannerNo}/toggle`

#### `types.ts` — `HeroBanner` 인터페이스 변경
- `id` → `bannerNo`, `type` → `bannerType`, `imageUrl` → `imgUrl`, `link` → `linkUrl`
- `isActive`: `boolean` → `number` (0/1)
- 미사용 필드 제거: `title`, `subtitle`, `label`, `buttons`, `isHtml`, `htmlContent`

---

### 8. 관리자 페이지 AppContext API 연동 (`context/AppContext.tsx`)

#### 수정 내용
관리자 로그인 시 Mock → 실제 API 자동 전환

**데이터 로딩** (`fetchAdminData`):
- `users` ← `GET /api/admin/members`
- `reports` ← `GET /api/admin/reports`
- `mannerHistory` ← `GET /api/admin/members/manner-history`

**액션 함수** (Mock → 실제 API + fetchAdminData 새로고침):
- `suspendUser()` → `PUT /api/admin/members/{memberNo}/suspend`
- `unsuspendUser()` → `PUT /api/admin/members/{memberNo}/unsuspend`
- `updateUserManner()` → `PUT /api/admin/members/{memberNo}/manner-temp`
- `updateUserPoints()` → `PUT /api/admin/members/{memberNo}/points`
- `updateUserRole()` → `PUT /api/admin/members/{memberNo}/role`
- `resolveReport()` → `PUT /api/admin/reports/{reportNo}/resolve`

**로그인 개선**: `login()` 시 `isAdmin` 필드 DB에서 가져와 설정

**타입 변환 헬퍼**: `extractMemberNo`, `mapMemberToUser`, `mapReportToFrontend`, `mapMannerHistoryToFrontend`

---

## 날짜: 2026-03-24

---

### 9. 중복 로그인 방지 (`pages/Login.tsx`)

#### 문제
이미 로그인된 계정으로 `/login` 페이지에 다시 접근하면 중복 로그인이 가능했음.

#### 수정 내용
- `useEffect`를 추가하여 `user` 상태가 존재하면 홈(`/`)으로 자동 리다이렉트 처리.

---

### 10. 아이디 중복확인 백엔드 연동 (`pages/Signup.tsx`)

#### 문제
`handleIdCheck` 함수가 `admin`, `user1` 두 개만 하드코딩으로 막는 Mock 상태였음.

#### 수정 내용
- 실제 백엔드 API `GET /api/members/check-userid?userId={userId}` 호출로 교체.
- 아이디 입력값 변경 시 중복확인 상태(`isIdChecked`) 자동 초기화.

---

### 11. 회원가입 백엔드 연동 (`pages/Signup.tsx`)

#### 문제
`handleSubmit`이 `console.log`만 찍고 실제 API 호출이 없었음.

#### 수정 내용
- `POST /api/members` 실제 호출로 교체.
- `birthDate` 형식 변환: `YYMMDD` (6자리) → `yyyy-MM-dd`.
- HTTP 상태코드별 에러 메시지 분기 처리.

---

### 12. 다기기 동시 로그인 방지 즉시 로그아웃 (`services/api.ts`, `context/AppContext.tsx`)

#### 문제
백엔드에서 다른 기기 로그인 감지 시 401을 반환해도, 프론트가 API를 호출하기 전까지는 로그아웃이 되지 않아 새로고침이 필요했음.

#### 수정 내용

**`context/AppContext.tsx`** — SSE `forceLogout` 이벤트 수신 즉시 로그아웃 처리

**`services/api.ts`** — 401 response interceptor 추가 (SSE 미연결 시 백업)

---

## 날짜: 2026-03-21

---

### 13. 알림 SSE 실시간 연동

#### 신규 생성
- **`hooks/useNotifications.ts`** — 로그인 시 알림 API 호출 + SSE 구독, 읽음 처리

#### 수정
- **`components/Layout.tsx`** — Mock → `useNotifications` 훅으로 교체
- **`pages/Inbox.tsx`** — Mock → `GET /api/notifications` API로 교체, "전체 읽음" 버튼 추가

---

## 남은 작업 (TODO)

| 항목 | 상태 | 설명 |
|------|------|------|
| `NoticeManagement.tsx` | Mock | `MOCK_NOTICES` 사용 중, 백엔드 공지 CRUD API 없음 |
| `InquiryManagement.tsx` | Mock | `MOCK_INQUIRIES` 사용 중, 백엔드 문의 CRUD API 없음 |
| `AdminDashboard.tsx` | Mock 일부 | 문의/공지 카운트 + 차트 데이터 하드코딩 |
| `sendAdminMessage` | 스텁 | AppContext에 console.warn만 존재 |
| `toggleMaintenanceMode` | 스텁 | AppContext에 console.warn만 존재 |
| emdNo | 하드코딩 | 회원가입 시 `emdNo: 1` 고정 |
