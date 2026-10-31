import Dialog from '../component/dialog/dialog.js';
import { getSiteBoard } from '../api/siteRequest.js';
import Header from '../component/header/header.js';
import { loginUrl } from '../utils/authNavigation.js';
import {
    authCheck,
    getQueryString,
    prependChild,
    resolveImageUrl,
} from '../utils/function.js';
import {
    createPost,
    updatePost,
    getBoardItem,
} from '../api/board-writeRequest.js';

const HTTP_OK = 200;
const HTTP_CREATED = 201;
const editorReturnPath = window.location.pathname + window.location.search;

const MAX_TITLE_LENGTH = 26;
const MAX_CONTENT_LENGTH = 1500;

const DEFAULT_PROFILE_IMAGE = '../public/image/profile/default.jpg';

const submitButton = document.querySelector('#submit');
const titleInput = document.querySelector('#title');
const contentInput = document.querySelector('#content');
const imageInput = document.querySelector('#image');
const imagePreviewText = document.getElementById('imagePreviewText');
const imagePreview = document.querySelector('#imagePreview');
const imagePreviewWrap = document.querySelector('#imagePreviewWrap');
const imageError = document.querySelector('#imageError');
const removeImageButton = document.querySelector('#removeImage');
const contentHelpElement = document.querySelector(
    '.inputBox p[name="content"]',
);

const boardWrite = {
    title: '',
    content: '',
};

let isModifyMode = false;
let modifyData = {};
let selectedImage = null;
let removeImage = false;
let previewObjectUrl = null;
let imageVersion = 0;
let imageLoading = false;
let communityId = null;
let communityLoaded = false;
let submitting = false;

/*
 * 기능 : 게시글 저장 버튼 활성화 판단
 * 설명 : 제목·내용·커뮤니티 로딩 상태를 확인하고, 사진을 읽거나 저장 중일 때는 중복 제출을 막습니다.
 */
const observeSignupData = () => {
    const { title, content } = boardWrite;
    if (!title.trim() || !content.trim() || !communityLoaded || submitting || imageLoading) {
        submitButton.disabled = true;
        submitButton.style.backgroundColor = '#ACA0EB';
    } else {
        submitButton.disabled = false;
        submitButton.style.backgroundColor = '#7F6AEE';
    }
};

// 엘리먼트 값 가져오기 title, content
/*
 * 기능 : 게시글 저장 데이터 구성
 * 설명 : 제목·본문·소속 커뮤니티와 기존 사진 제거 여부를 모아 반환합니다.
 */
const getBoardData = () => {
    return {
        title: boardWrite.title,
        content: boardWrite.content,
        communityId,
        removeImage,
    };
};

// 버튼 클릭시 이벤트
/*
 * 기능 : 게시글 작성 또는 수정 저장
 * 설명 : 입력값을 확인한 뒤 작성·수정 API를 호출합니다. 세션이 만료되면 현재 작성 화면을 복귀 주소로 보존해 로그인으로 이동합니다.
 */
const addBoard = async () => {
    if (submitting || imageLoading || !communityLoaded) return;
    const boardData = getBoardData();

    // boardData가 false일 경우 함수 종료
    if (!boardData) return Dialog('게시글', '게시글을 입력해주세요.');

    if (boardData.title.length > MAX_TITLE_LENGTH)
        return Dialog('게시글', '제목은 26자 이하로 입력해주세요.');

    if (!boardData.title.trim() || !boardData.content.trim()) return;
    submitting = true;
    observeSignupData();
    try {
        if (!isModifyMode) {
            const request = {
                title: boardData.title,
                content: boardData.content,
                communityId,
            };
            const { ok, status, data } = await createPost(request, selectedImage);
            if (status === 401) { window.location.assign(loginUrl(editorReturnPath)); return; }
            if (!ok) throw new Error('서버 응답 오류');

            if (status === HTTP_CREATED) {
                window.location.href = `/post/${data.id}`;
            } else {
                const helperElement = contentHelpElement;
                helperElement.textContent = '제목, 내용을 모두 작성해주세요.';
            }
        } else {
            const postId = getQueryString('postId');
            const { status } = await updatePost(postId, boardData, selectedImage);
            if (status === 401) { window.location.assign(loginUrl(editorReturnPath)); return; }

            if (status === HTTP_OK) {
                window.location.href = `/post/${postId}`;
            } else {
                Dialog('게시글', '게시글 수정 실패');
            }
        }
    } catch {
        Dialog('게시글', '저장하지 못했어요. 잠시 후 다시 시도해주세요.');
    } finally {
        submitting = false;
        observeSignupData();
    }
};
/*
 * 기능 : 게시글 입력값과 사진 선택 반영
 * 설명 : 제목·본문의 길이를 검사하고 사진 변경은 미리보기 처리로 전달합니다.
 */
