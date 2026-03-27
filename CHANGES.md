# 프론트엔드 수정사항

---

## 마이페이지 입찰·구매 탭 분리 (2026-03-27)

### `pages/mypage/MyPage.tsx` — 탭 구조 전면 개편

**탭 변경**
- 기존: 판매 내역 / 구매·입찰 내역 / 찜 목록 (3탭)
- 변경: 판매 내역 / **입찰 내역** / **구매 내역** / 찜 목록 (4탭)

**입찰 내역 (`bidding` 탭)**
- `GET /products/my-bidding` 호출 — `bidStatus` 필드 포함 응답
- 각 상품 카드 하단에 상태 뱃지 표시:
  - `bidding` → `<Gavel>` 아이콘 + "경매중" (파란색)
  - `won` → `<CheckCircle2>` 아이콘 + "낙찰" (초록색) + "낙찰 상세보기" 버튼 → `/won/:id` 이동
  - `lost` → `<XCircle>` 아이콘 + "낙찰실패" (빨간색, 클릭 불가)
- 낙찰 상품 카드 클릭 시 `/won/:id` 로 이동
- 경매중 상품 카드 클릭 시 `/product/:id` 로 이동

**구매 내역 (`purchased` 탭)**
- `GET /products/my-purchased` 호출 — 구매확정 완료된 상품만 표시
- 빈 탭일 때: "구매 완료된 상품이 없습니다." 안내

**Quick Stats 영역 (프로필 헤더)**
- 기존 2열(찜목록/구매·입찰내역) → 3열(입찰내역/구매내역/찜목록)으로 변경
- 각 통계 클릭 시 해당 탭으로 이동

**사이드바 네비게이션**
- 입찰 내역: `<Gavel>` 아이콘
- 구매 내역: `<ShoppingBag>` 아이콘

**타입 변경**
- `activeTab` 타입: `'selling' | 'buying' | 'wishlist'` → `'selling' | 'bidding' | 'purchased' | 'wishlist'`
- `mapToProduct()` 반환 타입: `Product & { bidStatus?: string }`

---

## 마이페이지 실제 API 연동 / 가격 알림 토스트 / 멀티탭 로그인 수정 (2026-03-26)

### `pages/mypage/MyPage.tsx` — Mock 데이터 완전 제거 및 실제 API 연동

**제거된 항목**
- `CURRENT_USER`, `MOCK_PRODUCTS`, `MOCK_REVIEWS`, `MOCK_REVIEW_TAGS` Mock 데이터 의존성 전부 제거
- 리뷰 탭 제거 (백엔드 리뷰 API 미구현)

**추가된 항목**
- `useAppContext().user` 로 프로필 정보 표시 (nickname, points, mannerTemp, joinedAt)
- `mapToProduct()` 헬퍼 — 백엔드 DTO → 프론트 `Product` 타입 변환
- 탭 전환 시 API 레이지 로딩 (`useEffect` + `useCallback`)
  - 판매 내역 → `GET /products/my-selling`
  - 입찰 내역 → `GET /products/my-bidding`
  - 찜 목록 → `GET /wishlists/my`
- 상품 삭제 → `DELETE /products/{id}` + 로컬 상태 즉시 반영
- 로딩 스피너 추가 (API 호출 중)
- 로그인 필요 화면 추가 (`user === null` 시)

### `pages/product/ProductDetail.tsx` — 입찰가 변동 시 가격 애니메이션 제거 및 토스트 알림 추가

**제거된 항목**
- `priceHighlight` state 제거
- 현재 입찰가 박스의 빨간 배경/스케일/펄스 애니메이션 효과 제거
- 입찰 모달 내 가격 강조 효과 제거

