@Getter @Builder
public class InquiryResponseDto {
    private Long inquiryNo;
    private Long memberNo;
    private String memberNickname;
    private String type;
    private String bugType;
    private String title;
    private String content;
    private Integer status;
    private String answer;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;
    private Long adminNo;
    private String adminNickname;

    public static InquiryResponseDto from(CsInquiry i, String nickname) {
        return InquiryResponseDto.builder()
                .inquiryNo(i.getInquiryNo())
                .memberNo(i.getMemberNo())
                .memberNickname(nickname)
                .type(i.getType())
                .bugType(i.getBugType())
                .title(i.getTitle())
                .content(i.getContent())
                .status(i.getStatus())
                .answer(i.getAnswer())
                .answeredAt(i.getAnsweredAt())
                .createdAt(i.getCreatedAt())
                .adminNo(i.getAdminNo())
                .adminNickname(i.getAdminNickname())
                .build();
    }
}