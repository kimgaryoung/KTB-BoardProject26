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
    private final PostImageStorage postImages;
    private final CommunityService communityService;
    private final SiteBoardService siteBoard;

    private final QueryCountInspector queryCountInspector;

    private final int DEFAULT_PAGE_SIZE=5; // 페이지네이션의 limit값.


    // 게시글 추가
    @Transactional
    /*
     * 기능 : 사진 없는 게시글 생성
     * 설명 : 소속 커뮤니티의 존재를 확인한 뒤 인증된 사용자의 글과 통계 데이터를 저장합니다.
     */
    public PostResponseDto createPost(final PostRequestDto dto, final PrincipalDetails user) {
        Long communityId = siteBoard.resolveCommunityId(dto.getCommunityId());
        communityService.getCommunity(communityId);
        return savePost(dto.getTitle(), dto.getContent(), null, user, communityId);
    }

    //260627 - 이미지가 있으면 S3 업로드 후 imageKey를 저장하는 게시글 생성 기능 추가
    @Transactional
    /*
     * 기능 : 사진을 첨부한 게시글 생성
     * 설명 : 커뮤니티를 확인하고 선택 사진을 저장소에 업로드한 뒤 공개 URL 대신 이미지 키를 게시글에 보관합니다.
     */
    public PostResponseDto createPost(final PostCreateRequestDto dto,
                                      final MultipartFile image,
                                      final PrincipalDetails user) {
        Long communityId = siteBoard.resolveCommunityId(dto.getCommunityId());
        communityService.getCommunity(communityId);
        String imageKey = image == null ? null : postImages.upload(image);
        return savePost(dto.getTitle(), dto.getContent(), imageKey, user, communityId);
    }

    /*
     * 기능 : 게시글과 초기 통계 저장
     * 설명 : 인증 정보의 작성자 ID로 게시글을 저장하고 조회수·댓글수·좋아요 통계 행을 함께 생성합니다.
     */
    private PostResponseDto savePost(String title, String content, String imageKey, PrincipalDetails user, Long communityId) {
        Post post = new Post(
                Long.parseLong(user.getUserId()),
                title,
                imageKey,
                LocalDateTime.now(),
                content,
                communityId
        );

        Post savedPost = postRepository.save(post);
        postProfileRepository.save(new PostProfile(savedPost));

        return PostResponseDto.from(savedPost);
    }

    // 게시글 삭제
    @Transactional
    /*
     * 기능 : 작성자 확인 후 게시글 삭제
     * 설명 : 요청 사용자가 글 작성자인지 확인하고 게시글 통계와 본문을 삭제합니다.
     */
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
    /*
     * 기능 : 사진 업로드 없는 게시글 수정
     * 설명 : 기존 JSON 요청을 공통 수정 로직에 전달해 본문 변경 또는 기존 사진 제거를 처리합니다.
     */
    public String updatePost(final Long postId, final PostUpdateRequestDto dto, final Long userId) throws Exception {
        return updatePost(postId, dto, null, userId);
    }

    @Transactional
    /*
     * 기능 : 게시글 본문과 첨부 사진 수정
     * 설명 : 사진 업로드 전에 작성자 권한을 확인합니다. 사진 교체와 제거를 동시에 요청하면 거부하며 기존 소속 커뮤니티는 유지합니다.
     */
    public String updatePost(final Long postId, final PostUpdateRequestDto dto, final MultipartFile image, final Long userId) throws Exception {
        Post post = findByPostId(postId);

        if (!post.getUserId().equals(userId)) {
            throw new com.example.boardproject.exception.BusinessException("POST_FORBIDDEN", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (dto.isRemoveImage() && image != null) {
            throw new com.example.boardproject.exception.BusinessException("CONFLICTING_IMAGE_UPDATE", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (image != null) post.updateImageKey(postImages.upload(image));
        else if (dto.isRemoveImage()) post.updateImageKey(null);

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
                    return PostGetResponseDto.of(post, userProfile, postProfile, postImages.getImageUrl(post.getPostImageKey()));
                })
                .collect(Collectors.toList());
    }

*/

    // findAll()

    @Transactional(readOnly = true)
    /*
     * 기능 : 전체 조회 기반의 게시글 목록 변환
     * 설명 : 모든 글을 조회한 뒤 메모리에서 정렬·구간 선택하고 작성자·통계·이미지 주소를 결합합니다. 현재 커뮤니티 목록은 별도의 cursor 조회를 사용합니다.
     */
    public List<PostGetResponseDto> getPostsFindAll(int offset, int limit) {
        return postRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getPostDate().compareTo(a.getPostDate()))
                .skip(offset)
                .limit(limit)
                .map(post -> {
                    UserProfile userProfile = userProfileRepository.findByUserId(post.getUserId());
                    PostProfile postProfile = findPostProfile(post.getPostId());
                    return PostGetResponseDto.of(post, userProfile, postProfile, postImages.getImageUrl(post.getPostImageKey()));
                })
                .collect(Collectors.toList());
    }




    //게시글 목록 조회 - 무한 스크롤링 (cursor형식)
    @Transactional(readOnly = true)
    /*
     * 기능 : 커뮤니티별 게시글 cursor 페이지 조회
     * 설명 : 최신 ID부터 5개씩 가져오고 작성자·통계는 묶어서 조회해 반복 쿼리를 줄입니다. 사진 주소를 포함한 목록 DTO를 반환합니다.
     */
    public List<PostGetResponseDto> getPosts(Long userId, Long cursor, Long communityId) {
        communityId = siteBoard.resolveCommunityId(communityId);
        if (communityId != null) communityService.getCommunity(communityId);


        PageRequest pageRequest =PageRequest.of(0,DEFAULT_PAGE_SIZE);

        Slice<Post>postList =
                postRepository.findPostByCursor(
                        cursor,
                        communityId,
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
                    return PostGetResponseDto.of(post, userProfile, postProfile, postImages.getImageUrl(post.getPostImageKey()));

                })
                .collect(Collectors.toList()); //  collect() : list형태로 만들겠다. [dto1, dto2, dto3]
    }

    // 게시글 검색- 검색으로 특정 위치로 이동해야하기 때문에 cursorX , offset 형식
    //현재 offset과 Limit를 JPQL로 변경하는 과정 필요함.
    @Transactional(readOnly = true)
    /*
     * 기능 : 커뮤니티 게시글 제목·본문 검색
     * 설명 : 페이지 범위를 검증하고 선택 커뮤니티의 검색 결과에 작성자·통계·사진 주소를 결합합니다.
     */
    public List<PostGetResponseDto> searchPosts(String keyword, int offset, int limit, Long communityId) {
        communityId = siteBoard.resolveCommunityId(communityId);
        if (communityId != null) communityService.getCommunity(communityId);
        if (offset < 0 || limit < 1 || limit > 50) {
            throw new com.example.boardproject.exception.BusinessException("INVALID_PAGINATION", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        List<Post> posts = postRepository.findSearchPostsByOffset(keyword, communityId, offset, limit);

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
                    return PostGetResponseDto.of(post, userProfile, postProfile, postImages.getImageUrl(post.getPostImageKey()));
                })
                .collect(Collectors.toList());
    }

    // 게시글 상세 조회
    @Transactional(readOnly = true)
    /*
     * 기능 : 게시글 상세 응답 구성
     * 설명 : 본문·사진·작성자·통계·댓글을 모으고 로그인한 사용자의 좋아요 여부를 함께 반환합니다.
     */
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
                post, userProfile, comments, postProfile, isLiked, postImages.getImageUrl(post.getPostImageKey())
        );
    }

    // 조회수 증가
    @Transactional
    /*
     * 기능 : 사용자별 중복 조회수 방지
     * 설명 : 사용자와 게시글의 열람 기록을 확인해 처음 조회한 경우에만 조회수를 1 증가시킵니다.
     */
    public void increaseViewCount(Long postId, Long userId) {
        findByPostId(postId);
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
    /*
     * 기능 : 게시글 좋아요 등록
     * 설명 : 사용자별 좋아요 상태를 기록하고 통계를 증가시킵니다. 이미 좋아요를 누른 경우 중복으로 처리합니다.
     */
    public Integer likePost(Long postId, Long userId) {
        findByPostId(postId);
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
    /*
     * 기능 : 최신 좋아요 상태 조회
     * 설명 : 전체 좋아요 개수와 해당 사용자의 선택 여부를 반환해 중복 요청 후 화면 상태를 맞출 수 있게 합니다.
     */
    public PostLikeResponseDto getLikeStatus(Long postId, Long userId) {
        findByPostId(postId);
        Viewer viewer = viewerRepository.findById(new ViewerId(postId, userId)).orElse(null);
        PostProfile postProfile = findPostProfile(postId);
        boolean isLiked = viewer != null && Boolean.TRUE.equals(viewer.getLikeCheck());

        return new PostLikeResponseDto(postProfile.getLikeCount(), isLiked);
    }

    // 좋아요 취소
    @Transactional
    /*
     * 기능 : 게시글 좋아요 취소
     * 설명 : 기존 좋아요가 있을 때만 상태를 해제하고 좋아요 수를 1 감소시킵니다.
     */
    public Integer unlikePost(Long postId, Long userId) {
        findByPostId(postId);
        Viewer viewer = viewerRepository.findById(new ViewerId(postId, userId)).orElse(null);

        if (viewer == null || !Boolean.TRUE.equals(viewer.getLikeCheck())) {
            throw new IllegalStateException("POST_ALREADY_UNLIKED");
        }

        viewer.updateLikeCheck(false);
        PostProfile postProfile = findPostProfile(postId);
        postProfile.updateLikeCount(-1);
        return postProfile.getLikeCount();
    }

    /*
     * 기능 : 게시글 존재 확인
     * 설명 : 대상 글을 조회하고 없으면 POST_NOT_FOUND 오류와 HTTP 404로 처리합니다.
     */
    private Post findByPostId(final Long postId) {
        Post post = postRepository.findByPostId(postId);
        if (post == null) throw new com.example.boardproject.exception.BusinessException("POST_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND);
        siteBoard.resolveCommunityId(post.getCommunityId());
        return post;
    }

    private PostProfile findPostProfile(final Long postId) {
        return postProfileRepository.findByPostId(postId);
    }
}
