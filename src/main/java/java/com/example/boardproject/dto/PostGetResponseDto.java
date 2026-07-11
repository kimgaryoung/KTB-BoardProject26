package java.com.example.boardproject.dto;

import java.com.example.boardproject.entity.Post;
import java.com.example.boardproject.entity.PostProfile;
import java.com.example.boardproject.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class PostGetResponseDto {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private Author author;
    private Integer commentCount;
    private Integer likeCount;

    @Getter
    @AllArgsConstructor
    public static class Author {
        private String nickname;
        private String profileImageUrl;
    }

    public static PostGetResponseDto of(Post post, UserProfile userProfile, PostProfile postProfile) {
        return PostGetResponseDto.builder()
                .id(post.getPostId())
                .title(post.getPostTitle())
                .createdAt(post.getPostDate())
                .viewCount(postProfile.getViewerCount())
                .author(new Author(userProfile.getNickname(), userProfile.getProfileImage()))
                .commentCount(postProfile.getCommnetCount())
                .likeCount(postProfile.getLikeCount())
                .build();
    }
}
