package com.javajava.project.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class InquiryAnswerDto {
    @NotBlank private String answer;
}