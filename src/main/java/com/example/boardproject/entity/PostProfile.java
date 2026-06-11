package com.example.boardproject.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name="POSTPROFILE")
public class PostProfile {

    @Id @Column(name ="post_id")
    private Long postId;

    @MapsId  // Post의 PK와 매핑
    @OneToOne(fetch = FetchType.LAZY)
    //1:1 지연로딩- 해당 게시글을 조회하는 시점에 조회되는 방식으로 조금 지연이 걸려도 괜찮은 값들이라고 판단했습니다.
    @JoinColumn(name = "post_id")
    private Post post;


    //좋아요수
    @Column
    private Integer likeCount;

    //댓글수
    @Column
    private Integer commnetCount;

    //조회수
    @Column
    private Integer viewerCount;

    //생성자- 모두 0으로 초기화
    public PostProfile(Post post)
    {
        this.post =post; //@MapsId방식이라 jpa가 자동으로  this.postId=postId; 해줌.
        this.likeCount=0;
        this.commnetCount =0;
        this.viewerCount=0;
    }

    public void updateCommentCount(Integer commnetCount){
        this.commnetCount+=commnetCount;

    }











}
