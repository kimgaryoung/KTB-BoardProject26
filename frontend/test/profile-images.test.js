import { test } from 'node:test';
import assert from 'node:assert/strict';
import { uploadProfileImage } from '../utils/profileImage.js';
import { userModify } from '../api/modifyInfoRequest.js';
import { resolveImageUrl } from '../utils/function.js';
import BoardItem from '../component/board/boardItem.js';

const MAX_BYTES = 5 * 1024 * 1024;
const VILLAGER = '/public/image/profile/villager.png';

const jsonResponse = (status, data) => new Response(JSON.stringify({ message: 'test response', data }), {
    status,
    headers: { 'Content-Type': 'application/json' },
});

for (const [label, file] of [
    ['missing file', undefined],
    ['null file', null],
    ['empty file', new File([], 'empty.png', { type: 'image/png' })],
    ['oversized file', new File([new Uint8Array(MAX_BYTES + 1)], 'large.png', { type: 'image/png' })],
]) {
    test(`profile upload rejects ${label} before making an HTTP request`, async t => {
        const fetch = t.mock.method(globalThis, 'fetch', async () => {
            throw new Error('Invalid local input must not reach the network');
        });
        await assert.rejects(uploadProfileImage(file));
        assert.equal(fetch.mock.callCount(), 0);
    });
}

test('profile upload submits the actual file under profileImage with authentication cookies', async t => {
    const file = new File(['avatar bytes'], 'avatar.png', { type: 'image/png' });
    let submitted;
    t.mock.method(globalThis, 'fetch', async (url, options) => {
        submitted = { url, options };
        return jsonResponse(201, { profileImageUrl: '/v1/images/posts/avatar.png' });
    });

    assert.equal(await uploadProfileImage(file), '/v1/images/posts/avatar.png');
    assert.equal(submitted.url, '/api/v1/users/upload/profile-image');
    assert.equal(submitted.options.method, 'POST');
    assert.equal(submitted.options.credentials, 'include');
    assert.ok(submitted.options.body instanceof FormData);
    assert.deepEqual([...submitted.options.body.keys()], ['profileImage']);
    const uploaded = submitted.options.body.get('profileImage');
    assert.equal(uploaded.name, 'avatar.png');
    assert.equal(uploaded.type, 'image/png');
    assert.equal(await uploaded.text(), 'avatar bytes');
    assert.equal(submitted.options.headers?.['Content-Type'], undefined,
        'The browser must generate the multipart boundary');
});

test('profile upload accepts a file exactly at the 5 MiB limit', async t => {
    t.mock.method(globalThis, 'fetch', async (_url, options) => {
        assert.equal(options.body.get('profileImage').size, MAX_BYTES);
        return jsonResponse(201, { profileImageUrl: '/v1/images/posts/limit.png' });
    });
    const file = new File([new Uint8Array(MAX_BYTES)], 'limit.png', { type: 'image/png' });
    assert.equal(await uploadProfileImage(file), '/v1/images/posts/limit.png');
});

for (const [status, message] of [[400, /PNG/], [413, /5MB/], [500, /업로드에 실패/]]) {
    test(`profile upload reports HTTP ${status} instead of accepting its response URL`, async t => {
        t.mock.method(globalThis, 'fetch', async () => jsonResponse(status, {
            profileImageUrl: '/v1/images/posts/must-not-be-used.png',
        }));
        await assert.rejects(uploadProfileImage(new File(['image'], 'avatar.png')), message);
    });
}

for (const [label, data] of [['missing URL', {}], ['empty URL', { profileImageUrl: '' }], ['null data', null]]) {
    test(`profile upload rejects a successful response with ${label}`, async t => {
        t.mock.method(globalThis, 'fetch', async () => jsonResponse(201, data));
        await assert.rejects(uploadProfileImage(new File(['image'], 'avatar.png')));
    });
}

test('profile upload rejects a non-JSON success page', async t => {
    t.mock.method(globalThis, 'fetch', async () => new Response('<html>proxy page</html>', {
        headers: { 'Content-Type': 'text/html' },
    }));
    await assert.rejects(uploadProfileImage(new File(['image'], 'avatar.png')));
});

test('profile upload propagates a network failure to the calling screen', async t => {
    const failure = new TypeError('Network unavailable');
    t.mock.method(globalThis, 'fetch', async () => { throw failure; });
    await assert.rejects(uploadProfileImage(new File(['image'], 'avatar.png')), error => error === failure);
});

for (const [label, image] of [['replacement', '/v1/images/posts/new-avatar.png'], ['removal', null]]) {
    test(`profile ${label} sends the selected image value and nickname with cookies`, async t => {
        let submitted;
        t.mock.method(globalThis, 'fetch', async (url, options) => {
            submitted = { url, options };
            return jsonResponse(200, null);
        });
        assert.equal((await userModify({ nickname: '마크주민', profileImageUrl: image })).ok, true);
        assert.equal(submitted.url, '/api/v1/users/me');
        assert.equal(submitted.options.method, 'PUT');
        assert.equal(submitted.options.credentials, 'include');
        assert.equal(submitted.options.headers['Content-Type'], 'application/json');
        assert.deepEqual(JSON.parse(submitted.options.body), { nickname: '마크주민', profileImageUrl: image });
    });
}

test('a failed profile save stays failed for the editing screen', async t => {
    t.mock.method(globalThis, 'fetch', async () => jsonResponse(401, null));
    const result = await userModify({ nickname: '마크주민', profileImageUrl: null });
    assert.equal(result.ok, false);
    assert.equal(result.status, 401);
});

test('an author without a profile photo gets the local villager face', () => {
    const card = BoardItem(1, '2026-10-31T09:00:00', '마크 이야기', 0, null, '주민', 0, 0);
    assert.ok(card.includes(`src="${VILLAGER}" alt="작성자 프로필"`));
    assert.equal(resolveImageUrl(null, VILLAGER), '/public/image/profile/villager.png');
});

test('a custom author photo is preserved instead of being replaced by the default avatar', () => {
    const custom = 'https://images.example.test/avatar.png';
    const card = BoardItem(1, '2026-10-31T09:00:00', '마크 이야기', 0, custom, '주민', 0, 0);
    assert.ok(card.includes(`src="${custom}" alt="작성자 프로필"`));
    assert.equal(resolveImageUrl('/v1/images/posts/avatar.png', VILLAGER), '/api/v1/images/posts/avatar.png');
    assert.equal(resolveImageUrl(custom, VILLAGER), custom);
});
