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
import java.util.Map;


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

    // 게시글 전체 조회 (페이지네이션)
    @GetMapping("/posts")
    public ResponseEntity<Result> getPosts(@RequestParam(defaultValue = "0") int offset,
                                           @RequestParam(defaultValue = "5") int limit) {
        List<PostGetResponseDto> response = postService.getPosts(offset, limit);
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
        try {
            Integer likeCount = postService.likePost(postId, user.getId());
            return ResponseEntity.ok(Result.of(Map.of("likeCount", likeCount)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.of("POST_ALREADY_LIKED"));
        }
    }

    // 좋아요 취소
    @DeleteMapping("/posts/{postId}/likes")
    public ResponseEntity<Result> unlikePost(@PathVariable final Long postId,
                                             @AuthenticationPrincipal final PrincipalDetails user) {
        try {
            Integer likeCount = postService.unlikePost(postId, user.getId());
            return ResponseEntity.ok(Result.of(Map.of("likeCount", likeCount)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.of("POST_ALREADY_UNLIKED"));
        }
    }
}
