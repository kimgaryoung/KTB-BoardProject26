import { checkNickname } from '../api/signupRequest.js';
import Dialog from '../component/dialog/dialog.js';
import Header from '../component/header/header.js';
import {
    authCheck,
    prependChild,
    getServerUrl,
    resolveImageUrl,
    validNickname,
} from '../utils/function.js';
import { userModify, userDelete } from '../api/modifyInfoRequest.js';
import { requestJson } from '../utils/request.js';
import { uploadProfileImage } from '../utils/profileImage.js';

const emailTextElement = document.querySelector('#id');
const nicknameInputElement = document.querySelector('#nickname');
const profileInputElement = document.querySelector('#profile');
const withdrawBtnElement = document.querySelector('#withdrawBtn');
const nicknameHelpElement = document.querySelector(
    '.inputBox p[name="nickname"]',
);
const modifyBtnElement = document.querySelector('#signupBtn');
const profilePreview = document.querySelector('#profilePreview');
const removeProfileButton = document.querySelector('#removeProfileButton');
const authDataReponse = await authCheck();
const authData = authDataReponse ? await authDataReponse.json() : { data: {} };
const changeData = {
    nickname: authData.data.nickname,
    profileImageUrl: authData.data.profileImageUrl,
};

const DEFAULT_PROFILE_IMAGE = '../public/image/profile/default.jpg';
const HTTP_OK = 200;
const profileHelp = document.querySelector('#profileStatus');
let profileVersion = 0;
let uploading = false;
let saving = false;
let nicknameValid = true;

/*
 * 기능 : 기존 회원정보 표시
 * 설명 : 현재 이메일·닉네임·프로필 사진을 폼에 채우고 사진 제거 버튼 상태를 맞춥니다.
 */
const setData = data => {
    if (!data.profileImageUrl) {
        profilePreview.src = DEFAULT_PROFILE_IMAGE;
        if (removeProfileButton) removeProfileButton.style.display = 'none';
    } else {
        profilePreview.src = resolveImageUrl(
            data.profileImageUrl,
            DEFAULT_PROFILE_IMAGE,
        );
        if (removeProfileButton) removeProfileButton.style.display = 'flex';
    }
    emailTextElement.textContent = data.email;
    nicknameInputElement.value = data.nickname;
};

/*
 * 기능 : 회원정보 변경 여부 확인
 * 설명 : 기존 닉네임·사진 주소와 편집값을 비교해 저장 버튼 상태를 결정합니다.
 */
const observeData = () => {
    const changed = authData.data.nickname !== changeData.nickname ||
        authData.data.profileImageUrl !== changeData.profileImageUrl;
    modifyBtnElement.disabled = !changed || !nicknameValid || uploading || saving;
    modifyBtnElement.style.backgroundColor = modifyBtnElement.disabled ? '#ACA0EB' : '#7F6AEE';
};

/*
 * 기능 : 회원정보 편집값 검사
 * 설명 : 닉네임 형식과 중복 여부를 확인하고 프로필 사진 변경을 편집 데이터에 반영합니다.
 */
const changeEventHandler = async (event, uid) => {
    if (uid === 'nickname') {
        const value = event.target.value;
        nicknameValid = false;
        changeData.nickname = value;
        observeData();
        if (!validNickname(value)) {
            nicknameHelpElement.textContent = '*닉네임은 2~10자의 영문자, 한글 또는 숫자로 입력해주세요.';
        } else if (value === authData.data.nickname) {
            nicknameValid = true;
            nicknameHelpElement.textContent = '';
        } else {
            try {
                const result = await checkNickname(value);
                if (nicknameInputElement.value !== value) return;
                nicknameValid = result.status === HTTP_OK;
                nicknameHelpElement.textContent = nicknameValid ? '' : '*사용할 수 없는 닉네임입니다.';
            } catch {
                nicknameHelpElement.textContent = '*닉네임 확인에 실패했습니다. 다시 입력해주세요.';
            }
        }
    } else if (uid === 'profile') {
        const file = event.target.files[0];
        if (!file) return;
        const current = ++profileVersion;
        uploading = true;
        profileHelp.textContent = '사진을 업로드하는 중…';
        observeData();
        try {
            const url = await uploadProfileImage(file);
            if (current !== profileVersion) return;
            changeData.profileImageUrl = url;
            profilePreview.src = resolveImageUrl(url, DEFAULT_PROFILE_IMAGE);
            removeProfileButton.style.display = 'flex';
            profileHelp.textContent = '사진이 준비됐어요. 수정하기를 눌러 저장해주세요.';
        } catch (error) {
            if (current !== profileVersion) return;
            profileInputElement.value = '';
            profileHelp.textContent = error.message;
        } finally {
            if (current === profileVersion) { uploading = false; observeData(); }
        }
    }
    observeData();
};

/*
 * 기능 : 회원정보 수정 저장
 * 설명 : 변경 정보를 서버에 보내고 결과 메시지를 다음 화면에서 보여주도록 저장합니다.
 */
