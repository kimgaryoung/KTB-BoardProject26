const DEFAULT_RETURN_PATH = '/html/index.html';
const PARSING_ORIGIN = 'https://app.invalid';

/*
 * 기능 : 로그인 후 복귀 경로 검증
 * 설명 : 사이트 내부 경로만 반환합니다. 외부 주소, 경로 정규화 후 이중 슬래시, 제어 문자와 로그인 화면 반복 이동은 기본 화면으로 대체합니다.
 */
export const safeReturnPath = value => {
    if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || /[\\\u0000-\u0020\u007f]/.test(value)) return DEFAULT_RETURN_PATH;
    try {
        const decoded = decodeURIComponent(value);
        if (decoded.startsWith('//') || /[\\\u0000-\u001f\u007f]/.test(decoded)) return DEFAULT_RETURN_PATH;
        const url = new URL(value, PARSING_ORIGIN);
        const pathname = decodeURIComponent(url.pathname);
        if (url.origin !== PARSING_ORIGIN || url.pathname.startsWith('//') || pathname.startsWith('//') || ['/html/login.html', '/html/signup.html'].includes(pathname)) return DEFAULT_RETURN_PATH;
        return url.pathname + url.search + url.hash;
    } catch { return DEFAULT_RETURN_PATH; }
};

/*
 * 기능 : 복귀 주소가 포함된 로그인 링크 생성
 * 설명 : 작성 중이던 커뮤니티로 돌아올 수 있도록 검증된 경로를 next 파라미터에 인코딩합니다.
 */
export const loginUrl = returnTo => returnTo
    ? `/html/login.html?next=${encodeURIComponent(safeReturnPath(returnTo))}`
    : '/html/login.html';
