import { getServerUrl } from '../../utils/function.js';

/*
 * 기능 : 계정 메뉴와 로그아웃 연결
 * 설명 : 회원정보·비밀번호 수정 링크를 만들고 로그아웃 요청 후 로그인 화면으로 이동합니다.
 */
const headerDropdownMenu = () => {
    const wrap = document.createElement('div');

    const modifyInfoLink = document.createElement('a');
    const modifyPasswordLink = document.createElement('a');
    const logoutLink = document.createElement('a');

    modifyInfoLink.textContent = '회원정보수정';
    modifyPasswordLink.textContent = '비밀번호수정';
    logoutLink.textContent = '로그아웃';

    modifyInfoLink.href = '/html/modifyInfo.html';
    modifyPasswordLink.href = '/html/modifyPassword.html';
    logoutLink.addEventListener('click', async () => {
        try {
            await fetch(`${getServerUrl()}/v1/auth/logout`, {
                method: 'POST',
                credentials: 'include',
            });
        } finally {
            location.href = '/html/login.html';
        }
    });

    wrap.classList.add('drop');

    wrap.appendChild(modifyInfoLink);
    wrap.appendChild(modifyPasswordLink);
    wrap.appendChild(logoutLink);

    return wrap;
};

// title : 헤더 타이틀
// leftBtn: 헤더 좌측 기능. 0 : None , 1: back , 2 : index
// rightBtn : 헤더 우측 기능. image 주소값 들어옴
/*
 * 기능 : 공통 헤더 생성
 * 설명 : 제목과 뒤로 가기·홈 이동 버튼을 구성합니다. 프로필 사진이 있으면 계정 메뉴를 추가합니다.
 */
const Header = (
    title,
    leftBtn = 0,
    profileImage = null,
) => {
    let leftBtnElement;
    let rightBtnElement;
    let headerElement;
    let h1Element;

    if (leftBtn == 1 || leftBtn == 2) {
        leftBtnElement = document.createElement('img');
        leftBtnElement.classList.add('back');
        leftBtnElement.src = '/public/navigate_before.svg';
        if (leftBtn == 1) {
            leftBtnElement.addEventListener('click', () => history.back());
        } else {
            leftBtnElement.addEventListener(
                'click',
                () => (location.href = '/'),
            );
        }
    }

    if (profileImage) {
        rightBtnElement = document.createElement('div');
        rightBtnElement.classList.add('profile');

        const profileElement = document.createElement('img');
        profileElement.classList.add('profile');
        profileElement.alt = '내 프로필';
        profileElement.loading = 'eager';
        profileElement.src = profileImage;
        profileElement.addEventListener('error', () => {
            const fallback = '/public/image/profile/default.jpg';
            if (!profileElement.src.endsWith(fallback)) profileElement.src = fallback;
        });

        const Drop = headerDropdownMenu();
        Drop.classList.add('none');

        profileElement.addEventListener('click', event => {
            Drop.classList.toggle('none');
            event.stopPropagation();
        });

        rightBtnElement.appendChild(profileElement);
        rightBtnElement.appendChild(Drop);
    }

    h1Element = document.createElement('h1');
    h1Element.textContent = title;

    headerElement = document.createElement('header');

    if (leftBtnElement) headerElement.appendChild(leftBtnElement);
    headerElement.appendChild(h1Element);
    if (rightBtnElement) headerElement.appendChild(rightBtnElement);

    return headerElement;
};

window.addEventListener('click', e => {
    const dropMenu = document.querySelector('.drop');
    if (dropMenu && !dropMenu.classList.contains('none')) {
        dropMenu.classList.add('none');
    }
});

export default Header;
