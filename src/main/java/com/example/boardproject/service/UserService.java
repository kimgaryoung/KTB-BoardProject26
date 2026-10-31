package com.example.boardproject.service;

import com.example.boardproject.auth.JwtProvider;
import com.example.boardproject.dto.*;
import com.example.boardproject.entity.User;


import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.exception.AuthorizedException;
import com.example.boardproject.repository.UserProfileRepository;
import com.example.boardproject.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;



@Service
@Validated
@RequiredArgsConstructor
public class UserService {


    private final UserProfileRepository userProfileRepository; //

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // 회원가입
    @Transactional
    /*
     * 기능 : 계정과 프로필 생성
     * 설명 : 이메일 중복을 확인하고 계정과 닉네임·프로필 사진 정보를 함께 저장합니다.
     */
    public void signup(UserSignRequestDto signupRequest) {

        // 1. User 저장

        // 중복처리
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }

        User user = new User(signupRequest.getEmail(), signupRequest.getPassword());
        user.updateSigninDate(LocalDateTime.now());
        userRepository.save(user);

        // 2. UserProfile 저장 - 닉네임, 프로필 사진
        UserProfile userProfile = new UserProfile(
                user.getUserId(),
                signupRequest.getNickname(),
                signupRequest.getProfileImageUrl()
        );
        userProfileRepository.save(userProfile);
    }


    // 로그인
    @Transactional
    /*
     * 기능 : 계정 확인과 로그인 토큰 발급
     * 설명 : 이메일·비밀번호를 확인한 뒤 accessToken과 refreshToken을 발급하고 갱신 토큰 및 로그인 시각을 저장합니다.
     */
    public UserLoginResult login(UserLoginRequestDto loginRequest) {
        // 1. 이메일로 유저 찾기
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AuthorizedException("INVALID_CREDENTIALS"));

        // 2. 비밀번호 검증
        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new AuthorizedException("INVALID_CREDENTIALS");
        }

        /*
        // 3. 닉네임, 프로필 사진 가져오기
        UserProfile userProfile = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new AuthorizedException("INVALID_CREDENTIALS"));

         */

        // 4. accessToken 발급
        String accessToken = jwtProvider.createAccessToken(user.getUserId());

        // 5. refreshToken 발급 - User 테이블에 저장
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());
        user.updatetoken(refreshToken,LocalDateTime.now().plusDays(1)); // 만료시간 24시간

        //로그인 시간 추가
        user.updateLoginDate(LocalDateTime.now());
        userRepository.save(user);

        // 6. 응답 반환
        return new UserLoginResult(
                UserLoginResponseDto.of(user, accessToken, jwtProvider.getAccessTokenValidityInMilliseconds()),
                refreshToken
        );
    }

    // 액세스 토큰 재발급
    @Transactional
    /*
     * 기능 : 로그인 토큰 재발급
     * 설명 : 저장된 refreshToken과 만료 여부를 확인하고 accessToken 및 새 refreshToken을 발급해 갱신 토큰을 교체합니다.
     */
    public TokenResult refreshAccessToken(String refreshToken) {
        // 1. refreshToken으로 유저 찾기
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AuthorizedException("UNAUTHORIZED"));

        // 2. 만료 여부 확인
        if (user. isExpired()) {
            throw new AuthorizedException("UNAUTHORIZED");
        }

        /*
        // 3. 닉네임, 프로필 사진 가져오기
        UserProfile userProfile = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new AuthorizedException("UNAUTHORIZED"));

         */

        // 4. 새 accessToken 발급
        String newAccessToken = jwtProvider.createAccessToken(
                user.getUserId()
                /*
                userProfile.getNickname(),
                userProfile.getProfileImage()
                */

        );

        // 5. RTR - 새 refreshToken 발급 후 User 테이블 업데이트
        String newRefreshToken = jwtProvider.createRefreshToken(user.getUserId());
        user.updatetoken(newRefreshToken, LocalDateTime.now().plusDays(1));//plusDays
        userRepository.save(user);

        // 6. 새 토큰 반환
        return new TokenResult(
                new TokenInfo(newAccessToken, jwtProvider.getAccessTokenValidityInMilliseconds()),
                newRefreshToken
        );
    }


    //유저 정보 수정
    @Transactional
    /*
     * 기능 : 회원 프로필 수정
     * 설명 : 사용자 ID에 연결된 닉네임과 프로필 사진 주소를 갱신합니다.
     */
    public String updateProfile(final Long userId, final UserProfileRequestDto dto) throws Exception {
        UserProfile userProfile = findByUserId(userId);

        userProfile.updateFields(
                dto.getNickname(),
                dto.getProfileImageUrl()
        );

        return "정상 반환되었습니다.";
    }

    //유저 아이디-프로필 변경할때 사용.
    private UserProfile findByUserId(final Long userId) throws Exception {
        return userProfileRepository.findByUserId(userId);

    }

    //유저 비밀번호 변경
    @Transactional
    /*
     * 기능 : 회원 비밀번호 수정
     * 설명 : 사용자 계정의 비밀번호 값을 변경합니다.
     */
    public String updatePassword(final Long userId, final UserProfileRequestDto dto) throws Exception {
        User user = findByUserid(userId);

        user.updateFields(
                dto.getPassword()

        );
        return "정상 반환.";
    }

    //유저 아이디-비밀번호 수정시 사용, 탈퇴시에도 사용.
    private User findByUserid(final Long userId) throws Exception {
        return userRepository.findByUserId(userId);

    }


    //회원 탈퇴.
    @Transactional
    /*
     * 기능 : 회원 계정 삭제
     * 설명 : 사용자 ID로 계정을 조회해 삭제합니다.
     */
    public String deleteUser(final Long userId) throws Exception {
        User user = findByUserid(userId);
        userRepository.delete(user);

        return "회원 탈퇴";
    }

    // 이메일 사용 가능 여부 (true = 사용 가능)
    /*
     * 기능 : 이메일 사용 가능 여부 확인
     * 설명 : 같은 이메일의 계정이 없으면 true를 반환합니다.
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // 닉네임 사용 가능 여부 (true = 사용 가능)
    /*
     * 기능 : 닉네임 사용 가능 여부 확인
     * 설명 : 같은 닉네임의 프로필이 없으면 true를 반환합니다.
     */
    public boolean isNicknameAvailable(String nickname) {
        return !userProfileRepository.existsByNickname(nickname);
    }

    // 현재 로그인 유저 정보 조회
    /*
     * 기능 : 현재 로그인 사용자 정보 구성
     * 설명 : 계정과 프로필 정보를 결합해 화면의 인증 확인과 헤더 표시에 필요한 값을 반환합니다.
     */
    public AuthCheckResponseDto getAuthCheck(Long userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId);
        User user = userRepository.findByUserId(userId);
        return AuthCheckResponseDto.of(userId, userProfile, user);
    }


}