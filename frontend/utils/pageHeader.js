import Header from '../component/header/header.js';
import { prependChild, resolveImageUrl, serverSessionCheck } from './function.js';

/*
 * 기능 : 공개 화면의 로그인 상태별 헤더 표시
 * 설명 : 세션에 따라 프로필·로그인 링크를 표시합니다. 첫 화면에서는 홈 이동 버튼을 생략하고 상세 화면에서는 유지합니다.
 */
export const mountPublicHeader = async ({ showHomeButton = true } = {}) => {
    let profileImage = null;
    let viewer = null;
    try {
        const response = await serverSessionCheck();
        if (response.ok) {
            const { data } = await response.json();
            viewer = data;
            profileImage = resolveImageUrl(data.profileImageUrl, '/public/image/profile/default.jpg');
        }
    } catch { /* Discovery remains usable if the session check fails. */ }
    const header = Header('마크안에 사람이 살잖아', showHomeButton ? 2 : 0, profileImage);
    if (!profileImage) {
        const login = document.createElement('a');
        login.href = '/html/login.html';
        login.className = 'accountLink';
        login.textContent = '로그인';
        header.appendChild(login);
    }
    prependChild(document.body, header);
    return viewer;
};
