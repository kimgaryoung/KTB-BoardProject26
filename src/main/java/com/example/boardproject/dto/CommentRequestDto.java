package com.example.boardproject.dto;

import lombok.*;


//댓글 추가 시 내용을 요청으로 보냄.기
@Getter // service에서 접근하기 위해 필요함.
public class CommentRequestDto {


    private String commentContent;

}






