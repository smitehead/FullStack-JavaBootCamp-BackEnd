package com.javajava.project.entity;

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
