# 팀원 공유 주의사항

> 담당: 오수환 | 최종 업데이트: 2026-03-21

---

## 1. CORS 전역 설정 추가

**파일:** `src/main/java/com/javajava/project/config/SecurityConfig.java`

`corsConfigurationSource()` 빈을 추가하여 CORS를 전역으로 허용했습니다.

- 허용 출처: 전체 (`*`) — 개발 단계 기준
- 허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS
- 허용 헤더: 전체 (Authorization 포함)

**프론트(React) 연동:** React 개발 서버(`localhost:3000`)에서 Spring Boot(`localhost:8080`)로 요청 시 CORS 오류 없이 연동됩니다.

**백엔드 팀원 영향:** 없음. Thunder Client는 브라우저가 아니므로 기존 테스트 방식 그대로 사용 가능합니다.

> 운영 배포 시에는 `allowedOriginPatterns("*")` 를 실제 React 배포 도메인으로 교체해야 합니다.

---

## 2. GlobalExceptionHandler — 예외 처리 범위 확장

**파일:** `src/main/java/com/javajava/project/config/GlobalExceptionHandler.java`

두 가지 예외 핸들러가 추가되었습니다.

### 추가된 핸들러

| 예외 | HTTP 상태 | 응답 |
|------|-----------|------|
| `JwtException` | 401 Unauthorized | `{"error": "토큰이 유효하지 않습니다."}` |
| `Exception` (catch-all) | 500 Internal Server Error | `{"error": "서버 오류가 발생했습니다."}` |

### ⚠️ 주의 — 디버깅 방식 변경

기존에는 처리되지 않은 예외가 발생하면 응답 본문에 스택 트레이스가 노출되었습니다.
이제는 catch-all 핸들러가 모든 미처리 예외를 잡아 `500` 과 단순 메시지만 응답으로 반환합니다.

**응답만 보면 오류 원인을 알 수 없으므로, 반드시 서버 콘솔 로그를 확인하세요.**

```
예외 발생 시 확인 순서:
1. 응답 상태코드 확인 (400 / 401 / 409 / 500)
2. VSCode 터미널 콘솔에서 스택 트레이스 확인
3. 필요 시 GlobalExceptionHandler에 해당 예외 타입 핸들러 추가
```

### 새로운 예외 타입을 추가하고 싶다면

`GlobalExceptionHandler.java` 에 아래 형식으로 추가하면 됩니다.

```java
@ExceptionHandler(YourException.class)
public ResponseEntity<Map<String, String>> handleYourException(YourException ex) {
    return ResponseEntity.status(HttpStatus.xxx)
            .body(Map.of("error", ex.getMessage()));
}
```

catch-all `Exception` 핸들러보다 구체적인 타입이 우선 적용되므로 기존 동작에 영향을 주지 않습니다.

---

## 3. SSE 실시간 알림 연동 (`2026-03-21`)

**파일:** `SseController.java`, `SseService.java`

프론트엔드가 `GET /api/sse/subscribe?clientId={memberNo}` 로 SSE 연결을 맺습니다.

- `clientId`는 로그인 후 localStorage에 저장된 `memberNo` 값 사용
- 백엔드에서 알림을 발송할 때는 `SseService.sendToClient(memberNo.toString(), dto)` 호출
- 이벤트 이름은 **`notification`** 으로 통일 (프론트에서 이 이름으로 수신)
- 브라우저 자동 재연결 기능 있으므로 서버 재시작 시 자동 복구됨

**백엔드 알림 발송 방법 (새 알림 생성 시):**
```java
// NotificationService 또는 각 Service에서
Notification saved = notificationRepository.save(notification);
sseService.sendToClient(String.valueOf(memberNo), NotificationResponseDto.from(saved));
```

---

## 4. 에러 응답 형식 규약

현재 프로젝트의 에러 응답 형식은 아래와 같이 통일되어 있습니다.
새로운 예외 처리 추가 시 동일한 형식을 사용해 주세요.

```json
{ "error": "오류 메시지" }
```

Bean Validation 실패(400)의 경우에만 필드별 형식으로 반환됩니다.

```json
{
  "userId": "아이디는 4~20자 영문+숫자여야 합니다.",
  "password": "비밀번호는 8자 이상이어야 합니다."
}
```

---

## 5. 회원가입 시 이메일 인증 필수 (`2026-03-27`)

회원가입 시 **실제 이메일로 인증번호를 받아야** 가입이 가능합니다.

### 흐름
1. 사용자가 이메일 입력 후 "인증번호 발송" 클릭
2. `POST /api/auth/send-email-code` → 해당 이메일로 6자리 인증번호 발송 (Gmail SMTP)
3. 사용자가 받은 인증번호 입력 후 "확인" 클릭
4. `POST /api/auth/verify-email-code` → 인증번호 검증 (3분 이내)
5. 인증 완료 후 회원가입 제출 가능

### 테스트 시 주의사항
- **실제 이메일 주소**를 입력해야 인증번호를 받을 수 있습니다
- 인증번호는 **3분** 후 만료됩니다
- 인증번호는 서버 메모리(ConcurrentHashMap)에 저장되므로, **서버 재시작 시 초기화**됩니다
