import { test } from 'node:test';
import assert from 'node:assert/strict';
import * as api from '../api/communityRequest.js';

test('hidden-mode discovery and preference mutations use authenticated scoped APIs', async () => {
    const calls = [];
    const original = globalThis.fetch;
    globalThis.fetch = async (url, options) => {
        calls.push({ url: new URL(url, 'http://localhost'), options });
        return new Response(JSON.stringify({ data: [] }), { headers: { 'content-type': 'application/json' } });
    };
    try {
        await api.searchCommunities('주술', 20, 20, true);
        assert.equal(calls[0].url.searchParams.get('hidden'), 'true');
        assert.equal(calls[0].url.searchParams.get('offset'), '20');
        await api.hideCommunity(7);
        await api.showCommunity(7);
        assert.equal(calls[1].url.pathname, '/api/v1/communities/7/hidden');
        assert.equal(calls[1].options.method, 'PUT');
        assert.equal(calls[2].options.method, 'DELETE');
        assert.ok(calls.every(call => call.options.credentials === 'include'));
    } finally { globalThis.fetch = original; }
});
