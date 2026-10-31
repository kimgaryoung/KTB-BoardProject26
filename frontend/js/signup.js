import Dialog from '../component/dialog/dialog.js';
import Header from '../component/header/header.js';
import {
    authCheckReverse,
    prependChild,
    validEmail,
    validPassword,
    validNickname,
    resolveImageUrl,
} from '../utils/function.js';
import {
    userSignup,
    checkEmail,
    checkNickname,
} from '../api/signupRequest.js';
import { uploadProfileImage } from '../utils/profileImage.js';

const MAX_PASSWORD_LENGTH = 20;
const HTTP_OK = 200;
const HTTP_CREATED = 201;
const HTTP_CONFLICT = 409;
let profileVersion = 0;
let profileUploading = false;
let profileFailed = false;
const profileStatus = document.querySelector('#profileStatus');
const profilePreview = document.querySelector('#signupProfilePreview');
const removeProfile = document.querySelector('#removeSignupProfile');

const signupData = {
    email: '',
    password: '',
    nickname: '',
    profileImageUrl: undefined,
};

/*
 * 기능 : 가입 필수 항목 확인
 * 설명 : 이메일·비밀번호·비밀번호 확인·닉네임이 채워졌는지 확인하고 가입 요청을 시작합니다.
 */
const getSignupData = () => {
    const { email, password, passwordCheck, nickname } = signupData;
    if (profileUploading || profileFailed) return false;
    if (!email || !password || !passwordCheck || !nickname) {
        Dialog('필수 입력 사항', '모든 값을 입력해주세요.');
        return false;
    }

    sendSignupData();
};

/*
 * 기능 : 회원가입 제출과 결과 처리
 * 설명 : 비밀번호 확인값을 제외한 가입 정보와 임시 프로필 주소를 보내고 응답에 따라 로그인 이동 또는 오류 안내를 처리합니다.
 */
const sendSignupData = async () => {
    const { passwordCheck, ...props } = signupData;

    if (props.password.length > MAX_PASSWORD_LENGTH) {
        Dialog('비밀번호', '비밀번호는 20자 이하로 입력해주세요.');
        return;
    }
    // signupData를 서버로 전송
    const { status, code } = await userSignup(props);

    // 응답이 성공적으로 왔을 경우
    if (status === HTTP_CREATED) {
        location.href = '/html/login.html';
    } else {
        if (code === 'ALREADY_EXIST_EMAIL') {
            Dialog('회원 가입 실패', '이미 사용 중인 이메일입니다.');
        } else if (code === 'ALREADY_EXIST_NICKNAME') {
            Dialog('회원 가입 실패', '이미 사용 중인 닉네임입니다.');
        } else if (code === 'INVALID_INPUT') {
            Dialog('회원 가입 실패', '입력값을 확인해주세요.');
        } else {
            Dialog('회원 가입 실패', '잠시 뒤 다시 시도해 주세요', () => {});
        }
        location.href = '/html/signup.html';
    }
};

const signupClick = () => {
    // signup 버튼 클릭 시
    const signupBtn = document.querySelector('#signupBtn');
    signupBtn.addEventListener('click', getSignupData);
};

/*
 * 기능 : 가입 프로필 사진 업로드·미리보기
 * 설명 : 가장 최근에 선택한 사진만 반영하고 업로드 중에는 가입을 막습니다. 실패 이유를 화면에 표시합니다.
 */
const changeEventHandler = async event => {
    const file = event.target.files[0];
    if (!file) return;
    const current = ++profileVersion;
    profileUploading = true;
    profileFailed = false;
    profileStatus.textContent = '사진을 업로드하는 중…';
    removeProfile.hidden = false;
    observeSignupData();
    try {
        const url = await uploadProfileImage(file);
        if (current !== profileVersion) return;
        signupData.profileImageUrl = url;
        profilePreview.src = resolveImageUrl(url);
        profileStatus.textContent = '회원가입 시 이 사진으로 저장됩니다.';
    } catch (error) {
        if (current !== profileVersion) return;
        profileFailed = true;
        profileStatus.textContent = error.message;
        event.target.value = '';
    } finally {
        if (current === profileVersion) { profileUploading = false; observeSignupData(); }
    }
};

/*
 * 기능 : 가입 입력 형식과 중복 검사
 * 설명 : 이메일·닉네임 형식 및 사용 가능 여부, 비밀번호 규칙과 확인값을 검사해 안내 문구를 갱신합니다.
 */
