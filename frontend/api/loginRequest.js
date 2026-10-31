import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 로그인 API 호출
 * 설명 : 이메일과 비밀번호를 전송하고 서버가 설정하는 인증 쿠키를 브라우저에서 받습니다.
 */
export const userLogin = async (email, password) => {
    const result = await requestJson(`${getServerUrl()}/v1/auth/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
            email: email,
            password: password,
        }),
    });
    return result;
};
