package com.example.boardproject.entity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;



@AllArgsConstructor // 기본 생성자 필수로 있어야 함.
@EqualsAndHashCode //JPA에서 복합키 판단하는
public class ViewerId implements Serializable { // 무조건 public class 이어야함.
    private Long postId;
    private Long userId;
}
