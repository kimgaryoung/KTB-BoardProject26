package com.example.boardproject.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@ConditionalOnProperty(name = "app.site.community-management-enabled", havingValue = "false", matchIfMissing = true)
public class RemovedCommunityController {
    /*
     * 기능 : 제거된 커뮤니티 관리 API 차단
     * 설명 : 예전 검색·생성·숨김 주소로 요청해도 데이터 변경 없이 기능 종료 상태를 명시적으로 반환합니다.
     */
    @RequestMapping(value = {"/communities", "/communities/{id}", "/communities/{id}/hidden"})
    public ResponseEntity<Map<String, String>> removed() {
        return ResponseEntity.status(410).body(Map.of("code", "COMMUNITY_MANAGEMENT_REMOVED"));
    }
}
