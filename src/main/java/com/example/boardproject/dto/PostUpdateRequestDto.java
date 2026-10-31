package com.example.boardproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/*
 * 기능 : 게시글 수정 요청 데이터
 * 설명 : 제목·본문과 사진 제거 여부를 받습니다. 새 사진은 multipart 파일로만 전달하며 기존 attachFileUrl 값은 무시합니다.
 */
public class PostUpdateRequestDto {
    private String title;
    private String content;
    private boolean removeImage;
    // Legacy field is ignored; replacement images are accepted only as uploaded files.
    @Deprecated
    private String attachFileUrl;
}
