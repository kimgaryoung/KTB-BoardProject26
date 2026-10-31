/*
 * 기능 : API 응답의 JSON 안전 파싱
 * 설명 : JSON 응답만 읽으며, 빈 응답이나 파싱 실패는 null로 처리합니다.
 */
export const parseJsonSafe = async response => {
    const contentType = response.headers.get('content-type') || '';
    if (!contentType.includes('application/json')) {
        return null;
    }
    try {
        return await response.json();
    } catch (error) {
        return null;
    }
};

/*
 * 기능 : 공통 API 요청 및 응답 정리
 * 설명 : HTTP 성공 여부, 상태 코드, 오류 코드와 data를 같은 형태로 반환해 화면별 응답 처리를 통일합니다.
 */
export const requestJson = async (url, options = {}) => {
    const response = await fetch(url, options);
    const body = await parseJsonSafe(response);
    return {
        response,
        ok: response.ok,
        status: response.status,
        code: body && (body.code || body.message) ? body.code || body.message : null,
        data: body && Object.prototype.hasOwnProperty.call(body, 'data')
            ? body.data
            : null,
        body,
    };
};
