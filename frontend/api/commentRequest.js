import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

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
