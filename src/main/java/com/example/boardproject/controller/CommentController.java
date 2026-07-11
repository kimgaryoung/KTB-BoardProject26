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


    //2.댓글 삭제
    //로그인된 사용자가 맞으면 ->
    @DeleteMapping("/posts/{postId}/comments/{commentsId}")
    public ResponseEntity<Result> deleteComment(@PathVariable final Long postId,
                                                @PathVariable final Integer commentsId,
                                                @AuthenticationPrincipal final PrincipalDetails user) {

        String response = commentService.deleteComment(postId, commentsId,user.getId());
        return ResponseEntity.ok(Result.of(response));
    }


    //3. 댓글 수정
    //댓글 내용 수정되고 날짜 update
    @PatchMapping("/posts/{postId}/comments/{commentsId}")
    public String patchComment(@RequestBody final CommentRequestDto dto,
                                              @PathVariable final Long postId,
                                              @PathVariable final Integer commentsId,
                                              @AuthenticationPrincipal final PrincipalDetails user)throws Exception{

        return commentService.patchComment(dto, postId,commentsId,user.getId());

    }




}
