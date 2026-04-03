package com.javajava.project.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequestDto {

    // 아이디: 영문+숫자 조합, 4~20자
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문+숫자 조합만 가능합니다.")
    private String userId;

    // 비밀번호: 8~20자 (서비스 레이어에서 BCrypt로 암호화됨)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 15, message = "닉네임은 2~15자여야 합니다.")
    private String nickname;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 50)
    private String email;

    // 휴대폰번호: 010-1234-5678 또는 011-123-4567 형식
    @NotBlank(message = "휴대폰번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "올바른 휴대폰번호 형식이 아닙니다.")
    private String phoneNum;

    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 200)
    private String addrRoad;

    @NotBlank(message = "상세주소는 필수입니다.")
    @Size(max = 255)
    private String addrDetail;

    @Size(max = 50)
    private String addrShort;

    // 생년월일: 만 14세 미만 가입 제한 검증은 서비스 레이어에서 수행
    @NotNull(message = "생년월일은 필수입니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    // 마케팅 수신 동의 (0: 미동의, 1: 동의) - 선택 항목이므로 null 허용
    private Integer marketingAgree;
}