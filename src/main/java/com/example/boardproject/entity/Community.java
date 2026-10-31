package com.example.boardproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "COMMUNITY")
/*
 * 기능 : 커뮤니티 데이터 저장
 * 설명 : 이름·중복 비교용 유니크 키·소개·생성자·생성 시각을 보관합니다.
 */
public class Community {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String name;
    @Column(name = "name_key", nullable = false, length = 100, unique = true)
    private String nameKey;
    @Column(nullable = false, length = 300)
    private String description;
    @Column(nullable = false)
    private Long creatorId;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Community(String name, String nameKey, String description, Long creatorId) {
        this.name = name;
        this.nameKey = nameKey;
        this.description = description;
        this.creatorId = creatorId;
        this.createdAt = LocalDateTime.now();
    }
}
