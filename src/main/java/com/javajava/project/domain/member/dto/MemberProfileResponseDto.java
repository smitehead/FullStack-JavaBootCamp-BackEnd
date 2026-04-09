package com.javajava.project.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberProfileResponseDto {
    private String nickname;
    private String email;
    private String phoneNum;
    private String addrRoad;
    private String addrDetail;
    private String addrShort;
    
    // Notification Settings (1 or 0)
    private Integer notifyAuctionEnd;
    private Integer notifyNewBid;
    private Integer notifyChat;
    private Integer marketingAgree;
}
