package com.example.boardproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
//260627 - multipart 게시글 생성 요청의 JSON 파트 DTO 추가
public class PostCreateRequestDto {
    private String title;
    private String content;
}
