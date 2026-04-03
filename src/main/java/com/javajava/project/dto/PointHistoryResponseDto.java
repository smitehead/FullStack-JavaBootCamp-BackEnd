//포인트 내역 응답
package com.javajava.project.dto;

import com.javajava.project.entity.PointHistory;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class PointHistoryResponseDto {
    //충전/낙찰차감/판매정산/출금
    private String type;
    //변동 포인트 (양수면 증가, 음수면 감소)
    private Long amount;
    //변동 후 잔액
    private Long balance;
    private String reason;
    private LocalDateTime createdAt;

    public static PointHistoryResponseDto from(PointHistory h){
        return PointHistoryResponseDto.builder()
                .type(h.getType())
                .amount(h.getAmount())
                .balance(h.getBalance())
                .reason(h.getReason())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
