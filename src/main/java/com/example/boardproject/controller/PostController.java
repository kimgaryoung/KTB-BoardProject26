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
    public ResponseEntity<Result> createPost(@RequestPart("request") final PostCreateRequestDto dto,
                                             @RequestPart(value = "image", required = false) final MultipartFile image,
                                             @AuthenticationPrincipal final PrincipalDetails user) {
        PostResponseDto response = postService.createPost(dto, image, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.of(response));
    }

    //260627 - 기존 JSON 방식 클라이언트 호환 API 유지
    @PostMapping(value = "/posts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result> createPostLegacy(@RequestBody final PostRequestDto dto,
                                                   @AuthenticationPrincipal final PrincipalDetails user) {
        PostResponseDto response = postService.createPost(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.of(response));
    }

    // 게시글 삭제
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Result> deletePost(@PathVariable final Long postId,
                                             @AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        postService.deletePost(postId, user.getId());
        return ResponseEntity.ok(Result.of("게시글 삭제"));
    }

    // 게시글 수정
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<Result> updatePost(@RequestBody final PostUpdateRequestDto dto,
                                             @PathVariable final Long postId,
                                             @AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        postService.updatePost(postId, dto, user.getId());
        return ResponseEntity.ok(Result.of("정상 수정."));
    }

    // 게시글 목록 조회 (페이지네이션- cursor로 수정 )
    // @AuthenticationPrincipal
    //@RequestParm는 URL의 쿼리 파라미터를 메서드 파라미터로 꺼내오는 애노테이션임.(사용자에게 노출되고 조작 가능성이 있어서 노출되도 괜찮은 값만)
    //ex) GET /v1/posts?postId=1&cursor=1043

    @GetMapping("/posts")
    public ResponseEntity<Result> getPosts(@AuthenticationPrincipal final PrincipalDetails principal ,
                                            @RequestParam(required = false) Long cursor) {
        Long userId = principal != null ? principal.getId():null;
        List<PostGetResponseDto> response = postService.getPosts(userId, cursor);
        return ResponseEntity.ok(Result.of(response));
    }




    // 게시글 검색
    @GetMapping("/posts/search")
    public ResponseEntity<Result> searchPosts(@RequestParam String keyword,
                                              @RequestParam(defaultValue = "0") int offset,
                                              @RequestParam(defaultValue = "5") int limit) {
        List<PostGetResponseDto> response = postService.searchPosts(keyword, offset, limit);
        return ResponseEntity.ok(Result.of(response));
    }

    // 게시글 상세 조회
    @GetMapping("/posts/{postId}")
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
