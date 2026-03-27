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
