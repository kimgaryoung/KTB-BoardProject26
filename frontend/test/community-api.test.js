import { test } from 'node:test';
import assert from 'node:assert/strict';
import { getPosts, searchPosts } from '../api/indexRequest.js';

test('community post list and search preserve the selected community, including free board 0', async () => {
    const urls = [];
    const previousFetch = globalThis.fetch;
    globalThis.fetch = async url => {
        urls.push(new URL(url, 'http://localhost'));
        return new Response(JSON.stringify({ data: [] }), { headers: { 'content-type': 'application/json' } });
    };
    try {
        await getPosts(42, 7);
        await searchPosts('하이큐', 5, 5, 'recent', 7);
        await getPosts(null, 0);
        assert.equal(urls[0].searchParams.get('communityId'), '7');
        assert.equal(urls[0].searchParams.get('cursor'), '42');
        assert.equal(urls[1].searchParams.get('communityId'), '7');
        assert.equal(urls[1].searchParams.get('keyword'), '하이큐');
        assert.equal(urls[2].searchParams.get('communityId'), '0');
    } finally {
        globalThis.fetch = previousFetch;
    }
});
