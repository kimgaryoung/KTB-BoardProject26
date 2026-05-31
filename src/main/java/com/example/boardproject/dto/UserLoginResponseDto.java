package com.example.boardproject.dto;

import com.example.boardproject.entity.User;
import com.example.boardproject.entity.UserProfile;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class UserLoginResponseDto {
    //private  User user;//직접 entity를 명시X

    private Long userId;

    private TokenInfo token;

    public static UserLoginResponseDto of(
            User user,
            String accessToken,
            long expiresIn
    ) {
        return new UserLoginResponseDto(
                user.getUserId(),

                /*액세스 토큰 무거워져서 뻄.
                userProfile.updateNickname(),      // 2. 닉네임 꺼내오기
                userProfile.updateProfileImage(),  // 3. 이미지 꺼내오기
                */


                new TokenInfo(accessToken, expiresIn)
        );
    }



}
