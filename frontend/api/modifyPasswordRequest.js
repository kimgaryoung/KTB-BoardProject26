import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 내 비밀번호 변경 요청
 * 설명 : 새 비밀번호를 JSON으로 전달하며 인증 쿠키로 변경할 계정을 식별합니다.
 */
export const changePassword = async password => {
    const result = requestJson(`${getServerUrl()}/v1/users/me/password`, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
            password,
        }),
    });
    return result;
};
