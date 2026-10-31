import express from 'express';
import dotenv from 'dotenv';
import fetch from 'node-fetch';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const app = express();

dotenv.config();

const port = Number(process.env.PORT) || 8080;
const backendBaseUrl = (
    process.env.BACKEND_BASE_URL || 'http://localhost:3000'
).replace(/\/+$/, '');

// 현재 파일의 URL에서 디렉토리 경로를 추출
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

/*
 * 기능 : API 요청 본문 수신
 * 설명 : JSON과 multipart 본문을 원시 바이트로 받아 프록시 전달 중 사진 파일이 변형되지 않게 합니다.
 */
app.use(
    '/server-api',
    express.raw({
        type: '*/*',
        limit: '10mb',
    }),
);

/*
 * 기능 : 백엔드 API 프록시
 * 설명 : 요청 메서드·본문·인증 쿠키를 백엔드로 전달하고 응답 상태·본문·쿠키를 프론트 주소에서 반환합니다.
 */
app.use('/server-api', async (req, res) => {
    const targetPath = req.originalUrl.replace(/^\/server-api/, '');
    const targetUrl = `${backendBaseUrl}${targetPath}`;
    const headers = { ...req.headers };

    delete headers.host;
    delete headers.connection;
    delete headers['content-length'];

    try {
        const backendResponse = await fetch(targetUrl, {
            method: req.method,
            headers,
            body: ['GET', 'HEAD'].includes(req.method) ? undefined : req.body,
            redirect: 'manual',
        });

        backendResponse.headers.forEach((value, key) => {
            if (
                [
                    'connection',
                    'content-encoding',
                    'content-length',
                    'transfer-encoding',
                ].includes(key.toLowerCase())
            ) {
                return;
            }
            res.setHeader(key, value);
        });

        const setCookieHeaders = backendResponse.headers.raw()['set-cookie'];
        if (setCookieHeaders) {
            res.setHeader('set-cookie', setCookieHeaders);
        }

        const buffer = Buffer.from(await backendResponse.arrayBuffer());
        res.status(backendResponse.status).send(buffer);
    } catch (error) {
        console.error('API proxy failed:', error);
        res.status(502).json({
            message: 'API 프록시 요청에 실패했습니다.',
            data: null,
        });
    }
});

app.use(express.static(__dirname));

/*
 * 기능 : 브라우저 실행 환경 설정 제공
 * 설명 : 환경변수의 API 주소를 JavaScript 설정으로 전달하고 캐시를 막아 변경 사항을 바로 반영합니다.
 */
app.get('/config.js', (req, res) => {
    const apiBaseUrl = process.env.API_BASE_URL || '/server-api';
    res.set('Cache-Control', 'no-store');
    res.type('application/javascript').send(
        `window.__APP_CONFIG__ = ${JSON.stringify({
            API_BASE_URL: apiBaseUrl,
        })};`,
    );
});

/*
 * 기능 : 첫 화면 경로 연결
 * 설명 : 루트 주소로 들어오면 마인크래프트 이미지가 있는 홈 화면으로 이동합니다.
 */
app.get('/', (req, res) => {
    res.redirect('/html/index.html');
});

/*
 * 기능 : 게시글 상세 화면 경로 연결
 * 설명 : 게시글 ID가 포함된 주소에 공통 상세 HTML을 제공하고 실제 내용은 브라우저에서 조회합니다.
 */
app.get('/post/:postId', (req, res) => {
    res.sendFile(`${__dirname}/html/board.html`);
});

/*
 * 기능 : 예전 커뮤니티 주소 호환
 * 설명 : 개별 작품 게시판 대신 마인크래프트 첫 화면으로 이동합니다.
 */
app.get('/community/:communityId', (req, res) => {
    res.redirect('/');
});

app.listen(port, () => {
    console.log(`Server is running on port ${port}`);
});