const sendModifyData = async () => {
    if (modifyBtnElement.disabled) return;
    saving = true;
    observeData();
    try {
        const result = await userModify(changeData);
        if (!result.ok) throw new Error('회원정보를 저장하지 못했어요. 다시 시도해주세요.');
        saveToastMessage('수정완료');
        location.href = '/html/modifyInfo.html';
    } catch (error) {
        Dialog('회원정보 수정 실패', error.message);
    } finally { saving = false; observeData(); }
};

// 회원 탈퇴
/*
 * 기능 : 회원 탈퇴 확인 및 요청
 * 설명 : 사용자 확인 후 탈퇴 API를 호출하고 성공하면 로그아웃을 요청한 뒤 로그인 화면으로 이동합니다.
 */
const deleteAccount = async () => {
    const callback = async () => {
        const { status } = await userDelete();

        if (status === HTTP_OK) {
            try {
                await requestJson(`${getServerUrl()}/v1/auth/logout`, {
                    method: 'POST',
                    credentials: 'include',
                });
            } catch (error) {
                console.error('로그아웃 요청 실패:', error);
            }
            location.href = '/html/login.html';
        } else {
            Dialog('회원 탈퇴 실패', '회원 탈퇴에 실패했습니다.');
        }
    };

    Dialog(
        '회원탈퇴 하시겠습니까?',
        '작성된 게시글과 댓글은 삭제 됩니다.',
        callback,
    );
};

/*
 * 기능 : 회원정보 편집 이벤트 연결
 * 설명 : 닉네임·사진 변경, 사진 제거, 저장과 탈퇴 버튼을 연결합니다.
 */
const addEvent = () => {
    nicknameInputElement.addEventListener('input', event =>
        changeEventHandler(event, 'nickname'),
    );
    profileInputElement.addEventListener('change', event =>
        changeEventHandler(event, 'profile'),
    );
    if (removeProfileButton) {
        removeProfileButton.addEventListener('click', () => {
            ++profileVersion;
            uploading = false;
            profileHelp.textContent = '수정하기를 누르면 기본 프로필로 저장됩니다.';
            profilePreview.src = DEFAULT_PROFILE_IMAGE;
            changeData.profileImageUrl = null;
            profileInputElement.value = '';
            removeProfileButton.style.display = 'none';
            observeData();
        });
    }
    modifyBtnElement.addEventListener('click', async () => sendModifyData());
    withdrawBtnElement.addEventListener('click', async () => deleteAccount());
};

/*
 * 기능 : 임시 결과 메시지 표시
 * 설명 : 수정 결과를 잠시 보여준 뒤 요소를 제거하고 선택적인 콜백을 실행합니다.
 */
const showToast = (message, duration = 3000, callback = null) => {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.classList.add('toastMessage');
    toast.textContent = message;

    container.appendChild(toast);

    // 메시지를 보여주기
    setTimeout(() => {
        toast.style.opacity = 1;
        // 조금 더 위로 올라가는 효과를 줄 수 있음
        toast.style.bottom = '30px';
    }, 100);

    // 메시지 숨기기 및 콜백 실행
    setTimeout(() => {
        toast.style.opacity = 0;
        // 원래 위치로 돌아가며 사라지는 효과
        toast.style.bottom = '20px';
        setTimeout(() => {
            // 페이드 아웃이 끝난 후 요소 제거
            toast.remove();
            // 콜백 함수가 있으면 실행
            if (callback) callback();
        }, 500); // CSS transition 시간에 맞춰 설정
    }, duration);
};

/*
 * 기능 : 화면 이동 전 결과 메시지 보관
 * 설명 : sessionStorage에 저장해 새로 열린 수정 화면에서도 결과를 보여줄 수 있게 합니다.
 */
const saveToastMessage = message => {
    sessionStorage.setItem('toastMessage', message);
};

// 토스트 메시지 표시 및 저장소에서 삭제
/*
 * 기능 : 보관된 결과 메시지 표시
 * 설명 : 저장한 메시지를 화면에 띄우고 표시가 끝나면 임시 값을 지웁니다.
 */
const displayToastFromStorage = () => {
    const message = sessionStorage.getItem('toastMessage');
    if (message) {
        showToast(message, 3000, () => {
            // 메시지 삭제
            sessionStorage.removeItem('toastMessage');
        }); // 메시지를 표시하는 기존 함수 사용
    }
};

/*
 * 기능 : 회원정보 수정 화면 초기화
 * 설명 : 인증된 사용자 정보로 헤더와 폼을 채우고 편집 이벤트 및 결과 메시지를 준비합니다.
 */
const init = () => {
    const profileImage =
        resolveImageUrl(authData.data.profileImageUrl, DEFAULT_PROFILE_IMAGE);

    prependChild(document.body, Header('마크안에 사람이 살잖아', 2, profileImage));
    setData(authData.data);
    observeData();
    addEvent();
    displayToastFromStorage();
};

if (authDataReponse) init();
