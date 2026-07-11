package com.example.boardproject.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * [예시 응답 JSON - data가 List일 경우]
 * {
 *     "msg": "성공",
 *     "data": [
 *          {
 *              "id": 1,
 *              "name": "kim"
 *          },
 *          {
 *              "id": 1,
 *              "name": "kim"
 *          }
 *     ]
 * }
 *
 * [예시 응답 JSON - data가 단순 객체일 경우]
 * {
 *     "msg": "성공",
 *     "data": {
 *          "id": 1,
 *          "name": "kim"
 *     }
 * }
 */
@Getter
@Setter
@AllArgsConstructor
public class Result<T> {

    private String message;
    private T data;

    //-- create RsData --//
    public static <T> Result<T> of(String msg, T data) {
        return new Result<>(msg, data);
    }

    public static <T> Result<T> of(String msg) {
        return of(msg, null);
    }

    public static <T> Result<T> of(T data) {
        return of("성공", data);
    }

    public static <T> Result<T> failOf(T data) {
        return of("실패", data);
    }
}