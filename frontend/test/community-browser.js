async (page) => {
    const base = 'http://localhost:18180';
    const communities = [{ id: 0, name: '자유게시판', description: '기존 게시글' }, { id: 1, name: '주술회전', description: '주술회전 팬들의 공간' }, { id: 2, name: '하이큐', description: '배구 이야기' }];
    await page.route('**/server-api/v1/**', async route => {
        const request = route.request();
        const url = new URL(request.url());
        let data = null;
        if (url.pathname.endsWith('/auth/check')) data = { userId: 1, idx: 1, profileImageUrl: null };
        else if (url.pathname.endsWith('/communities') && request.method() === 'POST') {
            const body = request.postDataJSON();
            data = { id: 3, ...body };
            communities.push(data);
        } else if (url.pathname.endsWith('/communities')) data = communities.filter(c => c.name.includes(url.searchParams.get('keyword') || ''));
        else if (/\/communities\/\d+$/.test(url.pathname)) data = communities.find(c => c.id === Number(url.pathname.split('/').pop()));
        else if (url.pathname.endsWith('/posts') || url.pathname.endsWith('/posts/search')) data = [];
        await route.fulfill({ status: request.method() === 'POST' ? 201 : 200, contentType: 'application/json', body: JSON.stringify({ data }) });
    });
    const assert = (condition, message) => { if (!condition) throw new Error(message); };
    await page.goto(base);
    assert(await page.getByRole('heading', { name: /좋아하는 애니/ }).count() === 1, 'landing must show discovery');
    await page.getByRole('searchbox', { name: '커뮤니티 검색', exact: true }).fill('주술회전');
    await page.getByRole('button', { name: '검색', exact: true }).click();
    await page.getByRole('link', { name: /주술회전/ }).click();
    await page.waitForURL('**/community/1');
    await page.getByRole('heading', { name: '주술회전', exact: true }).waitFor();
    await page.getByRole('searchbox', { name: '커뮤니티 검색', exact: true }).fill('하이큐');
    await page.getByRole('button', { name: '검색', exact: true }).click();
    await page.getByRole('link', { name: /하이큐/ }).click();
    await page.waitForURL('**/community/2');
    await page.getByRole('heading', { name: '하이큐', exact: true }).waitFor();
    await page.getByRole('button', { name: '+ 커뮤니티 직접 만들기', exact: true }).click();
    await page.getByRole('textbox', { name: '커뮤니티 이름', exact: true }).fill('진격의 거인');
    await page.getByRole('button', { name: '만들고 입장하기', exact: true }).click();
    await page.waitForURL('**/community/3');
    await page.getByRole('heading', { name: '진격의 거인', exact: true }).waitFor();
    await page.setViewportSize({ width: 390, height: 844 });
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), 'mobile page must not overflow');
    await page.unroute('**/server-api/v1/**');
    return { passed: ['landing search', 'enter community', 'switch community', 'create and enter', 'mobile width'] };
}
