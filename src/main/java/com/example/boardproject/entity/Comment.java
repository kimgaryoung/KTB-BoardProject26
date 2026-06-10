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

    private LocalDateTime commentDate;

    //4. postId  long, userId long 참조힘
    //한명의 사용자가 여러개의 댓글과 한 게시글에 대한 여러개의 댓글을 달 수 있음.
    // 댓글조회시에만 postid와 userid를 조회함.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;


    //생성자
    public Comment
    (String commentContent,User user, Post post) {
        this.commentContent = commentContent;
        this.user = user;
        this.post = post;
    }







}
