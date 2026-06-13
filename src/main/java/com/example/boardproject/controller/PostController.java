package com.example.boardproject.controller;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.auth.Result;
import com.example.boardproject.dto.*;
import com.example.boardproject.service.PostService;
import com.example.boardproject.service.UserService;
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
    public ResponseEntity<Result> createPost(@RequestBody final PostRequestDto dto,
                                             @AuthenticationPrincipal final PrincipalDetails user) {


        PostResponseDto response  = postService.createPost(dto, user);

        return ResponseEntity.ok(Result.of(response));

    }


    //게시글 삭제
    //글쓴이만 삭제할 수 있게 해야함.
    @DeleteMapping("/posts/{postId}")
    public String deletePost(@PathVariable("postId") final Long postId,
                             @AuthenticationPrincipal final PrincipalDetails user) throws Exception {

        return postService.deletePost(postId,user.getId());
    }

    //게시글 수정
    //글쓴이만 수정할 수 있게 해야함.
    @PatchMapping("/posts/{postId}")
    public String updatePost(@RequestBody final PostUpdateRequestDto dto,
                                @PathVariable("postId") final Long postId,
                                @AuthenticationPrincipal final PrincipalDetails user) throws Exception {


        return postService.updatePost(postId, dto, user.getId());
    }

    // 게시글 전체 조회= 게시글 목록 조회.
    @GetMapping("/posts")
    public ResponseEntity<Result> getPosts() {

        List<PostGetResponseDto> response = postService.getPosts();
        return ResponseEntity.ok(Result.of(response));
    }

    //게시글 상세 조회, 해당 api요청이 올때마다 조회수 증가, 한사람당 한명만 +1
    @GetMapping("/posts/{postId}")
    public ResponseEntity<Result> getPost(@PathVariable("postId") final Long postId , @AuthenticationPrincipal final PrincipalDetails principal ) throws Exception {

        // 조회수를 게시글 상세 조회와는 따로 구현해야 게시글 상세조회가 readolny를 유지할 수 있기 떄문에 controller에서 다른 서비스 요청.
        /* increaseViewCount 함수에 게시글 상세조회한 사용자의 userId가 필요하기 때문에
        @AuthenticationPrincipal final PrincipalDetails principal 을 사용함.

         @AuthenticationPrincipal : 사용하면 인증된 사용자 객체를 쉽게 주입 받으 수 있음.
          장점 : 직접 객체를 다루지 않고 주입 받을 수 있어서 간결함./ 인증된 사용자 정보만 주입 받아서 보안측면에서 유리함.
          주의할 점 : 



         */


        postService.increaseViewCount(postId, principal.getId());

        PostGetDetailResponseDto response = postService.getPost(postId);

        return ResponseEntity.ok(Result.of(response));
    }








}
