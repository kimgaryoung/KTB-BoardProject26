import Header from '../component/header/header.js';
import {
    authCheckReverse,
    prependChild,
    validEmail,
} from '../utils/function.js';
import { userLogin } from '../api/loginRequest.js';

const HTTP_OK = 200;
const MAX_PASSWORD_LENGTH = 8;

const loginData = {
    id: '',
    password: '',
};

const updateHelperText = (helperTextElement, message = '') => {
    helperTextElement.textContent = message;
};

const loginClick = async () => {
    const { id: email, password } = loginData;
    const helperTextElement = document.querySelector('.helperText');

    const { ok, status, code } = await userLogin(email, password);
    if (!ok) {
        updateHelperText(
            helperTextElement,
            code === 'INVALID_INPUT'
                ? '*입력값을 확인해주세요.'
                : '*입력하신 계정 정보가 정확하지 않았습니다.',
        );
        return;
    }

    if (status !== HTTP_OK) {
        updateHelperText(
            helperTextElement,
            '*입력하신 계정 정보가 정확하지 않았습니다.',
        );
        return;
    }
    updateHelperText(helperTextElement);

    location.href = '/html/index.html';
};

const observeSignupData = () => {
    const { id: email, password } = loginData;
    const button = document.querySelector('#login');
    const helperTextElement = document.querySelector('.helperText');

    const isValidEmail = validEmail(email);
    updateHelperText(
        helperTextElement,
        isValidEmail || !email
            ? ''
            : '*올바른 이메일 주소 형식을 입력해주세요. (예: example@example.com)',
    );

    button.disabled = !(
        email &&
        isValidEmail &&
        password &&
        password.length >= MAX_PASSWORD_LENGTH
    );
    button.style.backgroundColor = button.disabled ? '#ACA0EB' : '#7F6AEE';
};

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

    document
        .getElementById('id')
        .addEventListener('input', event => validateEmail(event.target));
};

const onChangeHandler = (event, uid) => {
    loginData[uid] = event.target.value;
    observeSignupData();
};

const validateEmail = input => {
    const regex = /^[A-Za-z0-9@.]+$/;
    if (!regex.test(input.value)) input.value = input.value.slice(0, -1);
};

let lottieInstance = null;
const lottieAnimation = type => {
    const container = document.getElementById('lottie-animation');
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

const init = async () => {
    await authCheckReverse();
    observeSignupData();
    prependChild(document.body, Header('커뮤니티', 0));
    eventSet();
    localStorage.clear();
};

init();
