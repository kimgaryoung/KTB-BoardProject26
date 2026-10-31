import { getServerUrl } from '../utils/function.js';
import { requestJson } from '../utils/request.js';

/*
 * 기능 : 마인크래프트 전용 게시판 정보 조회
 * 설명 : 서버가 관리하는 유일한 게시판 ID를 받아 기존 마크 글을 재사용합니다. 사용자에게 작품이나 게시판 선택을 요구하지 않습니다.
 */
export const getSiteBoard = () => requestJson(`${getServerUrl()}/v1/site/board`, { credentials: 'include' });
