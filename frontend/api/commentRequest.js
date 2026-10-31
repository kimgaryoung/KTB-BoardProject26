import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 댓글 삭제 API 호출
 * 설명 : 게시글 ID와 댓글 ID로 삭제 대상을 지정하고 인증 쿠키를 전달합니다.
 */
export const deleteComment = (postId, commentId) => {
    const result = requestJson(
        `${getServerUrl()}/v1/posts/${postId}/comments/${commentId}`,
        {
            method: 'DELETE',
            credentials: 'include',
        },
    );
    return result;
};

/*
 * 기능 : 댓글 내용 수정 API 호출
 * 설명 : 수정한 댓글 데이터를 JSON으로 전송합니다.
 */
export const updateComment = (postId, commentId, commentContent) => {
    const result = requestJson(
        `${getServerUrl()}/v1/posts/${postId}/comments/${commentId}`,
        {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify(commentContent),
        },
    );
    return result;
};
