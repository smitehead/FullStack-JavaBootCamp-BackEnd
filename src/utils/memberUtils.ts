import { User } from '@/types';

/**
 * User 객체에서 백엔드 memberNo를 안전하게 추출합니다.
 * 파싱 실패 시 null을 반환하여 타인 계정 접근을 방지합니다.
 */
export function getMemberNo(user: User | null | undefined): number | null {
  if (!user?.id) return null;
  const numStr = user.id.replace(/[^0-9]/g, '');
  if (!numStr) return null;
  const num = parseInt(numStr, 10);
  return isNaN(num) ? null : num;
}
