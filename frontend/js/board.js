import CommentItem from '../component/comment/comment.js';
import Dialog from '../component/dialog/dialog.js';
import { mountPublicHeader } from '../utils/pageHeader.js';
import {
    DEFAULT_PROFILE_IMAGE,
    serverSessionCheck,
    padTo2Digits,
    resolveImageUrl,
} from '../utils/function.js';
import {
    getPost,
    deletePost,
    writeComment,
    likePost,
    unlikePost,
} from '../api/boardRequest.js';

const MAX_COMMENT_LENGTH = 1000;
const HTTP_NOT_AUTHORIZED = 401;
const HTTP_OK = 200;

/*
 * 기능 : 게시글 통계 숫자 표시
 * 설명 : 조회수와 좋아요를 천·백만 단위로 줄여 표시합니다.
 */
const formatCount = value => {
    const count = Number(value);
    if (!Number.isFinite(count)) return value ?? '';
    if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`;
    if (count >= 1000) return `${(count / 1000).toFixed(1)}K`;
    return count.toLocaleString();
};

/*
 * 기능 : 좋아요 버튼 표시와 접근성 상태 갱신
 * 설명 : 선택 여부에 따라 아이콘·스타일·aria 속성을 함께 바꿉니다.
 */
const setLikeButtonState = (button, isLiked) => {
    const iconElement = button.querySelector('.postLikeIcon');

    button.classList.toggle('is-active', isLiked);
    button.setAttribute('aria-pressed', isLiked ? 'true' : 'false');
    button.setAttribute(
        'aria-label',
        isLiked ? '게시글 좋아요 취소' : '게시글 좋아요',
    );

    if (iconElement) {
        iconElement.textContent = isLiked ? '♥' : '♡';
    }
};

const getApiErrorCode = ({ code, data }) => {
    if (code) return code;
    if (typeof data === 'string') return data;
    return null;
};

const setLikeCountText = (countElements, likeCount) => {
    countElements.forEach(element => {
        element.textContent = formatCount(likeCount);
    });
};

const getQueryString = name => {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
};

/*
 * 기능 : 현재 게시글 ID 확인
 * 설명 : 쿼리 파라미터 또는 /post/{id} 경로에서 상세 조회 대상을 읽습니다.
 */
const getPostId = () => {
    const queryPostId = getQueryString('id');
    if (queryPostId) return queryPostId;

    const pathMatch = window.location.pathname.match(/^\/post\/([^/]+)$/);
    return pathMatch ? pathMatch[1] : null;
};

/*
 * 기능 : 게시글 상세 데이터 조회
 * 설명 : 상세 API 데이터를 반환하고 실패하면 화면 초기화에서 처리할 오류를 발생시킵니다.
 */
const getBoardDetail = async postId => {
    const { ok, data } = await getPost(postId);
    if (!ok)
        throw new Error('게시글 정보를 가져오는데 실패하였습니다.');
    return data;
};

/*
 * 기능 : 게시글 본문·사진·통계 렌더링
 * 설명 : 작성자와 본문, 원본 링크가 있는 첨부 사진을 표시합니다. 좋아요는 먼저 화면에 반영하고 서버 결과에 맞춰 보정합니다.
 */
const setBoardDetail = data => {
    // 헤드 정보
    const titleElement = document.querySelector('.title');
    const createdAtElement = document.querySelector('.createdAt');
    const imgElement = document.querySelector('.img');
    const nicknameElement = document.querySelector('.nickname');

    titleElement.textContent = data.title;
    const date = new Date(data.createdAt);
    const formattedDate = `${date.getFullYear()}-${padTo2Digits(date.getMonth() + 1)}-${padTo2Digits(date.getDate())} ${padTo2Digits(date.getHours())}:${padTo2Digits(date.getMinutes())}:${padTo2Digits(date.getSeconds())}`;
    createdAtElement.textContent = formattedDate;

    imgElement.src = resolveImageUrl(
        data.profileImage,
        DEFAULT_PROFILE_IMAGE,
    );

    nicknameElement.textContent = data.nickname;

    // 바디 정보
    const contentImgElement = document.querySelector('.contentImg');
    const postImageUrl = resolveImageUrl(data.postImageUrl);
    contentImgElement.replaceChildren();
    if (postImageUrl) {
        const img = document.createElement('img');
        img.src = postImageUrl;
        img.alt = `${data.title} 게시글 이미지`;
        img.decoding = 'async';
        const originalLink = document.createElement('a');
        originalLink.href = postImageUrl;
        originalLink.target = '_blank';
        originalLink.rel = 'noopener noreferrer';
        originalLink.setAttribute('aria-label', '첨부 사진 원본 보기');
        originalLink.appendChild(img);
        contentImgElement.appendChild(originalLink);
        img.addEventListener('error', () => {
            contentImgElement.textContent = '사진을 불러오지 못했어요.';
        });
    }
    const contentElement = document.querySelector('.content');
    contentElement.textContent = data.content;

    const likeButtonElement = document.querySelector('.postLikeButton');
    const likeButtonCountElement = likeButtonElement.querySelector('.postLikeCount');
    const likeSummaryCountElement = document.querySelector('.likeCountSummary h3');
    const likeCountElements = [likeButtonCountElement, likeSummaryCountElement];
    const postId = data.id ?? getPostId();
    let likeCount = Number(data.likeCount) || 0;
    let isLiked = Boolean(data.isLiked);
    let isLikeLoading = false;

    setLikeCountText(likeCountElements, likeCount);
    setLikeButtonState(likeButtonElement, isLiked);

    likeButtonElement.addEventListener('click', async () => {
        if (isLikeLoading) return;
        isLikeLoading = true;
        likeButtonElement.disabled = true;

        const previousIsLiked = isLiked;
        const previousLikeCount = likeCount;
        const nextIsLiked = !isLiked;
        const nextLikeCount = Math.max(0, likeCount + (nextIsLiked ? 1 : -1));

        isLiked = nextIsLiked;
        likeCount = nextLikeCount;
        setLikeButtonState(likeButtonElement, isLiked);
        setLikeCountText(likeCountElements, likeCount);

        try {
            const {
                ok,
                status,
                code,
                data: likeData,
            } = nextIsLiked
                ? await likePost(postId)
                : await unlikePost(postId);
            const apiErrorCode = getApiErrorCode({ code, data: likeData });

            if (ok) {
                if (likeData && likeData.likeCount !== undefined) {
                    likeCount = Number(likeData.likeCount);
                    setLikeCountText(likeCountElements, likeCount);
                }
                if (likeData && likeData.isLiked !== undefined) {
                    isLiked = Boolean(likeData.isLiked);
                    setLikeButtonState(likeButtonElement, isLiked);
                }
            } else if (
                status === 409 &&
                apiErrorCode === 'POST_ALREADY_LIKED'
            ) {
                isLiked = Boolean(likeData && likeData.isLiked);
                likeCount = Number(likeData && likeData.likeCount) || likeCount;
                setLikeButtonState(likeButtonElement, isLiked);
                setLikeCountText(likeCountElements, likeCount);
            } else if (
                status === 409 &&
                apiErrorCode === 'POST_ALREADY_UNLIKED'
            ) {
                isLiked = Boolean(likeData && likeData.isLiked);
                likeCount = Number(likeData && likeData.likeCount) || likeCount;
                setLikeButtonState(likeButtonElement, isLiked);
                setLikeCountText(likeCountElements, likeCount);
            } else if (status === HTTP_NOT_AUTHORIZED) {
                window.location.href = '/html/login.html';
            } else {
                isLiked = previousIsLiked;
                likeCount = previousLikeCount;
                setLikeButtonState(likeButtonElement, isLiked);
                setLikeCountText(likeCountElements, likeCount);
                Dialog('좋아요 실패', '좋아요 처리에 실패하였습니다.');
            }
        } finally {
            isLikeLoading = false;
            likeButtonElement.disabled = false;
        }
    });

    const viewCountElement = document.querySelector('.viewCount h3');
    viewCountElement.textContent = formatCount(data.viewCount);

    const commentCountElement = document.querySelector('.commentCount h3');
    commentCountElement.textContent = data.commentCount.toLocaleString();
};

/*
 * 기능 : 작성자용 수정·삭제 버튼 연결
 * 설명 : 로그인 사용자와 글 작성자가 같은 경우 편집 버튼을 표시하고 삭제 전 확인 창을 띄웁니다.
 */
const setBoardModify = async (data, myInfo) => {
    if (myInfo.idx === data.writerId) {
        const modifyElement = document.querySelector('.hidden');
        modifyElement.classList.remove('hidden');

        const modifyBtnElement = document.querySelector('#deleteBtn');
        const postId = getPostId();
        modifyBtnElement.addEventListener('click', () => {
            Dialog(
                '게시글을 삭제하시겠습니까?',
                '삭제한 내용은 복구 할 수 없습니다.',
                async () => {
                    const { ok } = await deletePost(postId);
                    if (ok) {
                        window.location.href = '/html/posts.html';
                    } else {
                        Dialog('삭제 실패', '게시글 삭제에 실패하였습니다.');
                    }
                },
            );
        });

        const modifyBtnElement2 = document.querySelector('#modifyBtn');
        modifyBtnElement2.addEventListener('click', () => {
            window.location.href = `/html/board-modify.html?postId=${data.id}`;
        });
    }
};

/*
 * 기능 : 게시글 댓글 목록 렌더링
 * 설명 : 댓글마다 현재 사용자 ID를 전달해 수정·삭제 버튼의 표시를 결정합니다.
 */
const setBoardComment = (comments, myInfo, postId) => {
    const commentListElement = document.querySelector('.commentList');
    if (commentListElement && comments) {
        comments.forEach(event => {
            const item = CommentItem(
                event,
                myInfo.userId,
                postId,
                event.commentId,
            );
            commentListElement.appendChild(item);
        });
    }
};

/*
 * 기능 : 댓글 등록과 화면 새로고침
 * 설명 : 입력 내용을 현재 게시글에 등록하고 성공 시 최신 댓글 목록을 표시합니다.
 */
const addComment = async () => {
    const comment = document.querySelector('textarea').value;
    const pageId = getPostId();

    const { ok } = await writeComment(pageId, comment);

    if (ok) {
        window.location.reload();
    } else {
        Dialog('댓글 등록 실패', '댓글 등록에 실패하였습니다.');
    }
};

/*
 * 기능 : 댓글 길이와 등록 버튼 상태 관리
 * 설명 : 댓글을 최대 1000자로 제한하고 빈 입력일 때 등록 버튼을 비활성화합니다.
 */
const inputComment = async () => {
    const textareaElement = document.querySelector(
        '.commentInputWrap textarea',
    );
    const commentBtnElement = document.querySelector('.commentInputBtn');

    if (textareaElement.value.length > MAX_COMMENT_LENGTH) {
        textareaElement.value = textareaElement.value.substring(
            0,
            MAX_COMMENT_LENGTH,
        );
    }
    if (textareaElement.value === '') {
        commentBtnElement.disabled = true;
        commentBtnElement.style.backgroundColor = '#ACA0EB';
    } else {
        commentBtnElement.disabled = false;
        commentBtnElement.style.backgroundColor = '#7F6AEE';
    }
};

/*
 * 기능 : 공개 게시글 상세 화면 초기화
 * 설명 : 로그인 상태별 댓글 입력을 설정하고 본문·댓글·커뮤니티 이동 링크를 표시합니다.
 */
const init = async () => {
    mountPublicHeader({ backHref: '/html/posts.html' });
    try {
        const session = await serverSessionCheck();
        const myInfo = session.ok ? (await session.json()).data : { userId: null, idx: null };
        const commentBtnElement = document.querySelector('.commentInputBtn');
        const textareaElement = document.querySelector(
            '.commentInputWrap textarea',
        );
        textareaElement.addEventListener('input', inputComment);
        commentBtnElement.addEventListener('click', () => {
            if (!myInfo.userId) window.location.assign('/html/login.html');
            else addComment();
        });
        commentBtnElement.disabled = Boolean(myInfo.userId);
        textareaElement.disabled = !myInfo.userId;
        if (!myInfo.userId) {
            textareaElement.placeholder = '로그인하고 댓글을 남겨보세요.';
            commentBtnElement.textContent = '로그인하고 댓글 쓰기';
        }

        const pageId = getPostId();

        const pageData = await getBoardDetail(pageId);
        document.title = `${pageData.title} | 마크안에 사람이 살잖아`;
        const communityLink = document.querySelector('#communityLink');
        communityLink.href = '/html/posts.html';
        communityLink.textContent = '← 마인크래프트 게시판으로';

        if (parseInt(pageData.userId, 10) === parseInt(myInfo.userId, 10)) {
            setBoardModify(pageData, myInfo);
        }
        setBoardDetail(pageData);
        setBoardComment(pageData.comments, myInfo, pageId);
    } catch (error) {
        console.error(error);
        document.querySelector('.title').textContent = '게시글을 불러오지 못했어요.';
    }
};

init();
