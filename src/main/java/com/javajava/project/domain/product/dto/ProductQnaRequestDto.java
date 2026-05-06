package com.javajava.project.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductQnaRequestDto {
    @NotBlank(message = "문의 내용은 필수입니다.")
    @Size(max = 1000, message = "문의 내용은 1000자 이하여야 합니다.")
    private String content;
}
