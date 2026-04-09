package com.javajava.project.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class MemberProfileUpdateDto {
    @NotBlank @Size(min=2, max=15)
    private String nickname;
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$")
    private String phoneNum;
    private String addrRoad;
    private String addrDetail;
    private String addrShort;
}
