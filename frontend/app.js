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

app.use(
    '/server-api',
    express.raw({
        type: '*/*',
        limit: '10mb',
    }),
);

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

app.get('/config.js', (req, res) => {
    const apiBaseUrl = process.env.API_BASE_URL || '/server-api';
    res.set('Cache-Control', 'no-store');
    res.type('application/javascript').send(
        `window.__APP_CONFIG__ = ${JSON.stringify({
            API_BASE_URL: apiBaseUrl,
        })};`,
    );
});

app.get('/', (req, res) => {
    res.redirect('/html/index.html');
});

app.get('/post/:postId', (req, res) => {
    res.sendFile(`${__dirname}/html/board.html`);
});

app.listen(port, () => {
    console.log(`Server is running on port ${port}`);
});
