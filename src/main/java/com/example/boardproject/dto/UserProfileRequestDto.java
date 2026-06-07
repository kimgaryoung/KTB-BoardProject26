package com.example.boardproject.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // Jackson이 JSON → 객체 변환할 때 기본 생성자 필요
public class UserProfileRequestDto {



    //2.  비밀번호 형식 검증
    @NotBlank(message = "REQUIRED")        // 공백 불가
    @Size(min = 8, message = "TOO_SHORT")  // 8자 미만 불가
    @Size(max = 20, message = "TOO_LONG")  // 20자 초과 불가
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "INVALID_FORMAT"     // 영문 + 숫자 + 특수문자 최소 1개씩 포함
    )
    private String password;
    private String nickName;
    private String profileImage;

}