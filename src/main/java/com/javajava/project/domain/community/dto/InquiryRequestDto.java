@Getter
public class InquiryRequestDto {
    @NotBlank private String type;
    private String bugType;
    @NotBlank @Size(max = 200) private String title;
    @NotBlank private String content;
}