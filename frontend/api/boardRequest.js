import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

export const getPost = postId => {
    const result = requestJson(`${getServerUrl()}/v1/posts/${postId}`, {
        credentials: 'include',
    });
    return result;
};

export const deletePost = async postId => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${postId}`, {
        method: 'DELETE',
        credentials: 'include',
    });
    return result;
};

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

export const getComments = async postId => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${postId}/comments`, {
        credentials: 'include',
    });
    return result;
};

export const likePost = async postId => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${postId}/likes`, {
        method: 'POST',
        credentials: 'include',
    });
    return result;
};

export const unlikePost = async postId => {
    const result = await requestJson(`${getServerUrl()}/v1/posts/${postId}/likes`, {
        method: 'DELETE',
        credentials: 'include',
    });
    return result;
};
