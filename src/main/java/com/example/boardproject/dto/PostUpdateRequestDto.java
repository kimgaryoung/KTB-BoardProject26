package com.example.boardproject.dto;



import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.w3c.dom.Text;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor // Jackson이 JSON → 객체 변환할 때 기본 생성자 필요
public class PostUpdateRequestDto {

    private Long userId;
    private Long postId;
    private String postTitle;
    private String postContent;
    private String postImage;
    private LocalDateTime postDate;
}