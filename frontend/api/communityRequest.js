import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 커뮤니티 검색 API 호출
 * 설명 : 검색어와 offset·limit을 전달합니다. hidden이 참이면 로그인한 사용자가 숨긴 커뮤니티만 요청합니다.
 */
export const searchCommunities = (keyword = '', offset = 0, limit = 20, hidden = false) => {
    const query = new URLSearchParams({ keyword, offset, limit });
    if (hidden) query.set('hidden', 'true');
    return requestJson(`${getServerUrl()}/v1/communities?${query}`, { credentials: 'include' });
};
/*
 * 기능 : 커뮤니티 정보 조회 API 호출
 * 설명 : 선택한 커뮤니티의 이름과 소개를 요청합니다.
 */
export const getCommunity = id => requestJson(
    `${getServerUrl()}/v1/communities/${encodeURIComponent(id)}`, { credentials: 'include' },
);
/*
 * 기능 : 커뮤니티 생성 API 호출
 * 설명 : 이름과 소개를 JSON으로 보내며 인증 쿠키를 포함합니다.
 */
export const createCommunity = request => requestJson(`${getServerUrl()}/v1/communities`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request), credentials: 'include',
});

/*
 * 기능 : 내 커뮤니티 숨기기 API 호출
 * 설명 : 현재 계정의 목록과 검색에서 해당 커뮤니티를 숨기도록 요청합니다.
 */
export const hideCommunity = id => requestJson(`${getServerUrl()}/v1/communities/${encodeURIComponent(id)}/hidden`, {
    method: 'PUT', credentials: 'include',
});

/*
 * 기능 : 숨긴 커뮤니티 복구 API 호출
 * 설명 : 현재 계정의 숨김 설정을 삭제해 커뮤니티를 다시 표시하도록 요청합니다.
 */
export const showCommunity = id => requestJson(`${getServerUrl()}/v1/communities/${encodeURIComponent(id)}/hidden`, {
    method: 'DELETE', credentials: 'include',
});
