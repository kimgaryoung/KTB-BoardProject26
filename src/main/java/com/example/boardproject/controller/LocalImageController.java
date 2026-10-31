package com.example.boardproject.controller;

import com.example.boardproject.service.LocalImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.images.storage", havingValue = "local")
public class LocalImageController {
    private final LocalImageStorage images;

    @GetMapping("/images/posts/{filename}")
    /*
     * 기능 : 로컬 첨부 사진 공개 조회 API
     * 설명 : 저장소에서 검증된 파일을 읽어 MIME·캐시·nosniff 헤더와 함께 반환합니다. 로컬 저장 모드에서만 활성화됩니다.
     */
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource file = images.load(filename);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(file);
    }
}
