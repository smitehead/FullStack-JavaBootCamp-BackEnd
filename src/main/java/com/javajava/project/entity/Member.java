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

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 15)
    private String nickname;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "PHONE_NUM", nullable = false, length = 15)
    private String phoneNum;

    @Column(name = "EMD_NO", nullable = false)
    private Long emdNo;

    @Column(name = "ADDR_DETAIL", nullable = false, length = 255)
    private String addrDetail;

    @Column(name = "BIRTH_DATE", nullable = false)
    private LocalDate birthDate;

    @Column(name = "PROFILE_IMG_NO")
    private Long profileImgNo;

    @Column(name = "PROFILE_IMG_URL", length = 500)
    private String profileImgUrl;

    @Builder.Default
    @Column(name = "MANNER_TEMP", nullable = false)
    private Double mannerTemp = 36.5;

    @Builder.Default
    @Column(nullable = false)
    private Long points = 0L;

    @Builder.Default
    @Column(name = "JOINED_AT", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive = 1;

    @Builder.Default
    @Column(name = "IS_ADMIN", nullable = false)
    private Integer isAdmin = 0; // ★ 이 부분에 @Builder.Default가 없어서 에러가 발생했습니다.

    @Builder.Default
    @Column(name = "IS_SUSPENDED", nullable = false)
    private Integer isSuspended = 0;

    @Column(name = "SUSPEND_UNTIL")
    private LocalDateTime suspendUntil;

    // 정지 사유
    @Column(name = "SUSPEND_REASON", length = 500)
    private String suspendReason;

    // 영구 정지 여부. 1이면 영구 정지
    @Column(name = "IS_PERMANENT_SUSPENDED")
    private Integer isPermanentSuspended;

    // 알림 수신 전체 ON/OFF
    @Builder.Default
    @Column(name = "NOTIFY_ON", nullable = false)
    private Integer notifyOn = 1;

    //경매 종료 알림. 0=꺼짐, 1=켜짐
    @Builder.Default
    @Column(name = "NOTI_AUCTION_END")
    private Integer notiAuctionEnd = 1;

    // 새 입찰 알림. 0=꺼짐, 1=켜짐
    @Builder.Default
    @Column(name = "NOTI_NEW_BID")
    private Integer notiNewBid = 1;

    // 채팅 알림. 0=꺼짐, 1=켜짐
    @Builder.Default
    @Column(name = "NOTI_CHAT")
    private Integer notiChat = 1;

    // 마케팅 알림. 0=꺼짐, 1=켜짐
    @Builder.Default
    @Column(name = "NOTI_MARKETING")
    private Integer notiMarketing = 0;

    @Builder.Default
    @Column(name = "MARKETING_AGREE", nullable = false)
    private Integer marketingAgree = 0;

    // 현재 유효한 JWT 토큰 (동시 로그인 방지용 - 새 로그인 시 기존 토큰 무효화)
    @Column(name = "CURRENT_TOKEN", length = 500)
    private String currentToken;
}