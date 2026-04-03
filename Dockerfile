# 1. 자바 17 버전을 베이스 이미지로 사용
FROM eclipse-temurin:17-jre-alpine

# 2. 작업할 폴더 지정
WORKDIR /app

# 3. 깃허브 액션이 배달해 줄 jar 파일을 도커 안으로 복사
COPY target/*.jar app.jar

# 4. 스프링 부트 서버 포트 열기
EXPOSE 8080

# 5. 서버 실행 명령어
CMD ["java", "-jar", "app.jar"]