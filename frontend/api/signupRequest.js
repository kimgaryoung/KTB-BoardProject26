import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 회원가입 API 호출
 * 설명 : 이메일·비밀번호·닉네임 등의 가입 정보를 JSON으로 전송합니다.
 */
export const userSignup = async data => {
    const result = await requestJson(`${getServerUrl()}/v1/auth/signup`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    });
    return result;
};

/*
 * 기능 : 이메일 중복 확인 요청
 * 설명 : 서버 응답 상태로 이메일 사용 가능 여부를 확인합니다.
 */
export const checkEmail = async email => {
    const result = await requestJson(
        `${getServerUrl()}/v1/users/email/check?email=${email}`,
        {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        },
    );
    return result;
};

/*
 * 기능 : 닉네임 중복 확인 요청
 * 설명 : 서버 응답 상태로 닉네임 사용 가능 여부를 확인합니다.
 */
export const checkNickname = async nickname => {
    const result = await requestJson(
        `${getServerUrl()}/v1/users/nickname/check?nickname=${nickname}`,
        {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        },
    );
    return result;
};

/*
 * 기능 : 프로필 사진 업로드 요청
 * 설명 : 프로필 사진을 담은 FormData를 업로드 API에 전달합니다.
 */
export const fileUpload = async file => {
    const result = await requestJson(
        `${getServerUrl()}/v1/users/upload/profile-image`,
        {
            method: 'POST',
            body: file,
            credentials: 'include',
        },
    );
    return result;
};
