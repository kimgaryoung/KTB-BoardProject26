package com.example.boardproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity // DB 테이블과 매핑
@Getter // service에서 DB데이터 읽을 때
@NoArgsConstructor // 기본 생성자 자동 생성
@Table(name = "POST", indexes = @Index(name = "idx_post_community_cursor", columnList = "community_id,post_id"))
public class Post {

    // PK 자동 증가
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    //제약조건 : NOT NULL
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Null keeps pre-existing posts in the free board without rewriting them.
    @Column(name = "community_id")
    private Long communityId;

    /*
     * 기능 : 기존 글의 자유게시판 ID 변환
     * 설명 : DB의 null 소속을 응답에서 자유게시판 ID 0으로 보여줘 기존 데이터를 유지합니다.
     */
    public Long getCommunityId() { return communityId == null ? 0L : communityId; }

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

    /*
     * 기능 : 커뮤니티 소속 게시글 생성
     * 설명 : 새 글에 소속을 지정하되 자유게시판 ID 0은 기존 저장 방식과 동일하게 null로 보관합니다.
     */
    public Post(Long userId, String title, String imageKey, LocalDateTime date, String content, Long communityId) {
        this(userId, title, imageKey, date, content);
        this.communityId = communityId == 0 ? null : communityId;
    }

    //260627 - 기존 수정 API가 이미지 URL로 imageKey를 덮어쓰지 않도록 이미지 변경 제거
    /*
     * 기능 : 게시글 본문 수정
     * 설명 : 제목·내용·수정 시각만 바꾸고 소속 커뮤니티와 사진 키는 유지합니다.
     */
    public void updatePost(String postTitle, String postContent, LocalDateTime postDate) {
        this.postTitle = postTitle;
        this.postContent = postContent;
        this.postDate = postDate;
    }

    /*
     * 기능 : 첨부 사진 참조 교체 또는 제거
     * 설명 : 새 저장 키로 사진을 교체하거나 null로 첨부를 해제합니다. 실제 파일 삭제는 수행하지 않습니다.
     */
    public void updateImageKey(String imageKey) { this.postImageKey = imageKey; }
}
