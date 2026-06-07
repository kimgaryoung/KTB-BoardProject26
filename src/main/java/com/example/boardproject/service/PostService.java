package com.example.boardproject.service;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.dto.*;
import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.User;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.repository.PostRepository;
import com.example.boardproject.repository.UserProfileRepository;
import com.example.boardproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;


    //게시글 추가
    @Transactional
    public PostResponseDto createPost(final PostRequestDto dto, final PrincipalDetails user)  {

        Post post = new Post(
                Long.parseLong(user.getUserId()),
                dto.getPostTitle(),
                dto.getPostImage(),
                dto.getPostDate(),
                dto.getPostContent().toString()
        );

        return PostResponseDto.from(postRepository.save(post));
    }

    //게시글  삭제 deletePost
    @Transactional
    public String deletePost(final Long postId) throws Exception {
        Post post = findByPostId(postId);
        postRepository.delete(post);

        return "게시글 삭제";
    }

    private Post findByPostId(final Long postId) throws Exception {
        return postRepository.findByPostId(postId);

    }

    //게시글 수정.
    @Transactional
    public String updatePost(final Long postId, final PostUpdateRequestDto dto, final Long userId) throws Exception {
        User user = findByUserId(userId);
        Post post = findByPostId(postId);

        post.updatePost(
                dto.getPostTitle(),
                dto.getPostContent(),
                dto.getPostImage(),
                dto.getPostDate()

        );

        return "정상 수정.";
    }



    private User findByUserId(final Long userId) throws Exception {
        return userRepository.findByUserId(userId);
    }




    //게시글 전체 조회.=게시글 목록 조회
    @Transactional(readOnly = true)
    public List<PostGetResponseDto> getPosts() {
        return postRepository.findAll()
                .stream()
                .map(post -> {
                    UserProfile userProfile = userProfileRepository.findByUserId(post.getUserId());
                    return PostGetResponseDto.of(post, userProfile);

                })
                .collect(Collectors.toList());

    }

    //게시글 상세조회

    @Transactional(readOnly = true)
    public PostGetDetailResponseDto getPost(final Long postId) throws Exception {
        Post post = findByPostId(postId);
        UserProfile userProfile = findByProfileUserId(post.getUserId());


        return PostGetDetailResponseDto.of(post,userProfile);
    }


    private UserProfile findByProfileUserId(final Long userId) throws Exception {
        return userProfileRepository.findByUserId(userId);
    }





}
