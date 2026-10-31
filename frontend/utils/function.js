import { loginUrl, safeReturnPath } from './authNavigation.js';

export const DEFAULT_PROFILE_IMAGE = '/public/image/profile/villager.png';

/*
 * 기능 : API 기본 주소 선택
 * 설명 : 서버가 주입한 실행 환경 설정을 우선 사용하고, 설정이 없으면 기본 API 경로를 반환합니다.
 */
export const getServerUrl = () => {
    const configUrl =
        typeof window !== 'undefined' &&
        window.__APP_CONFIG__ &&
        window.__APP_CONFIG__.API_BASE_URL
            ? String(window.__APP_CONFIG__.API_BASE_URL).trim()
            : '';

    if (configUrl) {
        return configUrl.replace(/\/+$/, '');
    }

    return '/api';
};

/*
 * 기능 : 이미지 표시 주소 변환
 * 설명 : HTTP 이미지 주소는 유지하고 서버의 상대 이미지 경로에는 API 주소를 붙입니다. 이미지가 없으면 기본값을 사용합니다.
 */
export const resolveImageUrl = (url, fallback = null) => {
    if (!url) return fallback;
    if (/^https?:\/\//i.test(url)) return url;
    return `${getServerUrl()}${url}`;
};

/*
 * 기능 : 현재 로그인 상태 조회
 * 설명 : 인증 쿠키로 현재 사용자를 확인하고 화면에 로그인 상태 클래스를 반영합니다. 공통 배경은 로그인 여부와 관계없이 표시됩니다.
 */
export const serverSessionCheck = async () => {
    const res = await fetch(`${getServerUrl()}/v1/auth/check`, {
        method: 'GET',
        credentials: 'include',
    });
    if (typeof document !== 'undefined') {
        document.body.classList.toggle('isAuthenticated', res.ok);
    }
    return res;
};

/*
 * 기능 : 로그인이 필요한 화면 접근 검사
 * 설명 : 세션이 유효할 때만 응답을 반환하고, 비로그인 또는 조회 실패 시 복귀 경로를 보존해 로그인 화면으로 이동합니다.
 */
export const authCheck = async (returnTo = null) => {
    try {
        const HTTP_OK = 200;
        const response = await serverSessionCheck();
        if (!response || response.status !== HTTP_OK) {
            location.href = loginUrl(returnTo);
            return null;
        }
        return response;
    } catch (e) {
        location.href = loginUrl(returnTo);
        return null;
    }
};

/*
 * 기능 : 로그인 사용자의 인증 화면 재진입 처리
 * 설명 : 이미 로그인한 사용자는 로그인·회원가입 화면 대신 검증된 복귀 경로로 이동합니다.
 */
export const authCheckReverse = async (returnTo = '/') => {
    const response = await serverSessionCheck();
    if (response && response.ok) {
        location.href = safeReturnPath(returnTo);
        return true;
    }
    return false;
};
// 이메일 유효성 검사
/*
 * 기능 : 이메일 형식 검사
 * 설명 : 이메일의 기본 주소 형식을 검사합니다. 하이픈과 더하기 기호가 있는 계정도 입력할 수 있습니다.
 */
export const validEmail = email => {
    const REGEX = /^[^\s@]+@[^\s@]+\.[a-zA-Z]{2,}$/;
    return REGEX.test(email);
};

/*
 * 기능 : 비밀번호 형식 검사
 * 설명 : 8~20자 길이와 영문 대문자·소문자·숫자·허용 특수문자 포함 여부를 검사합니다.
 */
export const validPassword = password => {
    const REGEX =
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/;
    return REGEX.test(password);
};

/*
 * 기능 : 닉네임 형식 검사
 * 설명 : 한글·영문·숫자로 구성된 2~10자 닉네임인지 확인합니다.
 */
export const validNickname = nickname => {
    const REGEX = /^[가-힣a-zA-Z0-9]{2,10}$/;
    return REGEX.test(nickname);
};

/*
 * 기능 : 공통 요소를 화면 맨 앞에 삽입
 * 설명 : 헤더 등의 요소를 부모 요소의 첫 번째 자식으로 추가합니다.
 */
export const prependChild = (parent, child) => {
    parent.insertBefore(child, parent.firstChild);
};

/**
 *
 * @param {File} file  이미지 파일
 * @param {boolean} isHigh? : true면 origin, false면  1/4 사이즈
 * @returns
 */
/*
 * 기능 : 이미지 파일의 미리보기 데이터 변환
 * 설명 : 파일을 읽어 캔버스에 그린 뒤 원본 또는 가로·세로 1/4 크기의 데이터 URL로 반환합니다.
 */
export const fileToBase64 = (file, isHigh) => {
    return new Promise((resolve, reject) => {
        const size = isHigh ? 1 : 4;
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = e => {
            const img = new Image();
            img.src = e.target.result;
            img.onload = () => {
                const width = img.width / size;
                const height = img.height / size;
                const elem = document.createElement('canvas');
                elem.width = width;
                elem.height = height;
                const ctx = elem.getContext('2d');
                ctx.drawImage(img, 0, 0, width, height);
                resolve(ctx.canvas.toDataURL());
            };
            img.onerror = e => {
                reject(e);
            };
        };
        reader.onerror = e => {
            reject(e);
        };
    });
};

/**
 *
 * @param {string} param
 * @returns
 */
/*
 * 기능 : 화면 주소의 쿼리 파라미터 조회
 * 설명 : 현재 URL에서 지정한 파라미터 값을 읽고, 없으면 null을 반환합니다.
 */
export const getQueryString = param => {
    const params = new URLSearchParams(window.location.search);
    return params.get(param);
};

/*
 * 기능 : 날짜와 시간의 두 자리 숫자 표시
 * 설명 : 한 자리 숫자 앞에 0을 붙여 날짜·시간 표시 형식을 맞춥니다.
 */
export const padTo2Digits = number => {
    return number.toString().padStart(2, '0');
};
