package com.example.boardproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostUpdateRequestDto {
    private String title;
    private String content;
    //260627 - 기존 요청 호환 필드이며 이미지 변경은 별도 S3 업로드 API 도입 전까지 미지원
    @Deprecated
    private String attachFileUrl;
}
