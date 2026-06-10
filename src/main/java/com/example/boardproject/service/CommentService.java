package com.example.boardproject.service;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.dto.CommentRequestDto;
import com.example.boardproject.entity.Comment;
import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.User;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.repository.CommentRepository;
import com.example.boardproject.repository.PostRepository;
import com.example.boardproject.repository.UserProfileRepository;
import com.example.boardproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserProfileRepository userProfileRepository;
    private final CommentRepository commentRepository;

    //댓글 추가
    @Transactional
    public String createComment(final CommentRequestDto dto, final Long postId, final PrincipalDetails user) {

        User findUser = findUserId(Long.parseLong(user.getUserId()));
        Post findPost = findPostId(postId);
        UserProfile userProfile = userProfileRepository.findByUserId(findUser.getUserId());

        Comment comment = new Comment(
                dto.getCommentContent(),
                findUser,
                findPost
        );

        commentRepository.save(comment);

        return "댓글 등록 완료되었습니다.";
    }

    private User findUserId(final Long userId) {
        return userRepository.findByUserId(userId);
    }

    private Post findPostId(final Long postId) {
        return postRepository.findByPostId(postId);
    }
}
