//카드 정보 응답 (Settings.tsx에서 사용)
package com.javajava.project.domain.point.dto;

import com.javajava.project.domain.point.entity.BillingKey;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class BillingKeyResponseDto {
    private String cardName;
    private String cardNo;
    private LocalDateTime createdAt;
    // 카드 등록 여부
    private boolean registered;

    public static BillingKeyResponseDto from(BillingKey billingKey) {
        return BillingKeyResponseDto.builder()
                .cardName(billingKey.getCardName())
                .cardNo(billingKey.getCardNo())
                .createdAt(billingKey.getCreatedAt())
                .registered(true)
                .build();
    }

    public static BillingKeyResponseDto notRegistered() {
        return BillingKeyResponseDto.builder()
                .registered(false)
                .build();
    }
}