**추가된 항목**
- `priceToast` state 추가
- SSE `priceUpdate` 이벤트 수신 시 토스트 표시 (`5초 후 자동 소멸`)
- 토스트 UI: 화면 상단 중앙 고정 (`fixed top-6 left-1/2 -translate-x-1/2`)
  - 흰 배경 + 빨간 텍스트 + 테두리 (경고 스타일)
  - `<AlertTriangle>` 아이콘 + "상품의 현재 입찰가가 변동되었습니다." 문구

### `services/api.ts` + `context/AppContext.tsx` — 멀티탭 로그인 세션 분리

**문제**: `localStorage`는 같은 브라우저의 모든 탭에서 공유 → 다른 계정으로 탭마다 로그인 시 데이터 혼재

**수정**: 토큰/유저 저장소를 `localStorage` → `sessionStorage` 로 전환

| 파일 | 변경 내용 |
|------|----------|
| `services/api.ts` | `localStorage.getItem('java_token')` → `sessionStorage.getItem('java_token')` |
| `context/AppContext.tsx` | 토큰/유저 저장·조회·삭제 전체 10곳 `localStorage` → `sessionStorage` (replace_all) |

**효과**: 각 탭/창이 독립된 세션을 유지하여 계정 간 데이터 혼재 방지

---

## 전체 코드리뷰 개선 (2026-03-26)

### `utils/memberUtils.ts` — `getMemberNo` 유틸 함수 신규 추가
- `parseInt(user.id.replace(/[^0-9]/g, '') || '1', 10)` 패턴이 9곳 이상 중복 사용 중
- 파싱 실패 시 기본값 `'1'`로 memberNo=1인 타인 계정에 API 호출되는 버그 존재
- `getMemberNo(user)` 함수 신규 작성 — 파싱 실패 시 `null` 반환, 호출부에서 API 차단
- AppContext, ProductCard, ProductDetail, ProductList, ProductRegister 등 9곳 전부 교체

### `pages/home/Home.tsx` — BACKEND_URL 상수 적용
- SSE EventSource URL에 `http://localhost:8080` 하드코딩 → 배포 시 동작 불가
- `BACKEND_URL` 상수로 교체 (AppContext.tsx, ProductDetail.tsx와 동일 방식)

### `pages/signup/Signup.tsx` — 인증코드 alert 노출 제거
- 이메일 인증코드를 `alert()`에 직접 출력 — 프로덕션에서 있으면 안 되는 패턴
- alert에서 코드 값 제거, 개발 환경에서만 `console.log('[DEV]')`로 출력하도록 변경

### `pages/mypage/MyPage.tsx` — 닉네임 기반 필터링 버그 수정
- 입찰 목록 필터 조건 `bidderName === CURRENT_USER.nickname` → 닉네임 변경 시 이전 입찰 내역 누락
- `BidHistory` 타입에 `bidderId` 필드 추가
- 필터 조건 `bidderId === CURRENT_USER.id` 로 변경

---

## 날짜: 2026-03-24

---

## 1. 중복 로그인 방지 (`pages/Login.tsx`)

### 문제
이미 로그인된 계정으로 `/login` 페이지에 다시 접근하면 중복 로그인이 가능했음.

### 수정 내용
- `useEffect`를 추가하여 `user` 상태가 존재하면 홈(`/`)으로 자동 리다이렉트 처리.
- `useAppContext`에서 `user` 추가로 구조분해.

```tsx
// 이미 로그인된 상태면 홈으로 리다이렉트 (중복 로그인 방지)
useEffect(() => {
  if (user) {
    navigate('/', { replace: true });
  }
}, [user, navigate]);
```

---

## 2. 아이디 중복확인 백엔드 연동 (`pages/Signup.tsx`)

### 문제
`handleIdCheck` 함수가 `admin`, `user1` 두 개만 하드코딩으로 막는 Mock 상태였음.

### 수정 내용
- 실제 백엔드 API `GET /api/members/check-userid?userId={userId}` 호출로 교체.
- 아이디 입력값 변경 시 중복확인 상태(`isIdChecked`) 자동 초기화.
- 중복확인을 통과하지 않으면 회원가입 제출 불가.

