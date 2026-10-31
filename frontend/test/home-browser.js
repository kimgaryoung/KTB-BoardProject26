async page => {
    const base = 'http://localhost:8080';
    const assert = (value, message) => { if (!value) throw new Error(message); };
    const errors = [];
    const onError = error => errors.push(error.message);
    page.on('pageerror',onError);
    await page.context().clearCookies();
    try {
        await page.goto(base);
        await page.locator('header').waitFor();
        assert(await page.locator('.homeScene').count() === 1,'home must show a Minecraft hero image');
        await page.waitForFunction(() => document.querySelector('.homeScene').naturalWidth > 0);
        assert(await page.locator('#postSearch, .boardList, header .back').count()===0,'home has no board list or redundant back button');
        for(const width of [320,390,768,1440]) {
            await page.setViewportSize({width,height:900});
            assert(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),'home fits viewport');
        }
        await page.getByRole('link',{name:'게시판 바로가기',exact:true}).click();
        await page.waitForURL('**/html/posts.html');
        await page.getByRole('heading',{name:'마인크래프트 게시판',exact:true}).waitFor();
        await page.locator('.boardItem').first().waitFor();
        await page.getByRole('searchbox',{name:'게시글 검색',exact:true}).fill('사진');
        await page.getByRole('button',{name:'검색',exact:true}).click();
        await page.getByRole('heading',{name:'마인크래프트 사진 첨부 예시',exact:true}).waitFor();
        await page.getByRole('heading',{name:'마인크래프트 사진 첨부 예시',exact:true}).click();
        await page.waitForURL('**/post/*');
        await page.getByRole('link',{name:'← 마인크래프트 게시판으로',exact:true}).click();
        await page.waitForURL('**/html/posts.html');
        await page.locator('header .back').click();
        await page.waitForURL('**/html/index.html');
        await page.getByRole('link',{name:'로그인하고 글쓰기',exact:true}).click();
        await page.waitForURL('**/html/login.html?next=*');
        assert(new URL(page.url()).searchParams.get('next')==='/html/board-write.html','home write action returns to editor after login');
        assert(errors.length===0,errors.join(', '));
        return {passed:['Minecraft image home','no redundant back button','responsive layout','board entry and search','detail returns to board','board returns home','login writing navigation']};
    } finally {
        page.off('pageerror',onError);
        await page.setViewportSize({width:1440,height:1000});
        await page.goto(base);
        await page.locator('header').waitFor();
        await page.screenshot({path:'/tmp/ktb-minecraft-home.png',fullPage:true});
    }
}
