package com.example.boardproject.dto;

import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.PostProfile;
import com.example.boardproject.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class PostGetDetailResponseDto {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;
    private String profileImage;
    //260627 - 프론트에서 바로 사용할 수 있는 게시글 이미지 URL 필드 추가
    private String postImageUrl;
    //260627 - 기존 응답 호환을 위해 filePath에도 동일한 공개 URL 반환
    private String filePath;
    private Boolean isLiked;
    private Integer likeCount;
    private Integer viewCount;
    private Integer commentCount;
    private Long userId;
    private Long writerId;
    private List<CommentInfo> comments;

    @Getter
    @AllArgsConstructor
    public static class CommentInfo {
        private Integer commentId;
        private String commentContent;
        private LocalDateTime commentDate;
        private String nickname;
        private String profileImage;
        private Long userId;
    }

    public static PostGetDetailResponseDto of(Post post,
                                              UserProfile userProfile,
                                              List<CommentInfo> comments,
                                              PostProfile postProfile,
                                              Boolean isLiked,
                                              String postImageUrl) {
        return PostGetDetailResponseDto.builder()
                .id(post.getPostId())
                .title(post.getPostTitle())
                .content(post.getPostContent())
                .createdAt(post.getPostDate())
                .nickname(userProfile.getNickname())
                .profileImage(userProfile.getProfileImage())
                .postImageUrl(postImageUrl)
                .filePath(postImageUrl)
                .isLiked(isLiked)
                .likeCount(postProfile.getLikeCount())
                .viewCount(postProfile.getViewerCount())
                .commentCount(postProfile.getCommnetCount())
                .userId(post.getUserId())
                .writerId(post.getUserId())
                .comments(comments)
                .build();
    }
}
