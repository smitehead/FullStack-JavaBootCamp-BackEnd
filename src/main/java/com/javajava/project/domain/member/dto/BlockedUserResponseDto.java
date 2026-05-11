package com.javajava.project.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BlockedUserResponseDto {
    private Long id;
    private String nickname;
    private String profileImage;
    private Double mannerTemp;
}
