package com.example.boardproject.controller;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.auth.Result;
import com.example.boardproject.dto.CommentRequestDto;
import com.example.boardproject.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    //1. 댓글 추가
    //@AuthenticationPrincipal를 사용해서 로그인한 유저만 작성 가능.
    //특정 게시글 -댓글 추가되는 방식이라 URL 이렇게 작성함.
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Result> createComment(@PathVariable final Long postId, @RequestBody final CommentRequestDto dto, @AuthenticationPrincipal final PrincipalDetails user) {

        String response = commentService.createComment(dto, postId, user);

        return ResponseEntity.ok(Result.of(response));
    }


    //2.
}
