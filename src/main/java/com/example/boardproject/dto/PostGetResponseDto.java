package com.example.boardproject.dto;

import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.PostProfile;
import com.example.boardproject.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class PostGetResponseDto {

    private Long id;
    private Long communityId;
    private String title;
    private String postImageUrl;
    private String thumbnailUrl;
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

    /*
     * 기능 : 게시글 목록과 썸네일 응답 생성
     * 설명 : 본문·작성자·통계 정보를 조합합니다. thumbnailUrl은 별도 축소 파일이 아닌 첨부 사진과 같은 주소이며 화면에서 작게 표시합니다.
     */
    public static PostGetResponseDto of(Post post, UserProfile userProfile, PostProfile postProfile, String imageUrl) {
        return PostGetResponseDto.builder()
                .id(post.getPostId())
                .communityId(post.getCommunityId())
                .title(post.getPostTitle())
                .postImageUrl(imageUrl)
                .thumbnailUrl(imageUrl)
                .createdAt(post.getPostDate())
                .viewCount(postProfile.getViewerCount())
                .author(new Author(userProfile.getNickname(), userProfile.getProfileImage()))
                .commentCount(postProfile.getCommnetCount())
                .likeCount(postProfile.getLikeCount())
                .build();
    }
}
