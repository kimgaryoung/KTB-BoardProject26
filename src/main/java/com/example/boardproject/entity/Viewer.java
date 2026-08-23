package com.example.boardproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "VIEWER")
@IdClass(ViewerId.class)// 복합키를 PK로 설정, (JPQL 쿼리 간단하게 쓰는게 좋아서 해당 방법으로 구현)
public class Viewer {

    @Id @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "user_id")
    private Long userId;


    //좋아요 누르면 좋아요수 증가 -> true로 변경
    @Column
    private Boolean likeCheck;

    //상세 조회하면 조회수 증가 ->  true로 변경
    @Column
    private Boolean viewCheck;

    public Viewer(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
        this.likeCheck = false;
        this.viewCheck = false;
    }


    //좋아요 수
    public void updateLikeCheck(Boolean likeCheck) {
        this.likeCheck = likeCheck;
    }

    //조횟
    public void updateViewCheck(Boolean viewCheck) {
        this.viewCheck = viewCheck;
    }
}
