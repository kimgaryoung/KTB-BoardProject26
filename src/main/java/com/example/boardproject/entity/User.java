package com.example.boardproject.entity;


import jakarta.persistence.*;
import lombok.Getter;

import lombok.NoArgsConstructor;// 빈 생성자
import lombok.RequiredArgsConstructor; // service,controller에서 DI사용
import java.time.LocalDateTime;

@Entity // Db테이블과 연결
@Getter  //serice에서 DB데이터 읽을떄
@NoArgsConstructor//기본생성자 자동생성
@Table(name="USER")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    //제약조건: notnull,글자수 20자이냉
    @Column(nullable = false, length = 20)
    private String password;

    @Column(nullable = false, length = 255,unique = true)
    private String email;


    @Column(length = 255)
    private String refreshToken;


    //제약조건 : 업데이트 될떄 현재 시간반영
    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime loginDate;

    private LocalDateTime signinDate;

    // ERD 설계에 없던 refreshTokenExpire값 추가
    private LocalDateTime refreshTokenExpireAt;

    //생성자: 회원가입할떄 필수-(닉네임, 프로필 사진은 user_profile에 있음)
    public User(String email, String password) {
        this.email = email;
        this.password = password;

    }

    /*
    // 회원가입 시 호출 - 자동생성된 userId
    public void assignUserId(Long user_id) {
        this.user_id = user_id;
    }
    */

    //로그인시 -로그인 날짜 업데이트됨.
    public void updateLoginDate(LocalDateTime loginDate){
        this.loginDate=loginDate;
    }


    //로그인시 - refresh토큰  만료되면 계속  재발급되면서 업데이트됨.
    public  void updatetoken(String refreshToken, LocalDateTime refreshTokenExpireAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpireAt=refreshTokenExpireAt;
    }

    //토큰의 만료 여부 판단 -비지니스 메서드
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.refreshTokenExpireAt);
    }

    //프로필 수정시 -비밀번호 변경
    public void updatePassword (String password ){
        this.password=password;
    }

    //프로필 수정시 -이메일 변경
    public void updateEmail(String email ){
        this.email=email;
    }

    // 로그아웃 시 -refreshToken 삭제
    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpireAt = null;
    }

}
