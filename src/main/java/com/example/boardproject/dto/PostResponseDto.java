package com.example.boardproject.dto;

import com.example.boardproject.entity.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
public class PostResponseDto {

    private Long id;
    private Long userId;

    public static PostResponseDto from(Post savedPost) {
        return PostResponseDto.builder()
                .id(savedPost.getPostId())
                .userId(savedPost.getUserId())
                .build();
    }
}
