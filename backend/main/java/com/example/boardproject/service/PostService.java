package com.example.boardproject.service;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.dto.*;
import com.example.boardproject.entity.PostProfile;
import com.example.boardproject.entity.*;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.exception.AuthorizedException;
import com.example.boardproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CommentRepository commentRepository;
    private final PostProfileRepository postProfileRepository;
    private final ViewerRepository viewerRepository;
    private final S3ImageService s3ImageService;


    // 게시글 추가
    @Transactional
    public PostResponseDto createPost(final PostRequestDto dto, final PrincipalDetails user) {
        return savePost(dto.getTitle(), dto.getContent(), null, user);
    }

    //260627 - 이미지가 있으면 S3 업로드 후 imageKey를 저장하는 게시글 생성 기능 추가
    @Transactional
    public PostResponseDto createPost(final PostCreateRequestDto dto,
                                      final MultipartFile image,
                                      final PrincipalDetails user) {
        String imageKey = image == null ? null : s3ImageService.upload(image);
        return savePost(dto.getTitle(), dto.getContent(), imageKey, user);
    }

    private PostResponseDto savePost(String title, String content, String imageKey, PrincipalDetails user) {
        Post post = new Post(
                Long.parseLong(user.getUserId()),
                title,
                imageKey,
                LocalDateTime.now(),
                content
        );

        Post savedPost = postRepository.save(post);
        postProfileRepository.save(new PostProfile(savedPost));

        return PostResponseDto.from(savedPost);
    }

    // 게시글 삭제
    @Transactional
    public String deletePost(final Long postId, final Long userId) throws Exception {
        Post post = findByPostId(postId);

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자가 아니라서 삭제 안됨.");
        }

        postProfileRepository.deleteById(postId);
        postRepository.delete(post);

        return "게시글 삭제";
    }

    // 게시글 수정
    @Transactional
    public String updatePost(final Long postId, final PostUpdateRequestDto dto, final Long userId) throws Exception {
        Post post = findByPostId(postId);

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자가 아니라서 수정 안됨.");
        }

        post.updatePost(
                dto.getTitle(),
                dto.getContent(),
                LocalDateTime.now()
        );

        return "정상 수정.";
    }

    // 게시글 전체 조회 (페이지네이션)
    @Transactional(readOnly = true)
    public List<PostGetResponseDto> getPosts(int offset, int limit) {
        return postRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getPostDate().compareTo(a.getPostDate()))
                .skip(offset)
                .limit(limit)
                .map(post -> {
                    UserProfile userProfile = userProfileRepository.findByUserId(post.getUserId());
                    PostProfile postProfile = findPostProfile(post.getPostId());
                    return PostGetResponseDto.of(post, userProfile, postProfile);
                })
                .collect(Collectors.toList());
    }

    // 게시글 검색
    @Transactional(readOnly = true)
    public List<PostGetResponseDto> searchPosts(String keyword, int offset, int limit) {
        return postRepository.findAll()
                .stream()
                .filter(post -> post.getPostTitle().contains(keyword) || post.getPostContent().contains(keyword))
                .sorted((a, b) -> b.getPostDate().compareTo(a.getPostDate()))
                .skip(offset)
                .limit(limit)
                .map(post -> {
                    UserProfile userProfile = userProfileRepository.findByUserId(post.getUserId());
                    PostProfile postProfile = findPostProfile(post.getPostId());
                    return PostGetResponseDto.of(post, userProfile, postProfile);
                })
                .collect(Collectors.toList());
    }

    // 게시글 상세 조회
    @Transactional(readOnly = true)
    public PostGetDetailResponseDto getPost(final Long postId, final Long userId) throws Exception {
        Post post = findByPostId(postId);
        UserProfile userProfile = userProfileRepository.findByUserId(post.getUserId());
        PostProfile postProfile = findPostProfile(postId);
        List<PostGetDetailResponseDto.CommentInfo> comments = commentRepository.findCommentInfoByPostId(postId);

        boolean isLiked = false;
        if (userId != null) {
            Viewer viewer = viewerRepository.findById(new ViewerId(postId, userId)).orElse(null);
            isLiked = viewer != null && Boolean.TRUE.equals(viewer.getLikeCheck());
        }

        //260627 - 상세 조회 시 imageKey를 공개 이미지 URL로 변환하도록 추가
        return PostGetDetailResponseDto.of(
                post, userProfile, comments, postProfile, isLiked, s3ImageService.getImageUrl(post.getPostImageKey())
        );
    }

    // 조회수 증가 (버그 수정: Viewer 저장 누락, viewCount 항상 1 세팅 → 증가로 변경)
    @Transactional
    public void increaseViewCount(Long postId, Long userId) {
        ViewerId viewerId = new ViewerId(postId, userId);
        boolean alreadyViewed = viewerRepository.existsByPostIdAndUserId(postId, userId);

        if (!alreadyViewed) {
            viewerRepository.save(new Viewer(postId, userId));
            findPostProfile(postId).updateViewCount(1);
        }
    }

    // 좋아요
    @Transactional
    public Integer likePost(Long postId, Long userId) {
        Viewer viewer = viewerRepository.findById(new ViewerId(postId, userId)).orElse(null);

        if (viewer == null) {
            viewer = new Viewer(postId, userId);
            viewer.updateLikeCheck(true);
            viewerRepository.save(viewer);
        } else {
            if (Boolean.TRUE.equals(viewer.getLikeCheck())) {
                throw new IllegalStateException("POST_ALREADY_LIKED");
            }
            viewer.updateLikeCheck(true);
        }

        PostProfile postProfile = findPostProfile(postId);
        postProfile.updateLikeCount(1);
        return postProfile.getLikeCount();
    }

    // 좋아요 취소
    @Transactional
    public Integer unlikePost(Long postId, Long userId) {
        Viewer viewer = viewerRepository.findById(new ViewerId(postId, userId)).orElse(null);

        if (viewer == null || !Boolean.TRUE.equals(viewer.getLikeCheck())) {
            throw new IllegalStateException("POST_ALREADY_UNLIKED");
        }

        viewer.updateLikeCheck(false);
        PostProfile postProfile = findPostProfile(postId);
        postProfile.updateLikeCount(-1);
        return postProfile.getLikeCount();
    }

    private Post findByPostId(final Long postId) throws Exception {
        return postRepository.findByPostId(postId);
    }

    private PostProfile findPostProfile(final Long postId) {
        return postProfileRepository.findByPostId(postId);
    }
}
