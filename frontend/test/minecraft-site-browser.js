async page => {
    const base = 'http://localhost:8080';
    const assert = (value, message) => { if (!value) throw new Error(message); };
    await page.unroute('**/server-api/v1/**');
    await page.context().clearCookies();
    await page.setViewportSize({ width: 1440, height: 900 });
    const errors = [];
    const onError = error => errors.push(error.message);
    page.on('pageerror', onError);
    let createdId = null;
    try {
        await page.goto(`${base}/html/posts.html`);
        await page.getByRole('heading', { name: '마인크래프트 게시판', exact: true }).waitFor();
        await page.getByRole('link', { name: '로그인 후 글 작성', exact: true }).waitFor();
        await page.locator('.boardItem').first().waitFor();
        assert(await page.locator('#communityExplorer, .communityCreateForm, .communityHiddenToggle').count() === 0, 'no community selector/create/hide UI');
        assert(!await page.locator('body').innerText().then(text => text.includes('주술회전')), 'other-board posts must not appear');
        const boardResponse = await page.request.get(`${base}/server-api/v1/site/board`);
        const board = (await boardResponse.json()).data;
        assert(board.name === '마인크래프트', 'canonical board from server');
        const search = page.getByRole('searchbox', { name: '게시글 검색', exact: true });
        await search.fill('사진');
        await page.getByRole('button', { name: '검색', exact: true }).click();
        await page.getByRole('heading', { name: '마인크래프트 사진 첨부 예시', exact: true }).waitFor();
        await search.fill('없는글-마크-전용-확인');
        await page.getByRole('button', { name: '검색', exact: true }).click();
        await page.getByText('검색 결과가 없어요. 다른 검색어로 찾아보세요.').waitFor();
        await page.goto(`${base}/community/2`);
        await page.waitForURL('**/html/index.html');
        await page.getByRole('link', {name:'게시판 바로가기',exact:true}).click();
        await page.waitForURL('**/html/posts.html');
        await page.getByRole('link', { name: '로그인 후 글 작성', exact: true }).click();
        await page.waitForURL('**/html/login.html?next=*');
        assert(new URL(page.url()).searchParams.get('next') === '/html/board-write.html', 'no board selection in login return');
        await page.locator('#id').fill('community-demo@example.test');
        await page.locator('#pw').fill('DemoCommunity1!');
        await page.getByRole('button', { name: '로그인', exact: true }).click();
        await page.waitForURL('**/html/board-write.html');
        await page.waitForFunction(() => !document.querySelector('#title').disabled);
        assert(await page.locator('#communityExplorer').count() === 0, 'editor has no selector');
        await page.locator('#title').fill('마크 전용 사이트 확인');
        await page.locator('#content').fill('마인크래프트 건축과 생존 이야기 테스트입니다.');
        await page.locator('#image').setInputFiles('/Users/garyeong/IdeaProjects/KTB-BoardProject26/frontend/public/background/minecraft-landscape.png');
        await page.waitForFunction(() => !document.querySelector('#submit').disabled);
        await page.getByRole('button', { name: '완료', exact: true }).click();
        await page.waitForURL('**/post/*');
        createdId = page.url().split('/').pop();
        await page.waitForFunction(() => document.querySelector('.contentImg img')?.naturalWidth > 0);
        const saved = await page.request.get(`${base}/server-api/v1/posts/${createdId}`);
        assert((await saved.json()).data.communityId === board.id, 'new post saved only in Minecraft');
        await page.getByRole('button', { name: '수정', exact: true }).first().click();
        await page.waitForFunction(() => !document.querySelector('#title').disabled);
        await page.locator('#content').fill('수정해도 마인크래프트 게시판과 사진이 유지됩니다.');
        await page.getByRole('button', { name: '수정하기', exact: true }).click();
        await page.waitForURL('**/post/*');
        await page.getByText('수정해도 마인크래프트 게시판과 사진이 유지됩니다.', { exact: true }).waitFor();
        await page.getByRole('link', { name: '← 마인크래프트 게시판으로', exact: true }).click();
        await page.getByRole('heading', { name: '마크 전용 사이트 확인', exact: true }).waitFor();
        await page.waitForFunction(() => [...document.querySelectorAll('.postThumbnail')].some(img => img.naturalWidth > 0));
        assert((await page.request.get(`${base}/server-api/v1/posts/4`)).status() === 404, 'old anime post blocked');
        assert((await page.request.post(`${base}/server-api/v1/communities`, { data: { name: '새 커뮤니티' } })).status() === 410, 'community creation removed');
        for (const size of [{ width: 320, height: 568 }, { width: 390, height: 844 }, { width: 768, height: 1024 }]) {
            await page.setViewportSize(size);
            assert(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth), 'responsive layout without horizontal overflow');
        }
        assert(errors.length === 0, `page errors: ${errors.join(', ')}`);
        return { passed: ['Minecraft home', 'no selection/create/hide UI', 'post search', 'legacy URL redirect', 'login-required writing', 'photo upload and thumbnail', 'edit preserves photo and board', 'other board blocked', 'management API removed', 'mobile layout'] };
    } finally {
        if (createdId) {
            const response = await page.request.delete(`${base}/server-api/v1/posts/${createdId}`);
            assert(response.ok(), 'temporary test post cleanup');
        }
        await page.request.post(`${base}/server-api/v1/auth/logout`);
        await page.setViewportSize({ width: 1440, height: 900 });
        await page.goto(`${base}/html/posts.html`);
        await page.getByRole('heading', { name: '마인크래프트 사진 첨부 예시', exact: true }).waitFor();
        await page.screenshot({ path: '/tmp/ktb-minecraft-only-home.png', fullPage: true });
        page.off('pageerror', onError);
    }
}
