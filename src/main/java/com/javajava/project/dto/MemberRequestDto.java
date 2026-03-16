package com.javajava.project.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequestDto {
    private String userId;
    private String password;
    private String nickname;
    private String email;
    private String phoneNum;
    private Long emdNo;
    private String addrDetail;
    private LocalDate birthDate;
    private Integer marketingAgree;
}