import Dialog from '../component/dialog/dialog.js';
import Header from '../component/header/header.js';
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

const MAX_TITLE_LENGTH = 26;
const MAX_CONTENT_LENGTH = 1500;

const DEFAULT_PROFILE_IMAGE = '../public/image/profile/default.jpg';

const submitButton = document.querySelector('#submit');
const titleInput = document.querySelector('#title');
const contentInput = document.querySelector('#content');
const imageInput = document.querySelector('#image');
const imagePreviewText = document.getElementById('imagePreviewText');
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

const observeSignupData = () => {
    const { title, content } = boardWrite;
    if (!title || !content || title === '' || content === '') {
        submitButton.disabled = true;
        submitButton.style.backgroundColor = '#ACA0EB';
    } else {
        submitButton.disabled = false;
        submitButton.style.backgroundColor = '#7F6AEE';
    }
};

// 엘리먼트 값 가져오기 title, content
const getBoardData = () => {
    return {
        title: boardWrite.title,
        content: boardWrite.content,
    };
};

// 버튼 클릭시 이벤트
const addBoard = async () => {
    const boardData = getBoardData();

    // boardData가 false일 경우 함수 종료
    if (!boardData) return Dialog('게시글', '게시글을 입력해주세요.');

    if (boardData.title.length > MAX_TITLE_LENGTH)
        return Dialog('게시글', '제목은 26자 이하로 입력해주세요.');

    if (!isModifyMode) {
        const request = {
            title: boardData.title,
            content: boardData.content,
        };
        const { ok, status, data } = await createPost(request, selectedImage);
        if (!ok) throw new Error('서버 응답 오류');

        if (status === HTTP_CREATED) {
            window.location.href = `/html/board.html?id=${data.id}`;
        } else {
            const helperElement = contentHelpElement;
            helperElement.textContent = '제목, 내용을 모두 작성해주세요.';
        }
    } else {
        const postId = getQueryString('postId');
        const { ok, status } = await updatePost(
            postId,
            boardData,
            selectedImage,
        );
        if (!ok) throw new Error('서버 응답 오류');

        if (status === HTTP_OK) {
            window.location.href = `/html/board.html?id=${postId}`;
        } else {
            Dialog('게시글', '게시글 수정 실패');
        }
    }
};
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
    } else if (uid == 'image') {
        const file = event.target.files[0]; // 사용자가 선택한 파일
        if (!file) {
            selectedImage = null;
            return;
        }

        selectedImage = file;

        if (imagePreviewText) {
            imagePreviewText.textContent = file.name;
            imagePreviewText.style.display = 'block';
        }

    } else if (uid === 'imagePreviewText') {
        selectedImage = null;
        imageInput.value = '';
        imagePreviewText.style.display = 'none';
    }

    observeSignupData();
};
// 수정모드시 사용하는 게시글 단건 정보 가져오기
const getBoardModifyData = async postId => {
    const { ok, data } = await getBoardItem(postId);
    if (!ok) throw new Error('서버 응답 오류');
    return data;
};

// 수정 모드인지 확인
const checkModifyMode = () => {
    const postId = getQueryString('postId');
    if (!postId) return false;
    return postId;
};

// 이벤트 등록
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
    if (imagePreviewText !== null) {
        imagePreviewText.addEventListener('click', event =>
            changeEventHandler(event, 'imagePreviewText'),
        );
    }
};

const setModifyData = data => {
    titleInput.value = data.title;
    contentInput.value = data.content;

    const fileUrl = data.postImageUrl;
    if (fileUrl) {
        const fileName = fileUrl.split('/').pop();
        imagePreviewText.replaceChildren(
            document.createTextNode(fileName),
        );
        imagePreviewText.style.display = 'block';
    } else {
        imagePreviewText.style.display = 'none';
    }

    boardWrite.title = data.title;
    boardWrite.content = data.content;

    observeSignupData();
};

const init = async () => {
    const dataResponse = await authCheck();

    if (!dataResponse || !dataResponse.ok) {
        location.href = '/html/login.html';
        return;
    }

    const data = await dataResponse.json();

    if (!data || !data.data) {
        location.href = '/html/login.html';
        return;
    }

    prependChild(document.body, Header('커뮤니티', 1, profileImage));

    if (modifyId) {
        isModifyMode = true;
        modifyData = await getBoardModifyData(modifyId);

        if (modifyData && data.data.idx !== modifyData.writerId) {
            Dialog('권한 없음', '권한이 없습니다.', () => {
                window.location.href = '/';
            });
        } else {
            setModifyData(modifyData);
        }
    }

    addEvent();
};

init();
