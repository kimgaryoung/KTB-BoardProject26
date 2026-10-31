package com.example.boardproject.service;

import com.example.boardproject.dto.*;
import com.example.boardproject.entity.Community;
import com.example.boardproject.entity.HiddenCommunityId;
import com.example.boardproject.exception.BusinessException;
import com.example.boardproject.exception.CommunityAlreadyExistsException;
import com.example.boardproject.repository.CommunityRepository;
import com.example.boardproject.repository.HiddenCommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private static final CommunityResponseDto FREE_BOARD = new CommunityResponseDto(0L, "자유게시판", "작품에 상관없이 애니 이야기를 나누는 공간입니다. 기존 게시글도 여기에서 볼 수 있어요.");
    private final CommunityRepository communityRepository;
    private final HiddenCommunityRepository hiddenCommunities;

    /*
     * 기능 : 마인크래프트 전용 게시판 준비
     * 설명 : 기존 마크 게시판을 재사용하고 새 설치에서는 시스템 게시판을 한 번 생성합니다. 독립 트랜잭션으로 동시 초기화 충돌을 격리합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommunityResponseDto getOrCreateMinecraftBoard() {
        return communityRepository.findByNameKey(nameKey("마인크래프트"))
                .map(CommunityResponseDto::from)
                .orElseGet(() -> CommunityResponseDto.from(communityRepository.saveAndFlush(new Community(
                        "마인크래프트", nameKey("마인크래프트"), "건축, 생존, 레드스톤, 모드와 서버 이야기를 함께 나누는 마인크래프트 게시판입니다.", 0L))));
    }

    @Transactional(readOnly = true)
    /*
     * 기능 : 개인 숨김을 반영한 커뮤니티 검색
     * 설명 : 이름 검색과 숨김 필터를 적용한 뒤 페이지를 반환합니다. DB에 없는 자유게시판 ID 0도 목록과 페이지 계산에 포함합니다.
     */
    public List<CommunityResponseDto> search(String keyword, int offset, int limit, Long userId, boolean hiddenOnly) {
        if (hiddenOnly && userId == null) throw new BusinessException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        if (offset < 0 || limit < 1 || limit > 50) throw badRequest("INVALID_PAGINATION");
        String key = nameKey(keyword);
        if (key.length() > 100) throw badRequest("INVALID_COMMUNITY_NAME");
        boolean freeHidden = userId != null && hiddenCommunities.existsById(new HiddenCommunityId(userId, 0L));
        boolean includeFree = nameKey(FREE_BOARD.name()).contains(key) && freeHidden == hiddenOnly;
        List<CommunityResponseDto> result = new ArrayList<>();
        if (includeFree && offset == 0) result.add(FREE_BOARD);
        int remaining = limit - result.size();
        if (remaining > 0) {
            int dbOffset = includeFree ? Math.max(0, offset - 1) : offset;
            communityRepository.search(key, dbOffset, remaining, userId, hiddenOnly).stream().map(CommunityResponseDto::from).forEach(result::add);
        }
        return result;
    }

    @Transactional
    /*
     * 기능 : 계정별 커뮤니티 숨김 설정
     * 설명 : 대상 커뮤니티가 존재하는지 확인한 뒤 사용자와 커뮤니티의 조합으로 숨김을 저장하거나 해제합니다.
     */
    public CommunityVisibilityResponseDto setHidden(Long communityId, Long userId, boolean hidden) {
        getCommunity(communityId);
        if (hidden) hiddenCommunities.hide(userId, communityId);
        else hiddenCommunities.show(userId, communityId);
        return new CommunityVisibilityResponseDto(communityId, hidden);
    }

    @Transactional(readOnly = true)
    /*
     * 기능 : 커뮤니티 단건 조회
     * 설명 : ID 0은 기존 글을 위한 가상 자유게시판으로 처리하고, 그 외에는 DB에서 조회해 없으면 404를 반환합니다.
     */
    public CommunityResponseDto getCommunity(Long id) {
        if (id == null || id < 0) throw badRequest("INVALID_COMMUNITY_ID");
        if (id == 0) return FREE_BOARD;
        return communityRepository.findById(id).map(CommunityResponseDto::from)
                .orElseThrow(() -> new BusinessException("COMMUNITY_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    /*
     * 기능 : 커뮤니티 이름 검증과 생성
     * 설명 : 이름·소개 길이와 제어 문자를 검사합니다. 정규화한 이름 키로 중복을 확인하고 DB 유니크 제약으로 동시 생성 충돌도 막습니다.
     */
    public CommunityResponseDto create(CommunityRequestDto request, Long creatorId) {
        String name = normalize(request.getName());
        String description = request.getDescription() == null ? "" : request.getDescription().strip();
        if (name.isBlank() || name.length() > 50 || name.codePoints().anyMatch(c -> Character.isISOControl(c) || Character.getType(c) == Character.FORMAT)) {
            throw badRequest("INVALID_COMMUNITY_NAME");
        }
        if (description.length() > 300) throw badRequest("INVALID_COMMUNITY_DESCRIPTION");
        findByName(name).ifPresent(existing -> { throw new CommunityAlreadyExistsException(existing.id()); });
        return CommunityResponseDto.from(communityRepository.saveAndFlush(new Community(name, nameKey(name), description, creatorId)));
    }

    @Transactional(readOnly = true)
    /*
     * 기능 : 정규화한 이름으로 기존 커뮤니티 찾기
     * 설명 : 공백·영문 대소문자를 무시한 키로 자유게시판과 저장된 커뮤니티의 중복 여부를 확인합니다.
     */
    public Optional<CommunityResponseDto> findByName(String name) {
        String key = nameKey(name);
        if (key.equals(nameKey(FREE_BOARD.name()))) return Optional.of(FREE_BOARD);
        return communityRepository.findByNameKey(key).map(CommunityResponseDto::from);
    }

    /*
     * 기능 : 커뮤니티 표시 이름 정규화
     * 설명 : Unicode NFKC 정규화 후 연속 공백을 하나로 줄이고 앞뒤 공백을 제거합니다.
     */
    private static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC).replaceAll("(?U)\\s+", " ").strip();
    }

    /*
     * 기능 : 커뮤니티 이름 비교 키 생성
     * 설명 : 정규화한 이름에서 공백을 제거하고 소문자로 바꿔 검색과 중복 판정에 같은 기준을 사용합니다.
     */
    private static String nameKey(String value) { return normalize(value).replace(" ", "").toLowerCase(Locale.ROOT); }
    private static BusinessException badRequest(String code) { return new BusinessException(code, HttpStatus.BAD_REQUEST); }
}
