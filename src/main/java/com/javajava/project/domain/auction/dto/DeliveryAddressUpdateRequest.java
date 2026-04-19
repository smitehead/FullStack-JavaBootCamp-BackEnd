package com.javajava.project.domain.auction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeliveryAddressUpdateRequest {
    private String addrRoad;
    private String addrDetail;
}
