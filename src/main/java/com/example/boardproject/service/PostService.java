package com.example.boardproject.service;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.dto.*;
import com.example.boardproject.entity.PostProfile;
import com.example.boardproject.entity.*;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.query.QueryCountInspector;
import com.example.boardproject.repository.*;
import lombok.RequiredArgsConstructor;


import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;

import lombok.extern.slf4j.Slf4j;


@Slf4j
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

    private final QueryCountInspector queryCountInspector;

    private final int DEFAULT_PAGE_SIZE=5; // 페이지네이션의 limit값.


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
/*
    // 게시글 전체 조회
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

*/

    // findAll()

    @Transactional(readOnly = true)
    public List<PostGetResponseDto> getPostsFindAll(int offset, int limit) {
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




    //게시글 목록 조회 - 무한 스크롤링 (cursor형식)
    @Transactional(readOnly = true)
    public List<PostGetResponseDto> getPosts(Long userId ,Long cursor) {


        PageRequest pageRequest =PageRequest.of(0,DEFAULT_PAGE_SIZE);

        Slice<Post>postList =
                postRepository.findPostByCursor(
                        cursor,
                        pageRequest
                );

        log.info( //로그로 페이지 네이션 확인
                "조회된 게시글 수={}, pageSize={}, hasNext={}",
                postList.getNumberOfElements(),
                pageRequest.getPageSize(),
                postList.hasNext()
        );

        List<Post> posts = postList.getContent();

        // N+1 방지: 게시글마다 작성자/통계 조회하지 않고 페이지 단위로 한번에 배치 조회
        List<Long> userIds = posts.stream().map(Post::getUserId).distinct().collect(Collectors.toList());
        List<Long> postIds = posts.stream().map(Post::getPostId).collect(Collectors.toList());

        Map<Long, UserProfile> userProfileById = userProfileRepository.
                findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, up -> up));

        Map<Long, PostProfile> postProfileById = postProfileRepository.findByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(PostProfile::getPostId, pp -> pp));

        return posts.stream()

                //map() : 각 요소들의 형태를 바꾸겠다.
                //post1 -> dto1 , post2 -> dto2, post3 -> dto3
                .map(post -> {
                    UserProfile userProfile = userProfileById.get(post.getUserId()); //작성자 프로필 조회
                    PostProfile postProfile = postProfileById.get(post.getPostId());
                    return PostGetResponseDto.of(post, userProfile, postProfile); //dto로 조합

                })
                .collect(Collectors.toList()); //  collect() : list형태로 만들겠다. [dto1, dto2, dto3]
    }

    // 게시글 검색- 검색으로 특정 위치로 이동해야하기 때문에 cursorX , offset 형식
    //현재 offset과 Limit를 JPQL로 변경하는 과정 필요함.
    @Transactional(readOnly = true)
    public List<PostGetResponseDto> searchPosts(String keyword, int offset, int limit) {
        List<Post> posts = postRepository.findSearchPostsByOffset(keyword, offset, limit);

        log.info( //로그로 검색 페이지네이션 확인
                "검색된 게시글 수={}, keyword={}, offset={}, limit={}",
                posts.size(),
                keyword,
                offset,
                limit
        );

        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = posts.stream().map(Post::getUserId).distinct().collect(Collectors.toList());
        List<Long> postIds = posts.stream().map(Post::getPostId).collect(Collectors.toList());

        Map<Long, UserProfile> userProfileById = userProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, up -> up));

        Map<Long, PostProfile> postProfileById = postProfileRepository.findByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(PostProfile::getPostId, pp -> pp));

        return posts.stream()
                .map(post -> {
                    UserProfile userProfile = userProfileById.get(post.getUserId());
                    PostProfile postProfile = postProfileById.get(post.getPostId());
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

    // 조회수 증가
    @Transactional
    public void increaseViewCount(Long postId, Long userId) {
        Viewer viewer = viewerRepository.findById(new ViewerId(postId, userId)).orElse(null);

        if (viewer == null) {
            viewer = new Viewer(postId, userId);
            viewer.updateViewCheck(true);
            viewerRepository.save(viewer);
            findPostProfile(postId).updateViewCount(1);
        } else if (!Boolean.TRUE.equals(viewer.getViewCheck())) {
            viewer.updateViewCheck(true);
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

    @Transactional(readOnly = true)
    public PostLikeResponseDto getLikeStatus(Long postId, Long userId) {
        Viewer viewer = viewerRepository.findById(new ViewerId(postId, userId)).orElse(null);
        PostProfile postProfile = findPostProfile(postId);
        boolean isLiked = viewer != null && Boolean.TRUE.equals(viewer.getLikeCheck());

        return new PostLikeResponseDto(postProfile.getLikeCount(), isLiked);
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
