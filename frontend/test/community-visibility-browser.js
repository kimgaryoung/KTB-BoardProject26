async page => {
    const base = 'http://localhost:8080';
    await page.unroute('**/server-api/v1/**');
    page.setDefaultTimeout(6000);
    const login = await page.request.post(`${base}/server-api/v1/auth/login`, { data: { email: 'community-demo@example.test', password: 'DemoCommunity1!' } });
    if (!login.ok()) throw new Error('Demo login failed');
    const assert = (value, message) => { if (!value) throw new Error(message); };
    await page.request.delete(`${base}/server-api/v1/communities/1/hidden`);
    await page.request.delete(`${base}/server-api/v1/communities/2/hidden`);
    try {
        await page.goto(base);
        await page.getByRole('button', { name: '마인크래프트 숨기기', exact: true }).click();
        await page.locator('.communityVisibilityStatus').filter({ hasText: '숨겼어요' }).waitFor();
        assert(await page.locator('.communityResultsList a[href="/community/1"]').count() === 0, 'hidden room must leave discovery');
        assert(await page.evaluate(() => document.activeElement === document.querySelector('.communityHiddenToggle')), 'focus must move to the hidden-list toggle after the focused hide button is removed');
        await page.reload();
        await page.locator('.communityResultsList a[href="/community/2"]').waitFor();
        assert(await page.locator('.communityResultsList a[href="/community/1"]').count() === 0, 'hide preference must survive reload');
        await page.getByRole('button', { name: '숨긴 커뮤니티 보기', exact: true }).click();
        await page.getByRole('button', { name: '마인크래프트 다시 표시', exact: true }).waitFor();
        await page.getByRole('searchbox', { name: '커뮤니티 검색', exact: true }).fill('마인크래프트');
        await page.getByRole('button', { name: '검색', exact: true }).click();
        await page.getByRole('button', { name: '마인크래프트 다시 표시', exact: true }).click();
        await page.locator('.communityVisibilityStatus').filter({ hasText: '다시 표시' }).waitFor();
        assert(await page.locator('.communityResultsList a[href="/community/1"]').count() === 0, 'restored room must leave hidden list');
        await page.getByRole('button', { name: '전체 커뮤니티 보기', exact: true }).click();
        await page.getByRole('link', { name: /마인크래프트/ }).waitFor();
        await page.goto(`${base}/community/1`);
        await page.getByRole('button', { name: '이 커뮤니티 숨기기', exact: true }).click();
        await page.waitForURL('**/html/index.html');
        await page.locator('.communityResultsList a[href="/community/2"]').waitFor();
        assert(await page.locator('.communityResultsList a[href="/community/1"]').count() === 0, 'current-room action must hide and return to discovery');
        await page.getByRole('button', { name: '숨긴 커뮤니티 보기', exact: true }).click();
        await page.getByRole('button', { name: '마인크래프트 다시 표시', exact: true }).click();
        await page.goto(base);
        await page.route('**/server-api/v1/communities/2/hidden', route => route.fulfill({ status: 503, contentType: 'application/json', body: '{}' }));
        await page.getByRole('button', { name: '주술회전 숨기기', exact: true }).click();
        await page.locator('.communityVisibilityStatus').filter({ hasText: '다시 시도' }).waitFor();
        assert(await page.locator('.communityResultsList a[href="/community/2"]').count() === 1, 'failed hide must keep the room visible');
        await page.unroute('**/server-api/v1/communities/2/hidden');
        return { passed: ['hide', 'refresh persistence', 'hidden search', 'restore', 'current-room hide', 'failed hide preserves room'] };
    } finally {
        await page.unroute('**/server-api/v1/communities/2/hidden');
        await page.request.delete(`${base}/server-api/v1/communities/1/hidden`);
        await page.request.delete(`${base}/server-api/v1/communities/2/hidden`);
    }
}
