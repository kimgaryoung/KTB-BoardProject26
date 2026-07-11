package com.example.boardproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity // DB 테이블과 매핑
@Getter // service에서 DB데이터 읽을 때
@NoArgsConstructor // 기본 생성자 자동 생성
@Table(name = "POST")
public class Post {

    // PK 자동 증가
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    //제약조건 : NOT NULL
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 제약조건: NOT NULL, 최대 26자
    @Column(nullable = false, length = 26)
    private String postTitle;

    //260627 - Java 필드명을 postImageKey로 명확히 하고 기존 DB 컬럼명은 유지
    @Column(name = "post_image", length = 255)
    private String postImageKey;

    // 제약조건: NOT NULL, 글자수 제한 없음
    @Column(nullable = false, columnDefinition = "TEXT")
    private String postContent;

    // 게시글 등록 시- 현재시간 자동입력 /수정 시- 자동 업데이트
    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime postDate;

    // 생성자: 게시글 작성 시 -사용자에게 받는 값 (postDate는 DB가 자동입력),
    public Post(Long userId, String postTitle, String postImageKey, LocalDateTime postDate , String postContent) {
        this.userId = userId;
        this.postTitle = postTitle;
        this.postImageKey = postImageKey;
        this.postDate = postDate;
        this.postContent = postContent;
    }

    //260627 - 기존 수정 API가 이미지 URL로 imageKey를 덮어쓰지 않도록 이미지 변경 제거
    public void updatePost(String postTitle, String postContent, LocalDateTime postDate) {
        this.postTitle = postTitle;
        this.postContent = postContent;
        this.postDate = postDate;
    }
}
