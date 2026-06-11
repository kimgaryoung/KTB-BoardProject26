package com.example.boardproject.service;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.dto.*;
import com.example.boardproject.entity.PostProfile;
import com.example.boardproject.entity.*;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.repository.*;
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
    private final CommentRepository commentRepository;
    private final PostProfileRepository postProfileRepository;


    //게시글 추가
    @Transactional
    public PostResponseDto createPost(final PostRequestDto dto, final PrincipalDetails user)  {

        Post post = new Post(
                Long.parseLong(user.getUserId()),
                dto.getPostTitle(),
                dto.getPostImage(),
                dto.getPostDate(),
                dto.getPostContent()
        );

        Post savedPost = postRepository.save(post);

        //게시글 저장되고 이후 -> PostProfile이 추가한 뒤 return되어야 나중에 postProfile 조회할떄 null값 안나옴
        postProfileRepository.save(new PostProfile(savedPost));

        return PostResponseDto.from(savedPost);
    }

    //게시글  삭제 deletePost
    @Transactional
    public String deletePost(final Long postId, final Long userId) throws Exception {
        //User user = findByUserId(userId);
        Post post = findByPostId(postId);

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자가 아니라서 삭제 안됨..");
        }

        // 삭제
        postProfileRepository.deleteById(postId);
        postRepository.delete(post);

        return "게시글 삭제";



    }

    private Post findByPostId(final Long postId) throws Exception {
        return postRepository.findByPostId(postId);

    }

    //게시글 수정.
    @Transactional
    public String updatePost(final Long postId, final PostUpdateRequestDto dto, final Long userId) throws Exception {
        //User user = findByUserId(userId);
        Post post = findByPostId(postId);

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자가 아니라서 수정 안됨.");
        }

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
    //-----------댓글 마무리 하고 여기 N+1문제 다시 생각해보기---------------
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
    //댓글도 함께 조회되게 수정
    //PostProfile의 좋아요수, 댓글수 , 조회수도 함께 조회되게 동작
    @Transactional(readOnly = true)
    public PostGetDetailResponseDto getPost(final Long postId) throws Exception {
        Post post = findByPostId(postId);
        UserProfile userProfile = findByProfileUserId(post.getUserId());
        PostProfile postProfile =findPostProfile(postId);

        //작성한 댓글 List로 전부다 조회되게
        List<PostGetDetailResponseDto.CommentInfo> comments = commentRepository.findCommentInfoByPostId(postId);

        //Postprofile-> 댓글수,좋아요수, 조회수도 함께 불러오기.
        return PostGetDetailResponseDto.of(post,userProfile,comments,postProfile);
    }


    //프로필 유저 Id조회
    private UserProfile findByProfileUserId(final Long userId) throws Exception {
        return userProfileRepository.findByUserId(userId);
    }


    // 댓글수-매핑된 postId찾기
    private PostProfile findPostProfile(final Long postId) {
        return postProfileRepository.findByPostId(postId);
    }









}
