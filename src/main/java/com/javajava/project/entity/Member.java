package com.javajava.project.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "MEMBER_SEQ", allocationSize = 1)
    @Column(name = "MEMBER_NO")
    private Long memberNo;

    @Column(name = "USER_ID", nullable = false, unique = true, length = 20)
    private String userId;

    @Column(nullable = false, length = 255) // bcrypt 암호화 고려
    private String password;

    @Column(nullable = false, unique = true, length = 15)
    private String nickname;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "PHONE_NUM", nullable = false, length = 15)
    private String phoneNum;

    @Column(name = "EMD_NO", nullable = false)
    private Long emdNo; // 읍면동번호 (FK)

    @Column(name = "ADDR_DETAIL", nullable = false, length = 255)
    private String addrDetail;

    @Column(name = "BIRTH_DATE", nullable = false)
    private LocalDate birthDate;

    @Column(name = "PROFILE_IMG_NO")
    private Long profileImgNo; // 이미지번호 (FK, Nullable)

    @Column(name = "MANNER_TEMP", nullable = false)
    private Double mannerTemp = 36.5;

    @Column(nullable = false)
    private Long points = 0L;

    @Column(name = "JOINED_AT", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive = 1; // 1: 활성, 0: 탈퇴

    @Column(name = "IS_ADMIN", nullable = false)
    private Integer isAdmin = 0; // 1: 관리자

    @Column(name = "IS_SUSPENDED", nullable = false)
    private Integer isSuspended = 0; // 1: 정지

    @Column(name = "SUSPEND_UNTIL")
    private LocalDateTime suspendUntil;

    @Column(name = "NOTIFY_ON", nullable = false)
    private Integer notifyOn = 1;

    @Column(name = "MARKETING_AGREE", nullable = false)
    private Integer marketingAgree = 0;
}