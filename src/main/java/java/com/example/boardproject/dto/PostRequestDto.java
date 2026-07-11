package java.com.example.boardproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostRequestDto {
    private String title;
    private String content;
    //260627 - 기존 JSON 요청 호환 필드이며 URL은 DB에 저장하지 않음
    @Deprecated
    private String attachFileUrl;
}
