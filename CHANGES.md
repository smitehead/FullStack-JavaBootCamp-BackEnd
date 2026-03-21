# 변경 내역

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
  - **최종: `spring.jpa.database-platform=org.hibernate.community.dialect.Oracle10gDialect`** (ROWNUM 사용, 11g 완전 호환)

### `pom.xml`
- jjwt 의존성 추가 (버전 0.12.6)
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson`

### `.gitignore`
- `CLAUDE.md`, `CHANGES.md`, `FLOW.md` 제외 추가 → CHANGES.md는 이후 커밋에 포함하기로 변경

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
