import { searchCommunities, createCommunity, hideCommunity, showCommunity } from '../../api/communityRequest.js';

let componentId = 0;
/*
 * 기능 : 커뮤니티 이동 주소 생성
 * 설명 : 커뮤니티 ID를 URL에 안전하게 넣어 게시판 진입 주소를 만듭니다.
 */
export const communityUrl = id => `/community/${encodeURIComponent(id)}`;

// Community names and descriptions always enter the DOM as text.
/*
 * 기능 : 공통 커뮤니티 탐색 화면 생성
 * 설명 : 여러 화면에서 검색·직접 만들기·입장·개인 숨김·복구를 이용할 수 있도록 UI와 이벤트를 연결합니다.
 */
export const mountCommunitySearch = (container, { expanded = false } = {}) => {
    if (!container) return;
    const id = `community-search-${++componentId}`;
    container.classList.add('communityExplorer');
    container.innerHTML = `
        <form class="communitySearchForm" role="search">
            <label for="${id}">커뮤니티 검색</label>
            <div class="communitySearchRow">
                <input id="${id}" type="search" maxlength="50" placeholder="주술회전, 하이큐… 좋아하는 애니를 검색하세요" autocomplete="off">
                <button type="submit">검색</button>
            </div>
        </form>
        <div class="communityVisibilityTools">
            <button class="communityHiddenToggle secondaryButton" type="button" aria-pressed="false">숨긴 커뮤니티 보기</button>
            <span>숨김은 내 계정에만 적용돼요.</span>
        </div>
        <p class="communityVisibilityStatus" role="status" aria-live="polite"></p>
        <div class="communitySearchResults" ${expanded ? '' : 'hidden'}>
            <p class="communitySearchStatus" role="status" aria-live="polite"></p>
            <div class="communityResultsList"></div>
            <button class="communityMore secondaryButton" type="button" hidden>더 보기</button>
        </div>
        <button class="communityCreateToggle secondaryButton" type="button" aria-expanded="false" aria-controls="${id}-create">+ 커뮤니티 직접 만들기</button>
        <form id="${id}-create" class="communityCreateForm" hidden>
            <label for="${id}-name">커뮤니티 이름</label>
            <input id="${id}-name" name="name" maxlength="50" required placeholder="예: 주술회전">
            <label for="${id}-description">소개 <span>(선택)</span></label>
            <textarea id="${id}-description" name="description" maxlength="300" rows="2" placeholder="어떤 이야기를 나누는 공간인가요?"></textarea>
            <button type="submit">만들고 입장하기</button>
            <p class="communityCreateStatus" role="status" aria-live="polite"></p>
        </form>`;
    const searchForm = container.querySelector('.communitySearchForm');
    const input = searchForm.querySelector('input');
    const results = container.querySelector('.communitySearchResults');
    const status = container.querySelector('.communitySearchStatus');
    const list = container.querySelector('.communityResultsList');
    const more = container.querySelector('.communityMore');
    const toggle = container.querySelector('.communityCreateToggle');
    const form = container.querySelector('.communityCreateForm');
    const createStatus = container.querySelector('.communityCreateStatus');
    const hiddenToggle = container.querySelector('.communityHiddenToggle');
    const visibilityStatus = container.querySelector('.communityVisibilityStatus');
    let showingHidden = false;
    let version = 0;
    let offset = 0;
    let keyword = '';
    let timer;
    let loading = false;
    const pageSize = 20;

    /*
     * 기능 : 커뮤니티 검색 결과와 숨김 버튼 표시
     * 설명 : 이름과 소개는 텍스트로 삽입합니다. 숨김 변경 후 목록을 갱신하고 필요하면 키보드 초점을 복구합니다.
     */
    const render = community => {
        const row = document.createElement('div');
        row.className = 'communityResultRow';
        row.dataset.communityId = community.id;
        const link = document.createElement('a');
        link.className = 'communityResult';
        link.href = communityUrl(community.id);
        const name = document.createElement('strong');
        name.textContent = community.name;
        const description = document.createElement('span');
        description.textContent = community.description || '이 커뮤니티에서 첫 이야기를 시작해보세요.';
        link.append(name, description);
        const action = document.createElement('button');
        action.type = 'button';
        action.className = 'communityVisibilityAction secondaryButton';
        const restore = showingHidden;
        action.textContent = restore ? '다시 표시' : '숨기기';
        action.setAttribute('aria-label', `${community.name} ${action.textContent}`);
        action.addEventListener('click', async () => {
            if (action.disabled) return;
            const hadFocus = document.activeElement === action;
            action.disabled = true;
            try {
                const response = await (restore ? showCommunity(community.id) : hideCommunity(community.id));
                if (response.status === 401) { window.location.assign('/html/login.html'); return; }
                if (!response.ok) throw new Error('visibility failed');
                await search();
                visibilityStatus.textContent = restore ? `${community.name} 커뮤니티를 다시 표시합니다.` : `${community.name} 커뮤니티를 숨겼어요. ‘숨긴 커뮤니티 보기’에서 복구할 수 있어요.`;
                if (hadFocus && document.activeElement === document.body) hiddenToggle.focus();
            } catch {
                visibilityStatus.textContent = '숨김 설정을 변경하지 못했어요. 다시 시도해주세요.';
            } finally { action.disabled = false; }
        });
        row.append(link, action);
        return row;
    };
    /*
     * 기능 : 커뮤니티 검색 결과 갱신 및 더 보기
     * 설명 : 일반·숨김 모드별로 페이지를 조회합니다. 요청 버전이 지난 응답은 무시해 최신 검색 결과를 유지합니다.
     */
    const search = async (append = false) => {
        const current = ++version;
        if (!append) { offset = 0; keyword = input.value.trim(); list.replaceChildren(); }
        loading = true;
        results.hidden = false;
        more.hidden = true;
        status.textContent = '커뮤니티를 찾고 있어요…';
        try {
            const response = await searchCommunities(keyword, offset, pageSize, showingHidden);
            if (current !== version) return;
            if (response.status === 401 && showingHidden) {
                status.textContent = '로그인하면 숨긴 커뮤니티를 확인하고 복구할 수 있어요.';
                return;
            }
            if (!response.ok) throw new Error('search failed');
            const communities = response.data || [];
            communities.forEach(community => list.appendChild(render(community)));
            offset += communities.length;
            status.textContent = showingHidden
                ? (offset ? '내가 숨긴 커뮤니티입니다. ‘다시 표시’로 복구할 수 있어요.' : '숨긴 커뮤니티가 없거나 검색 결과가 없어요.')
                : offset ? (keyword ? '검색한 커뮤니티를 선택해 입장하세요.' : '관심 있는 커뮤니티에 입장해보세요.')
                : '아직 커뮤니티가 없어요. 직접 만들거나 숨긴 커뮤니티를 확인해보세요.';
            more.hidden = communities.length < pageSize;
        } catch {
            if (current === version) status.textContent = '커뮤니티를 불러오지 못했어요. 검색 버튼으로 다시 시도해주세요.';
        } finally { if (current === version) loading = false; }
    };
    searchForm.addEventListener('submit', event => { event.preventDefault(); clearTimeout(timer); search(); });
    /*
     * 기능 : 검색 입력 지연 처리
     * 설명 : 입력이 바뀌면 이전 응답을 무효화하고 250ms 뒤 검색해 연속 입력 중 요청 수를 줄입니다.
     */
    input.addEventListener('input', () => {
        ++version;
        more.hidden = true;
        clearTimeout(timer);
        timer = setTimeout(() => search(), 250);
    });
    more.addEventListener('click', () => { if (!loading) search(true); });
    /*
     * 기능 : 일반 목록과 숨긴 목록 전환
     * 설명 : 개인 숨김 모드를 바꾸고 현재 검색어로 목록을 다시 조회합니다.
     */
    hiddenToggle.addEventListener('click', () => {
        showingHidden = !showingHidden;
        hiddenToggle.textContent = showingHidden ? '전체 커뮤니티 보기' : '숨긴 커뮤니티 보기';
        hiddenToggle.setAttribute('aria-pressed', String(showingHidden));
        visibilityStatus.textContent = '';
        clearTimeout(timer);
        search();
    });
    toggle.addEventListener('click', () => {
        form.hidden = !form.hidden;
        toggle.setAttribute('aria-expanded', String(!form.hidden));
        if (!form.hidden) { form.elements.name.value = input.value.trim(); form.elements.name.focus(); }
    });
    /*
     * 기능 : 커뮤니티 직접 생성 및 입장
     * 설명 : 생성 성공 시 새 커뮤니티로 이동하고, 중복 이름이면 기존 커뮤니티의 입장 링크를 표시합니다.
     */
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const button = form.querySelector('button[type="submit"]');
        if (button.disabled) return;
        const name = form.elements.name.value.trim();
        if (!name) { createStatus.textContent = '커뮤니티 이름을 입력해주세요.'; return; }
        button.disabled = true;
        createStatus.textContent = '커뮤니티를 만들고 있어요…';
        try {
            const response = await createCommunity({ name, description: form.elements.description.value.trim() });
            if (response.ok) window.location.assign(communityUrl(response.data.id));
            else if (response.status === 401) window.location.assign('/html/login.html');
            else if (response.status === 409 && response.body?.communityId !== undefined) {
                const link = document.createElement('a');
                link.href = communityUrl(response.body.communityId);
                link.textContent = '이미 있는 커뮤니티로 입장하기 →';
                createStatus.replaceChildren(document.createTextNode('같은 이름의 커뮤니티가 있어요. '), link);
            } else createStatus.textContent = response.status === 400 ? '이름은 1~50자, 소개는 300자 이하로 입력해주세요.' : '만들지 못했어요. 잠시 후 다시 시도해주세요.';
        } catch { createStatus.textContent = '서버에 연결하지 못했어요. 다시 시도해주세요.'; }
        finally { button.disabled = false; }
    });
    if (expanded) return search();
};
