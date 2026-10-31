package com.example.boardproject.controller;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.auth.Result;
import com.example.boardproject.dto.*;
import com.example.boardproject.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 추가
    //260627 - JSON 요청값과 선택 이미지 파일을 받는 multipart 생성 API 추가
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    /*
     * 기능 : multipart 게시글 작성 API
     * 설명 : 인증된 사용자와 본문 JSON·선택 사진을 서비스에 전달하고 생성 성공 시 HTTP 201을 반환합니다.
     */
    public ResponseEntity<Result> createPost(@RequestPart("request") final PostCreateRequestDto dto,
                                             @RequestPart(value = "image", required = false) final MultipartFile image,
                                             @AuthenticationPrincipal final PrincipalDetails user) {
        PostResponseDto response = postService.createPost(dto, image, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.of(response));
    }

    //260627 - 기존 JSON 방식 클라이언트 호환 API 유지
    @PostMapping(value = "/posts", consumes = MediaType.APPLICATION_JSON_VALUE)
    /*
     * 기능 : JSON 게시글 작성 API
     * 설명 : 사진 없는 기존 JSON 형식을 지원합니다. 이 요청도 보안 필터에서 로그인을 요구합니다.
     */
    public ResponseEntity<Result> createPostLegacy(@RequestBody final PostRequestDto dto,
                                                   @AuthenticationPrincipal final PrincipalDetails user) {
        PostResponseDto response = postService.createPost(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.of(response));
    }

    // 게시글 삭제
    @DeleteMapping("/posts/{postId}")
    /*
     * 기능 : 게시글 삭제 API
     * 설명 : 인증된 사용자 ID와 게시글 ID를 전달해 작성자 확인 후 삭제하도록 합니다.
     */
    public ResponseEntity<Result> deletePost(@PathVariable final Long postId,
                                             @AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        postService.deletePost(postId, user.getId());
        return ResponseEntity.ok(Result.of("게시글 삭제"));
    }

    // 게시글 수정
    @PatchMapping(value = "/posts/{postId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    /*
     * 기능 : JSON 게시글 수정 API
     * 설명 : 본문 변경과 사진 제거 요청을 인증된 사용자 정보와 함께 전달합니다.
     */
    public ResponseEntity<Result> updatePost(@RequestBody final PostUpdateRequestDto dto,
                                             @PathVariable final Long postId,
                                             @AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        postService.updatePost(postId, dto, user.getId());
        return ResponseEntity.ok(Result.of("정상 수정."));
    }

    @PatchMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    /*
     * 기능 : 첨부 사진을 교체하는 게시글 수정 API
     * 설명 : 수정 JSON과 선택 이미지 파일을 함께 받아 공통 수정 로직으로 전달합니다.
     */
    public ResponseEntity<Result> updatePostImage(@RequestPart("request") PostUpdateRequestDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @PathVariable Long postId, @AuthenticationPrincipal PrincipalDetails user) throws Exception {
        postService.updatePost(postId, dto, image, user.getId());
        return ResponseEntity.ok(Result.of("정상 수정."));
    }

    // 게시글 목록 조회 (페이지네이션- cursor로 수정 )
    // @AuthenticationPrincipal
    //@RequestParm는 URL의 쿼리 파라미터를 메서드 파라미터로 꺼내오는 애노테이션임.(사용자에게 노출되고 조작 가능성이 있어서 노출되도 괜찮은 값만)
    //ex) GET /v1/posts?postId=1&cursor=1043

    @GetMapping("/posts")
    /*
     * 기능 : 공개 게시글 목록 API
     * 설명 : 선택 커뮤니티와 cursor를 받아 다음 목록을 반환합니다. 비로그인도 조회할 수 있습니다.
     */
    public ResponseEntity<Result> getPosts(@AuthenticationPrincipal final PrincipalDetails principal ,
                                            @RequestParam(required = false) Long cursor,
                                            @RequestParam(required = false) Long communityId) {
        Long userId = principal != null ? principal.getId():null;
        List<PostGetResponseDto> response = postService.getPosts(userId, cursor, communityId);
        return ResponseEntity.ok(Result.of(response));
    }




    // 게시글 검색
    @GetMapping("/posts/search")
    /*
     * 기능 : 공개 게시글 검색 API
     * 설명 : 제목·내용 검색어와 커뮤니티·페이지 범위를 서비스에 전달합니다.
     */
    public ResponseEntity<Result> searchPosts(@RequestParam String keyword,
                                              @RequestParam(defaultValue = "0") int offset,
                                              @RequestParam(defaultValue = "5") int limit,
                                              @RequestParam(required = false) Long communityId) {
        List<PostGetResponseDto> response = postService.searchPosts(keyword, offset, limit, communityId);
        return ResponseEntity.ok(Result.of(response));
    }

    // 게시글 상세 조회
    @GetMapping("/posts/{postId}")
    /*
     * 기능 : 공개 게시글 상세 API
     * 설명 : 로그인 사용자는 열람 기록을 반영한 뒤 상세 정보를 반환하며 비로그인도 본문을 볼 수 있습니다.
     */
    public ResponseEntity<Result> getPost(@PathVariable final Long postId,
                                          @AuthenticationPrincipal final PrincipalDetails principal) throws Exception {
        Long userId = principal != null ? principal.getId() : null;

        if (userId != null) {
            postService.increaseViewCount(postId, userId);
        }

        PostGetDetailResponseDto response = postService.getPost(postId, userId);
        return ResponseEntity.ok(Result.of(response));
    }

    // 좋아요
    @PostMapping("/posts/{postId}/likes")
    /*
     * 기능 : 좋아요 등록 API
     * 설명 : 등록 성공 시 최신 개수와 상태를 반환하고 중복 등록은 HTTP 409와 현재 상태로 응답합니다.
     */
    public ResponseEntity<Result> likePost(@PathVariable final Long postId,
                                           @AuthenticationPrincipal final PrincipalDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.of("UNAUTHORIZED"));
        }

        try {
            Integer likeCount = postService.likePost(postId, user.getId());
            return ResponseEntity.ok(Result.of(new PostLikeResponseDto(likeCount, true)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Result.of("POST_ALREADY_LIKED", postService.getLikeStatus(postId, user.getId())));
        }
    }

    // 좋아요 취소
    @DeleteMapping("/posts/{postId}/likes")
    /*
     * 기능 : 좋아요 취소 API
     * 설명 : 취소 성공 시 최신 상태를 반환하고 이미 취소된 요청은 HTTP 409로 처리합니다.
     */
    public ResponseEntity<Result> unlikePost(@PathVariable final Long postId,
                                             @AuthenticationPrincipal final PrincipalDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.of("UNAUTHORIZED"));
        }

        try {
            Integer likeCount = postService.unlikePost(postId, user.getId());
            return ResponseEntity.ok(Result.of(new PostLikeResponseDto(likeCount, false)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Result.of("POST_ALREADY_UNLIKED", postService.getLikeStatus(postId, user.getId())));
        }
    }


}
