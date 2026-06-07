package com.example.boardproject.controller;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.auth.Result;
import com.example.boardproject.dto.*;
import com.example.boardproject.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;


    //게시글 추가
    @PostMapping("/posts")
    public ResponseEntity<Result> createPost(@RequestBody final PostRequestDto dto, @AuthenticationPrincipal final PrincipalDetails user) {


        PostResponseDto response  = postService.createPost(dto, user);

        return ResponseEntity.ok(Result.of(response));

    }


    //게시글 삭제
    @DeleteMapping("/posts/{postId}")
    public String deletePost(@PathVariable("postId") final Long postId) throws Exception {

        return postService.deletePost(postId);
    }

    //게시글 수정
    @PatchMapping("/posts/{postId}")
    public String updatePost(@RequestBody final PostUpdateRequestDto dto,
                                @PathVariable("postId") final Long postId,
                                @AuthenticationPrincipal final PrincipalDetails user) throws Exception {


        return postService.updatePost(postId, dto, user.getId());
    }

    // 게시글 전체 조회
    @GetMapping("/posts")
    public ResponseEntity<Result> getPosts() {

        List<PostGetResponseDto> response = postService.getPosts();
        return ResponseEntity.ok(Result.of(response));
    }

    //게시글 상세 조회
//    @GetMapping("/posts/{postId}")
//    public ResponseEntity<Result> getPost() {
//
//        List<PostResponseDto> response = postService.getPost();
//        return ResponseEntity.ok(Result.of(response));
//    }






}
