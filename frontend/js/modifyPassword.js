import { changePassword } from '../api/modifyPasswordRequest.js';
import Dialog from '../component/dialog/dialog.js';
import Header from '../component/header/header.js';
import {
    authCheck,
    getServerUrl,
    prependChild,
    resolveImageUrl,
    validPassword,
} from '../utils/function.js';

const button = document.querySelector('#signupBtn');

const DEFAULT_PROFILE_IMAGE = '../public/image/profile/default.jpg';
const HTTP_OK = 200;

const dataResponse = await authCheck();
const data = await dataResponse.json();
const profileImage = resolveImageUrl(
    data.data.profileImageUrl,
    DEFAULT_PROFILE_IMAGE,
);

const modifyData = {
    password: '',
    passwordCheck: '',
};

/*
 * 기능 : 비밀번호 변경 버튼 상태 관리
 * 설명 : 새 비밀번호와 확인값이 모두 존재하고 일치하는지 확인합니다.
 */
const observeData = () => {
    const { password, passwordCheck } = modifyData;

    // id, pw, pwck, nickname, profile 값이 모두 존재하는지 확인
    if (!password || !passwordCheck || password !== passwordCheck) {
        button.disabled = true;
        button.style.backgroundColor = '#ACA0EB';
    } else {
        button.disabled = false;
        button.style.backgroundColor = '#7F6AEE';
    }
};

/*
 * 기능 : 새 비밀번호 규칙과 확인값 검사
 * 설명 : 비밀번호 형식 및 재입력 일치 여부를 검사해 안내와 편집 데이터를 갱신합니다.
 */
const blurEventHandler = async (event, uid) => {
    if (uid == 'pw') {
        const value = event.target.value;
        const isValidPassword = validPassword(value);
        const helperElement = document.querySelector(
            `.inputBox p[name="${uid}"]`,
        );
        const helperElementCheck = document.querySelector(
            `.inputBox p[name="pwck"]`,
        );

        if (!helperElement) return;

        if (value == '' || value == null) {
            helperElement.textContent = '*비밀번호를 입력해주세요.';
            helperElementCheck.textContent = '';
        } else if (!isValidPassword) {
            helperElement.textContent =
                '*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.';
            helperElementCheck.textContent = '';
        } else {
            helperElement.textContent = '';
            modifyData.password = value;
        }
    } else if (uid == 'pwck') {
        const value = event.target.value;
        const helperElement = document.querySelector(
            `.inputBox p[name="${uid}"]`,
        );
        // pw 입력란의 현재 값
        const password = modifyData.password;

        if (value == '' || value == null) {
            helperElement.textContent = '*비밀번호 한번 더 입력해주세요.';
        } else if (password !== value) {
            helperElement.textContent = '*비밀번호가 다릅니다.';
        } else {
            helperElement.textContent = '';
            modifyData.passwordCheck = value;
        }
    }

    observeData();
};

/*
 * 기능 : 비밀번호 입력 이벤트 연결
 * 설명 : 비밀번호와 확인 입력이 바뀔 때마다 검사 함수를 호출합니다.
 */
const addEventForInputElements = () => {
    const InputElement = document.querySelectorAll('input');
    InputElement.forEach(element => {
        const id = element.id;

        element.addEventListener('input', event => blurEventHandler(event, id));
    });
};

/*
 * 기능 : 비밀번호 변경과 재로그인 안내
 * 설명 : 변경 API가 성공하면 로그아웃을 요청하고 임시 저장값을 정리한 뒤 로그인 화면으로 이동합니다.
 */
const modifyPassword = async () => {
    const { password } = modifyData;

    const { status } = await changePassword(password);

    if (status == HTTP_OK) {
        try {
            await fetch(`${getServerUrl()}/v1/auth/logout`, {
                method: 'POST',
                credentials: 'include',
            });
        } catch (error) {
            console.error('로그아웃 요청 실패:', error);
        }
        localStorage.clear();
        location.href = '/html/login.html';
    } else {
        Dialog('비밀번호 변경 실패', () => {
            location.href = '/html/modifyPassword.html';
        });
    }
};

/*
 * 기능 : 비밀번호 변경 화면 초기화
 * 설명 : 인증 사용자 헤더와 입력 검사, 저장 버튼을 준비합니다.
 */
const init = () => {
    button.addEventListener('click', modifyPassword);
    prependChild(document.body, Header('마크안에 사람이 살잖아', 1, profileImage));
    addEventForInputElements();
    observeData();
};

init();
