package com.example.boardproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "VIEWER")
@IdClass(ViewerId.class)// 복합키를 PK로 설정, (JPQL 쿼리 간단하게 쓰는게 좋아서 해당 방법으로 구현)
public class Viewer {  //해당 테이블에 속성으로 insert되었다는 게 조회한적이 있다는 의미라 조회수레 자동으로 포함됨.

    @Id @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "user_id")
    private Long userId;


    //좋아요 누르면 true로 변경
    @Column
    private Boolean likeCheck;


    //생성자 - post상세 조회 될때 -> 행이 생성되면
    public Viewer(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
        this.likeCheck = false;
    }



    public void updateLikeCheck(Boolean likeCheck) {
        this.likeCheck = likeCheck;
    }
}
