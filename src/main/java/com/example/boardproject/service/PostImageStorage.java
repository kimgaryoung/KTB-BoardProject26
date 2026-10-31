package com.example.boardproject.service;

import org.springframework.web.multipart.MultipartFile;

/*
 * 기능 : 게시글 사진 저장소 공통 계약
 * 설명 : S3와 로컬 구현이 업로드 후 키 반환 및 키의 조회 URL 변환을 같은 방식으로 제공하도록 정의합니다.
 */
public interface PostImageStorage {
    String upload(MultipartFile image);
    String getImageUrl(String imageKey);
}
