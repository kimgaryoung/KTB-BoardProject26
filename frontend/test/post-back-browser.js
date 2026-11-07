async (page, { base = 'http://localhost:8080' } = {}) => {
    const assertPath = (currentPage, expected) => {
        const actual = new URL(currentPage.url()).pathname;
        if (actual !== expected) throw new Error(`Expected back navigation to ${expected}, got ${actual}`);
    };
    const clickBack = async currentPage => {
        const previous = currentPage.url();
        await Promise.all([
            currentPage.waitForURL(url => url.href !== previous),
            currentPage.locator('header .back').click(),
        ]);
    };

    await page.goto(`${base}/html/posts.html`);
    await page.locator('.boardItem').first().click();
    await page.waitForURL('**/post/*');
    const detailPath = new URL(page.url()).pathname;
    const postId = detailPath.split('/').at(-1);
    await clickBack(page);
    assertPath(page, '/html/posts.html');

    for (const entry of [detailPath, `/html/board.html?id=${encodeURIComponent(postId)}`]) {
        const direct = await page.context().newPage();
        try {
            await direct.setViewportSize({ width: 390, height: 844 });
            await direct.goto(`${base}${entry}`);
            await clickBack(direct);
            assertPath(direct, '/html/posts.html');
        } finally {
            await direct.close();
        }
    }

    await clickBack(page);
    if (!['/', '/html/index.html'].includes(new URL(page.url()).pathname)) {
        throw new Error('The post list back button must still return home');
    }
    return { passed: ['detail back returns to list', 'direct post URL back returns to list', 'legacy detail URL back returns to list', 'list back returns home'] };
}
