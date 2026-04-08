package com.javajava.project.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class BlockedUserId implements Serializable {

    @Column(name = "MEMBER_NO")
    private Long memberNo;

    @Column(name = "BLOCKED_MEMBER_NO")
    private Long blockedMemberNo;
}

