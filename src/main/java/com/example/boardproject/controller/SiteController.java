package com.example.boardproject.controller;

import com.example.boardproject.auth.Result;
import com.example.boardproject.dto.CommunityResponseDto;
import com.example.boardproject.service.SiteBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SiteController {
    private final SiteBoardService siteBoard;

    /*
     * 기능 : 마인크래프트 사이트 게시판 공개 조회
     * 설명 : 로그인이나 커뮤니티 선택 없이 사용할 유일한 게시판의 ID와 소개를 반환합니다.
     */
    @GetMapping("/site/board")
    public Result<CommunityResponseDto> getBoard() { return Result.of(siteBoard.getBoard()); }
}
