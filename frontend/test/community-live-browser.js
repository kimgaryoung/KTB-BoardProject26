async (page) => {
    const base = 'http://localhost:8080';
    await page.setExtraHTTPHeaders({});
    await page.unroute('**/server-api/v1/**');
    page.setDefaultTimeout(7000);
    const suffix = Date.now().toString().slice(-7);
    const firstName = `주술회전 ${suffix}`;
    const secondName = `하이큐 ${suffix}`;
    const assert = (condition, message) => { if (!condition) throw new Error(message); };
    const account = { email: `browser-${suffix}@example.test`, password: 'TestPass1!', nickname: `팬${suffix}` };
    const signup = await page.request.post(`${base}/server-api/v1/auth/signup`, { data: account });
    assert(signup.status() === 201, `signup: ${signup.status()}`);
    const login = await page.request.post(`${base}/server-api/v1/auth/login`, { data: account });
    assert(login.ok(), `login: ${login.status()}`);

    await page.goto(base);
    const search = () => page.getByRole('searchbox', { name: '커뮤니티 검색', exact: true });
    const find = async name => {
        await search().fill(name);
        await page.getByRole('button', { name: '검색', exact: true }).click();
    };
    const create = async name => {
        await page.getByRole('button', { name: '+ 커뮤니티 직접 만들기', exact: true }).click();
        await page.getByRole('textbox', { name: '커뮤니티 이름', exact: true }).fill(name);
        await page.getByRole('button', { name: '만들고 입장하기', exact: true }).click();
        await page.getByRole('heading', { name, exact: true }).waitFor();
        return page.url().split('/').pop();
    };
    await find(firstName);
    await page.getByText('아직 커뮤니티가 없어요.', { exact: false }).waitFor();
    const firstId = await create(firstName);
    await page.getByRole('link', { name: '글 작성', exact: true }).click();
    await page.locator('#title').fill('첫 커뮤니티의 이야기');
    await page.locator('#content').fill('첫 번째 커뮤니티에만 보여야 하는 내용');
    await page.getByRole('button', { name: '완료', exact: true }).click();
    await page.waitForURL('**/post/*');
    await page.locator('.title').filter({ hasText: '첫 커뮤니티의 이야기' }).waitFor();
    const firstPostId = page.url().split('/').pop();
    assert(await page.locator('#communityLink').getAttribute('href') === `/community/${firstId}`, 'post must keep community context');

    await page.getByRole('button', { name: '수정', exact: true }).click();
    await page.locator('#title').fill('수정한 첫 커뮤니티 이야기');
    await page.getByRole('button', { name: '수정하기', exact: true }).click();
    await page.waitForURL('**/post/*');
    await page.getByRole('heading', { name: '수정한 첫 커뮤니티 이야기', exact: true }).waitFor();
    const secondId = await create(secondName);
    assert(firstId !== secondId, 'creation must create independent communities');
    await page.getByRole('link', { name: '글 작성', exact: true }).click();
    await page.locator('#title').fill('둘째 커뮤니티의 이야기');
    await page.locator('#content').fill('둘째 커뮤니티에만 보여야 합니다');
    await page.getByRole('button', { name: '완료', exact: true }).click();
    await page.waitForURL('**/post/*');
    await page.getByRole('heading', { name: '둘째 커뮤니티의 이야기', exact: true }).waitFor();
    await find(firstName);
    await page.getByRole('link', { name: new RegExp(firstName) }).click();
    await page.getByRole('heading', { name: firstName, exact: true }).waitFor();
    await page.getByRole('heading', { name: '수정한 첫 커뮤니티 이야기', exact: true }).waitFor();
    assert(await page.getByText('둘째 커뮤니티의 이야기', { exact: true }).count() === 0, 'other community posts must be excluded');
    await page.getByRole('searchbox', { name: '이 커뮤니티의 게시글 검색', exact: true }).fill('둘째');
    await page.getByRole('button', { name: '글 검색', exact: true }).click();
    await page.getByText('검색 결과가 없어요.', { exact: false }).waitFor();

    await page.context().clearCookies();
    await page.goto(`${base}/post/${firstPostId}`);
    await page.getByRole('heading', { name: '수정한 첫 커뮤니티 이야기', exact: true }).waitFor({ timeout: 5000 });
    assert(page.url().includes(`/post/${firstPostId}`), 'guest post browsing must stay on the post');
    await find(secondName);
    await page.getByRole('link', { name: new RegExp(secondName) }).click();
    await page.getByRole('heading', { name: secondName, exact: true }).waitFor();
    return { firstId, secondId, firstPostId, passed: ['real signup/login', 'create communities', 'multipart post', 'edit post', 'post isolation', 'scoped search', 'guest detail', 'guest community switching'] };
}