const changeEventHandler = async (event, uid) => {
    if (uid == 'title') {
        const value = event.target.value;
        const helperElement = contentHelpElement;
        if (!value || value == '') {
            boardWrite[uid] = '';
            helperElement.textContent = '제목을 입력해주세요.';
        } else if (value.length > MAX_TITLE_LENGTH) {
            helperElement.textContent = '제목은 26자 이하로 입력해주세요.';
            titleInput.value = value.substring(0, MAX_TITLE_LENGTH);
            boardWrite[uid] = value.substring(0, MAX_TITLE_LENGTH);
        } else {
            boardWrite[uid] = value;
            helperElement.textContent = '';
        }
    } else if (uid == 'content') {
        const value = event.target.value;
        const helperElement = contentHelpElement;
        if (!value || value == '') {
            boardWrite[uid] = '';
            helperElement.textContent = '내용을 입력해주세요.';
        } else if (value.length > MAX_CONTENT_LENGTH) {
            helperElement.textContent = '내용은 1500자 이하로 입력해주세요.';
            contentInput.value = value.substring(0, MAX_CONTENT_LENGTH);
            boardWrite[uid] = value.substring(0, MAX_CONTENT_LENGTH);
        } else {
            boardWrite[uid] = value;
            helperElement.textContent = '';
        }
    } else if (uid === 'image') {
        await selectImage(event.target.files[0]);
    }

    observeSignupData();
};
/*
 * 기능 : 첨부 사진 미리보기 표시
 * 설명 : 이미지 주소와 파일 설명을 채우고 미리보기 영역을 엽니다.
 */
const showImage = (url, label) => {
    imagePreview.src = url;
    imagePreviewText.textContent = label;
    imagePreviewWrap.hidden = false;
};

/*
 * 기능 : 첨부 사진 제거
 * 설명 : 선택 파일과 임시 URL을 정리하고 기존 사진도 저장 시 제거되도록 표시합니다. 진행 중이던 이미지 읽기는 무효화합니다.
 */
const clearImage = () => {
    ++imageVersion;
    if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl);
    previewObjectUrl = null;
    selectedImage = null;
    removeImage = true;
    imageLoading = false;
    imageInput.value = '';
    imagePreview.removeAttribute('src');
    imagePreviewWrap.hidden = true;
    imageError.textContent = '';
    observeSignupData();
};

/*
 * 기능 : 업로드 사진 검사와 미리보기 준비
 * 설명 : 5MB 이하의 지원 이미지인지 확인하고 실제 디코딩에 성공한 파일만 선택합니다. 이전 비동기 결과가 최신 선택을 덮어쓰지 않게 합니다.
 */
const selectImage = async file => {
    if (!file) return;
    const current = ++imageVersion;
    let candidateUrl = null;
    imageLoading = true;
    observeSignupData();
    try {
        if (file.size === 0 || file.size > 5 * 1024 * 1024) throw new Error('사진은 비어 있지 않은 5MB 이하 파일을 선택해주세요.');
        const supported = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'];
        if (file.type && !supported.includes(file.type)) throw new Error('JPG, PNG, GIF, WebP, BMP 사진을 선택해주세요.');
        candidateUrl = URL.createObjectURL(file);
        const probe = new Image();
        probe.src = candidateUrl;
        try { await probe.decode(); }
        catch { throw new Error('이미지를 읽을 수 없어요. 다른 사진을 선택해주세요.'); }
        if (current !== imageVersion) return;
        if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl);
        previewObjectUrl = candidateUrl;
        candidateUrl = null;
        selectedImage = file;
        removeImage = false;
        showImage(previewObjectUrl, file.name);
        imageError.textContent = '';
    } catch (error) {
        if (current === imageVersion) {
            imageInput.value = '';
            imageError.textContent = error.message;
        }
    } finally {
        if (candidateUrl) URL.revokeObjectURL(candidateUrl);
        if (current === imageVersion) { imageLoading = false; observeSignupData(); }
    }
};
window.addEventListener('pagehide', () => { if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl); });

