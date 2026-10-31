import { test } from 'node:test';
import assert from 'node:assert/strict';
import BoardItem from '../component/board/boardItem.js';
import { updatePost } from '../api/board-writeRequest.js';

test('post card shows an optional thumbnail and escapes title/name text', () => {
    const card = BoardItem(1, '2026-10-30T09:00:00', '<img src=x onerror=alert(1)>', 0, null, '<b>writer</b>', 0, 0, '/v1/images/posts/photo.png');
    assert.match(card, /class="postThumbnail"/);
    assert.match(card, /src="\/api\/v1\/images\/posts\/photo.png"/);
    assert.match(card, /&lt;img src=x onerror=alert\(1\)&gt;/);
    assert.doesNotMatch(card, /<b>writer<\/b>/);
    const noImage = BoardItem(1, '2026-10-30T09:00:00', '글', 0, null, '작성자', 0, 0);
    assert.doesNotMatch(noImage, /postThumbnail/);
});

test('image replacement uses multipart while text-only updates preserve JSON compatibility', async () => {
    const originalFetch = globalThis.fetch;
    const calls = [];
    globalThis.fetch = async (url, options) => {
        calls.push({ url, options });
        return new Response(JSON.stringify({ data: null }), { headers: { 'content-type': 'application/json' } });
    };
    try {
        const image = new File(['sample'], 'photo.png', { type: 'image/png' });
        await updatePost(7, { title: '글', content: '내용' }, image);
        assert.ok(calls[0].options.body instanceof FormData);
        assert.equal(calls[0].options.body.get('image').name, 'photo.png');
        assert.deepEqual(JSON.parse(await calls[0].options.body.get('request').text()), { title: '글', content: '내용' });
        await updatePost(7, { title: '글', content: '내용', removeImage: true });
        assert.equal(calls[1].options.headers['Content-Type'], 'application/json');
        assert.equal(JSON.parse(calls[1].options.body).removeImage, true);
    } finally { globalThis.fetch = originalFetch; }
});
