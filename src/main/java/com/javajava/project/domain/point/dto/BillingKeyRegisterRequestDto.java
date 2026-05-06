package com.javajava.project.domain.point.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BillingKeyRegisterRequestDto {
    @NotBlank(message = "카드번호는 필수입니다.")
    @Pattern(regexp = "\\d{16}", message = "카드번호는 숫자 16자리여야 합니다.")
    private String cardNumber;

    @NotBlank(message = "유효기간은 필수입니다.")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "유효기간은 YYYY-MM 형식이어야 합니다.")
    private String expiry;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Size(min = 6, max = 6, message = "생년월일은 6자리여야 합니다.")
    @Pattern(regexp = "\\d{6}", message = "생년월일은 숫자 6자리여야 합니다.")
    private String birth;

    @NotBlank(message = "비밀번호 앞 2자리는 필수입니다.")
    @Size(min = 2, max = 2, message = "비밀번호는 2자리여야 합니다.")
    @Pattern(regexp = "\\d{2}", message = "비밀번호는 숫자 2자리여야 합니다.")
    private String pwd2digit;
}
