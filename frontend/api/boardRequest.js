import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 게시글 상세 조회 요청
 * 설명 : 게시글 본문·사진·통계·댓글 정보를 가져옵니다.
 */
export const getPost = postId => {
    const result = requestJson(`${getServerUrl()}/v1/posts/${postId}`, {
        credentials: 'include',
    });
    return result;
};

/*
 * 기능 : 게시글 삭제 요청
 * 설명 : 인증 쿠키를 포함해 선택한 게시글의 삭제를 요청합니다.
 */
export const deletePost = async postId => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${postId}`, {
        method: 'DELETE',
        credentials: 'include',
    });
    return result;
};

/*
 * 기능 : 댓글 등록 요청
 * 설명 : 현재 게시글에 입력한 댓글 내용을 JSON으로 전송합니다.
 */
export const writeComment = async (pageId, comment) => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${pageId}/comments`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ commentContent: comment }),
    });
    return result;
};

/*
 * 기능 : 댓글 목록 요청
 * 설명 : 지정한 게시글의 댓글 목록 API를 호출합니다.
 */
export const getComments = async postId => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${postId}/comments`, {
        credentials: 'include',
    });
    return result;
};

/*
 * 기능 : 게시글 좋아요 요청
 * 설명 : 현재 로그인 사용자의 좋아요를 등록합니다.
 */
export const likePost = async postId => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${postId}/likes`, {
        method: 'POST',
        credentials: 'include',
    });
    return result;
};

/*
 * 기능 : 게시글 좋아요 취소 요청
 * 설명 : 현재 로그인 사용자의 좋아요를 해제합니다.
 */
export const unlikePost = async postId => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${postId}/likes`, {
        method: 'DELETE',
        credentials: 'include',
    });
    return result;
};
