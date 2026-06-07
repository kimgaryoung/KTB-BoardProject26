package com.example.boardproject.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name="POSTPROFILE")
public class PostProfile {

    @Id @Column(name ="post_id")
    private Long postId;


    @OneToOne(fetch = FetchType.LAZY)//1:1 식별관계
    @MapsId //@id로 지정한 컬럼에 외래키를 매핑
    @JoinColumn(name="post_id")
    private Post post;



}
