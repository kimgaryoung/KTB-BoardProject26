package com.example.boardproject.service;

import com.example.boardproject.auth.JwtProvider;
import com.example.boardproject.dto.UserProfileRequestDto;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.repository.UserProfileRepository;
import com.example.boardproject.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class UserServiceProfileTest {
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final UserService users = new UserService(profiles, mock(UserRepository.class), mock(JwtProvider.class));

    @Test
    void replacesTheRequestedUsersPhotoWithoutChangingTheirNickname() throws Exception {
        UserProfile profile = new UserProfile(7L, "마크주민", "/v1/images/posts/old.png");
        when(profiles.findByUserId(7L)).thenReturn(profile);

        users.updateProfile(7L, request("마크주민", "/v1/images/posts/new.png"));

        assertThat(profile.getProfileImage()).isEqualTo("/v1/images/posts/new.png");
        assertThat(profile.getNickname()).isEqualTo("마크주민");
    }

    @Test
    void removingAPhotoClearsOnlyTheRequestedUsersImage() throws Exception {
        UserProfile selected = new UserProfile(7L, "마크주민", "/v1/images/posts/old.png");
        UserProfile other = new UserProfile(8L, "다른주민", "/v1/images/posts/other.png");
        when(profiles.findByUserId(7L)).thenReturn(selected);
        when(profiles.findByUserId(8L)).thenReturn(other);

        users.updateProfile(7L, request("마크주민", null));

        assertThat(selected.getProfileImage()).isNull();
        assertThat(selected.getNickname()).isEqualTo("마크주민");
        assertThat(other.getProfileImage()).isEqualTo("/v1/images/posts/other.png");
        assertThat(other.getNickname()).isEqualTo("다른주민");
    }

    @Test
    void nicknameEditKeepsTheSubmittedExistingPhoto() throws Exception {
        UserProfile profile = new UserProfile(7L, "이전이름", "/v1/images/posts/existing.png");
        when(profiles.findByUserId(7L)).thenReturn(profile);

        users.updateProfile(7L, request("새로운이름", "/v1/images/posts/existing.png"));

        assertThat(profile.getNickname()).isEqualTo("새로운이름");
        assertThat(profile.getProfileImage()).isEqualTo("/v1/images/posts/existing.png");
    }

    private static UserProfileRequestDto request(String nickname, String image) {
        UserProfileRequestDto request = new UserProfileRequestDto();
        request.setNickname(nickname);
        request.setProfileImageUrl(image);
        return request;
    }
}
