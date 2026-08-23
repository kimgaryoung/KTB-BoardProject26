import BoardItem from '../component/board/boardItem.js';
import Dialog from '../component/dialog/dialog.js';
import Header from '../component/header/header.js';
import { authCheck, getServerUrl, prependChild, resolveImageUrl } from '../utils/function.js';
import { getPosts, searchPosts } from '../api/indexRequest.js';
import { likePost, unlikePost } from '../api/boardRequest.js';

const DEFAULT_PROFILE_IMAGE = '../public/image/profile/default.jpg';
const HTTP_NOT_AUTHORIZED = 401;
const SCROLL_THRESHOLD = 0.9;
const ITEMS_PER_LOAD = 5;
const DEFAULT_SORT = 'recent';
let currentKeyword = '';
let currentSort = DEFAULT_SORT;
let cursor = null;
let offset = 0;
let isEnd = false;
let isProcessing = false;

const updateSortVisibility = () => {
    const sortRow = document.querySelector('#searchSortRow');
    if (!sortRow) return;
    const isSearching = currentKeyword.trim().length > 0;
    sortRow.classList.toggle('isHidden', !isSearching);
    sortRow.setAttribute('aria-hidden', String(!isSearching));
};

// getBoardItem 함수
const getBoardItem = async () => {
    const result =
        currentKeyword.trim() === ''
            ? await getPosts(cursor)
            : await searchPosts(
                  currentKeyword,
                  offset,
                  ITEMS_PER_LOAD,
                  currentSort,
              );
    if (!result.ok) {
        throw new Error('Failed to load post list.');
    }
    return result.data;
};

const setBoardItem = boardData => {
    const boardList = document.querySelector('.boardList');
    if (boardList && boardData) {
        const itemsHtml = boardData
            .map(data =>
                BoardItem(
                    data.id,
                    data.createdAt,
                    data.title,
                    data.viewCount,
                    data.author ? data.author.profileImageUrl : null,
                    data.author ? data.author.nickname : null,
                    data.commentCount,
                    data.likeCount,
                ),
            )
            .join('');
        boardList.innerHTML += ` ${itemsHtml}`;
    }
};

const resetBoardList = () => {
    const boardList = document.querySelector('.boardList');
    if (boardList) {
        boardList.innerHTML = '';
    }
};

const loadBoardItems = async ({ reset = false } = {}) => {
    if (isProcessing || (!reset && isEnd)) return;
    isProcessing = true;

    try {
        if (reset) {
            cursor = null;
            offset = 0;
            isEnd = false;
            resetBoardList();
        }
        const items = await getBoardItem();
        if (!items || items.length === 0) {
            isEnd = true;
            return;
        }
        setBoardItem(items);
        if (currentKeyword.trim() === '') {
            cursor = items[items.length - 1].id;
        } else {
            offset += ITEMS_PER_LOAD;
        }
    } catch (error) {
        console.error('Error fetching items:', error);
        isEnd = true;
    } finally {
        isProcessing = false;
    }
};

const setItemLikeState = (button, isLiked) => {
    button.dataset.liked = String(isLiked);
    button.classList.toggle('is-active', isLiked);
    button.setAttribute('aria-pressed', String(isLiked));
};

const addLikeEvent = () => {
    const boardList = document.querySelector('.boardList');
    if (!boardList) return;

    boardList.addEventListener('click', async event => {
        const button = event.target.closest('.likeButton');
        if (!button) return;
        event.preventDefault();
        event.stopPropagation();

        if (button.dataset.loading === 'true') return;
        button.dataset.loading = 'true';

        const postId = button.dataset.postId;
        const isLiked = button.dataset.liked === 'true';
        const countElement = button.querySelector('b');

        try {
            const { ok, status, code, data } = isLiked
                ? await unlikePost(postId)
                : await likePost(postId);

            if (ok) {
                setItemLikeState(button, !isLiked);
                if (data && data.likeCount !== undefined) {
                    countElement.textContent = data.likeCount;
                }
            } else if (status === HTTP_NOT_AUTHORIZED) {
                window.location.href = '/html/login.html';
            } else if (
                (!isLiked && status === 409 && code === 'POST_ALREADY_LIKED') ||
                (isLiked && status === 409 && code === 'POST_ALREADY_UNLIKED')
            ) {
                setItemLikeState(button, !isLiked);
            }
        } finally {
            button.dataset.loading = 'false';
        }
    });
};

const addSearchEvent = () => {
    const searchInput = document.querySelector('#searchInput');
    const searchButton = document.querySelector('.searchButton');
    if (!searchInput || !searchButton) return;

    const runSearch = async () => {
        const trimmedKeyword = searchInput.value.trim();
        if (trimmedKeyword.length > 0 && trimmedKeyword.length < 2) {
            Dialog('검색 실패', '검색어는 2글자 이상 입력해주세요.');
            return;
        }
        currentKeyword = trimmedKeyword;
        updateSortVisibility();
        await loadBoardItems({ reset: true });
    };

    searchButton.addEventListener('click', runSearch);
    searchInput.addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            runSearch();
        }
    });
};

const addSortEvent = () => {
    const sortSelect = document.querySelector('#searchSortSelect');
    if (!sortSelect) return;
    sortSelect.value = currentSort;

    sortSelect.addEventListener('change', async () => {
        currentSort = sortSelect.value || DEFAULT_SORT;
        if (currentKeyword.trim().length === 0) return;
        await loadBoardItems({ reset: true });
    });
};

// 스크롤 이벤트 추가
const addInfinityScrollEvent = () => {
    window.addEventListener('scroll', async () => {
        const hasScrolledToThreshold =
            window.scrollY + window.innerHeight >=
            document.documentElement.scrollHeight * SCROLL_THRESHOLD;
        if (hasScrolledToThreshold) {
            loadBoardItems();
        }
    });
};

const init = async () => {
    try {
        const response = await authCheck();
        if (!response) return;
        const data = await response.json();
        if (response.status === HTTP_NOT_AUTHORIZED) {
            window.location.href = '/html/login.html';
            return;
        }

        const profileImageUrl = resolveImageUrl(
            data.data.profileImageUrl,
            DEFAULT_PROFILE_IMAGE,
        );

        prependChild(
            document.body,
            Header('Community', 0, profileImageUrl),
        );

        updateSortVisibility();
        await loadBoardItems({ reset: true });

        addSearchEvent();
        addSortEvent();
        addInfinityScrollEvent();
        addLikeEvent();
    } catch (error) {
        console.error('Initialization failed:', error);
        window.location.href = '/html/login.html';
    }
};

init();
