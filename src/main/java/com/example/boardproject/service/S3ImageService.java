package com.example.boardproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ImageService {

    //260627 - 업로드 이미지 크기 및 확장자 제한 추가
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.base-url}")
    private String baseUrl;

    //260627 - 이미지 검증 후 S3에 업로드하고 imageKey를 반환하는 기능 추가
    public String upload(MultipartFile image) {
        validate(image);

        String imageKey = "posts/" + UUID.randomUUID() + extensionOf(image.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(image.getContentType())
                .contentLength(image.getSize())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));
            return imageKey;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.", e);
        }
    }

    //260627 - 저장된 imageKey를 프론트용 공개 URL로 변환하는 기능 추가
    public String getImageUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }
        return baseUrl.replaceAll("/+$", "") + "/" + imageKey.replaceFirst("^/+", "");
    }

    private void validate(MultipartFile image) {
        if (image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일이 비어 있습니다.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 5MB 이하만 업로드할 수 있습니다.");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String extensionOf(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            return "";
        }
        return "." + extension.toLowerCase(Locale.ROOT);
    }
}
