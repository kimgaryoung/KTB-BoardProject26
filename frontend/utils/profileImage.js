import { fileUpload } from '../api/signupRequest.js';

/*
 * 기능 : 프로필 사진 검사 및 업로드
 * 설명 : 가입·수정 화면에서 빈 파일과 5MB 초과 파일을 안내하고, 서버가 검증·저장한 사진 주소만 반환합니다.
 */
export const uploadProfileImage = async file => {
    if (!file || file.size === 0) throw new Error('비어 있지 않은 사진을 선택해주세요.');
    if (file.size > 5 * 1024 * 1024) throw new Error('프로필 사진은 5MB 이하로 선택해주세요.');
    const body = new FormData();
    body.append('profileImage', file);
    const result = await fileUpload(body);
    if (!result.ok || !result.data?.profileImageUrl) {
        if (result.status === 413) throw new Error('프로필 사진은 5MB 이하로 선택해주세요.');
        if (result.status === 400) throw new Error('PNG, JPG, GIF, WebP, BMP 사진을 선택해주세요.');
        throw new Error('사진 업로드에 실패했습니다. 다시 선택해주세요.');
    }
    return result.data.profileImageUrl;
};