// 수정모드시 사용하는 게시글 단건 정보 가져오기
/*
 * 기능 : 수정할 게시글 조회
 * 설명 : 기존 본문과 사진을 불러오고 상세 조회 실패 시 오류로 처리합니다.
 */
const getBoardModifyData = async postId => {
    const { ok, data } = await getBoardItem(postId);
    if (!ok) throw new Error('서버 응답 오류');
    return data;
};

// 수정 모드인지 확인
/*
 * 기능 : 작성 화면과 수정 화면 구분
 * 설명 : 현재 URL에 게시글 ID가 있는지 확인해 수정 대상 ID를 반환합니다.
 */
const checkModifyMode = () => {
    const postId = getQueryString('postId');
    if (!postId) return false;
    return postId;
};

// 이벤트 등록
/*
 * 기능 : 게시글 편집 화면 이벤트 연결
 * 설명 : 저장·본문 입력·사진 선택·사진 제거 동작을 각 입력 요소에 연결합니다.
 */
const addEvent = () => {
    submitButton.addEventListener('click', addBoard);
    titleInput.addEventListener('input', event =>
        changeEventHandler(event, 'title'),
    );
    contentInput.addEventListener('input', event =>
        changeEventHandler(event, 'content'),
    );
    imageInput.addEventListener('change', event =>
        changeEventHandler(event, 'image'),
    );
    removeImageButton.addEventListener('click', clearImage);

};

/*
 * 기능 : 기존 게시글을 수정 폼에 표시
 * 설명 : 제목·본문·현재 사진을 채우고 편집 데이터와 저장 버튼 상태를 갱신합니다.
 */
const setModifyData = data => {
    titleInput.value = data.title;
    contentInput.value = data.content;

    const fileUrl = resolveImageUrl(data.postImageUrl);
    if (fileUrl) showImage(fileUrl, '현재 첨부 사진');

    boardWrite.title = data.title;
    boardWrite.content = data.content;

    observeSignupData();
};

/*
 * 기능 : 로그인 후 마인크래프트 글 편집 시작
 * 설명 : 서버의 전용 게시판을 자동으로 사용하고 기존 주소의 선택 파라미터는 사용하지 않습니다. 수정은 작성자만 허용합니다.
 */
const init = async () => {
    const dataResponse = await authCheck(editorReturnPath);

    if (!dataResponse || !dataResponse.ok) {
        location.href = loginUrl(editorReturnPath);
        return;
    }

    const data = await dataResponse.json();

    if (!data || !data.data) {
        location.href = loginUrl(editorReturnPath);
        return;
    }

    const profileImage = resolveImageUrl(
        data.data.profileImageUrl,
        DEFAULT_PROFILE_IMAGE,
    );
    const modifyId = checkModifyMode();

    prependChild(document.body, Header('마크안에 사람이 살잖아', 1, profileImage));
    try {
        const board = await getSiteBoard();
        if (!board.ok || board.data?.id === undefined) throw new Error('board unavailable');
        communityId = board.data.id;
        if (modifyId) {
            isModifyMode = true;
            modifyData = await getBoardModifyData(modifyId);
            if (modifyData.communityId !== communityId) { window.location.replace('/'); return; }
            if (data.data.idx !== modifyData.writerId) {
                Dialog('권한 없음', '작성자만 수정할 수 있습니다.', () => window.location.assign('/'));
                return;
            }
            setModifyData(modifyData);
        }
        const link = document.querySelector('#communityLink');
        link.href = '/html/posts.html';
        link.textContent = '← 마인크래프트 게시판으로';
        communityLoaded = true;
        titleInput.disabled = false;
        contentInput.disabled = false;
        imageInput.disabled = false;
    } catch {
        communityId = null;
        observeSignupData();
        contentHelpElement.textContent = '게시글 또는 게시판을 불러오지 못했어요. 게시판으로 돌아가 다시 시도해주세요.';
        return;
    }

    observeSignupData();
};

addEvent();
init();
