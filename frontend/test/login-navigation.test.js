import { test } from 'node:test';
import assert from 'node:assert/strict';
import * as navigation from '../utils/authNavigation.js';
import { validEmail } from '../utils/function.js';

test('login return accepts local application paths and rejects remote/ambiguous targets', () => {
    assert.equal(navigation.safeReturnPath('/html/board-write.html?communityId=2'), '/html/board-write.html?communityId=2');
    assert.equal(navigation.safeReturnPath('/community/1'), '/community/1');
    for (const path of [null, '', 'https://evil.example/', '//evil.example/', '/a/..//evil.example/path', '/a/%2e%2e//evil.example/path', '/a/../%2fevil.example/path', '/\\evil.example/', '/%5cevil.example/', '/\n/evil.example/', 'javascript:alert(1)', '/html/login.html?next=/html/login.html']) {
        assert.equal(navigation.safeReturnPath(path), '/html/index.html', String(path));
    }
    assert.equal(navigation.loginUrl('/html/board-write.html?communityId=2'), '/html/login.html?next=%2Fhtml%2Fboard-write.html%3FcommunityId%3D2');
});

test('login accepts the real demo account email without removing typed characters', () => {
    assert.equal(validEmail('community-demo@example.test'), true);
    assert.equal(validEmail('fan+anime@example.co.kr'), true);
    for (const invalid of ['fan', 'fan@@example.com', 'fan@example', 'fan name@example.com']) assert.equal(validEmail(invalid), false);
});
