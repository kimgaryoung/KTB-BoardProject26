import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 게시글과 첨부 사진의 multipart 구성
 * 설명 : 본문 JSON을 request 파트에 넣고 선택한 사진이 있을 때만 image 파트를 추가합니다.
 */
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

/*
 * 기능 : 로그인 사용자의 게시글 생성 요청
 * 설명 : 선택한 커뮤니티와 본문·사진을 인증 쿠키와 함께 전송합니다.
 */
export const createPost = (request, image) => {
    const result = requestJson(`${getServerUrl()}/v1/posts`, {
        method: 'POST',
        body: createPostFormData(request, image),
        credentials: 'include',
    });
    return result;
};

/*
 * 기능 : 게시글 본문과 사진 수정 요청
 * 설명 : 새 사진이 있으면 multipart를 사용하고, 본문 수정이나 사진 제거만 요청할 때는 JSON을 사용합니다.
 */
export const updatePost = (postId, request, image = null) => {
    const options = image ? { body: createPostFormData(request, image) } : {
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
    };
    const result = requestJson(`${getServerUrl()}/v1/posts/${postId}`, {
        method: 'PATCH',
        ...options,
        credentials: 'include',
    });

    return result;
};

/*
 * 기능 : 수정할 게시글의 기존 내용 조회
 * 설명 : 수정 화면을 채우기 위해 게시글 상세 정보를 요청합니다.
 */
export const getBoardItem = postId => {
    const result = requestJson(getServerUrl() + `/v1/posts/${postId}`, {
        method: 'GET',
        credentials: 'include',
    });

    return result;
};
