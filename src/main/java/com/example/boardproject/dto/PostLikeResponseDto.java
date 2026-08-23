package com.example.boardproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostLikeResponseDto {
    private Integer likeCount;
    private Boolean isLiked;
}
