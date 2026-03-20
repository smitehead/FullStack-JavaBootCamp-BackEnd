package com.javajava.project.config;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러.
 * @RestControllerAdvice: 모든 @RestController에서 발생한 예외를 한 곳에서 처리.
 * 각 예외 유형에 맞는 HTTP 상태코드를 일관되게 반환하여 프론트와의 에러 처리 규약을 통일.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 처리 (400 Bad Request)
     * 발생 시점: @Valid 검증 실패 시 (MemberController.join 등)
     * 응답 형태: { "필드명": "오류메시지", ... } — 어떤 필드가 왜 실패했는지 알 수 있음
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * 비즈니스 로직 중복 충돌 처리 (409 Conflict)
     * 발생 시점: 아이디·닉네임·이메일 중복 등 이미 존재하는 데이터와 충돌할 때
     * 예: throw new IllegalStateException("이미 존재하는 아이디입니다.")
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * 잘못된 인자 처리 (400 Bad Request)
     * 발생 시점: 존재하지 않는 회원 조회, 만 14세 미만 가입 시도 등
     * 예: throw new IllegalArgumentException("만 14세 미만은 가입할 수 없습니다.")
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * JWT 토큰 오류 처리 (401 Unauthorized)
     * 발생 시점: 토큰이 만료됐거나 변조된 경우
     * 응답 형태: { "error": "토큰이 유효하지 않습니다." }
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, String>> handleJwtException(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "토큰이 유효하지 않습니다."));
    }

    /**
     * 그 외 처리되지 않은 예외 (500 Internal Server Error)
     * 발생 시점: 예상치 못한 서버 오류
     * 응답 형태: { "error": "서버 오류가 발생했습니다." }
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "서버 오류가 발생했습니다."));
    }
}
