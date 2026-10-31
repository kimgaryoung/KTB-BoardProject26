async (page) => {
    await page.unroute('**/server-api/v1/**');
    try {
        await page.route('**/server-api/v1/**', async route => {
            const url = route.request().url();
            if (url.endsWith('/communities/7')) return route.abort('failed');
            if (url.endsWith('/auth/check')) return route.fulfill({ status: 401, body: '' });
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: {
                id: 123, communityId: 7, title: '정상 게시글', content: '커뮤니티 이름 조회가 실패해도 본문은 보여야 합니다.',
                createdAt: '2026-10-30T09:00:00', nickname: '팬', profileImage: null,
                postImageUrl: null, isLiked: false, likeCount: 0, viewCount: 0, commentCount: 0,
                userId: 1, writerId: 1, comments: [],
            } }) });
        });
        await page.goto('http://localhost:18180/post/123');
        await page.getByRole('heading', { name: '정상 게시글', exact: true }).waitFor({ timeout: 2000 });
        await page.getByText('커뮤니티 이름 조회가 실패해도 본문은 보여야 합니다.', { exact: true }).waitFor();
        if (await page.locator('#communityLink').getAttribute('href') !== '/community/7') throw new Error('fallback link must preserve community');
        return { passed: ['post detail survives community metadata failure', 'community navigation fallback'] };
    } finally { await page.unroute('**/server-api/v1/**'); }
}
