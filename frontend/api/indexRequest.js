import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 커뮤니티 게시글의 다음 목록 조회
 * 설명 : 마지막 게시글 ID를 cursor로 전달합니다. communityId가 0인 자유게시판도 필터에서 제외하지 않습니다.
 */
export const getPosts = (cursor, communityId) => {
    const query = new URLSearchParams();
    if (cursor !== null && cursor !== undefined) query.set('cursor', cursor);
    if (communityId !== null && communityId !== undefined) query.set('communityId', communityId);
    const result = requestJson(`${getServerUrl()}/v1/posts?${query}`, {
        credentials: 'include',
    });
    return result;
};

/*
 * 기능 : 커뮤니티 안에서 게시글 검색
 * 설명 : 검색어와 offset·limit, 선택한 커뮤니티 ID를 전달해 제목·내용 검색 결과를 요청합니다.
 */
export const searchPosts = (keyword, offset = 0, limit = 5, sort = 'recent', communityId) => {
    const query = new URLSearchParams({
        keyword,
        offset,
        limit,
        sort,
    });
    if (communityId !== null && communityId !== undefined) query.set('communityId', communityId);
    const result = requestJson(
        `${getServerUrl()}/v1/posts/search?${query.toString()}`,
        {
            credentials: 'include',
        },
    );
    return result;
};
