package com.javajava.project.domain.community.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTagId implements Serializable {
    private Long reviewNo;
    private Long tagId;
}
