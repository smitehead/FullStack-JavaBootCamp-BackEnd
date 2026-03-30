import React from 'react';

/**
 * 마이페이지 세부 기능 관련 Stub(뻐대) 컴포넌트 목록
 * (26_03_30_진행상황.md 기반 추가 사항)
 */

export const ProfileEditStub: React.FC = () => {
    return (
        <div>
            <h2>회원정보 수정 (진행 예정)</h2>
            {/* TODO: 닉네임, 휴대폰번호, 이메일, 주소 수정 폼 구현 파악 */}
        </div>
    );
};

export const MembershipWithdrawalStub: React.FC = () => {
    return (
        <div>
            <h2>회원 탈퇴 (진행 예정)</h2>
            {/* TODO: 회원 탈퇴 로직 파악 및 확인 모달 구현 */}
        </div>
    );
};

export const ReviewManagementStub: React.FC = () => {
    return (
        <div>
            <h2>리뷰 및 관리 (진행 예정)</h2>
            {/* TODO: 내가 작성한 리뷰, 나에게 달린 리뷰 목록 표시 */}
        </div>
    );
};

export const NoticePageStub: React.FC = () => {
    return (
        <div>
            <h2>공지사항 (진행 예정)</h2>
            {/* TODO: 시스템 공지사항 리스트 노출 구현 */}
        </div>
    );
};
