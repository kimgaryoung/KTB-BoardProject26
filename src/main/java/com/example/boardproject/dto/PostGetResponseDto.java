package com.example.boardproject.dto;

import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.UserProfile;
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
public class PostGetResponseDto {

    private String nickname;
    private String profileImage;
    private Long postId;
    private String postTitle;
    private LocalDateTime postDate;




    //from : 하나, of : 여러개 반화시
    public static PostGetResponseDto of(Post post, UserProfile userProfile) {
        return PostGetResponseDto.builder()
                .nickname(userProfile.getNickname())
                .profileImage(userProfile.getProfileImage())
                .postId(post.getPostId())
                .postTitle(post.getPostTitle())
                .postDate(post.getPostDate())
                .build();
    }

//    public static List<PostGetResponseDto> from(List<Post> post) {
//        return post.stream()
//                .map(PostGetResponseDto::of)
//                .toList();
//    }


}