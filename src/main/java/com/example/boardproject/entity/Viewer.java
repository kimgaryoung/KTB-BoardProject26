package com.example.boardproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "VIEWER")
public class Viewer {  //해당 테이블에 속성으로 insert되었다는 게 조회한적이 있다는 의미라 조회수레 자동으로 포함됨.

    @Id @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "user_id")
    private Long userId;


    //좋아요 누르면 true로 변경
    @Column
    private Boolean likeCheck;


    //생성자 - 행이 생성되면 조회수 +1
    public Viewer(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
        this.likeCheck = false;
    }



    public void updateLikeCheck(Boolean likeCheck) {
        this.likeCheck = likeCheck;
    }
}
