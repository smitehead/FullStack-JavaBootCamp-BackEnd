public interface InquiryService {
    Long create(Long memberNo, InquiryRequestDto dto);
    Page<InquiryResponseDto> getMyInquiries(Long memberNo, int page, int size);
    InquiryResponseDto getDetail(Long inquiryNo, Long memberNo);
    // 관리자
    Page<InquiryResponseDto> getAll(Integer status, int page, int size);
    void answer(Long inquiryNo, Long adminNo, String adminNickname, InquiryAnswerDto dto);
    void delete(Long inquiryNo);
}