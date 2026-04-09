package com.javajava.project.domain.member.dto;

import lombok.Getter;

@Getter
public class NotificationSettingDto {
    private Boolean auctionEnd;
    private Boolean newBid;
    private Boolean chat;
    private Boolean marketing;
}
