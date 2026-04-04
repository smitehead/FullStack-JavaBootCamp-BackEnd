package com.javajava.project.domain.member.entity;

import lombok.*;
import java.io.Serializable;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BlockedUserId implements Serializable {

    private Long memberNo;
    private Long blockedMemberNo;
}
