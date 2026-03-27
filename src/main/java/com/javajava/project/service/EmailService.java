package com.javajava.project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // 이메일 → {인증번호, 만료시각} 저장 (서버 메모리)
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    private static final int EXPIRE_MINUTES = 3;

    /**
     * 6자리 인증번호 생성 후 이메일 발송
     */
    public void sendVerificationCode(String email) throws MessagingException {
        String code = String.format("%06d", new Random().nextInt(1000000));
        codeStore.put(email, new CodeEntry(code, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES)));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setTo(email);
        helper.setSubject("[JAVAJAVA] 이메일 인증번호");
        helper.setText(
            "<div style='font-family:sans-serif;max-width:480px;margin:0 auto;padding:32px;border:1px solid #eee;border-radius:12px'>"
            + "<h2 style='color:#FF5A5A;margin-bottom:8px'>JAVAJAVA</h2>"
            + "<p style='color:#333;font-size:15px'>아래 인증번호를 입력해주세요.</p>"
            + "<div style='font-size:32px;font-weight:bold;letter-spacing:8px;color:#111;margin:24px 0'>" + code + "</div>"
            + "<p style='color:#999;font-size:12px'>인증번호는 3분간 유효합니다.</p>"
            + "</div>",
            true
        );
        mailSender.send(message);
    }

    /**
     * 인증번호 검증
     * @return true: 인증 성공 / false: 틀리거나 만료
     */
    public boolean verifyCode(String email, String code) {
        CodeEntry entry = codeStore.get(email);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expireAt())) {
            codeStore.remove(email);
            return false;
        }
        if (!entry.code().equals(code)) return false;
        codeStore.remove(email); // 인증 성공 시 코드 삭제
        return true;
    }

    private record CodeEntry(String code, LocalDateTime expireAt) {}
}
