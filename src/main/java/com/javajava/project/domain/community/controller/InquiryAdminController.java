@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class InquiryAdminController {

    private final InquiryService inquiryService;
    private final MemberRepository memberRepository;

    private Long getCurrentMemberNo() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** 전체 문의 목록 */
    @GetMapping
    public ResponseEntity<Page<InquiryResponseDto>> getAll(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inquiryService.getAll(status, page, size));
    }

    /** 문의 상세 조회 (관리자) */
    @GetMapping("/{inquiryNo}")
    public ResponseEntity<InquiryResponseDto> getDetail(@PathVariable Long inquiryNo) {
        return ResponseEntity.ok(inquiryService.getDetail(inquiryNo, null));
    }

    /** 답변 등록 */
    @PatchMapping("/{inquiryNo}/answer")
    public ResponseEntity<Void> answer(
            @PathVariable Long inquiryNo,
            @Valid @RequestBody InquiryAnswerDto dto) {
        Long adminNo = getCurrentMemberNo();
        Member admin = memberRepository.findById(adminNo)
            .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        
        inquiryService.answer(inquiryNo, adminNo, admin.getNickname(), dto);
        return ResponseEntity.ok().build();
    }

    /** 문의 삭제 */
    @DeleteMapping("/{inquiryNo}")
    public ResponseEntity<Void> delete(@PathVariable Long inquiryNo) {
        inquiryService.delete(inquiryNo);
        return ResponseEntity.ok().build();
    }
}