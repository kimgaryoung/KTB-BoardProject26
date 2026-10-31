import { mountPublicHeader } from '../utils/pageHeader.js';
import { loginUrl } from '../utils/authNavigation.js';

/*
 * 기능 : 마인크래프트 홈 화면 초기화
 * 설명 : 사진 중심의 홈에서는 게시판으로 이동하고, 글쓰기는 로그인 상태에 맞춰 작성 화면 또는 로그인 복귀 경로로 연결합니다.
 */
const initHome = async () => {
    const viewer = await mountPublicHeader({ showHomeButton: false });
    const writeLink = document.querySelector('#homeWriteLink');
    const editor = '/html/board-write.html';
    writeLink.href = viewer ? editor : loginUrl(editor);
    writeLink.textContent = viewer ? '나의 이야기 쓰기' : '로그인하고 글쓰기';
};
initHome();
