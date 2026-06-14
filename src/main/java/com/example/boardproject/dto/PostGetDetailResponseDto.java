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
                                              Boolean isLiked) {
        return PostGetDetailResponseDto.builder()
                .id(post.getPostId())
                .title(post.getPostTitle())
                .content(post.getPostContent())
                .createdAt(post.getPostDate())
                .nickname(userProfile.getNickname())
                .profileImage(userProfile.getProfileImage())
                .filePath(post.getPostImage())
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
