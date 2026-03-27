# 프론트엔드 수정사항

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

getMemberNo 유틸 함수 신규 추가 (타인 계정 접근 버그 수정)
Home.tsx BACKEND_URL 하드코딩 제거
Signup.tsx 인증코드 alert 노출 제거
MyPage.tsx 닉네임 기반 필터링 버그 수정

---

## 날짜: 2026-03-27 (낙찰 페이지 구현 및 버그 수정)

---

### 13. WonProductDetail Mock 제거 및 API 연동

#### 수정
- **`WonProductDetail.tsx`** — 전체 Mock 데이터 제거 후 실제 API 연동
  - 마운트 시 `GET /api/auction-results/product/{id}` 호출
  - 결제 → `POST /api/auction-results/{resultNo}/pay`
  - 구매 확정 → `POST /api/auction-results/{resultNo}/confirm`
  - 거래 취소 → `POST /api/auction-results/{resultNo}/cancel`
  - tradeType 비교 조건 `"face-to-face"` → `"직거래"` 수정 (백엔드 실제 값 기준)
  - 이미지 `resolveImageUrl()` 적용, 다중 이미지 좌우 화살표 네비게이션 추가

---

### 14. MyPage 입찰·구매 탭 분리

#### 수정
- **`MyPage.tsx`** — 탭 구조 3탭 → 4탭으로 분리
  - `'buying'` → `'bidding'` / `'purchased'` 두 탭으로 분리
  - 입찰 내역 탭: `GET /products/my-bidding` 호출, `bidStatus` 값으로 상태 배지 렌더링
    - `"won"` → 초록 "낙찰" 배지 + "낙찰 상세보기" 버튼 (`/won/:id` 이동)
    - `"lost"` → 빨간 "낙찰실패" 배지
    - `"bidding"` → 파란 "경매중" 배지
  - 구매 내역 탭: `GET /products/my-purchased` 호출 (구매확정 완료 상품만)
  - ProductCard `isWon={p.bidStatus === 'won'}` prop 전달로 카드 클릭 시 `/won/:id` 이동

---

### 15. ProductRegister 타임존 버그 수정

#### 수정
- **`ProductRegister.tsx`** — `endTime` 계산 시 `toISOString()` → 로컬 시간 변환 함수로 교체
  - `toISOString()`은 KST를 UTC로 변환하여 서버 KST 기준과 9시간 차이 발생
  - `toLocalISO()` 헬퍼 함수로 로컬 시간 그대로 ISO 문자열 생성
  - 직접입력 모드: `${manualDate}T${manualTime}:00` 형식으로 직접 조합

---

### 16. ProductDetail Recharts 에러 수정

#### 수정
- **`ProductDetail.tsx`** — `ResponsiveContainer`에 `minWidth={0}` 추가
  - 초기 렌더링 시 부모 크기 미계산으로 발생하던 `width(-1) height(-1)` 콘솔 에러 제거
