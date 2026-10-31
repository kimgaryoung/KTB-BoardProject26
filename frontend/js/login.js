import Header from '../component/header/header.js';
import { safeReturnPath } from '../utils/authNavigation.js';
import {
    authCheckReverse,
    prependChild,
    validEmail,
} from '../utils/function.js';
import { userLogin } from '../api/loginRequest.js';

const HTTP_OK = 200;
const MIN_PASSWORD_LENGTH = 8;
const returnPath = safeReturnPath(new URLSearchParams(window.location.search).get('next'));
let isSubmitting = false;

const loginData = {
    id: '',
    password: '',
};

/*
 * 기능 : 로그인 입력 안내 표시
 * 설명 : 로그인 오류나 이메일 형식 안내를 화면의 도움말 영역에 출력합니다.
 */
const updateHelperText = (helperTextElement, message = '') => {
    helperTextElement.textContent = message;
};

/*
 * 기능 : 로그인 제출 및 원래 화면 복귀
 * 설명 : 중복 제출을 막고 인증 성공 시 검증된 복귀 주소로 이동합니다. 실패하면 오류를 표시하고 다시 입력할 수 있게 합니다.
 */
const loginClick = async () => {
    if (isSubmitting || document.querySelector('#login').disabled) return;
    const { id: email, password } = loginData;
    const helperTextElement = document.querySelector('.helperText');
    isSubmitting = true;
    observeSignupData(true);
    try {
        const { ok, status, code } = await userLogin(email, password);
        if (!ok || status !== HTTP_OK) {
            updateHelperText(helperTextElement, code === 'INVALID_INPUT'
                ? '*입력값을 확인해주세요.' : '*입력하신 계정 정보가 정확하지 않았습니다.');
            return;
        }
        updateHelperText(helperTextElement);
        location.href = returnPath;
    } catch {
        updateHelperText(helperTextElement, '*서버에 연결하지 못했어요. 다시 시도해주세요.');
    } finally {
        isSubmitting = false;
        observeSignupData(true);
    }
};

/*
 * 기능 : 로그인 버튼 상태 갱신
 * 설명 : 이메일 형식과 비밀번호 길이를 확인해 버튼을 활성화합니다. 요청 중에는 버튼을 잠그고 필요하면 오류 문구를 유지합니다.
 */
const observeSignupData = (preserveMessage = false) => {
    const { id: email, password } = loginData;
    const button = document.querySelector('#login');
    const helperTextElement = document.querySelector('.helperText');

    const isValidEmail = validEmail(email);
    if (!preserveMessage) updateHelperText(
        helperTextElement,
        isValidEmail || !email
            ? ''
            : '*올바른 이메일 주소 형식을 입력해주세요. (예: example@example.com)',
    );

    button.disabled = isSubmitting || !(
        email &&
        isValidEmail &&
        password &&
        password.length >= MIN_PASSWORD_LENGTH
    );
    button.style.backgroundColor = button.disabled ? '#ACA0EB' : '#7F6AEE';
};

/*
 * 기능 : 로그인 입력과 제출 이벤트 연결
 * 설명 : 이메일·비밀번호 입력, 버튼 클릭과 Enter 키 제출 동작을 등록합니다.
 */
const eventSet = () => {
    document.getElementById('login').addEventListener('click', loginClick);

    document.addEventListener('keypress', event => {
        if (event.key === 'Enter') {
            loginClick();
        }
    });

    ['id', 'pw'].forEach(field => {
        const inputElement = document.getElementById(field);
        inputElement.addEventListener('input', event =>
            onChangeHandler(event, field === 'id' ? 'id' : 'password'),
        );

        if (field === 'id') {
            inputElement.addEventListener('focusout', event =>
                lottieAnimation(validEmail(event.target.value) ? 1 : 2),
            );
        }
    });
};

/*
 * 기능 : 로그인 입력 데이터 반영
 * 설명 : 이메일의 앞뒤 공백만 정리해 저장하고 입력한 하이픈 등의 문자는 삭제하지 않습니다.
 */
const onChangeHandler = (event, uid) => {
    loginData[uid] = uid === 'id' ? event.target.value.trim() : event.target.value;
    observeSignupData();
};

let lottieInstance = null;
/*
 * 기능 : 이메일 검사 결과 애니메이션 표시
 * 설명 : 선택 라이브러리가 있을 때만 애니메이션을 실행해 외부 스크립트가 없어도 로그인 폼을 사용할 수 있게 합니다.
 */
const lottieAnimation = type => {
    const container = document.getElementById('lottie-animation');
    if (!container || !window.lottie?.loadAnimation) return;
    const animationPaths = [
        '/public/check_anim.json',
        '/public/denied_anim.json',
    ];
    if (lottieInstance) lottieInstance.destroy();
    container.innerHTML = '';
    lottieInstance = window.lottie.loadAnimation({
        container,
        renderer: 'svg',
        loop: false,
        autoplay: true,
        path: animationPaths[type - 1],
    });
};

/*
 * 기능 : 로그인 화면 초기화
 * 설명 : 폼을 먼저 준비하고 글 작성 때문에 이동한 경우 안내를 표시합니다. 이미 로그인한 사용자는 복귀 화면으로 보냅니다.
 */
const init = async () => {
    prependChild(document.body, Header('마크안에 사람이 살잖아', 0));
    eventSet();
    observeSignupData();
    if (returnPath.startsWith('/html/board-write.html') || returnPath.startsWith('/html/board-modify.html')) {
        const notice = document.querySelector('#loginNotice');
        notice.textContent = '글을 작성하려면 로그인이 필요합니다. 로그인 후 작성 화면으로 이동합니다.';
        notice.hidden = false;
    }
    try { await authCheckReverse(returnPath); }
    catch { /* The login form remains usable if the initial session check fails. */ }
};

init();
