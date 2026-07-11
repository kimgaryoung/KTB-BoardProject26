package java.com.example.boardproject.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity // DB 테이블과 매핑
@Getter  //serice에서 DB데이터 읽을떄
@NoArgsConstructor//기본생성자 자동생성
@Table(name = "USER_PROFILE")
public class UserProfile {

    // USER 테이블의 user_id와 같은 값 (PK,FK)
    @Id
    @Column(name = "user_id")
    private Long userId;

    //제약조건  NOT NULL, 최대 10자
    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(length = 255)
    private String profileImage;


    //생성자: 회원가입할떄 필수-(emil,pw는 USER에 있음), userId 자동생성 안됨.
    public UserProfile(Long userId, String nickname, String profileImage) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }


    //프로필 변경
    public void updateFields( String nickname,String profileImage) {
        updateNickname(nickname);
        updateProfileImage(profileImage);

    }

    //프로필 수정 - 낙내임 변경.
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 프로필 수정시 -프로필 사진 .
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}