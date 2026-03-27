# 환경변수 설정 가이드

프로젝트 루트에 `.env` 파일을 생성하고 아래 내용을 채워주세요.

```
DB_URL="실제값"
DB_USERNAME="실제값"
DB_PASSWORD="실제값"

JWT_SECRET="실제값"

MAIL_USERNAME="실제값"
MAIL_PASSWORD="실제값"
```

> 실제 값은 팀장에게 문의하세요.

---

## 주의사항

- `.env` 파일은 깃에 올라가지 않습니다. 직접 생성해야 합니다.
- `.env` 파일 위치: 프로젝트 루트 (`pom.xml`과 같은 위치)
