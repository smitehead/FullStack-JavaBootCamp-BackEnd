package com.javajava.project.domain.member.dto;

import com.javajava.project.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberSummaryDto {
    private Long memberNo;
    private String nickname;
    private String profileImgUrl;
    private Double mannerTemp;
    private Long points;
    private Integer isAdmin;
    private LocalDateTime joinedAt;

    public static MemberSummaryDto from(Member m) {
        return MemberSummaryDto.builder()
                .memberNo(m.getMemberNo())
                .nickname(m.getNickname())
                .profileImgUrl(m.getProfileImgUrl())
                .mannerTemp(m.getMannerTemp())
                .points(m.getPoints())
                .isAdmin(m.getIsAdmin())
                .joinedAt(m.getJoinedAt())
                .build();
    }
}
