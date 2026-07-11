package java.com.example.boardproject.dto;

import java.com.example.boardproject.entity.User;
import java.com.example.boardproject.entity.UserProfile;

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




                new TokenInfo(accessToken, expiresIn)
        );
    }



}
