import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

const createPostFormData = (request, image) => {
    const formData = new FormData();
    formData.append(
        'request',
        new Blob([JSON.stringify(request)], { type: 'application/json' }),
    );

    if (image) {
        formData.append('image', image);
    }

    return formData;
};

export const createPost = (request, image) => {
    const result = requestJson(`${getServerUrl()}/v1/posts`, {
        method: 'POST',
        body: createPostFormData(request, image),
        credentials: 'include',
    });
    return result;
};

export const updatePost = (postId, request, image) => {
    const result = requestJson(`${getServerUrl()}/v1/posts/${postId}`, {
        method: 'PATCH',
        body: createPostFormData(request, image),
        credentials: 'include',
    });

    return result;
};

export const getBoardItem = postId => {
    const result = requestJson(getServerUrl() + `/v1/posts/${postId}`, {
        method: 'GET',
        credentials: 'include',
    });

    return result;
};
