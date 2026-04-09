package com.javajava.project.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BlockedUserResponseDto {
    private Long id; // match frontend 'user.id'
    private String nickname;
    private String profileImage; // match frontend 'user.profileImage'
    private Double mannerTemp;
}
