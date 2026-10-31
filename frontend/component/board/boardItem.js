import { padTo2Digits, resolveImageUrl } from '../../utils/function.js';

/*
 * 기능 : 게시글 카드 텍스트 이스케이프
 * 설명 : HTML 문자열에 넣는 제목·닉네임 등의 특수문자를 바꿔 태그로 해석되지 않도록 합니다.
 */
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
})[character]);

/*
 * 기능 : 게시글 목록 카드 생성
 * 설명 : 제목·작성자·통계와 선택적인 사진 썸네일을 표시하는 카드 HTML을 반환합니다.
 */
const BoardItem = (
    postId,
    date,
    title,
    viewCount,
    imgUrl,
    writer,
    commentCount,
    likeCount,
    thumbnailUrl = null,
) => {
    // 파라미터 값이 없으면 리턴
    if (
        !date ||
        !title ||
        viewCount === undefined ||
        likeCount === undefined ||
        commentCount === undefined ||
        !writer
    ) {
        return;
    }

    // 날짜 포맷 변경 YYYY-MM-DD hh:mm:ss
    const dateObj = new Date(date);
    const year = dateObj.getFullYear();
    const month = dateObj.getMonth() + 1;
    const day = dateObj.getDate();
    const hours = dateObj.getHours();
    const minutes = dateObj.getMinutes();
    const seconds = dateObj.getSeconds();

    const formattedDate = `${year}-${padTo2Digits(month)}-${padTo2Digits(day)} ${padTo2Digits(hours)}:${padTo2Digits(minutes)}:${padTo2Digits(seconds)}`;

    const DEFAULT_PROFILE_IMAGE = '../public/image/profile/default.jpg';
    const profileImageUrl = resolveImageUrl(imgUrl, DEFAULT_PROFILE_IMAGE);
    const thumbnail = thumbnailUrl ? `<img class="postThumbnail" src="${escapeHtml(resolveImageUrl(thumbnailUrl))}" alt="${escapeHtml(title)} 사진" width="120" height="90" loading="lazy" decoding="async">` : '';
    return `
    <a href="/post/${encodeURIComponent(postId)}">
        <div class="boardItem">
            <div class="postCardMain">
                <h2 class="title">${escapeHtml(title)}</h2>
                ${thumbnail}
            </div>
            <div class="info">
                <button
                    class="views likeButton"
                    type="button"
                    data-post-id="${escapeHtml(postId)}"
                    data-liked="false"
                    aria-pressed="false"
                >좋아요 <b>${escapeHtml(likeCount)}</b></button>
                <h3 class="views">댓글 <b>${escapeHtml(commentCount)}</b></h3>
                <h3 class="views">조회수 <b>${escapeHtml(viewCount)}</b></h3>
                <p class="date">${formattedDate}</p>
            </div>
            <div class="writerInfo">
            <picture class="img">
                <img src="${escapeHtml(profileImageUrl)}" alt="작성자 프로필">
            </picture>
            <h2 class="writer">${escapeHtml(writer)}</h2>
        </div>
        </div>
    </a>
`;
};

export default BoardItem;