const inputEventHandler = async (event, uid) => {
    if (uid == 'email') {
        const value = event.target.value;
        const isValidEmail = validEmail(value);
        const helperElement = document.querySelector(
            `.inputBox p[name="${uid}"]`,
        );
        let isComplete = false;

        if (!helperElement) return;

        if (value == '' || value == null) {
            helperElement.textContent = '*이메일을 입력해주세요.';
        } else if (!isValidEmail) {
            helperElement.textContent =
                '*올바른 이메일 주소 형식을 입력해주세요. (예: example@example.com)';
        } else {
            const { status } = await checkEmail(value);
            if (status === HTTP_OK) {
                helperElement.textContent = '';
                isComplete = true;
            } else if (status === HTTP_CONFLICT) {
                helperElement.textContent = '*중복된 이메일 입니다.';
            } else {
                helperElement.textContent =
                    '*이메일 확인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
            }
        }
        if (isComplete) {
            signupData.email = value;
        } else {
            signupData.email = '';
        }
    } else if (uid == 'pw') {
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
            signupData.password = value;
        }
    } else if (uid == 'pwck') {
        const value = event.target.value;
        const helperElement = document.querySelector(
            `.inputBox p[name="${uid}"]`,
        );
        // pw 입력란의 현재 값
        const password = signupData.password;

        if (value == '' || value == null) {
            helperElement.textContent = '*비밀번호 한번 더 입력해주세요.';
        } else if (password !== value) {
            helperElement.textContent = '*비밀번호가 다릅니다.';
        } else {
            signupData.passwordCheck = value;
            helperElement.textContent = '';
        }
    } else if (uid == 'nickname') {
        const value = event.target.value;
        const isValidNickname = validNickname(value);
        const helperElement = document.querySelector(
            `.inputBox p[name="${uid}"]`,
        );
        let isComplete = false;

        if (value == '' || value == null) {
            helperElement.textContent = '*닉네임을 입력해주세요.';
        } else if (value.includes(' ')) {
            helperElement.textContent = '*뛰어쓰기를 없애주세요.';
        } else if (value.length > 10) {
            helperElement.textContent =
                '*닉네임은 최대 10자까지 작성 가능합니다.';
        } else if (!isValidNickname) {
            helperElement.textContent =
                '*닉네임에 특수 문자는 사용할 수 없습니다.';
        } else {
            const { status } = await checkNickname(value);

            if (status === HTTP_OK) {
                helperElement.textContent = '';
                isComplete = true;
            } else if (status === HTTP_CONFLICT) {
                helperElement.textContent = '*중복된 닉네임 입니다.';
            } else {
                helperElement.textContent =
                    '*닉네임 확인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
            }
        }

        if (isComplete) {
            signupData.nickname = value;
        } else {
            signupData.nickname = '';
        }
    }
    observeSignupData();
};

/*
 * 기능 : 회원가입 입력 이벤트 연결
 * 설명 : 프로필 파일 변경과 나머지 입력값 변경을 각각의 검사 함수에 연결합니다.
 */
const addEventForInputElements = () => {
    const InputElement = document.querySelectorAll('input');
    InputElement.forEach(element => {
        const id = element.id;
        if (id === 'profile') {
            element.addEventListener('change', event =>
                changeEventHandler(event, id),
            );
        } else {
            element.addEventListener('input', event =>
                inputEventHandler(event, id),
            );
        }
    });
};

/*
 * 기능 : 회원가입 버튼 활성화 판단
 * 설명 : 필수 가입 정보와 형식 검사 결과를 확인해 제출 버튼 상태를 갱신합니다.
 */
const observeSignupData = () => {
    const { email, password, passwordCheck, nickname } = signupData;
    const button = document.querySelector('#signupBtn');

    if (
        profileUploading ||
        profileFailed ||
        !email ||
        !validEmail(email) ||
        !password ||
        !validPassword(password) ||
        !nickname ||
        !validNickname(nickname) ||
        !passwordCheck
    ) {
        button.disabled = true;
        button.style.backgroundColor = '#ACA0EB';
    } else {
        button.disabled = false;
        button.style.backgroundColor = '#7F6AEE';
    }
};

/*
 * 기능 : 가입 프로필 사진 선택 취소
 * 설명 : 진행 중인 이전 응답을 무시하고 기본 사진으로 되돌립니다. 다른 화면·계정의 임시 사진을 재사용하지 않습니다.
 */
const resetProfile = () => {
    ++profileVersion;
    profileUploading = false;
    profileFailed = false;
    signupData.profileImageUrl = undefined;
    document.querySelector('#profile').value = '';
    profilePreview.src = '/public/image/profile/default.jpg';
    profileStatus.textContent = '선택 사항 · 최대 5MB';
    removeProfile.hidden = true;
    observeSignupData();
};

/*
 * 기능 : 회원가입 화면 초기화
 * 설명 : 로그인 여부를 확인한 뒤 헤더·입력 검사·가입 버튼·사진 업로드 이벤트를 준비합니다.
 */
const init = async () => {
    await authCheckReverse();
    prependChild(document.body, Header('마크안에 사람이 살잖아', 1));
    observeSignupData();
    addEventForInputElements();
    signupClick();
    removeProfile.addEventListener('click', resetProfile);
};

init();
