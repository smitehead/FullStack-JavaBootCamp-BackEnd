package com.javajava.project.domain.member.dto;

import com.javajava.project.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MemberResponseDto {

    private Long memberNo;
    private String userId;
    private String nickname;
    private String email;
    private String phoneNum;
    private String addrRoad;
    private String addrDetail;
    private String addrShort;
    private LocalDate birthDate;
    private String profileImgUrl;
    private Double mannerTemp;
    private Long points;
    private LocalDateTime joinedAt;
    private Integer isActive;
    private Integer isAdmin;
    private Integer isSuspended;
    private LocalDateTime suspendUntil;
    private String suspendReason;
    private Integer isPermanentSuspended;
    private Integer notifyOn;
    private Integer notiAuctionEnd;
    private Integer notiNewBid;
    private Integer notiChat;
    private Integer notiMarketing;
    private Integer marketingAgree;

    public static MemberResponseDto from(Member m) {
        return MemberResponseDto.builder()
                .memberNo(m.getMemberNo())
                .userId(m.getUserId())
                .nickname(m.getNickname())
                .email(m.getEmail())
                .phoneNum(m.getPhoneNum())
                .addrRoad(m.getAddrRoad())
                .addrDetail(m.getAddrDetail())
                .addrShort(m.getAddrShort())
                .birthDate(m.getBirthDate())
                .profileImgUrl(m.getProfileImgUrl())
                .mannerTemp(m.getMannerTemp())
                .points(m.getPoints())
                .joinedAt(m.getJoinedAt())
                .isActive(m.getIsActive())
                .isAdmin(m.getIsAdmin())
                .isSuspended(m.getIsSuspended())
                .suspendUntil(m.getSuspendUntil())
                .suspendReason(m.getSuspendReason())
                .isPermanentSuspended(m.getIsPermanentSuspended())
                .notifyOn(m.getNotifyOn())
                .notiAuctionEnd(m.getNotiAuctionEnd())
                .notiNewBid(m.getNotiNewBid())
                .notiChat(m.getNotiChat())
                .notiMarketing(m.getNotiMarketing())
                .marketingAgree(m.getMarketingAgree())
                .build();
    }
}
