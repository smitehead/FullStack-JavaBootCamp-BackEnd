package com.javajava.project.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class InquiryRequestDto {
    @NotBlank private String type;
    private String bugType;
    @NotBlank @Size(max = 200) private String title;
    @NotBlank private String content;
}