import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 내 회원정보 수정 요청
 * 설명 : 닉네임과 프로필 사진 주소의 변경 사항을 인증 쿠키와 함께 전송합니다.
 */
export const userModify = async changeData => {
    const result = await requestJson(`${getServerUrl()}/v1/users/me`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(changeData),
    });
    return result;
};

/*
 * 기능 : 회원 탈퇴 요청
 * 설명 : 현재 인증된 계정의 삭제를 요청합니다.
 */
export const userDelete = async () => {
    const result = await requestJson(`${getServerUrl()}/v1/users/me`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
    });
    return result;
};