```tsx
const res = await api.get(`/members/check-userid?userId=${formData.userId}`);
if (res.data.duplicate) {
  // 중복
} else {
  setIsIdChecked(true); // 확인 완료 플래그
}
```

---

## 3. 회원가입 백엔드 연동 (`pages/Signup.tsx`)

### 문제
`handleSubmit`이 `console.log`만 찍고 실제 API 호출이 없었음.

### 수정 내용
- `POST /api/members` 실제 호출로 교체.
- `birthDate` 형식 변환: 입력값 `YYMMDD` (6자리) → 백엔드 요구 형식 `yyyy-MM-dd`.
- HTTP 상태코드별 에러 메시지 분기 처리.

#### birthDate 변환 로직
```tsx
const yy = parseInt(formData.birthDate.substring(0, 2), 10);
const currentYY = new Date().getFullYear() % 100;
const fullYear = yy > currentYY
  ? `19${formData.birthDate.substring(0, 2)}`
  : `20${formData.birthDate.substring(0, 2)}`;
const birthDateFormatted = `${fullYear}-${formData.birthDate.substring(2, 4)}-${formData.birthDate.substring(4, 6)}`;
```

#### API 호출 및 에러 처리
```tsx
await api.post('/members', {
  userId, password, nickname, email, phoneNum,
  emdNo: 1,          // ⚠️ 임시값 - 아래 TODO 참고
  addrDetail,
  birthDate: birthDateFormatted,
  marketingAgree: terms.marketing ? 1 : 0,
});
```

| 상태코드 | 처리 |
|---------|------|
| 200 OK  | 가입 완료 화면으로 이동 |
| 409 Conflict | "이미 사용 중인 아이디, 닉네임, 또는 이메일" 알림 |
| 400 Bad Request | 서버 메시지 또는 기본 안내 알림 |
| 그 외 | "잠시 후 다시 시도" 안내 알림 |

---

---

## 4. 다기기 동시 로그인 방지 즉시 로그아웃 (`services/api.ts`, `context/AppContext.tsx`)

### 문제
백엔드에서 다른 기기 로그인 감지 시 401을 반환해도, 프론트가 API를 호출하기 전까지는 로그아웃이 되지 않아 새로고침이 필요했음.

### 수정 내용

#### `context/AppContext.tsx` — SSE `forceLogout` 이벤트 수신 즉시 처리
- 백엔드가 새 로그인 시 SSE로 `forceLogout` 이벤트를 전송하면 즉시 로그아웃 처리.
- 새로고침 없이 실시간으로 alert + `/login` 이동.

```tsx
eventSource.addEventListener('forceLogout', () => {
  eventSource.close();
  localStorage.removeItem('java_token');
  localStorage.removeItem('java_user');
  alert('다른 기기에서 로그인되어 자동 로그아웃 처리되었습니다.');
  window.location.href = '/login';
});
```

#### `services/api.ts` — 401 response interceptor 추가 (SSE 미연결 시 백업)
- SSE 연결이 없거나 오프라인 상태에서 API 요청 시 401 수신 → 자동 로그아웃.
- `/auth/logout` 요청 자체는 제외 (무한루프 방지).

```typescript
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url ?? '';
      if (!url.includes('/auth/logout')) {
        localStorage.removeItem('java_token');
        localStorage.removeItem('java_user');
        alert('다른 기기에서 로그인되어 자동 로그아웃 처리되었습니다.');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

### 동작 흐름
```
B기기 로그인
  → 백엔드: SSE forceLogout 이벤트 전송 (즉시)
  → A기기 AppContext: 이벤트 수신 → 즉시 로그아웃 (새로고침 불필요)

A기기가 SSE 미연결 상태라면
  → A기기 다음 API 요청 시 401 수신
  → api.ts 인터셉터: 자동 로그아웃
```

---


