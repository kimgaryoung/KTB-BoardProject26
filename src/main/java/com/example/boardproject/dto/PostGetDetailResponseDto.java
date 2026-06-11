package com.example.boardproject.dto;

import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.entity.PostProfile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 게시글 목록 조회 응답  - 사용자에게 전달할 게시글
//댓글도 상세조회 항목에 추가하고 싶음.
// 내용은 목록에서 제외
@Getter
@AllArgsConstructor
@Builder
public class PostGetDetailResponseDto {

    private String nickname;
    private String profileImage;
    private Long postId;
    private String postTitle;
    private String postContent;
    private String postImage;
    private LocalDateTime postDate;

    //댓글전체를 상세조회할때 함꼐 조회하기 위해서 추가
    private List<CommentInfo> comments;


    //postProfile 값들
    private Integer commentCount;
    private Integer likeCount;
    private Integer viewerCount;


    //comment 전체를 다 요청하니까 댓글안에 또 post내용이 중첩되는 상황이 발생해서 필요한 값만 뽑아서 사용
    @Getter
    @AllArgsConstructor
    public static class CommentInfo {
        private Integer commentId;
        private String commentContent;
        private LocalDateTime commentDate;

        //userprofile출신
        private String nickname;
        private String profileImage;



    }

    //from : 하나, of : 여러개 반화시
    //댓글도 함께 조회되게 변경
    public static PostGetDetailResponseDto of(Post post,
                                              UserProfile userProfile,
                                              List<CommentInfo> comments,
                                              PostProfile postProfile) {
        return PostGetDetailResponseDto.builder()
                .nickname(userProfile.getNickname())
                .profileImage(userProfile.getProfileImage())
                .postId(post.getPostId())
                .postImage(post.getPostImage())
                .postContent(post.getPostContent())
                .postTitle(post.getPostTitle())
                .postDate(post.getPostDate())
                .comments(comments)
                .commentCount(postProfile.getCommnetCount())
                .likeCount(postProfile.getLikeCount())
                .viewerCount(postProfile.getViewerCount())
                .build();



    }

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