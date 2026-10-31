package com.example.boardproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.images.storage", havingValue = "s3", matchIfMissing = true)
public class S3ImageService implements PostImageStorage {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.base-url}")
    private String baseUrl;

    //260627 - 이미지 검증 후 S3에 업로드하고 imageKey를 반환하는 기능 추가
    /*
     * 기능 : S3 사진 업로드
     * 설명 : 이미지 검증 후 UUID 키와 실제 형식의 Content-Type으로 업로드하고 저장 키를 반환합니다.
     */
    public String upload(MultipartFile image) {
        var validated = ImageUploadValidator.validate(image);

        String imageKey = "posts/" + UUID.randomUUID() + "." + validated.extension();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(validated.contentType())
                .contentLength((long) validated.bytes().length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(validated.bytes()));
        return imageKey;
    }

    //260627 - 저장된 imageKey를 프론트용 공개 URL로 변환하는 기능 추가
    /*
     * 기능 : S3 사진 공개 주소 생성
     * 설명 : 저장된 이미지 키 앞에 공개 기본 주소를 붙입니다. 키가 없으면 null을 반환합니다.
     */
    public String getImageUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }
        return baseUrl.replaceAll("/+$", "") + "/" + imageKey.replaceFirst("^/+", "");
    }

}
