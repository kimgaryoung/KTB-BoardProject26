package com.example.boardproject.service;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.dto.CommentRequestDto;
import com.example.boardproject.entity.Comment;
import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.PostProfile;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.repository.CommentRepository;
import com.example.boardproject.repository.PostProfileRepository;
import com.example.boardproject.repository.PostRepository;
import com.example.boardproject.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.example.boardproject.entity.QComment.comment;
import static com.example.boardproject.entity.QPostProfile.postProfile;

@Service
@RequiredArgsConstructor
public class CommentService {

    //private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserProfileRepository userProfileRepository;
    private final CommentRepository commentRepository;
    private final PostProfileRepository postProfileRepository;
    private final SiteBoardService siteBoard;

    //1. 댓글 추가
    //추가하면 댓글 수 +1 , 삭제하면 -1
    @Transactional
    /*
     * 기능 : 게시글 댓글 생성
     * 설명 : 인증된 작성자의 프로필과 게시글을 연결해 댓글을 저장하고 댓글 통계를 1 증가시킵니다.
     */
    public String createComment(final CommentRequestDto dto, final Long postId, final PrincipalDetails user) {

        //User findUser = findUserId(Long.parseLong(user.getUserId()));
        Post findPost = findPostId(postId);
        UserProfile userProfile = userProfileRepository.findByUserId(user.getId());

        //PostProfile -댓글수
        PostProfile postProfile = findPostProfile(postId);


        Comment comment = new Comment(
                dto.getCommentContent(),
                userProfile,
                findPost
        );

        comment.updateCommentDate(LocalDateTime.now());// 시간추가

        Comment saved =commentRepository.save(comment);

        //댓글 수 추가- 추가,삭제 같은 함수로 업데이트
        postProfile.updateCommentCount(1);



        return "댓글 등록 완료 (id: " + saved.getCommentId() + ")";
    }




    //2. 게시글 id찾기
    private Post findPostId(final Long postId) {
        Post post = postRepository.findByPostId(postId);
        if (post == null) throw new com.example.boardproject.exception.BusinessException("POST_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND);
        siteBoard.resolveCommunityId(post.getCommunityId());
        return post;
    }

    // 댓글수-매핑된 postId찾기
    private PostProfile findPostProfile(final Long postId) {
         return postProfileRepository.findByPostId(postId);
    }



    //3. 댓글 삭제
    //게스글 -> 특정 댓글 삭제 (내가 쓴 댓글만 삭제 가능.)
    //postId와 userId를 같이 조회하는 방식.
    //추가하면 댓글 수 +1 , 삭제하면 -1
    @Transactional
    /*
     * 기능 : 게시글에 속한 댓글 삭제
     * 설명 : 게시글 ID와 댓글 ID로 대상을 조회해 삭제하고 댓글 통계를 1 감소시킵니다.
     */
    public String deleteComment(final Long postId,final Integer commentId,final Long userId)  {
        findPostId(postId);

        //postId와 commentId를 각각 호출하면 2번의 쿼라를 실행해야해서 그냥 새로 만들어서  1번만 실행되는 걸 목표로 인자에 같이 넣었습니다.
        Comment comments = commentRepository.findByComment(postId, commentId)
            .orElseThrow(()-> new IllegalArgumentException("이 게시들에 속한 댓글 아님."));

        PostProfile postProfile =findPostProfile(postId);

        commentRepository.delete(comments);

        //댓글수 삭제 -추가,삭제 같은 함수로 업데이트
        postProfile.updateCommentCount(-1);


        return "댓글 삭제 성공!";

    }


    //4. 댓글 수정  patchComment - 내용, 시간
    //삭제와 마찬가지로 postId와 userId를 같이 조회하는 방식.(만들어 둔거 재사용)
    @Transactional
    /*
     * 기능 : 댓글 본문과 수정 시각 갱신
     * 설명 : 게시글과 댓글 ID가 일치하는 대상을 찾아 내용과 시간을 변경합니다.
     */
    public String patchComment(final CommentRequestDto dto,
                               final Long postId ,
                               final Integer  commentsId,
                               final Long userId) throws Exception {
        findPostId(postId);

        Comment comments =commentRepository.findByComment(postId,commentsId)
                        .orElseThrow(()->new IllegalArgumentException("이 게시글에 속한 댓글이 아닙니다. 접근이 잘못되엇습니다."));

        comments.updateCommentContent(

                dto.getCommentContent()
        );

        comments.updateCommentDate(LocalDateTime.now());

        return "댓글 정상 수정되었습니다.";
    }




}
