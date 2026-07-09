package com.example.boardproject.dto;

import com.example.boardproject.entity.User;
import com.example.boardproject.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthCheckResponseDto {
    private Long userId;
    private Long idx;
    private String nickname;
    private String profileImageUrl;
    private String email;

    public static AuthCheckResponseDto of(Long userId, UserProfile userProfile, User user) {
        return new AuthCheckResponseDto(
                userId,
                userId,
                userProfile.getNickname(),
                userProfile.getProfileImage(),
                user.getEmail()
        );
    }
}
