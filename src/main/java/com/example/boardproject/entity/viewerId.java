package com.example.boardproject.entity;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;



@NoArgsConstructor
@EqualsAndHashCode //JPA에서 복합키 판단
public class viewerId implements Serializable {
    private Long postId;
    private Long userId;
}
