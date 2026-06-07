package com.example.boardproject.dto;

import com.example.boardproject.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 게시글 목록 조회 응답  - 사용자에게 전달할 게시글
// 내용은 목록에서 제외
@Getter
@AllArgsConstructor
@Builder
public class PostGetDetailResponseDto {

    private Long userId;
    private String nickname;
    private String profileImage;
    private Long postId;
    private String postTitle;
    private String postContent;
    private String postImage;
    private LocalDateTime postDate;

//
//    public static PostGetDetailResponseDto from(Post savedPost) {
//
//        return PostGetDetailResponseDto.builder()
//                .userId(savedPost.getUserId())
//                .postId(savedPost.getPostId())
//                .postTitle(savedPost.getPostTitle())
//                .postContent(savedPost.getPostContent())
//                .postImage(savedPost.getPostImage())
//                .postDate(savedPost.getPostDate())
//                .build();
//    }
//
//    public static PostGetDetailResponseDto from(Post post) {
//        return PostGetDetailResponseDto.builder()
//                .id(post.getPostId())
//                .semesterName(post.getSemesterName())
//                .build();
//    }

//    public static List<PostGetDetailResponseDto> from(List<Post> post) {
//        return post.stream()
//                .map(PostGetDetailResponseDto::from)
//                .toList();
//    }


}