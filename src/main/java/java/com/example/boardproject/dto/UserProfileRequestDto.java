package java.com.example.boardproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileRequestDto {
    private String password;
    private String nickname;
    private String profileImageUrl;
}
