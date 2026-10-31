async (page) => {
    const base = 'http://localhost:18180';
    await page.unroute('**/server-api/v1/**');
    page.setDefaultTimeout(5000);
    let createMode = 'duplicate';
    let failSearch = true;
    const assert = (condition, message) => { if (!condition) throw new Error(message); };
    const response = (route, status, body) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    try {
        await page.route('**/server-api/v1/**', async route => {
            const request = route.request();
            const url = new URL(request.url());
            if (url.pathname.endsWith('/auth/check')) return response(route, 401, {});
            if (request.method() === 'POST') {
                if (createMode === 'duplicate') return response(route, 409, { code: 'COMMUNITY_ALREADY_EXISTS', communityId: 2 });
                if (createMode === 'offline') return response(route, 503, {});
                return response(route, 401, {});
            }
            const keyword = url.searchParams.get('keyword') || '';
            if (keyword === '주술') {
                await new Promise(resolve => setTimeout(resolve, 500));
                return response(route, 200, { data: [{ id: 1, name: '주술회전', description: '' }] });
            }
            if (keyword === '실패' && failSearch) return response(route, 503, {});
            if (keyword) return response(route, 200, { data: [{ id: 2, name: '<img src=x onerror=alert(1)>하이큐', description: '안전한 텍스트' }] });
            return response(route, 200, { data: [] });
        });
        await page.goto(base);
        const input = page.getByRole('searchbox', { name: '커뮤니티 검색', exact: true });
        const button = page.getByRole('button', { name: '검색', exact: true });
        await input.fill('주술');
        await button.click();
        await input.fill('하이큐');
        await button.click();
        await page.getByRole('link', { name: /하이큐/ }).waitFor();
        await page.waitForTimeout(600);
        assert(await page.getByRole('link', { name: /주술회전/ }).count() === 0, 'stale response must not replace current results');
        assert(await page.locator('.communityResultsList img').count() === 0, 'community names must render as text');

        await input.fill('실패');
        await button.click();
        await page.getByText('커뮤니티를 불러오지 못했어요.', { exact: false }).waitFor();
        failSearch = false;
        await button.click();
        await page.getByRole('link', { name: /하이큐/ }).waitFor();

        await page.getByRole('button', { name: '+ 커뮤니티 직접 만들기', exact: true }).click();
        await page.getByRole('textbox', { name: '커뮤니티 이름', exact: true }).fill('하이큐');
        const create = page.getByRole('button', { name: '만들고 입장하기', exact: true });
        await create.click();
        const duplicate = page.getByRole('link', { name: '이미 있는 커뮤니티로 입장하기 →', exact: true });
        await duplicate.waitFor();
        assert(await duplicate.getAttribute('href') === '/community/2', 'duplicate response must link to existing community');
        createMode = 'offline';
        await create.click();
        await page.getByText('만들지 못했어요.', { exact: false }).waitFor();
        assert(await create.isEnabled(), 'failed create must be retryable');
        createMode = 'anonymous';
        await create.click();
        await page.waitForURL('**/html/login.html');
        return { passed: ['stale search protection', 'text-safe names', 'search retry', 'duplicate link', 'create retry', 'creation login requirement'] };
    } finally { await page.unroute('**/server-api/v1/**'); }
}
