async page => {
    await page.unroute('**/server-api/v1/**');
    const rooms = Array.from({ length: 21 }, (_, i) => ({ id: i + 1, name: `Room ${i + 1}`, description: '' }));
    try {
        await page.route('**/server-api/v1/**', async route => {
            const request = route.request();
            const url = new URL(request.url());
            if (request.method() === 'PUT') return route.fulfill({ status: 503, contentType: 'application/json', body: '{}' });
            if (url.pathname.endsWith('/auth/check')) return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: { userId: 1, idx: 1, profileImageUrl: null } }) });
            const offset = Number(url.searchParams.get('offset') || 0);
            if (offset) await new Promise(resolve => setTimeout(resolve, 700));
            return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: rooms.slice(offset, offset + 20) }) });
        });
        await page.goto('http://localhost:8080/');
        await page.getByRole('button', { name: '더 보기', exact: true }).click();
        await page.getByRole('button', { name: 'Room 1 숨기기', exact: true }).click();
        await page.locator('.communityVisibilityStatus').filter({ hasText: '다시 시도' }).waitFor();
        await page.getByRole('link', { name: 'Room 21 이 커뮤니티에서 첫 이야기를 시작해보세요.', exact: true }).waitFor({ timeout: 3000 });
        return { passed: ['failed hide does not discard an in-flight next page'] };
    } finally { await page.unroute('**/server-api/v1/**'); }
}
