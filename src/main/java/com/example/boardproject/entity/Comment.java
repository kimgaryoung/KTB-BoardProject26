package com.example.boardproject.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity // Db테이블과 연결
@Getter  //serice에서 DB데이터 읽을떄
@NoArgsConstructor//기본생성자 자동생성
@Table(name="Comment")
public class Comment {


    //1.  commentId INT PK
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="commnetId",nullable = false)
    private Integer commentId;


    //2. commentContent string(255) length>=1 (공백은 댓글로 등록 X)
    @NotBlank
    @Column(length = 255)
    private String commentContent;


    //3. commentDate DateTime update current_timestamp (기본값: 현재시간, 수정하면 다시 현재시간으로 업데이트)
    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime commentDate;

    //4. postId  long, userId long 참조힘
    //한명의 사용자가 여러개의 댓글과 한 게시글에 대한 여러개의 댓글을 달 수 있음.
    //해당 table에는 profileImage와 nickname, userId만 있음. 이 값은 댓글 전체 조회시 항상 필요함.
    //쿼리에서 바로 접근하고 싶어서 EAGER 사용..
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private UserProfile userProfile;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;


    //생성자
    public Comment(String commentContent,UserProfile userProfile, Post post)
    {
        this.commentContent = commentContent;
        this.userProfile = userProfile;
        this.post = post;
    }


    //댓글 단 시간 추가
    public void updateCommentDate(LocalDateTime commentDate)
    {
        this.commentDate = commentDate;
    }

    public void updateCommentContent(String commentContent)
    {
        this.commentContent =commentContent;
    }


}
