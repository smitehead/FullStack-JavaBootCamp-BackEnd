package com.javajava.project.domain.member.dto;

import com.javajava.project.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberPublicResponseDto {
    private Long memberNo;
    private String nickname;
    private String profileImgUrl;
    private Double mannerTemp;
    private LocalDateTime joinedAt;

    public static MemberPublicResponseDto from(Member m) {
        return MemberPublicResponseDto.builder()
                .memberNo(m.getMemberNo())
                .nickname(m.getNickname())
                .profileImgUrl(m.getProfileImgUrl())
                .mannerTemp(m.getMannerTemp())
                .joinedAt(m.getJoinedAt())
                .build();
    }
}
