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

---

### 18. 관리자 페이지 새로고침 권한 오류 수정

#### 문제
관리자 계정으로 로그인 후 관리자 페이지에서 새로고침 시 "관리자 권한이 필요합니다" alert이 발생하고 홈으로 리다이렉트되는 문제.

#### 원인
`AdminLayout.tsx`의 권한 체크가 sessionStorage 복원 완료 전에 실행되어 `user`가 아직 `null`인 상태에서 비관리자로 판단.

#### 수정 내용
- **`context/AppContext.tsx`** — `isInitialized` 상태 추가. sessionStorage 복원 완료 후 `setIsInitialized(true)` 호출. `AppContext.Provider` value에 포함.
- **`pages/admin/AdminLayout.tsx`** — `isInitialized` 사용하여 초기화 완료 전 권한 체크 차단.
  - `useEffect`: `if (!isInitialized) return;` 조건 추가
  - 렌더링: `!isInitialized || !user || !user.isAdmin` 조건으로 초기화 전 null 반환

---

## 날짜: 2026-03-28 (스케줄러 최적화 및 자동입찰 설계)

---

### 프론트엔드 수정사항

- 입찰시 상품가격 변동 토스트 알람이랑 입찰완료 토스트 알람 겹치는거 수정

라인 314: showToast('입찰이 완료되었습니다!') — 내 입찰 성공 응답
라인 176: showToast('새로운 입찰이 생겼습니다!') — SSE priceUpdate 브로드캐스트 (내 입찰도 전파됨)
useRef 플래그로 내가 방금 입찰한 직후엔 SSE 토스트를 건너뛰도록 수정

---

## 날짜: 2026-03-29

---

### 19. WonProductDetail 환불하기 기능 삭제 (박종권)

#### 수정
- **`WonProductDetail.tsx`** — 결제완료 상태에서 "환불 요청" 버튼 제거, "구매 확정하기" 단독 버튼으로 교체
  - 안전 거래 안내 문구 중 "환불 요청" 관련 안내 문구 제거
- **`InquiryManagement.tsx`** — 환불 관련 코드 정리
- **`InquiryCreate.tsx`** — 환불 관련 코드 정리
- **`mockData.ts`** — 환불 관련 mock 데이터 제거 및 정리
- **`types.ts`** — 환불 관련 타입 정의 제거

---

## 날짜: 2026-03-30 (마이페이지 세부 기능 및 뼈대 코드 추가)

---

### 20. 마이페이지 미구현 기능 뼈대 컴포넌트 추가 (박종권)

#### 신규 생성
- **`MyPageStubs.tsx`** — 추후 구현 예정 기능의 stub 컴포넌트 모음
  - `ProfileEditStub` — 회원정보 수정 (닉네임, 휴대폰번호, 이메일, 주소)
  - `MembershipWithdrawalStub` — 회원 탈퇴 확인 모달
  - `ReviewManagementStub` — 내가 작성한/받은 리뷰 목록
  - `NoticePageStub` — 공지사항 리스트

### 마이페이지 추가 작업 예정 목록
- [ ] 입찰기록 - 판매 구매기록 연동
- [ ] 회원정보 수정 UI 및 기능 구현
- [ ] 회원탈퇴 처리 로직 
- [ ] 리뷰 및 관리 컴포넌트 추가
- [ ] 등록된 물품관리 리스트업
- [ ] 공지사항 페이지 생성
- [ ] 찜목록 가져오기 연동
- [ ] 입찰기록 상세 페이지 연동

---

### 21. 자동입찰 관련 마이페이지 UI 수정

#### 수정
- **`MyPage.tsx`**
  - 입찰 상태 배지 텍스트 변경: `"경매중"` → `"입찰중"`, `"낙찰"` → `"낙찰성공"`
  - 낙찰 상품 버튼 텍스트/색상 변경: `"낙찰 상세보기"` (초록) → `"결제대기중"` (노란/amber)
  - 구매내역 탭에 `"구매완료"` (indigo) 배지 추가
- **`WonProductDetail.tsx`** — 소폭 UI 수정

---

### 22. 알림함 DB 연동 및 대화탭 정리 (오수환)

#### 문제
알림 목록이 mock 데이터로만 구성되어 실제 DB 알림이 표시되지 않았음. 알림탭 열어도 읽음 처리 API가 호출되지 않음.

#### 수정
- **`context/AppContext.tsx`**
  - `notifications`, `chats` 초기값을 mock 데이터 → 빈 배열(`[]`)로 교체
  - `fetchNotifications()` 추가 — `GET /api/notifications` 호출 후 상태 매핑 (`notiNo`, `content`, `isRead`, `linkUrl`, `type`)
  - 로그인/로그아웃 시 알림 목록 자동 갱신 (`useEffect` — `user` 의존성)
  - `markAllNotificationsAsRead()` 추가 — `PATCH /notifications/read-all` 호출
  - `markNotificationAsRead()` — `PATCH /notifications/{id}/read` API 호출 추가 (기존엔 로컬 상태만 변경)
- **`components/Layout.tsx`**
  - 알림 드롭다운 열 때 `markAllNotificationsAsRead()` 자동 호출 (전체 읽음 처리)
  - 알림 목록 비어있을 때 "알림이 없습니다." 빈 상태 표시 추가
  - 대화 탭 채팅 목록 내용 제거 (미구현 상태 정리)
  - `chats` 의존성 및 `markChatAsRead` 사용 제거

---

### 23. SSE 디버그 로그 추가 (오수환, 임시)

#### 수정
- **`context/AppContext.tsx`** — SSE 연결/메시지 디버깅용 콘솔 로그 추가 (추후 제거 예정)
  - `eventSource.onopen`, `eventSource.onmessage` 로그
  - `notification` 이벤트 수신 로그

---

## 날짜: 2026-03-31 (자동입찰 UI 구현)

---

### 24. 자동입찰 등록/수정/취소 UI (`pages/product/ProductDetail.tsx`)

#### 신규 상태
- `activeAutoBid: { autoBidNo: number; maxPrice: number } | null` — 내 활성 자동입찰 정보

#### 수정 내용

**자동입찰 상태 초기 로드**
- `fetchProduct()` 내에서 `GET /api/auto-bid/active?productNo=` 호출
- 200 응답 시 `activeAutoBid` 설정, 204(미존재) 시 `null`

**자동입찰 등록/수정 모달 연동**
- `openBidModal('auto')` 호출 시 `activeAutoBid`가 있으면 기존 `maxPrice`로 입력값 초기화
- `handleBidSubmit()` — `modalType === 'auto'`이면 `POST /api/auto-bid`로 `{productNo, maxPrice}` 전송
- 성공 시 `activeAutoBid` 상태 갱신 + 토스트 표시
- 자동입찰 버튼 텍스트: `activeAutoBid` 존재 여부에 따라 "자동 입찰" / "자동입찰 수정"으로 전환

**자동입찰 취소 버튼**
- `activeAutoBid` 존재 시 버튼 아래 "설정중 · 최대 {maxPrice}원 / 취소" 배지 표시
- 취소 클릭 → `DELETE /api/auto-bid/{productNo}` 호출
- 400 응답도 성공으로 처리 (시스템이 이미 취소한 경우 멱등 처리)
- 성공 시 `setActiveAutoBid(null)` + 토스트 표시

**SSE 실시간 배지 갱신**
- `pointUpdate` 이벤트 수신 시 `GET /api/auto-bid/active` 재조회 → 자동입찰이 취소되었으면 배지 자동 제거
  - 근거: 자동입찰 취소(경쟁 패배/상위 입찰)는 반드시 포인트 환불 SSE를 동반함
