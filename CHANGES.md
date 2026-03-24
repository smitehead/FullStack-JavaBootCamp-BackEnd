# 프론트엔드 수정사항

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

## ⚠️ 남은 작업 (TODO)

### emdNo (읍면동 코드) 처리
백엔드 회원가입 API에서 `emdNo`가 필수 FK 컬럼임.
현재 임시로 `emdNo: 1`을 하드코딩했으므로, **DB에 해당 값이 없으면 회원가입 실패**.

**해결 방법 (둘 중 하나):**
1. 백엔드 팀에서 주소 검색 API(`GET /api/address?keyword=xxx`) 제공 후 프론트 주소 검색 버튼과 연동
2. 임시로 백엔드에서 `emdNo` 없이도 가입 가능하도록 nullable 처리

### 이메일 인증 연동
현재 이메일 인증이 프론트에서 랜덤 숫자를 생성해 `alert`로 노출하는 Mock 방식.
실제 이메일 발송 API가 백엔드에 준비되면 연동 필요.
