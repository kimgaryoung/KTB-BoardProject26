import BoardItem from '../component/board/boardItem.js';
import { mountPublicHeader } from '../utils/pageHeader.js';
import { loginUrl } from '../utils/authNavigation.js';
import { getSiteBoard } from '../api/siteRequest.js';
import { getPosts, searchPosts } from '../api/indexRequest.js';
import { likePost, unlikePost } from '../api/boardRequest.js';

const boardList = document.querySelector('.boardList');
const status = document.querySelector('.postListStatus');
const more = document.querySelector('#loadMorePosts');
let communityId = null;
let cursor = null;
let offset = 0;
let keyword = '';
let loading = false;
let ended = false;
let version = 0;
const viewer = mountPublicHeader();

/*
 * 기능 : 첫 화면에서 마인크래프트 글 조회·검색
 * 설명 : 작품 선택 없이 전용 게시판만 조회합니다. 검색 조건이 바뀌면 이전 응답을 무시하고, 더 보기는 중복 요청을 막습니다.
 */
const loadPosts = async (reset = false) => {
    if (!reset && (loading || ended)) return;
    const current = ++version;
    loading = true;
    if (reset) { cursor = null; offset = 0; ended = false; boardList.replaceChildren(); }
    status.textContent = '게시글을 불러오는 중…';
    more.hidden = true;
    try {
        if (communityId === null) {
            const board = await getSiteBoard();
            if (current !== version) return;
            if (!board.ok || board.data?.id === undefined) throw new Error('board unavailable');
            communityId = board.data.id;
        }
        const response = keyword
            ? await searchPosts(keyword, offset, 5, 'recent', communityId)
            : await getPosts(cursor, communityId);
        if (current !== version) return;
        if (!response.ok) throw new Error('posts unavailable');
        const posts = response.data || [];
        boardList.insertAdjacentHTML('beforeend', posts.map(post => BoardItem(
            post.id, post.createdAt, post.title, post.viewCount, post.author?.profileImageUrl,
            post.author?.nickname, post.commentCount, post.likeCount,
            post.thumbnailUrl ?? post.postImageUrl,
        )).join(''));
        if (posts.length) cursor = posts.at(-1).id;
        offset += posts.length;
        ended = posts.length < 5;
        status.textContent = offset ? '' : keyword ? '검색 결과가 없어요. 다른 검색어로 찾아보세요.' : '아직 게시글이 없어요. 첫 마인크래프트 이야기를 남겨보세요!';
        more.hidden = ended;
    } catch {
        if (current !== version) return;
        status.textContent = '게시글을 불러오지 못했어요. 더 보기 버튼으로 다시 시도해주세요.';
        more.hidden = false;
    } finally { if (current === version) loading = false; }
};

boardList.addEventListener('error', event => {
    if (event.target.matches('.postThumbnail')) event.target.hidden = true;
}, true);

/*
 * 기능 : 목록에서 좋아요 등록·취소
 * 설명 : 서버가 반환한 최신 개수와 상태를 표시하며 비로그인은 로그인 화면으로 안내합니다.
 */
boardList.addEventListener('click', async event => {
    const button = event.target.closest('.likeButton');
    if (!button) return;
    event.preventDefault();
    if (button.disabled) return;
    button.disabled = true;
    const liked = button.dataset.liked === 'true';
    try {
        const result = await (liked ? unlikePost(button.dataset.postId) : likePost(button.dataset.postId));
        if (result.status === 401) { window.location.assign(loginUrl('/html/posts.html')); return; }
        if (result.ok || result.status === 409) {
            const next = result.data?.isLiked ?? !liked;
            button.dataset.liked = String(next);
            button.classList.toggle('is-active', next);
            button.setAttribute('aria-pressed', String(next));
            if (result.data?.likeCount !== undefined) button.querySelector('b').textContent = result.data.likeCount;
        }
    } catch { status.textContent = '좋아요를 처리하지 못했어요. 다시 시도해주세요.'; }
    finally { button.disabled = false; }
});

/*
 * 기능 : 글쓰기·검색·더 보기 초기화
 * 설명 : 로그인하면 바로 작성 화면으로, 비로그인이면 로그인 후 작성 화면으로 이동합니다. 새 글의 게시판은 서버가 마인크래프트로 지정합니다.
 */
const init = async () => {
    viewer.then(user => {
        const link = document.querySelector('#writeLink');
        const editor = '/html/board-write.html';
        link.href = user ? editor : loginUrl(editor);
        link.textContent = user ? '글 작성' : '로그인 후 글 작성';
        link.hidden = false;
    });
    document.querySelector('#postSearch').addEventListener('submit', event => {
        event.preventDefault();
        keyword = document.querySelector('#searchInput').value.trim();
        loadPosts(true);
    });
    more.addEventListener('click', () => loadPosts());
    await loadPosts(true);
};
init();
