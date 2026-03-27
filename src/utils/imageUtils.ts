/**
 * 이미지 URL 유틸리티
 *
 * 백엔드에서 반환하는 이미지 경로(/api/images/uuid.jpg)를
 * 브라우저에서 접근 가능한 전체 URL로 변환합니다.
 *
 * 이 함수 하나로 관리하면, 서버 주소가 바뀌어도 여기만 수정하면 됩니다.
 */

// 백엔드 기본 URL
// Vite 환경변수 VITE_API_BASE_URL이 설정된 경우 우선 사용, 없으면 localhost:8080
const getApiBaseUrl = (): string => {
  // Vite는 import.meta.env를 통해 환경변수에 접근합니다
  // .env 파일에 VITE_API_BASE_URL=https://your-server.com 형태로 설정 가능
  try {
    const env = (import.meta as any).env;
    if (env?.VITE_API_BASE_URL) return env.VITE_API_BASE_URL;
  } catch (e) {
    // 환경변수를 읽을 수 없으면 기본값 사용
  }
  return 'http://localhost:8080';
};

const API_BASE_URL = getApiBaseUrl();

/** 백엔드 서버 루트 URL (SSE, 직접 URL 등에 사용) */
export const BACKEND_URL = API_BASE_URL;

/**
 * 백엔드 이미지 경로를 완전한 URL로 변환합니다.
 * - '/api/images/uuid.jpg' → 'http://localhost:8080/api/images/uuid.jpg'
 * - 이미 완전한 URL이면 그대로 반환
 * - null/undefined/빈 문자열이면 null 반환
 */
export function resolveImageUrl(imagePath: string | null | undefined): string | null {
  if (!imagePath) return null;
  
  // 이미 완전한 URL인 경우 (http:// 또는 https://)
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath;
  }
  
  // /로 시작하는 상대 경로 → 백엔드 절대 URL로 변환
  if (imagePath.startsWith('/')) {
    return `${API_BASE_URL}${imagePath}`;
  }
  
  // 기타 경우 (예외 처리)
  return imagePath;
}

/**
 * 이미지 경로 배열을 완전한 URL 배열로 변환합니다.
 * null이나 빈 문자열은 필터링합니다.
 */
export function resolveImageUrls(imagePaths: (string | null | undefined)[]): string[] {
  if (!imagePaths || imagePaths.length === 0) return [];
  return imagePaths
    .map(resolveImageUrl)
    .filter((url): url is string => !!url);
}
