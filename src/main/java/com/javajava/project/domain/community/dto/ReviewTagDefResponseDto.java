package com.javajava.project.domain.community.dto;

import com.javajava.project.domain.community.entity.ReviewTagDef;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewTagDefResponseDto {

    private Long tagId;
    private String tagName;
    private String applicableRole;

    public static ReviewTagDefResponseDto from(ReviewTagDef def) {
        return ReviewTagDefResponseDto.builder()
                .tagId(def.getTagId())
                .tagName(def.getTagName())
                .applicableRole(def.getApplicableRole())
                .build();
    }
}
