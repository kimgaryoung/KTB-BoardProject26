package com.example.boardproject.dto;

import com.example.boardproject.entity.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;


import java.time.LocalDateTime;

// 게시글 목록 조회 응답  - 사용자에게 전달할 게시글
// 내용은 목록에서 제외
@Getter
@AllArgsConstructor
@Builder
public class PostResponseDto {

    private Long userId;
    private Long postId;
    private String postTitle;
    private String postContent;
    private String postImage;
    private LocalDateTime postDate;


    public static PostResponseDto from(Post savedPost) {

        return PostResponseDto.builder()
                .userId(savedPost.getUserId())
                .postId(savedPost.getPostId())
                .postTitle(savedPost.getPostTitle())
                .postContent(savedPost.getPostContent())
                .postImage(savedPost.getPostImage())
                .postDate(savedPost.getPostDate())
                .build();
    }




}